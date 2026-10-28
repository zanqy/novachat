package com.novachat.data.remote.providers

import com.novachat.domain.model.ProviderKind
import kotlinx.serialization.json.Json

val NovaJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

/** Joins a base URL and a path without producing double slashes. */
fun joinUrl(base: String, path: String): String {
    val b = base.trim().trimEnd('/')
    val p = if (path.startsWith("/")) path else "/$path"
    return b + p
}

object ModelCatalog {

    fun defaultBaseUrl(kind: ProviderKind): String = when (kind) {
        ProviderKind.OPENAI_COMPATIBLE -> "https://openrouter.ai/api/v1"
        ProviderKind.ANTHROPIC -> "https://api.anthropic.com"
        ProviderKind.GEMINI -> "https://generativelanguage.googleapis.com"
        ProviderKind.ECHO -> ""
    }

    fun defaultModelId(kind: ProviderKind): String = when (kind) {
        ProviderKind.OPENAI_COMPATIBLE -> "openai/gpt-4o-mini"
        ProviderKind.ANTHROPIC -> "claude-3-5-sonnet-20241022"
        ProviderKind.GEMINI -> "gemini-2.0-flash"
        ProviderKind.ECHO -> "echo-1"
    }

    fun kindLabel(kind: ProviderKind): String = when (kind) {
        ProviderKind.OPENAI_COMPATIBLE -> "OpenAI-compatible"
        ProviderKind.ANTHROPIC -> "Anthropic"
        ProviderKind.GEMINI -> "Gemini"
        ProviderKind.ECHO -> "Echo (offline)"
    }

    fun baseUrlHint(kind: ProviderKind): String = when (kind) {
        ProviderKind.OPENAI_COMPATIBLE -> "https://openrouter.ai/api/v1 (Ollama: http://<host>:11434/v1)"
        ProviderKind.ANTHROPIC -> "https://api.anthropic.com"
        ProviderKind.GEMINI -> "https://generativelanguage.googleapis.com"
        ProviderKind.ECHO -> "No server needed"
    }
}
