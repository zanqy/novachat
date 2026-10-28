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
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Gemini generateContent API.
 * POST {baseUrl}/v1beta/models/{model}:streamGenerateContent?alt=sse&key={apiKey}
 * USER -> "user", ASSISTANT -> "model". No [DONE] sentinel — the server
 * simply closes the stream.
 */
class GeminiProvider(
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
        onEvent: suspend (ChatEvent) -> Unit
    ) {
        if (profile.apiKey.isBlank()) {
            onEvent(ChatEvent.Failed("API key is missing — add it in Settings."))
            return
        }
        if (modelId.isBlank()) {
            onEvent(ChatEvent.Failed("Model ID is empty — pick a model first."))
            return
        }
        val base = profile.baseUrl.ifBlank { ModelCatalog.defaultBaseUrl(profile.kind) }
        val body = buildJsonObject {
            putJsonArray("contents") {
                for ((role, text) in normalize(messages)) {
                    addJsonObject {
                        put("role", role)
                        putJsonArray("parts") {
                            addJsonObject { put("text", text) }
                        }
                    }
                }
            }
            if (!systemPrompt.isNullOrBlank()) {
                putJsonObject("systemInstruction") {
                    putJsonArray("parts") {
                        addJsonObject { put("text", systemPrompt) }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", temperature.coerceIn(0f, 2f))
                if (maxTokens != null && maxTokens > 0) put("maxOutputTokens", maxTokens)
            }
        }
        val request = Request.Builder()
            .url(joinUrl(base, "/v1beta/models/$modelId:streamGenerateContent?alt=sse&key=${profile.apiKey}"))
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Accept", "text/event-stream")
            .build()
        try {
            var usageIn: Int? = null
            var usageOut: Int? = null
            sse.events(request).collect { ev ->
                val data = ev.data.trim()
                if (data.isEmpty()) return@collect
                val root = runCatching {
                    NovaJson.parseToJsonElement(data).jsonObject
                }.getOrNull() ?: return@collect
                root["error"]?.jsonObject?.let { err ->
                    val msg = err["message"]?.jsonPrimitive?.contentOrNull
                        ?: data.take(300)
                    onEvent(ChatEvent.Failed("Provider error: $msg"))
                    return@collect
                }
                val parts = root["candidates"]?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("content")?.jsonObject
                    ?.get("parts")?.jsonArray
                val chunk = parts.orEmpty().mapNotNull { part ->
                    part.jsonObject["text"]?.jsonPrimitive?.contentOrNull
                }.joinToString("")
                if (chunk.isNotEmpty()) onEvent(ChatEvent.Delta(chunk))
                root["usageMetadata"]?.jsonObject?.let { meta ->
                    meta["promptTokenCount"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                        ?.let { usageIn = it }
                    meta["candidatesTokenCount"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
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
            if (profile.apiKey.isBlank()) error("API key is missing.")
            val base = profile.baseUrl.ifBlank { ModelCatalog.defaultBaseUrl(profile.kind) }
            val request = Request.Builder()
                .url(joinUrl(base, "/v1beta/models?key=${profile.apiKey}"))
                .get()
                .build()
            http.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("HTTP ${resp.code}: ${text.take(300)}")
                val root = NovaJson.parseToJsonElement(text).jsonObject
                root["models"]?.jsonArray.orEmpty().mapNotNull { el ->
                    val name = el.jsonObject["name"]?.jsonPrimitive?.contentOrNull
                        ?: return@mapNotNull null
                    ModelInfo(name.removePrefix("models/"))
                }
            }
        }

    private fun normalize(messages: List<ChatMessage>): List<Pair<String, String>> {
        val mapped = messages
            .filter { it.content.isNotBlank() }
            .map { (if (it.role == Role.USER) "user" else "model") to it.content }
        return mapped.dropWhile { it.first != "user" }
    }
}

