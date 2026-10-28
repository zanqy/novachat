package com.novachat.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.novachat.AppContainer
import com.novachat.domain.model.ChatMessage
import com.novachat.domain.model.ProviderKind
import com.novachat.domain.model.ProviderProfile
import com.novachat.domain.model.Role
import java.util.UUID
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class SettingsViewModel(val container: AppContainer) : ViewModel() {

    val profiles: StateFlow<List<ProviderProfile>> =
        container.providerRepository.observeProfiles()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val temperature: StateFlow<Float> = container.settings.temperature
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.7f)

    val maxTokens: StateFlow<Int?> = container.settings.maxTokens
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val themeMode: StateFlow<String> = container.settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val dynamicColor: StateFlow<Boolean> = container.settings.dynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    var counts by mutableStateOf(0 to 0)
        private set

    init {
        refreshCounts()
    }

    fun refreshCounts() {
        viewModelScope.launch {
            counts = container.chatRepository.counts()
        }
    }

    fun setTemperature(value: Float) {
        viewModelScope.launch { container.settings.setTemperature(value) }
    }

    fun setMaxTokens(value: Int?) {
        viewModelScope.launch { container.settings.setMaxTokens(value) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { container.settings.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { container.settings.setDynamicColor(enabled) }
    }

    fun saveProfile(profile: ProviderProfile) {
        viewModelScope.launch { container.providerRepository.upsert(profile) }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch { container.providerRepository.deleteProfile(id) }
    }

    fun deleteAllChats() {
        viewModelScope.launch {
            container.chatRepository.deleteAllChats()
            refreshCounts()
        }
    }

    /** Runs one tiny request against the profile; returns a human-readable result. */
    suspend fun testProfile(profile: ProviderProfile): String {
        if (profile.kind != ProviderKind.ECHO && profile.apiKey.isBlank()) {
            return "Missing API key."
        }
        if (profile.kind == ProviderKind.OPENAI_COMPATIBLE && profile.baseUrl.isBlank()) {
            return "Base URL is empty."
        }
        val model = profile.defaultModelId.ifBlank { return "Model ID is empty." }
        return try {
            withTimeout(30_000) {
                var gotText = false
                var outcome = "OK (empty reply)."
                val probe = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    chatId = "",
                    role = Role.USER,
                    content = "Reply with exactly: ok",
                    errorText = null,
                    modelId = model,
                    usageIn = null,
                    usageOut = null,
                    stoppedManually = false,
                    createdAt = System.currentTimeMillis()
                )
                container.providerFor(profile.kind).stream(
                    profile = profile,
                    modelId = model,
                    messages = listOf(probe),
                    systemPrompt = null,
                    temperature = 0f,
                    maxTokens = 8
                ) { event ->
                    when (event) {
                        is com.novachat.domain.model.ChatEvent.Delta -> gotText = true
                        is com.novachat.domain.model.ChatEvent.Done ->
                            outcome = if (gotText) "OK — model responded." else "OK (empty reply)."
                        is com.novachat.domain.model.ChatEvent.Failed ->
                            outcome = "Error: ${event.message}"
                    }
                }
                outcome
            }
        } catch (e: TimeoutCancellationException) {
            "Timed out after 30s."
        } catch (e: Exception) {
            "Error: ${e.message ?: e.toString()}"
        }
    }
}

class SettingsViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SettingsViewModel(container) as T
}
