package com.howmanyhours.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.howmanyhours.repository.Period
import com.howmanyhours.viewmodel.TimeTrackingViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodHistoryScreen(
    viewModel: TimeTrackingViewModel,
    projectId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToPeriodDetail: (Period) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.loadAllPeriods(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Period History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.allPeriods.reversed()) { period ->
                PeriodHistoryCard(
                    viewModel = viewModel,
                    period = period,
                    onClick = { onNavigateToPeriodDetail(period) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodHistoryCard(
    viewModel: TimeTrackingViewModel,
    period: Period,
    onClick: () -> Unit
) {
    val entries by viewModel.backupRepository.getEntriesForPeriod(period).collectAsState(initial = emptyList())
    val totalHours = entries.sumOf { it.getDurationInMinutes() }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        period.getLabel(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    if (period.isCurrent) {
                        Spacer(Modifier.width(8.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text("Current")
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    formatDateRange(period.startTime, period.endTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatDuration(totalHours),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${entries.size} entries",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDateRange(start: Date, end: Date): String {
    val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    fmt.timeZone = TimeZone.getDefault()
    return "${fmt.format(start)} - ${fmt.format(end)}"
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
