package com.novachat.data.remote.sse

import java.io.IOException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

data class SseEvent(val type: String?, val data: String)

/** Terminal HTTP/network failure of an SSE stream. code == null means no HTTP response. */
class SseFailureException(
    val code: Int?,
    val body: String?,
    cause: Throwable?
) : IOException(code?.let { "HTTP $it" } ?: (cause?.message ?: "Network error"), cause)

/**
 * Shared SSE wrapper (OkHttp EventSource + callbackFlow).
 * - Emits parsed SSE frames until the server closes the stream.
 * - Non-2xx responses surface as [SseFailureException] carrying the body text.
 * - Cancelling the collecting coroutine closes the socket immediately.
 */
class SseClient(private val client: OkHttpClient) {

    fun events(request: Request): Flow<SseEvent> = callbackFlow {
        val factory = EventSources.createFactory(client)
        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) = Unit

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                message: String
            ) {
                trySend(SseEvent(type, message))
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                val body = try {
                    response?.body?.string()
                } catch (_: Exception) {
                    null
                }
                close(SseFailureException(response?.code, body, t))
            }
        }
        val source = factory.newEventSource(request, listener)
        awaitClose { source.cancel() }
    }
}
