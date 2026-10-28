package com.novachat.data.remote.providers

import com.novachat.domain.ChatProvider
import com.novachat.domain.model.ChatEvent
import com.novachat.domain.model.ChatMessage
import com.novachat.domain.model.ModelInfo
import com.novachat.domain.model.ProviderProfile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

/**
 * Built-in offline test provider. Streams a fixed markdown answer word by
 * word so the whole UI/streaming pipeline works without any API key.
 */
class EchoProvider : ChatProvider {

    override suspend fun stream(
        profile: ProviderProfile,
        modelId: String,
        messages: List<ChatMessage>,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int?,
        onEvent: suspend (ChatEvent) -> Unit
    ) {
        val lastUser = messages.lastOrNull { it.role.name == "USER" }?.content
        val answer = buildString {
            appendLine("# Echo reply")
            appendLine()
            appendLine("You said:")
            appendLine()
            appendLine("> ${(lastUser?.take(240) ?: "(nothing)").trim()}")
            appendLine()
            appendLine("This is a **streaming** test with *formatting*, `inline code`, and a list:")
            appendLine()
            appendLine("- first item")
            appendLine("- second item")
            appendLine()
            appendLine("```kotlin")
            appendLine("fun greet(name: String) {")
            appendLine("    println(\"Hello, \$name!\")")
            appendLine("}")
            appendLine("```")
        }
        val words = answer.split(" ")
        for (word in words) {
            currentCoroutineContext().ensureActive()
            onEvent(ChatEvent.Delta("$word "))
            delay(30)
        }
        onEvent(ChatEvent.Done(12, 34))
    }

    override suspend fun listModels(profile: ProviderProfile): List<ModelInfo> =
        listOf(ModelInfo("echo-1", "echo-1 (offline test)"))
}
