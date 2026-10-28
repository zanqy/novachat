package com.novachat

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.novachat.data.chat.RoomChatRepository
import com.novachat.data.db.NovaChatDb
import com.novachat.data.providers.AnthropicProvider
import com.novachat.data.providers.EchoProvider
import com.novachat.data.providers.GeminiProvider
import com.novachat.data.providers.ModelCatalog
import com.novachat.data.providers.OpenAICompatibleProvider
import com.novachat.data.providers.RoomProviderRepository
import com.novachat.data.remote.sse.SseClient
import com.novachat.data.settings.SettingsStore
import com.novachat.domain.ChatProvider
import com.novachat.domain.ChatRepository
import com.novachat.domain.ProviderRepository
import com.novachat.domain.model.ProviderKind
import com.novachat.domain.model.ProviderProfile
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class AppContainer(
    private val app: Application,
    val appScope: CoroutineScope
) {
    val db: NovaChatDb by lazy {
        Room.databaseBuilder(app, NovaChatDb::class.java, "novachat.db").build()
    }

    val settings: SettingsStore by lazy { SettingsStore(app) }

    val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val sseClient: SseClient by lazy { SseClient(http) }

    val chatRepository: ChatRepository by lazy {
        RoomChatRepository(db.chatDao(), db.messageDao())
    }

    val providerRepository: ProviderRepository by lazy {
        RoomProviderRepository(db.providerProfileDao())
    }

    private val echo = EchoProvider()
    private val openAi = OpenAICompatibleProvider(sseClient, http)
    private val anthropic = AnthropicProvider(sseClient, http)
    private val gemini = GeminiProvider(sseClient, http)

    fun providerFor(kind: ProviderKind): ChatProvider = when (kind) {
        ProviderKind.OPENAI_COMPATIBLE -> openAi
        ProviderKind.ANTHROPIC -> anthropic
        ProviderKind.GEMINI -> gemini
        ProviderKind.ECHO -> echo
    }

    /** Seeds the two starter profiles on first launch. Runs once per process. */
    fun seedIfEmpty() {
        appScope.launch {
            if (providerRepository.count() > 0) return@launch
            val now = System.currentTimeMillis()
            providerRepository.upsert(
                ProviderProfile(
                    id = UUID.randomUUID().toString(),
                    kind = ProviderKind.OPENAI_COMPATIBLE,
                    name = "OpenRouter",
                    baseUrl = ModelCatalog.defaultBaseUrl(ProviderKind.OPENAI_COMPATIBLE),
                    apiKey = "",
                    defaultModelId = ModelCatalog.defaultModelId(ProviderKind.OPENAI_COMPATIBLE),
                    enabled = true,
                    createdAt = now
                )
            )
            providerRepository.upsert(
                ProviderProfile(
                    id = UUID.randomUUID().toString(),
                    kind = ProviderKind.ECHO,
                    name = "Echo (offline test)",
                    baseUrl = "",
                    apiKey = "",
                    defaultModelId = ModelCatalog.defaultModelId(ProviderKind.ECHO),
                    enabled = true,
                    createdAt = now + 1
                )
            )
        }
    }

    suspend fun defaultTemperature(): Float = settings.temperature.first()

    suspend fun defaultMaxTokens(): Int? = settings.maxTokens.first()
}

class NovaChatApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(
            this,
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
        )
        container.seedIfEmpty()
    }
}

fun Context.appContainer(): AppContainer =
    (applicationContext as NovaChatApp).container
