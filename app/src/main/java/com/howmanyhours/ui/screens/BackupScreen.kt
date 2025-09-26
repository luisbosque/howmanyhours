package com.howmanyhours.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.howmanyhours.backup.BackupInfo
import com.howmanyhours.backup.BackupManager
import com.howmanyhours.backup.BackupType
import com.howmanyhours.repository.TimeTrackingRepository
import com.howmanyhours.viewmodel.TimeTrackingViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: TimeTrackingViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Initialize backup manager
    val backupManager = remember {
        BackupManager(context, viewModel.backupRepository)
    }

    var backups by remember { mutableStateOf<List<BackupInfo>>(emptyList()) }
    var totalBackupSize by remember { mutableStateOf(0L) }
    var isAutoBackupEnabled by remember { mutableStateOf(true) }
    var isCreatingBackup by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Load backup data
    LaunchedEffect(Unit) {
        backups = backupManager.getAvailableBackups()
        totalBackupSize = backupManager.getTotalBackupSize()
        isAutoBackupEnabled = backupManager.isAutoBackupEnabled
    }

    // Refresh backups when returning to screen
    LaunchedEffect(backups) {
        // Refresh automatically when screen becomes visible
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Backup & Recovery",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Auto-backup toggle
                item {
                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Automatic Backups",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Create daily backups and before major operations",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isAutoBackupEnabled,
                                    onCheckedChange = { enabled ->
                                        isAutoBackupEnabled = enabled
                                        backupManager.isAutoBackupEnabled = enabled
                                    }
                                )
                            }
                        }
                    }
                }

                // Storage info and manual backup
                item {
                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Storage Used",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = formatFileSize(totalBackupSize),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                isCreatingBackup = true
                                                val backup = backupManager.createBackup(BackupType.MANUAL)
                                                if (backup != null) {
                                                    backups = backupManager.getAvailableBackups()
                                                    totalBackupSize = backupManager.getTotalBackupSize()
                                                    snackbarHostState.showSnackbar("Manual backup created")
                                                } else {
                                                    snackbarHostState.showSnackbar("Failed to create backup")
                                                }
                                                isCreatingBackup = false
                                            }
                                        },
                                        enabled = !isCreatingBackup
                                    ) {
                                        if (isCreatingBackup) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Create Backup")
                                    }
                                }
                            }
                        }
                    }
                }

                // Backup list header
                if (backups.isNotEmpty()) {
                    item {
                        Text(
                            text = "Available Backups (${backups.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Backup list
                items(backups) { backup ->
                    BackupCard(backup = backup)
                }

                // Cleanup section
                if (backups.isNotEmpty()) {
                    item {
                        Card {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Cleanup",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { showDeleteConfirmation = true },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete All Backups")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete All Backups") },
            text = { Text("This will permanently delete all ${backups.size} backup files. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            if (backupManager.deleteAllBackups()) {
                                backups = emptyList()
                                totalBackupSize = 0L
                                snackbarHostState.showSnackbar("All backups deleted")
                            } else {
                                snackbarHostState.showSnackbar("Failed to delete backups")
                            }
                            showDeleteConfirmation = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BackupCard(
    backup: BackupInfo
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = getBackupTypeDisplayName(backup.type),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(backup.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Backup statistics
            Text(
                text = "${backup.projectCount} projects • ${backup.entryCount} entries",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            backup.lastEntryDate?.let { lastEntry ->
                Text(
                    text = "Last entry: ${SimpleDateFormat("MMM dd 'at' HH:mm", Locale.getDefault()).format(lastEntry)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Size: ${formatFileSize(backup.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getBackupTypeDisplayName(type: BackupType): String {
    return when (type) {
        BackupType.DAILY -> "Daily Backup"
        BackupType.PRE_MIGRATION -> "Pre-Migration Backup"
        BackupType.MANUAL -> "Manual Backup"
        BackupType.MONTHLY -> "Monthly Backup"
        BackupType.EMERGENCY -> "Emergency Backup"
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes == 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}