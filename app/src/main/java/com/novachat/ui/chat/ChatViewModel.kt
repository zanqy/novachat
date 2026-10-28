package com.novachat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.novachat.AppContainer
import com.novachat.domain.StreamingController
import com.novachat.domain.model.Chat
import com.novachat.domain.model.ChatEvent
import com.novachat.domain.model.ChatMessage
import com.novachat.domain.model.ProviderKind
import com.novachat.domain.model.ProviderProfile
import com.novachat.domain.model.Role
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatId: String,
    private val container: AppContainer
) : ViewModel() {

    private val chats = container.chatRepository
    private val providers = container.providerRepository
    private val controller = StreamingController(viewModelScope)

    val chat: StateFlow<Chat?> = chats.observeChat(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val messages: StateFlow<List<ChatMessage>> = chats.observeMessages(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profiles: StateFlow<List<ProviderProfile>> = providers.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _streaming = MutableStateFlow(false)
    val streaming: StateFlow<Boolean> = _streaming.asStateFlow()

    /** In-flight streamed text per assistant message id (faster than DB round-trips). */
    private val _drafts = MutableStateFlow<Map<String, String>>(emptyMap())
    val drafts: StateFlow<Map<String, String>> = _drafts.asStateFlow()

    private val _notice = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val notice: SharedFlow<String> = _notice

    fun activeProfile(): ProviderProfile? =
        profiles.value.find { it.id == chat.value?.profileId }

    fun canSend(): Boolean {
        val p = activeProfile() ?: return false
        return p.kind == ProviderKind.ECHO || p.apiKey.isNotBlank()
    }

    fun missingKeyHint(): String? {
        val p = activeProfile() ?: return null
        return if (p.kind != ProviderKind.ECHO && p.apiKey.isBlank()) {
            "Add an API key for \"${p.name}\" in Settings"
        } else {
            null
        }
    }

    fun send(raw: String) {
        val text = raw.trim()
        if (text.isEmpty() || _streaming.value) return
        val chat = chat.value ?: return
        val profile = activeProfile() ?: return
        if (!canSend()) {
            _notice.tryEmit(missingKeyHint() ?: "Cannot send right now")
            return
        }
        viewModelScope.launch {
            chats.addMessage(chatId, Role.USER, text, chat.modelId)
            val assistant = chats.addMessage(chatId, Role.ASSISTANT, "", chat.modelId)
            val history = messages.value
            controller.start {
                runGeneration(assistant.id, history, profile)
            }
        }
    }

    fun retry(failedMessageId: String) {
        if (_streaming.value) return
        val chat = chat.value ?: return
        val profile = activeProfile() ?: return
        if (!canSend()) {
            _notice.tryEmit(missingKeyHint() ?: "Cannot send right now")
            return
        }
        viewModelScope.launch {
            val current = messages.value
            val failed = current.find { it.id == failedMessageId } ?: return@launch
            val lastUser = current
                .filter { it.role == Role.USER && it.createdAt <= failed.createdAt }
                .maxByOrNull { it.createdAt } ?: return@launch
            val history = current.filter {
                it.errorText == null && it.content.isNotBlank() &&
                    it.id != failedMessageId && it.createdAt <= lastUser.createdAt
            }
            val assistant = chats.addMessage(chatId, Role.ASSISTANT, "", chat.modelId)
            val fullHistory = history + lastUser.let {
                // lastUser already in history unless it was blank — ensure presence
                if (history.any { m -> m.id == it.id }) emptyList() else listOf(it)
            }
            controller.start {
                runGeneration(assistant.id, fullHistory, profile)
            }
        }
    }

    /** Deletes the last assistant reply and re-streams it from the same history. */
    fun regenerate() {
        if (_streaming.value) return
        val chat = chat.value ?: return
        val profile = activeProfile() ?: return
        if (!canSend()) {
            _notice.tryEmit(missingKeyHint() ?: "Cannot send right now")
            return
        }
        viewModelScope.launch {
            val current = chats.getMessagesOnce(chatId)
            val lastAssistant = current.lastOrNull { it.role == Role.ASSISTANT }
            if (lastAssistant == null) {
                _notice.tryEmit("Nothing to regenerate")
                return@launch
            }
            val history = current.filter {
                it.createdAt < lastAssistant.createdAt &&
                    it.errorText == null && it.content.isNotBlank()
            }
            if (history.none { it.role == Role.USER }) {
                _notice.tryEmit("Nothing to regenerate from")
                return@launch
            }
            chats.deleteMessage(lastAssistant.id)
            val assistant = chats.addMessage(chatId, Role.ASSISTANT, "", chat.modelId)
            controller.start {
                runGeneration(assistant.id, history, profile)
            }
        }
    }

    /**
     * Replaces a user message, deletes everything after it, and re-sends.
     */
    fun editAndResend(targetId: String, newText: String) {
        val text = newText.trim()
        if (text.isEmpty() || _streaming.value) return
        val chat = chat.value ?: return
        val profile = activeProfile() ?: return
        if (!canSend()) {
            _notice.tryEmit(missingKeyHint() ?: "Cannot send right now")
            return
        }
        viewModelScope.launch {
            val target = chats.getMessage(targetId) ?: return@launch
            if (target.role != Role.USER || target.chatId != chatId) return@launch
            chats.updateMessageContent(targetId, text)
            chats.deleteMessagesAfter(chatId, target.createdAt)
            val assistant = chats.addMessage(chatId, Role.ASSISTANT, "", chat.modelId)
            val history = chats.getMessagesOnce(chatId).filter {
                it.errorText == null && it.content.isNotBlank()
            }
            controller.start {
                runGeneration(assistant.id, history, profile)
            }
        }
    }

    fun stop() {
        controller.stop()
    }

    fun rename(title: String) {
        viewModelScope.launch { chats.renameChat(chatId, title) }
    }

    fun clearMessages() {
        if (_streaming.value) return
        viewModelScope.launch { chats.clearMessages(chatId) }
    }

    fun deleteChat(onDone: () -> Unit) {
        viewModelScope.launch {
            controller.stopAndJoin()
            chats.deleteChat(chatId)
            onDone()
        }
    }

    fun updateModel(modelId: String) {
        viewModelScope.launch { chats.updateChatModel(chatId, modelId) }
    }

    fun pickModel(profile: ProviderProfile, modelId: String) {
        viewModelScope.launch {
            val current = chat.value ?: return@launch
            if (profile.id != current.profileId) {
                chats.moveChatToProfile(chatId, profile.id, modelId)
            } else {
                chats.updateChatModel(chatId, modelId)
            }
        }
    }

    fun switchProfile(profile: ProviderProfile) {
        viewModelScope.launch {
            val current = chat.value ?: return@launch
            val model = current.modelId.ifBlank { profile.defaultModelId }
            chats.moveChatToProfile(chatId, profile.id, model)
        }
    }

    fun updateSystemPrompt(prompt: String?) {
        viewModelScope.launch { chats.updateSystemPrompt(chatId, prompt) }
    }

    /**
     * After the first assistant reply, title an untitled chat from the first
     * user message. No extra LLM call.
     */
    private suspend fun maybeAutoTitle(history: List<ChatMessage>) {
        val current = chat.value ?: return
        if (current.title != "New chat" && current.title.isNotBlank()) return
        val firstUser = history.firstOrNull { it.role == Role.USER }
            ?.content?.trim()?.takeIf { it.isNotEmpty() } ?: return
        chats.renameChat(chatId, firstUser.take(40))
    }

    override fun onCleared() {
        // Leaving the screen cancels the stream; the CancellationException
        // path in runGeneration persists the partial text.
        controller.stop()
        super.onCleared()
    }

    private suspend fun runGeneration(
        assistantId: String,
        history: List<ChatMessage>,
        profile: ProviderProfile
    ) {
        _streaming.value = true
        val chat = chat.value ?: run { _streaming.value = false; return }
        val provider = container.providerFor(profile.kind)
        val sb = StringBuilder()
        var updates = 0
        try {
            provider.stream(
                profile = profile,
                modelId = chat.modelId,
                messages = history,
                systemPrompt = chat.systemPrompt,
                temperature = chat.temperature,
                maxTokens = chat.maxTokens
            ) { event ->
                when (event) {
                    is ChatEvent.Delta -> {
                        sb.append(event.text)
                        _drafts.update { it + (assistantId to sb.toString()) }
                        updates++
                        if (updates % 15 == 0) {
                            chats.updateMessageContent(assistantId, sb.toString())
                        }
                    }
                    is ChatEvent.Done -> {
                        chats.finalizeAssistantMessage(
                            assistantId, sb.toString(),
                            event.usageIn, event.usageOut, null, false
                        )
                    }
                    is ChatEvent.Failed -> {
                        chats.finalizeAssistantMessage(
                            assistantId, sb.toString(),
                            null, null, event.message, false
                        )
                    }
                }
            }
            chats.touchChat(chatId, System.currentTimeMillis())
            maybeAutoTitle(history)
        } catch (e: CancellationException) {
            chats.finalizeAssistantMessage(
                assistantId, sb.toString(), null, null, null, true
            )
            chats.touchChat(chatId, System.currentTimeMillis())
            throw e
        } finally {
            _drafts.update { it - assistantId }
            _streaming.value = false
        }
    }
}

class ChatViewModelFactory(
    private val container: AppContainer,
    private val chatId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ChatViewModel(chatId, container) as T
}
