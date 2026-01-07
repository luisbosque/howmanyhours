package net.luisico.howmanyhours.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.luisico.howmanyhours.backup.BackupInfo
import net.luisico.howmanyhours.backup.BackupManager
import net.luisico.howmanyhours.backup.BackupType
import net.luisico.howmanyhours.backup.RestoreResult
import net.luisico.howmanyhours.repository.TimeTrackingRepository
import net.luisico.howmanyhours.viewmodel.TimeTrackingViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.TimeZone

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
    var isAutoExportEnabled by remember { mutableStateOf(false) }
    var autoExportFolderUri by remember { mutableStateOf<String?>(null) }
    var isCreatingBackup by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showValidationDialog by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<Pair<BackupInfo, net.luisico.howmanyhours.backup.BackupValidation>?>(null) }
    var showRestartDialog by remember { mutableStateOf(false) }

    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importUri ->
            scope.launch {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val backupName = "imported_backup_$timestamp.db"
                val importedBackup = backupManager.importBackupFromUri(importUri, backupName)
                if (importedBackup != null) {
                    backups = backupManager.getAvailableBackups()
                    snackbarHostState.showSnackbar("Backup imported successfully")
                } else {
                    snackbarHostState.showSnackbar("Import failed")
                }
            }
        }
    }

    // Folder picker for auto-export
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { folderUri ->
            scope.launch {
                backupManager.setAutoExportFolderUri(folderUri.toString())
                backupManager.setAutoExportEnabled(true)
                isAutoExportEnabled = true
                autoExportFolderUri = folderUri.toString()
                snackbarHostState.showSnackbar("Auto-export folder selected")
            }
        } ?: run {
            // User canceled folder selection
            isAutoExportEnabled = false
            backupManager.setAutoExportEnabled(false)
        }
    }

    // Load backup data
    LaunchedEffect(Unit) {
        backups = backupManager.getAvailableBackups()
        totalBackupSize = backupManager.getTotalBackupSize()
        isAutoBackupEnabled = backupManager.isAutoBackupEnabled
        isAutoExportEnabled = backupManager.isAutoExportEnabled()
        autoExportFolderUri = backupManager.getAutoExportFolderUri()
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

                // Auto-export to external folder toggle
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
                                        text = "Auto-Export to External Folder",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (isAutoExportEnabled) {
                                            "All new backups will be exported to external folder"
                                        } else {
                                            "Enable to automatically export backups to external storage"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isAutoExportEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled) {
                                            // Open folder picker when enabling
                                            folderPickerLauncher.launch(null)
                                        } else {
                                            // Just disable when turning off
                                            isAutoExportEnabled = false
                                            backupManager.setAutoExportEnabled(false)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Auto-export disabled")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Storage info
                item {
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
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
                        }
                    }
                }

                // Backup actions
                item {
                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                enabled = !isCreatingBackup,
                                modifier = Modifier.fillMaxWidth()
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

                            OutlinedButton(
                                onClick = {
                                    importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import Backup")
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
                    BackupCard(
                        backup = backup,
                        onRestore = {
                            scope.launch {
                                // Validate backup first
                                val validation = backupManager.validateBackup(backup)

                                if (!validation.isValid) {
                                    // Show error message for invalid backups
                                    snackbarHostState.showSnackbar(validation.message)
                                } else if (validation.requiresDestructiveMigration) {
                                    // Show warning dialog for backups requiring destructive migration
                                    pendingRestore = Pair(backup, validation)
                                    showValidationDialog = true
                                } else {
                                    // Compatible backup - restore directly
                                    when (val result = backupManager.restoreFromBackup(backup)) {
                                        is RestoreResult.Success -> {
                                            showRestartDialog = true
                                        }
                                        is RestoreResult.Failed -> {
                                            snackbarHostState.showSnackbar("Restore failed: ${result.error}")
                                        }
                                    }
                                }
                            }
                        }
                    )
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
                        showDeleteConfirmation = false // Close dialog immediately
                        scope.launch {
                            if (backupManager.deleteAllBackups()) {
                                backups = emptyList()
                                totalBackupSize = 0L
                                snackbarHostState.showSnackbar("All backups deleted")
                            } else {
                                snackbarHostState.showSnackbar("Failed to delete backups")
                            }
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

    // Validation warning dialog
    if (showValidationDialog && pendingRestore != null) {
        val (backup, validation) = pendingRestore!!
        AlertDialog(
            onDismissRequest = {
                showValidationDialog = false
                pendingRestore = null
            },
            title = { Text("Backup Compatibility Warning") },
            text = {
                Column {
                    Text(validation.message)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Do you want to proceed with the restore?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showValidationDialog = false
                        scope.launch {
                            when (val result = backupManager.restoreFromBackup(backup)) {
                                is RestoreResult.Success -> {
                                    showRestartDialog = true
                                }
                                is RestoreResult.Failed -> {
                                    snackbarHostState.showSnackbar("Restore failed: ${result.error}")
                                }
                            }
                            pendingRestore = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Restore Anyway")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showValidationDialog = false
                        pendingRestore = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restart dialog after successful restore
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { /* Don't allow dismissing - user must restart */ },
            title = { Text("Restore Successful") },
            text = {
                Text(
                    "Backup restored successfully. The app needs to restart to apply the changes.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Restart the app
                        val intent = (context as? Activity)?.intent
                        (context as? Activity)?.finish()
                        context.startActivity(intent)
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                ) {
                    Text("Restart App")
                }
            }
        )
    }
}

@Composable
fun BackupCard(
    backup: BackupInfo,
    onRestore: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = getBackupTypeDisplayName(backup.type),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = run {
                            val fmt = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                            fmt.timeZone = TimeZone.getDefault()
                            fmt.format(backup.timestamp)
                        },
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
                            text = "Last entry: ${run {
                                val fmt = SimpleDateFormat("MMM dd 'at' HH:mm", Locale.getDefault())
                                fmt.timeZone = TimeZone.getDefault()
                                fmt.format(lastEntry)
                            }}",
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

                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Restore", fontSize = 12.sp)
                }
            }
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