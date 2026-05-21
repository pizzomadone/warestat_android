package com.warestat.android.ui.screens.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warestat.android.i18n.LocalStrings
import com.warestat.android.ui.theme.*
import com.warestat.android.viewmodel.BackupViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BackupScreen(viewModel: BackupViewModel = hiltViewModel()) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showRestoreConfirm by remember { mutableStateOf<File?>(null) }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.restoreBackup(it) }
    }

    LaunchedEffect(state.successMessage, state.error) {
        state.successMessage?.let { scope.launch { snackbarHostState.showSnackbar(it) }; viewModel.clearMessages() }
        state.error?.let { scope.launch { snackbarHostState.showSnackbar("${strings.error}$it") }; viewModel.clearMessages() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(strings.backupTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            // Action buttons
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.performBackup() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.Backup, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(strings.createBackup)
                    }
                    OutlinedButton(
                        onClick = { restoreLauncher.launch("*/*") },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isLoading
                    ) {
                        Icon(Icons.Default.Restore, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.restore)
                    }
                }
            }

            // Auto backup settings
            item {
                Card(elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(strings.backupSettings, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(strings.autoBackup, fontWeight = FontWeight.Medium)
                                Text(strings.autoBackupDesc, fontSize = 12.sp, color = Color.Gray)
                            }
                            Switch(checked = state.autoBackupEnabled, onCheckedChange = { viewModel.setAutoBackup(it) })
                        }

                        Divider()

                        Column {
                            Text("${strings.availableBackups}: ${state.retentionDays}", fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Slider(
                                value = state.retentionDays.toFloat(),
                                onValueChange = { viewModel.setRetentionDays(it.toInt()) },
                                valueRange = 1f..30f,
                                steps = 28
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("1", fontSize = 11.sp, color = Color.Gray)
                                Text("30", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Info card
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF))) {
                    Row(Modifier.padding(12.dp)) {
                        Icon(Icons.Default.Info, null, tint = Secondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${strings.backupSettings}. ${strings.restoreWarning}",
                            fontSize = 12.sp, color = Color.DarkGray
                        )
                    }
                }
            }

            // Backup list
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.availableBackups, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${state.backupFiles.size} file", fontSize = 12.sp, color = Color.Gray)
                }
            }

            if (state.backupFiles.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(strings.noBackupAvailable, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(state.backupFiles, key = { it.name }) { file ->
                    BackupFileCard(file, restoreLabel = strings.restore, onRestore = { showRestoreConfirm = file })
                }
            }
        }
    }

    val context = LocalContext.current

    showRestoreConfirm?.let { file ->
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = null },
            title = { Text(strings.restoreBackupTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.restoreBackupTitle + ":")
                    Text(file.name, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5))) {
                        Row(Modifier.padding(8.dp)) {
                            Icon(Icons.Default.Warning, null, tint = Danger, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.restoreWarning, fontSize = 12.sp, color = Danger)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context, "${context.packageName}.provider", file
                        )
                        viewModel.restoreBackup(uri)
                        showRestoreConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) { Text(strings.restore) }
            },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = null }) { Text(strings.cancel) } }
        )
    }
}

@Composable
private fun BackupFileCard(file: File, restoreLabel: String, onRestore: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dateStr = sdf.format(Date(file.lastModified()))
    val sizeKb = file.length() / 1024

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Storage, null, tint = Primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(dateStr, fontSize = 11.sp, color = Color.Gray)
                    Text("${sizeKb} KB", fontSize = 11.sp, color = Color.Gray)
                }
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Default.Restore, restoreLabel, tint = Primary)
            }
        }
    }
}
