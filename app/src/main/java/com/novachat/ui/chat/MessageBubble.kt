package com.novachat.ui.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.novachat.domain.model.ChatMessage
import com.novachat.domain.model.Role

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    draft: String?,
    isStreaming: Boolean,
    onCopy: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (message.role == Role.USER) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .padding(start = 48.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { onCopy(message.content) }
                    )
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    } else {
        val display = draft ?: message.content
        Column(
            modifier = modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { if (display.isNotBlank()) onCopy(display) }
                )
        ) {
            if (display.isBlank() && isStreaming) {
                StreamingCaret()
            } else if (display.isNotBlank()) {
                MarkdownText(display)
            }
            if (message.errorText != null) {
                Spacer(Modifier.height(6.dp))
                ErrorCard(message.errorText, onRetry)
            } else if (isStreaming && display.isBlank()) {
                // caret shown above; nothing else
            }
            val meta = buildMeta(message)
            if (meta != null) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun buildMeta(message: ChatMessage): String? {
    val parts = mutableListOf<String>()
    message.modelId?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
    if (message.usageIn != null || message.usageOut != null) {
        parts.add("in ${message.usageIn ?: "?"} · out ${message.usageOut ?: "?"}")
    }
    if (message.stoppedManually) parts.add("stopped")
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
private fun StreamingCaret() {
    val alpha by rememberInfiniteTransition(label = "caret").animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "caretAlpha"
    )
    Text(
        text = "●",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.alpha(alpha)
    )
}

@Composable
private fun ErrorCard(error: String, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "Generation failed",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Button(onClick = onRetry) { Text("Retry") }
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}
