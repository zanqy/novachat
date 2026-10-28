package com.novachat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.novachat.AppContainer
import com.novachat.data.remote.providers.ModelCatalog
import com.novachat.domain.model.ProviderKind
import com.novachat.domain.model.ProviderProfile
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun ProviderEditDialog(
    initial: ProviderProfile?,
    onDismiss: () -> Unit,
    onSave: (ProviderProfile) -> Unit,
    onDelete: ((String) -> Unit)?,
    onTest: suspend (ProviderProfile) -> String
) {
    val scope = rememberCoroutineScope()
    var kind by remember { mutableStateOf(initial?.kind ?: ProviderKind.OPENAI_COMPATIBLE) }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var baseUrl by remember { mutableStateOf(initial?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf(initial?.apiKey ?: "") }
    var showKey by remember { mutableStateOf(false) }
    var modelId by remember { mutableStateOf(initial?.defaultModelId ?: "") }
    var enabled by remember { mutableStateOf(initial?.enabled ?: true) }
    var kindMenu by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    fun current(): ProviderProfile = ProviderProfile(
        id = initial?.id ?: UUID.randomUUID().toString(),
        kind = kind,
        name = name.trim().ifBlank { ModelCatalog.kindLabel(kind) },
        baseUrl = baseUrl.trim(),
        apiKey = apiKey.trim(),
        defaultModelId = modelId.trim(),
        enabled = enabled,
        createdAt = initial?.createdAt ?: System.currentTimeMillis()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add provider" else "Edit provider") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Type", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(
                    onClick = { kindMenu = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(ModelCatalog.kindLabel(kind)) }
                DropdownMenu(expanded = kindMenu, onDismissRequest = { kindMenu = false }) {
                    for (k in ProviderKind.entries) {
                        DropdownMenuItem(
                            text = { Text(ModelCatalog.kindLabel(k)) },
                            onClick = {
                                kind = k
                                if (baseUrl.isBlank()) baseUrl = ModelCatalog.defaultBaseUrl(k)
                                if (modelId.isBlank()) modelId = ModelCatalog.defaultModelId(k)
                                kindMenu = false
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = baseUrl, onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    placeholder = { Text(ModelCatalog.baseUrlHint(kind)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey, onValueChange = { apiKey = it; testResult = null },
                    label = { Text("API key") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showKey) "Hide key" else "Show key"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelId, onValueChange = { modelId = it; testResult = null },
                    label = { Text("Default model") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enabled", modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            testing = true
                            testResult = null
                            testResult = onTest(current())
                            testing = false
                        }
                    },
                    enabled = !testing,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (testing) "Testing…" else "Test") }
                if (testResult != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        testResult!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (testResult!!.startsWith("OK"))
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(current()); onDismiss() }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (initial != null && onDelete != null) {
                    TextButton(onClick = { onDelete(initial.id); onDismiss() }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    navController: NavController
) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(container))
    val profiles by vm.profiles.collectAsState()
    val temperature by vm.temperature.collectAsState()
    val maxTokens by vm.maxTokens.collectAsState()
    val themeMode by vm.themeMode.collectAsState()
    val dynamicColor by vm.dynamicColor.collectAsState()

    var editing by remember { mutableStateOf<ProviderProfile?>(null) }
    var adding by remember { mutableStateOf(false) }
    var maxTokensText by remember(maxTokens) { mutableStateOf(maxTokens?.toString() ?: "") }
    var showDeleteAll1 by remember { mutableStateOf(false) }
    var showDeleteAll2 by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Settings") }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionTitle("Providers")
            for (p in profiles) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { editing = p }
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                p.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                if (p.apiKey.isNotBlank()) "key ✓" else "key ✗",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (p.apiKey.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${ModelCatalog.kindLabel(p.kind)} · ${p.baseUrl.ifBlank { "—" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!p.enabled) {
                            Text(
                                "disabled",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { adding = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add provider") }

            Spacer(Modifier.height(24.dp))
            SectionTitle("Generation defaults")
            Text(
                "Temperature: ${"%.2f".format(temperature)}",
                style = MaterialTheme.typography.bodyMedium
            )
            androidx.compose.material3.Slider(
                value = temperature,
                onValueChange = { vm.setTemperature(it) },
                valueRange = 0f..2f
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = maxTokensText,
                onValueChange = { v ->
                    maxTokensText = v
                    val n = v.trim().toIntOrNull()
                    vm.setMaxTokens(if (v.isBlank()) null else n)
                },
                label = { Text("Default max tokens (empty = provider default)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            SectionTitle("Appearance")
            for ((value, label) in listOf(
                "system" to "System", "light" to "Light", "dark" to "Dark"
            )) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.setThemeMode(value) }
                ) {
                    RadioButton(
                        selected = themeMode == value,
                        onClick = { vm.setThemeMode(value) }
                    )
                    Text(label)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dynamic color (Android 12+)", modifier = Modifier.weight(1f))
                Switch(checked = dynamicColor, onCheckedChange = { vm.setDynamicColor(it) })
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("Data")
            Text(
                "${vm.counts.first} chats · ${vm.counts.second} messages",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showDeleteAll1 = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Delete all chats", color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (adding) {
        ProviderEditDialog(
            initial = null,
            onDismiss = { adding = false },
            onSave = { vm.saveProfile(it) },
            onDelete = null,
            onTest = { vm.testProfile(it) }
        )
    }
    if (editing != null) {
        ProviderEditDialog(
            initial = editing,
            onDismiss = { editing = null },
            onSave = { vm.saveProfile(it) },
            onDelete = { vm.deleteProfile(it) },
            onTest = { vm.testProfile(it) }
        )
    }
    if (showDeleteAll1) {
        AlertDialog(
            onDismissRequest = { showDeleteAll1 = false },
            title = { Text("Delete all chats?") },
            text = { Text("This removes every chat and message on this device.") },
            confirmButton = {
                TextButton(onClick = { showDeleteAll1 = false; showDeleteAll2 = true }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAll1 = false }) { Text("Cancel") }
            }
        )
    }
    if (showDeleteAll2) {
        AlertDialog(
            onDismissRequest = { showDeleteAll2 = false },
            title = { Text("Really delete everything?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteAllChats()
                    showDeleteAll2 = false
                }) { Text("Yes, delete everything") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAll2 = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
