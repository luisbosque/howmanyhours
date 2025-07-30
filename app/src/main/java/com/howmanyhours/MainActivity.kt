package com.howmanyhours

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.howmanyhours.data.database.AppDatabase
import com.howmanyhours.repository.TimeTrackingRepository
import com.howmanyhours.ui.screens.MainScreen
import com.howmanyhours.ui.screens.SettingsScreen
import com.howmanyhours.ui.theme.HowManyHoursTheme
import com.howmanyhours.viewmodel.TimeTrackingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
                        factory = TimeTrackingViewModel.Factory(repository)
                    )
                    
                    when (currentScreen) {
                        "main" -> MainScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = { currentScreen = "settings" }
                        )
                        "settings" -> SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = "main" }
                        )
                    }
                }
            }
        }
    }
}