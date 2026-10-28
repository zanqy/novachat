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
 * Anthropic Messages API. POST {baseUrl}/v1/messages with the system prompt
 * as a TOP-LEVEL param, stream=true, and event-typed SSE frames
 * (message_start / content_block_delta / message_delta / message_stop).
 */
class AnthropicProvider(
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
        if (profile.apiKey.isBlank()) {
            onEvent(ChatEvent.Failed("API key is missing — add it in Settings."))
            return
        }
        if (modelId.isBlank()) {
            onEvent(ChatEvent.Failed("Model ID is empty — pick a model first."))
            return
        }
        val normalized = normalize(messages)
        if (normalized.isEmpty()) {
            onEvent(ChatEvent.Failed("No usable messages to send."))
            return
        }
        val body = buildJsonObject {
            put("model", modelId)
            put("max_tokens", maxTokens?.takeIf { it > 0 } ?: 4096)
            if (!systemPrompt.isNullOrBlank()) put("system", systemPrompt)
            putJsonArray("messages") {
                for ((role, content) in normalized) {
                    addJsonObject {
                        put("role", role)
                        put("content", content)
                    }
                }
            }
            put("stream", true)
            put("temperature", temperature.coerceIn(0f, 1f))
        }
        val request = Request.Builder()
            .url(joinUrl(profile.baseUrl.ifBlank { ModelCatalog.defaultBaseUrl(profile.kind) }, "/v1/messages"))
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("x-api-key", profile.apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Accept", "text/event-stream")
            .build()
        try {
            var usageIn: Int? = null
            var usageOut: Int? = null
            var failed: String? = null
            sse.events(request).collect { ev ->
                val data = ev.data.trim()
                if (data.isEmpty()) return@collect
                val root = runCatching {
                    NovaJson.parseToJsonElement(data).jsonObject
                }.getOrNull() ?: return@collect
                when (ev.type) {
                    "message_start" -> {
                        usageIn = root["message"]?.jsonObject
                            ?.get("usage")?.jsonObject
                            ?.get("input_tokens")?.jsonPrimitive
                            ?.contentOrNull?.toIntOrNull()
                    }
                    "content_block_delta" -> {
                        val text = root["delta"]?.jsonObject
                            ?.takeIf { it["type"]?.jsonPrimitive?.contentOrNull == "text_delta" }
                            ?.get("text")?.jsonPrimitive?.contentOrNull
                        if (!text.isNullOrEmpty()) onEvent(ChatEvent.Delta(text))
                    }
                    "message_delta" -> {
                        usageOut = root["usage"]?.jsonObject
                            ?.get("output_tokens")?.jsonPrimitive
                            ?.contentOrNull?.toIntOrNull()
                    }
                    "error" -> {
                        failed = root["error"]?.jsonObject
                            ?.get("message")?.jsonPrimitive?.contentOrNull
                            ?: data.take(300)
                        return@collect
                    }
                    else -> Unit
                }
            }
            if (failed != null) {
                onEvent(ChatEvent.Failed("Provider error: $failed"))
                return
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
            val request = Request.Builder()
                .url(joinUrl(profile.baseUrl.ifBlank { ModelCatalog.defaultBaseUrl(profile.kind) }, "/v1/models"))
                .get()
                .header("x-api-key", profile.apiKey)
                .header("anthropic-version", "2023-06-01")
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

    /**
     * Anthropic requires history to start with "user" and alternate roles.
     * Merge consecutive same-role messages and drop leading non-user turns.
     */
    internal fun normalize(messages: List<ChatMessage>): List<Pair<String, String>> {
        val merged = mutableListOf<Pair<String, String>>()
        for (m in messages) {
            if (m.content.isBlank()) continue
            val role = if (m.role == Role.USER) "user" else "assistant"
            val last = merged.lastOrNull()
            if (last != null && last.first == role) {
                merged[merged.lastIndex] = role to (last.second + "\n\n" + m.content)
            } else {
                merged.add(role to m.content)
            }
        }
        return merged.dropWhile { it.first != "user" }
    }
}
