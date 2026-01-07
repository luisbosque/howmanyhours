package net.luisico.howmanyhours.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.luisico.howmanyhours.viewmodel.TimeTrackingViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    viewModel: TimeTrackingViewModel,
    projectId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToPeriodHistory: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val project by viewModel.getProjectById(projectId).collectAsState(initial = null)

    LaunchedEffect(projectId) {
        viewModel.loadCurrentPeriod(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Project") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CurrentPeriodCard(
                    period = uiState.currentPeriod,
                    totalHours = uiState.monthlyHours,
                    project = project
                )
            }

            item {
                PeriodModeCard(
                    project = project,
                    viewModel = viewModel,
                    projectId = projectId,
                    isTracking = uiState.isTracking && uiState.runningTimeEntry?.projectId == projectId
                )
            }

            item {
                Button(
                    onClick = { onNavigateToPeriodHistory(projectId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("View Period History")
                }
            }
        }
    }
}

@Composable
fun CurrentPeriodCard(
    period: net.luisico.howmanyhours.repository.Period?,
    totalHours: Long,
    project: net.luisico.howmanyhours.data.entities.Project?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            if (period != null && project != null) {
                // Title depends on period mode
                if (project.periodMode == "monthly") {
                    Text(
                        "This Month",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    // Manual mode - show "This period (since...)"
                    Column {
                        Text(
                            "This Period",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "(since ${formatDateTimeCompact(period.startTime)})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem("Total Hours", formatDuration(totalHours))
                }
            } else {
                Text("Loading...", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun PeriodModeCard(
    project: net.luisico.howmanyhours.data.entities.Project?,
    viewModel: TimeTrackingViewModel,
    projectId: Long,
    isTracking: Boolean
) {
    var showModeDialog by remember { mutableStateOf(false) }
    var showCloseDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Period Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (project?.periodMode == "monthly") "Auto-close monthly" else "Manual close",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = { showModeDialog = true }) {
                    Text("Change")
                }
            }

            if (project?.periodMode == "manual") {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showCloseDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Close Current Period")
                }
            }
        }
    }

    if (showModeDialog) {
        PeriodModeDialog(
            currentMode = project?.periodMode ?: "monthly",
            isTracking = isTracking,
            onDismiss = { showModeDialog = false },
            onConfirm = { newMode ->
                viewModel.changePeriodMode(
                    projectId, newMode,
                    onSuccess = { showModeDialog = false },
                    onError = { /* TODO: Show error */ }
                )
            }
        )
    }

    if (showCloseDialog) {
        ClosePeriodDialog(
            isTracking = isTracking,
            onDismiss = { showCloseDialog = false },
            onConfirm = {
                viewModel.closeCurrentPeriod(
                    projectId,
                    onSuccess = { showCloseDialog = false },
                    onError = { /* TODO: Show error */ }
                )
            }
        )
    }
}

@Composable
fun PeriodModeDialog(
    currentMode: String,
    isTracking: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Period Mode") },
        text = {
            Column {
                if (isTracking) {
                    Text(
                        "Note: Changing period mode will not stop your current tracking. Your running entry will continue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Text("All existing period checkpoints will be preserved.")
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedMode == "monthly",
                        onClick = { selectedMode = "monthly" }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Monthly (Automatic)", fontWeight = FontWeight.Medium)
                        Text(
                            "Periods close at month end",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedMode == "manual",
                        onClick = { selectedMode = "manual" }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Manual", fontWeight = FontWeight.Medium)
                        Text(
                            "You close periods manually",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedMode) }) {
                Text("Change")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ClosePeriodDialog(
    isTracking: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Close Current Period") },
        text = {
            Column {
                if (isTracking) {
                    Text("Warning: You are currently tracking time.")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "The running entry will be stopped and the period will close at the current timestamp. A new period will begin immediately.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text("The current period will close at the current timestamp. A new period will begin immediately.")
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Close Period")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StatItem(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatDate(date: Date): String {
    val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    fmt.timeZone = TimeZone.getDefault()
    return fmt.format(date)
}

private fun formatDateTimeCompact(date: Date): String {
    val fmt = SimpleDateFormat("yy-MM-dd HH:mm", Locale.getDefault())
    fmt.timeZone = TimeZone.getDefault()
    return fmt.format(date)
}

private fun formatDuration(minutes: Long): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) {
        "${hours}h ${mins}m"
    } else {
        "${mins}m"
    }
}
