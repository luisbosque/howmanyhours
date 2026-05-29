package net.luisico.howmanyhours.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.luisico.howmanyhours.data.entities.TimeEntry
import net.luisico.howmanyhours.repository.Period
import net.luisico.howmanyhours.viewmodel.TimeTrackingViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodDetailScreen(
    viewModel: TimeTrackingViewModel,
    @Suppress("UNUSED_PARAMETER") projectId: Long,
    period: Period,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var renamingEntry by remember { mutableStateOf<TimeEntry?>(null) }

    LaunchedEffect(period) {
        viewModel.selectPeriod(period)
    }

    renamingEntry?.let { entry ->
        LaunchedEffect(Unit) { viewModel.loadRecentEntryNames() }
        RenameEntryDialog(
            currentName = entry.name ?: "",
            recentNames = uiState.recentEntryNames,
            onDismiss = { renamingEntry = null },
            onRename = { newName ->
                viewModel.updateTimeEntryName(entry.id, newName)
                renamingEntry = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(period.getLabel()) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        formatDateRange(period.startTime, period.endTime),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(
                            "Total Hours",
                            formatDuration(uiState.periodEntries.sumOf { it.getDurationInMinutes() })
                        )
                        StatItem(
                            "Entries",
                            uiState.periodEntries.size.toString()
                        )
                    }
                }
            }

            val entriesByDay = uiState.periodEntries.groupBy { entry ->
                val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                fmt.timeZone = TimeZone.getDefault()
                fmt.format(entry.startTime)
            }

            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                entriesByDay.forEach { (day, entries) ->
                    item {
                        Text(
                            day,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(entries) { entry ->
                        EntryDetailCard(
                            entry = entry,
                            period = period,
                            onEditName = { renamingEntry = entry }
                        )
                    }
                }

                if (entriesByDay.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No entries in this period",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EntryDetailCard(
    entry: TimeEntry,
    @Suppress("UNUSED_PARAMETER") period: Period,
    onEditName: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!entry.name.isNullOrEmpty()) {
                        Text(
                            entry.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            "Untitled entry",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (entry.isManual) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "manual",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    formatTimeRange(entry.startTime, entry.endTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatDuration(entry.getDurationInMinutes()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onEditName) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit name",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatTimeRange(start: Date, end: Date?): String {
    val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    fmt.timeZone = TimeZone.getDefault()
    return if (end != null) {
        "${fmt.format(start)} → ${fmt.format(end)}"
    } else {
        "${fmt.format(start)} → Running"
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
