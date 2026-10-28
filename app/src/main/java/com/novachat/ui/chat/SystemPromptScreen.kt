package com.novachat.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.novachat.AppContainer
import kotlinx.coroutines.launch

/**
 * Per-chat system prompt editor. Multiline, saved to the chat on Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemPromptScreen(
    container: AppContainer,
    navController: NavController,
    chatId: String
) {
    val chat by container.chatRepository.observeChat(chatId)
        .collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var edited by remember { mutableStateOf<String?>(null) }

    val initial = chat?.systemPrompt.orEmpty()
    val current = edited ?: initial
    val dirty = current != initial

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = { Text("System prompt") },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                container.chatRepository.updateSystemPrompt(
                                    chatId,
                                    current.ifBlank { null }
                                )
                                navController.popBackStack()
                            }
                        },
                        enabled = dirty && chat != null
                    ) { Text("Save") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "Applies to future replies in \"${chat?.title ?: "… "}\". Empty means no system prompt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = current,
                onValueChange = { edited = it },
                placeholder = { Text("e.g. You are a concise coding assistant…") },
                minLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
        }
    }
}
