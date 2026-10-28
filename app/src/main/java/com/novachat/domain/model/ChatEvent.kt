package com.novachat.domain.model

sealed interface ChatEvent {
    data class Delta(val text: String) : ChatEvent
    data class Done(val usageIn: Int?, val usageOut: Int?) : ChatEvent
    data class Failed(val message: String, val cause: Throwable? = null) : ChatEvent
}
