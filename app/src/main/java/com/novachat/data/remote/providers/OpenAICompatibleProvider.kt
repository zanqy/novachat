package com.novachat.data.remote.providers

import com.novachat.data.remote.sse.SseClient
import com.novachat.data.remote.sse.SseFailureException
import com.novachat.domain.ChatProvider
import com.novachat.domain.model.ChatEvent
import com.novachat.domain.model.ChatMessage
import com.novachat.domain.model.ModelInfo
import com.novachat.domain.model.ProviderProfile
import com.novachat.domain.model.Role
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * OpenAI-compatible chat completions (OpenRouter, Groq, Together, Ollama,
 * LM Studio, vLLM, ...). POST {baseUrl}/chat/completions, SSE with
 * choices[0].delta.content chunks and a "data: [DONE]" terminator.
 */
class OpenAICompatibleProvider(
    private val sse: SseClient,
    private val http: OkHttpClient
) : ChatProvider {

    override suspend fun stream(
        profile: ProviderProfile,
        modelId: String,
        messages: List<ChatMessage>,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int?,
        onEvent: (ChatEvent) -> Unit
    ) {
        if (profile.baseUrl.isBlank()) {
            onEvent(ChatEvent.Failed("Base URL is empty — set it in Settings."))
            return
        }
        if (modelId.isBlank()) {
            onEvent(ChatEvent.Failed("Model ID is empty — pick a model first."))
            return
        }
        val body = buildJsonObject {
            put("model", modelId)
            putJsonArray("messages") {
                if (!systemPrompt.isNullOrBlank()) {
                    addJsonObject {
                        put("role", "system")
                        put("content", systemPrompt)
                    }
                }
                for (m in messages) {
                    if (m.content.isBlank()) continue
                    addJsonObject {
                        put(
                            "role",
                            if (m.role == Role.USER) "user" else "assistant"
                        )
                        put("content", m.content)
                    }
                }
            }
            put("stream", true)
            put("temperature", temperature.coerceIn(0f, 2f))
            if (maxTokens != null && maxTokens > 0) put("max_tokens", maxTokens)
        }
        val request = Request.Builder()
            .url(joinUrl(profile.baseUrl, "/chat/completions"))
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Accept", "text/event-stream")
            .apply {
                if (profile.apiKey.isNotBlank()) {
                    header("Authorization", "Bearer ${profile.apiKey}")
                }
            }
            .build()
        try {
            var usageIn: Int? = null
            var usageOut: Int? = null
            sse.events(request).collect { ev ->
                val data = ev.data.trim()
                if (data.isEmpty() || data == "[DONE]") return@collect
                val root = runCatching {
                    NovaJson.parseToJsonElement(data).jsonObject
                }.getOrNull() ?: return@collect
                root["error"]?.jsonObject?.let { err ->
                    val msg = err["message"]?.jsonPrimitive?.contentOrNull
                        ?: err.toString().take(300)
                    onEvent(ChatEvent.Failed("Provider error: $msg"))
                    return
                }
                val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                val delta = choice?.get("delta")?.jsonObject
                    ?.get("content")?.jsonPrimitive?.contentOrNull
                if (!delta.isNullOrEmpty()) onEvent(ChatEvent.Delta(delta))
                root["usage"]?.jsonObject?.let { usage ->
                    usage["prompt_tokens"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                        ?.let { usageIn = it }
                    usage["completion_tokens"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                        ?.let { usageOut = it }
                }
            }
            onEvent(ChatEvent.Done(usageIn, usageOut))
        } catch (e: CancellationException) {
            throw e
        } catch (e: SseFailureException) {
            onEvent(ChatEvent.Failed(httpErrorMessage(e), e))
        } catch (e: Exception) {
            onEvent(ChatEvent.Failed(e.message ?: "Stream failed", e))
        }
    }

    override suspend fun listModels(profile: ProviderProfile): List<ModelInfo> =
        withContext(Dispatchers.IO) {
            if (profile.baseUrl.isBlank()) error("Base URL is empty.")
            val request = Request.Builder()
                .url(joinUrl(profile.baseUrl, "/models"))
                .get()
                .apply {
                    if (profile.apiKey.isNotBlank()) {
                        header("Authorization", "Bearer ${profile.apiKey}")
                    }
                }
                .build()
            http.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("HTTP ${resp.code}: ${text.take(300)}")
                val root = NovaJson.parseToJsonElement(text).jsonObject
                root["data"]?.jsonArray.orEmpty().mapNotNull { el ->
                    el.jsonObject["id"]?.jsonPrimitive?.contentOrNull?.let { ModelInfo(it) }
                }
            }
        }
}

internal fun httpErrorMessage(e: SseFailureException): String {
    val detail = e.body?.take(300)?.trim().orEmpty()
    return if (e.code != null) {
        if (detail.isNotEmpty()) "HTTP ${e.code}: $detail" else "HTTP ${e.code}"
    } else {
        "Network error: ${e.cause?.message ?: e.message}"
    }
}
