package net.luisico.howmanyhours

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import net.luisico.howmanyhours.data.database.AppDatabase
import net.luisico.howmanyhours.repository.TimeTrackingRepository
import net.luisico.howmanyhours.repository.Period
import net.luisico.howmanyhours.ui.screens.*
import net.luisico.howmanyhours.ui.theme.HowManyHoursTheme
import net.luisico.howmanyhours.viewmodel.TimeTrackingViewModel

sealed class Screen {
    object Main : Screen()
    object Settings : Screen()
    object Backup : Screen()
    data class ProjectDetail(val projectId: Long) : Screen()
    data class PeriodHistory(val projectId: Long) : Screen()
    data class PeriodDetail(val projectId: Long, val period: Period) : Screen()
}

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean ->
        // Handle permission result if needed
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Try to initialize database with auto-recovery on failure
        var databaseRecovered = false
        val database = try {
            AppDatabase.getDatabase(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Database initialization failed: ${e.message}", e)
            // Auto-recover: Delete corrupted database and start fresh
            AppDatabase.emergencyReset(this)
            databaseRecovered = true
            // Try again with clean database
            AppDatabase.getDatabase(this)
        }

        val repository = TimeTrackingRepository(
            database.projectDao(),
            database.timeEntryDao(),
            database.periodCloseDao()
        )

        setContent {
            HowManyHoursTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
                    var showRecoveryDialog by remember { mutableStateOf(databaseRecovered) }

                    val viewModel: TimeTrackingViewModel = viewModel(
                        factory = TimeTrackingViewModel.Factory(repository, this@MainActivity)
                    )

                    // Show recovery dialog if database was auto-recovered
                    if (showRecoveryDialog) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showRecoveryDialog = false },
                            title = { androidx.compose.material3.Text("Database Recovered") },
                            text = {
                                androidx.compose.material3.Text(
                                    "The app detected corrupted data and automatically reset the database to prevent crashes.\n\n" +
                                    "All data has been cleared. You can now:\n" +
                                    "• Restore a recent backup (Settings → Manage Backups)\n" +
                                    "• Start fresh with a clean app\n\n" +
                                    "Sorry for the inconvenience!"
                                )
                            },
                            confirmButton = {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        showRecoveryDialog = false
                                        currentScreen = Screen.Backup
                                    }
                                ) {
                                    androidx.compose.material3.Text("Restore Backup")
                                }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(onClick = { showRecoveryDialog = false }) {
                                    androidx.compose.material3.Text("Start Fresh")
                                }
                            }
                        )
                    }

                    when (val screen = currentScreen) {
                        is Screen.Main -> MainScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = { currentScreen = Screen.Settings },
                            onNavigateToProjectDetail = { projectId ->
                                currentScreen = Screen.ProjectDetail(projectId)
                            }
                        )
                        is Screen.Settings -> SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = Screen.Main },
                            onNavigateToBackup = { currentScreen = Screen.Backup }
                        )
                        is Screen.Backup -> BackupScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = Screen.Settings }
                        )
                        is Screen.ProjectDetail -> ProjectDetailScreen(
                            viewModel = viewModel,
                            projectId = screen.projectId,
                            onNavigateBack = { currentScreen = Screen.Main },
                            onNavigateToPeriodHistory = { projectId ->
                                currentScreen = Screen.PeriodHistory(projectId)
                            }
                        )
                        is Screen.PeriodHistory -> PeriodHistoryScreen(
                            viewModel = viewModel,
                            projectId = screen.projectId,
                            onNavigateBack = { currentScreen = Screen.ProjectDetail(screen.projectId) },
                            onNavigateToPeriodDetail = { period ->
                                currentScreen = Screen.PeriodDetail(screen.projectId, period)
                            }
                        )
                        is Screen.PeriodDetail -> PeriodDetailScreen(
                            viewModel = viewModel,
                            projectId = screen.projectId,
                            period = screen.period,
                            onNavigateBack = { currentScreen = Screen.PeriodHistory(screen.projectId) }
                        )
                    }
                }
            }
        }
    }
}