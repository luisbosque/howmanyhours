package com.howmanyhours

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
import com.howmanyhours.data.database.AppDatabase
import com.howmanyhours.repository.TimeTrackingRepository
import com.howmanyhours.ui.screens.MainScreen
import com.howmanyhours.ui.screens.SettingsScreen
import com.howmanyhours.ui.screens.BackupScreen
import com.howmanyhours.ui.theme.HowManyHoursTheme
import com.howmanyhours.viewmodel.TimeTrackingViewModel

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
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
        
        val database = AppDatabase.getDatabase(this)
        val repository = TimeTrackingRepository(
            database.projectDao(),
            database.timeEntryDao()
        )
        
        setContent {
            HowManyHoursTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("main") }
                    
                    val viewModel: TimeTrackingViewModel = viewModel(
                        factory = TimeTrackingViewModel.Factory(repository, this@MainActivity)
                    )
                    
                    when (currentScreen) {
                        "main" -> MainScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = { currentScreen = "settings" }
                        )
                        "settings" -> SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = "main" },
                            onNavigateToBackup = { currentScreen = "backup" }
                        )
                        "backup" -> BackupScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = "settings" }
                        )
                    }
                }
            }
        }
    }
}