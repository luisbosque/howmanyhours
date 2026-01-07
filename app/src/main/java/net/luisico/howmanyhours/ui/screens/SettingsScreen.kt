package net.luisico.howmanyhours.ui.screens

import android.content.Context
import android.content.Intent
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileWriter
import java.io.IOException
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.luisico.howmanyhours.BuildConfig
import net.luisico.howmanyhours.viewmodel.TimeTrackingViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.TimeZone

data class DownloadResult(
    val success: Boolean,
    val filename: String = "",
    val filePath: String = "",
    val error: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TimeTrackingViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToBackup: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Header with title and back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
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
            item {
                SettingsSection(title = "Data Export") {
                    SettingsItem(
                        title = "Download CSV",
                        subtitle = "Download all time tracking data as CSV file",
                        icon = Icons.Default.Share,
                        onClick = {
                            scope.launch {
                                isExporting = true
                                try {
                                    val result = exportCsvFile(context, viewModel)
                                    if (result.success) {
                                        snackbarHostState.showSnackbar(
                                            message = "CSV saved to: ${result.filePath}",
                                            duration = SnackbarDuration.Long
                                        )
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            message = "Download failed: ${result.error}",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        isLoading = isExporting
                    )
                }
            }

            item {
                SettingsSection(title = "Data Backup & Recovery") {
                    SettingsItem(
                        title = "Manage Backups",
                        subtitle = "Configure automatic backups and restore data",
                        icon = Icons.Default.Backup,
                        onClick = onNavigateToBackup
                    )
                }
            }

            item {
                SettingsSection(title = "About") {
                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "How Many Hours",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Version ${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "A simple time tracking app for your projects",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    Card(
        onClick = onClick,
        enabled = !isLoading
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

private suspend fun exportCsvFile(context: Context, viewModel: TimeTrackingViewModel): DownloadResult {
    val fmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    fmt.timeZone = TimeZone.getDefault()
    val timestamp = fmt.format(Date())
    val fileName = "time_tracking_$timestamp.csv"

    return try {
        // Use app-specific external storage (Documents folder)
        // This is private to the app but accessible via file manager
        // No storage permission needed, more secure than public Downloads
        val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: return DownloadResult(success = false, error = "External storage not available")

        val file = File(documentsDir, fileName)

        // Stream CSV data directly to file in batches to avoid memory issues
        file.outputStream().use { outputStream ->
            viewModel.exportToCsvToStream(outputStream)
        }

        DownloadResult(success = true, filename = fileName, filePath = file.absolutePath)

    } catch (e: Exception) {
        DownloadResult(success = false, error = e.message ?: "Unknown error")
    }
}