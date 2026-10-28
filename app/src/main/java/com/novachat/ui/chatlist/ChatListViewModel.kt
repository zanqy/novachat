package com.novachat.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.novachat.AppContainer
import com.novachat.domain.model.Chat
import com.novachat.domain.model.ChatMessage
import com.novachat.domain.model.ProviderProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatListItem(
    val chat: Chat,
    val lastMessage: ChatMessage?
)

class ChatListViewModel(private val container: AppContainer) : ViewModel() {

    val profiles: StateFlow<List<ProviderProfile>> =
        container.providerRepository.observeProfiles()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<ChatListItem>> =
        container.chatRepository.observeChats()
            .flatMapLatest { chats ->
                if (chats.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        chats.map { chat ->
                            container.chatRepository.observeLastMessage(chat.id)
                                .map { last -> ChatListItem(chat, last) }
                        }
                    ) { it.toList() }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun profileFor(chat: Chat): ProviderProfile? =
        profiles.value.find { it.id == chat.profileId }

    /** Creates a chat with the default provider; returns its id, or null. */
    suspend fun createChat(): String? {
        val profile = container.providerRepository.defaultProfile()
            ?: profiles.value.firstOrNull()
            ?: return null
        val chat = container.chatRepository.createChat(
            profile = profile,
            modelId = null,
            temperature = container.defaultTemperature(),
            maxTokens = container.defaultMaxTokens()
        )
        return chat.id
    }

    fun renameChat(chatId: String, title: String) {
        viewModelScope.launch { container.chatRepository.renameChat(chatId, title) }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch { container.chatRepository.deleteChat(chatId) }
    }
}

class ChatListViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ChatListViewModel(container) as T
}
