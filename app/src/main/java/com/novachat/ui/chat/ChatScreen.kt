package com.novachat.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.novachat.AppContainer
import com.novachat.domain.model.Chat
import com.novachat.domain.model.ChatMessage
import com.novachat.domain.model.Role
import com.novachat.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    container: AppContainer,
    navController: NavController,
    chatId: String
) {
    val vm: ChatViewModel = viewModel(factory = ChatViewModelFactory(container, chatId))
    val chat by vm.chat.collectAsState()
    val messages by vm.messages.collectAsState()
    val profiles by vm.profiles.collectAsState()
    val streaming by vm.streaming.collectAsState()
    val drafts by vm.drafts.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var composer by remember { mutableStateOf("") }
    var follow by remember { mutableStateOf(true) }
    var showSheet by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var editText by remember { mutableStateOf("") }

    fun copy(text: String) {
        clipboard.setText(AnnotatedString(text))
        scope.launch { snackbar.showSnackbar("Copied to clipboard") }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val snapshot = chat
        if (snapshot == null) {
            scope.launch { snackbar.showSnackbar("Chat not loaded") }
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(buildMarkdownExport(snapshot, messages).toByteArray())
                        out.flush()
                    } ?: error("Cannot open file")
                }.isSuccess
            }
            snackbar.showSnackbar(if (ok) "Exported" else "Export failed")
        }
    }

    LaunchedEffect(vm) {
        vm.notice.collect { snackbar.showSnackbar(it) }
    }

    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            if (total == 0) {
                true
            } else {
                val last = info.visibleItemsInfo.lastOrNull()
                last != null && last.index >= total - 1 &&
                    last.offset + last.size <= info.viewportEndOffset + 8
            }
        }
    }

    LaunchedEffect(listState.isScrollInProgress, atBottom) {
        if (listState.isScrollInProgress && !atBottom) follow = false
    }

    LaunchedEffect(messages.size, drafts) {
        if (follow && messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    val profile = profiles.find { it.id == chat?.profileId }
    val hint = vm.missingKeyHint()
    val lastAssistantId = messages.lastOrNull { it.role == Role.ASSISTANT }?.id

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = chat?.title ?: "…",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable {
                                renameText = chat?.title ?: ""
                                showRename = true
                            }
                        )
                        Text(
                            text = if (profile != null && chat != null) {
                                "${profile.name} · ${chat!!.modelId}"
                            } else {
                                "…"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { showSheet = true }
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("System prompt…") },
                            onClick = {
                                menuOpen = false
                                navController.navigate(Routes.sysPrompt(chatId))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Regenerate reply") },
                            onClick = { menuOpen = false; vm.regenerate() }
                        )
                        DropdownMenuItem(
                            text = { Text("Export as Markdown") },
                            onClick = {
                                menuOpen = false
                                val title = chat?.title
                                exportLauncher.launch("${slug(title)}.md")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear messages") },
                            onClick = { menuOpen = false; showClearConfirm = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete chat") },
                            onClick = { menuOpen = false; showDeleteConfirm = true }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(Modifier.imePadding()) {
                ComposerBar(
                    text = composer,
                    onTextChange = { composer = it },
                    streaming = streaming,
                    canSend = vm.canSend(),
                    hint = hint,
                    onSend = {
                        vm.send(composer)
                        composer = ""
                        follow = true
                    },
                    onStop = { vm.stop() }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            if (chat == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            draft = drafts[message.id],
                            isStreaming = streaming && drafts.containsKey(message.id),
                            showRegenerate = message.id == lastAssistantId &&
                                !streaming && message.errorText == null,
                            onCopy = ::copy,
                            onEdit = {
                                editTarget = message
                                editText = message.content
                            },
                            onRetry = { vm.retry(message.id) },
                            onRegenerate = { vm.regenerate() },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
                if (!atBottom && messages.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = {
                            follow = true
                            scope.launch {
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        Text("↓ latest")
                    }
                }
            }
        }
    }

    if (showSheet && chat != null) {
        ModelPickerSheet(
            container = container,
            chat = chat!!,
            profiles = profiles,
            onPick = { p, m -> vm.pickModel(p, m) },
            onDismiss = { showSheet = false }
        )
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename chat") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.rename(renameText.trim())
                    showRename = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Cancel") }
            }
        )
    }

    if (editTarget != null) {
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("Edit & resend") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = editTarget!!.id
                    val text = editText
                    editTarget = null
                    follow = true
                    vm.editAndResend(id, text)
                }) { Text("Resend") }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear messages?") },
            text = { Text("All messages in this chat will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearMessages()
                    showClearConfirm = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete chat?") },
            text = { Text("This chat and all its messages will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    vm.deleteChat { navController.popBackStack() }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

private fun slug(title: String?): String {
    val s = title?.lowercase(Locale.US).orEmpty()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .take(60)
    return s.ifEmpty { "chat" }
}

private fun buildMarkdownExport(chat: Chat, messages: List<ChatMessage>): String {
    val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        .format(Date(chat.updatedAt))
    return buildString {
        appendLine("# ${chat.title}")
        appendLine()
        appendLine("- Model: `${chat.modelId}`")
        appendLine("- Exported: $date")
        if (!chat.systemPrompt.isNullOrBlank()) {
            appendLine()
            appendLine("System: ${chat.systemPrompt}")
        }
        for (m in messages) {
            appendLine()
            appendLine(if (m.role == Role.USER) "## You" else "## Assistant")
            appendLine()
            val body = m.content.ifBlank {
                if (m.errorText != null) "*Failed: ${m.errorText}*" else "*(empty)*"
            }
            appendLine(body)
        }
    }
}
