package com.novachat.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novachat.AppContainer
import com.novachat.domain.model.Chat
import com.novachat.domain.model.ModelInfo
import com.novachat.domain.model.ProviderProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerSheet(
    container: AppContainer,
    chat: Chat,
    profiles: List<ProviderProfile>,
    onPick: (ProviderProfile, String) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val enabled = profiles.filter { it.enabled }
    var selProfile by remember(chat.profileId, profiles) {
        mutableStateOf(
            profiles.find { it.id == chat.profileId } ?: enabled.firstOrNull()
        )
    }
    var models by remember { mutableStateOf<List<ModelInfo>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var manual by remember(chat.modelId) { mutableStateOf(chat.modelId) }
    var menuOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Model", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Current: ${chat.modelId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))

            Text("Provider", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = { menuOpen = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    selProfile?.name ?: "No provider",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Filled.ExpandMore, contentDescription = null)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                for (p in enabled) {
                    DropdownMenuItem(
                        text = { Text(p.name) },
                        onClick = {
                            selProfile = p
                            models = null
                            error = null
                            menuOpen = false
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        val sp = selProfile ?: return@Button
                        scope.launch {
                            loading = true
                            error = null
                            try {
                                models = container.providerFor(sp.kind).listModels(sp)
                            } catch (e: Exception) {
                                error = e.message ?: "Fetch failed"
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading && selProfile != null
                ) {
                    Text("Fetch models")
                }
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            models?.let { list ->
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(220.dp)) {
                    items(list, key = { it.id }) { m ->
                        Text(
                            text = m.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val sp = selProfile ?: return@clickable
                                    onPick(sp, m.id)
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Or enter a model ID manually", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = manual,
                onValueChange = { manual = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val sp = selProfile ?: return@Button
                    val id = manual.trim()
                    if (id.isNotEmpty()) {
                        onPick(sp, id)
                        onDismiss()
                    }
                },
                enabled = manual.isNotBlank() && selProfile != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use this model")
            }
        }
    }
}
