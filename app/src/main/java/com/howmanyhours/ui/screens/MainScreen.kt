package com.howmanyhours.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.howmanyhours.R
import com.howmanyhours.backup.BackupManager
import com.howmanyhours.data.entities.Project
import com.howmanyhours.viewmodel.TimeTrackingViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: TimeTrackingViewModel = viewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToProjectDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val projects by viewModel.projects.collectAsState(initial = emptyList())

    // Backup manager to check auto-export status
    val backupManager = remember { BackupManager(context, viewModel.backupRepository) }
    var isAutoExportEnabled by remember { mutableStateOf(backupManager.isAutoExportEnabled()) }

    // Refresh auto-export status when screen becomes visible
    LaunchedEffect(Unit) {
        isAutoExportEnabled = backupManager.isAutoExportEnabled()
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Project?>(null) }
    var showAddEntryDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showBackupStatusTooltip by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Project")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Header with title and settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "How Many Hours",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Very subtle backup status indicator
                    Box {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Backup status",
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { showBackupStatusTooltip = !showBackupStatusTooltip },
                            tint = if (isAutoExportEnabled) {
                                Color(0xFF4CAF50) // Green for enabled
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) // Grey for disabled
                            }
                        )

                        // Floating tooltip using Popup
                        if (showBackupStatusTooltip) {
                            Popup(
                                alignment = Alignment.BottomEnd,
                                onDismissRequest = { showBackupStatusTooltip = false },
                                properties = PopupProperties(focusable = true)
                            ) {
                                Surface(
                                    modifier = Modifier.width(200.dp),
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shadowElevation = 8.dp,
                                    tonalElevation = 2.dp
                                ) {
                                    Text(
                                        text = if (isAutoExportEnabled) {
                                            "Backups are automatically exported to external storage."
                                        } else {
                                            "Backups stored in app only. Export to external storage for safety."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            // Active Project Section
            ActiveProjectCard(
                activeProject = uiState.activeProject,
                runningTimeEntry = uiState.runningTimeEntry,
                monthlyHours = uiState.monthlyHours,
                currentPeriod = uiState.currentPeriod,
                isTracking = uiState.isTracking && uiState.activeProject?.id == uiState.runningTimeEntry?.projectId,
                onStartStop = {
                    if (uiState.isTracking && uiState.activeProject?.id == uiState.runningTimeEntry?.projectId) {
                        viewModel.stopTracking()
                    } else {
                        uiState.activeProject?.let { project ->
                            viewModel.startTracking(project)
                        }
                    }
                },
                onDiscard = {
                    viewModel.discardTracking()
                },
                onAddEntry = {
                    showAddEntryDialog = true
                },
                onRenameEntry = {
                    showRenameDialog = true
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Projects List
            Text(
                text = "Projects",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(projects) { project ->
                    ProjectCard(
                        project = project,
                        isActive = project.id == uiState.activeProject?.id,
                        isTracking = uiState.isTracking && project.id == uiState.runningTimeEntry?.projectId,
                        monthlyHours = uiState.projectMonthlyHours[project.id] ?: 0L,
                        onProjectClick = { viewModel.selectProject(project) },
                        onDeleteClick = { showDeleteDialog = project },
                        onDetailsClick = { onNavigateToProjectDetail(project.id) }
                    )
                }
            }
        }
    }

    // Create Project Dialog
    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreateProject = { name ->
                viewModel.createProject(name)
                showCreateDialog = false
            }
        )
    }

    // Delete Project Dialog
    showDeleteDialog?.let { project ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Project") },
            text = { Text("Are you sure you want to delete \"${project.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProject(project)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Switch Project Confirmation Dialog
    if (uiState.showSwitchConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelSwitchProject() },
            title = { Text(stringResource(R.string.confirm_switch_project)) },
            text = { Text(stringResource(R.string.confirm_switch_message)) },
            confirmButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { viewModel.confirmSwitchProject() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.stop_and_start))
                    }
                    TextButton(
                        onClick = { viewModel.discardAndSwitchProject() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.discard_and_start))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelSwitchProject() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Add Entry Dialog
    if (showAddEntryDialog && uiState.activeProject != null) {
        AddEntryDialog(
            onDismiss = { showAddEntryDialog = false },
            onAddEntry = { minutes, name ->
                viewModel.addTimeEntry(uiState.activeProject!!.id, minutes, name)
                showAddEntryDialog = false
            }
        )
    }

    // Rename Entry Dialog
    if (showRenameDialog && uiState.runningTimeEntry != null) {
        RenameEntryDialog(
            currentName = uiState.runningTimeEntry?.name ?: "",
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                viewModel.updateRunningEntryName(newName)
                showRenameDialog = false
            }
        )
    }
}

@Composable
fun ActiveProjectCard(
    activeProject: Project?,
    runningTimeEntry: com.howmanyhours.data.entities.TimeEntry?,
    monthlyHours: Long,
    currentPeriod: com.howmanyhours.repository.Period?,
    isTracking: Boolean,
    onStartStop: () -> Unit,
    onDiscard: () -> Unit,
    onAddEntry: () -> Unit,
    onRenameEntry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = activeProject?.name ?: "No active project",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Current Run Time (only show when tracking)
            if (isTracking) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current Run",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF2E7D32), // Dark green text
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatDuration(runningTimeEntry?.getDurationInMinutes() ?: 0),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32) // Dark green text
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Entry name display and rename button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = runningTimeEntry?.name?.let { "\"$it\"" } ?: "Unnamed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (runningTimeEntry?.name != null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                            fontStyle = if (runningTimeEntry?.name == null) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onRenameEntry,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Rename entry",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Monthly Hours
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Centered period hours content (always centered)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Period-aware title
                    if (activeProject?.periodMode == "monthly") {
                        Text(
                            text = "This Month",
                            style = if (isTracking) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isTracking) FontWeight.Normal else FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "This Period",
                            style = if (isTracking) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isTracking) FontWeight.Normal else FontWeight.Medium
                        )
                    }
                    Text(
                        text = formatDuration(monthlyHours),
                        style = if (isTracking) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Add button aligned with the hours counter text baseline
                if (!isTracking && activeProject != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .offset(y = 8.dp), // Adjust this offset to align with hours text
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            // Light grey circular background
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        shape = CircleShape
                                    )
                            )
                            IconButton(
                                onClick = onAddEntry,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add new entry",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            if (isTracking) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onStartStop,
                        modifier = Modifier.size(width = 140.dp, height = 56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Stop",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    
                    IconButton(
                        onClick = onDiscard,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.discard_tracking),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                Button(
                    onClick = onStartStop,
                    enabled = activeProject != null,
                    modifier = Modifier.size(width = 160.dp, height = 56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    isActive: Boolean,
    isTracking: Boolean,
    monthlyHours: Long,
    onProjectClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isActive) 
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    )
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        onClick = onProjectClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        if (isTracking) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = Color(0xFF4CAF50),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                    
                    if (monthlyHours > 0) {
                        Text(
                            text = "This month: ${formatDuration(monthlyHours)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onDetailsClick) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Project details",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete project",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreateProject: (String) -> Unit
) {
    var projectName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Project") },
        text = {
            OutlinedTextField(
                value = projectName,
                onValueChange = { if (it.length <= 64) projectName = it },
                label = { Text("Project Name") },
                supportingText = { Text("${projectName.length}/64") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (projectName.isNotBlank()) {
                        onCreateProject(projectName.trim())
                    }
                },
                enabled = projectName.isNotBlank()
            ) {
                Text("Create")
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
fun AddEntryDialog(
    onDismiss: () -> Unit,
    onAddEntry: (Long, String?) -> Unit
) {
    var minutesText by remember { mutableStateOf("") }
    var entryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_time_entry)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it },
                    label = { Text(stringResource(R.string.minutes_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = entryName,
                    onValueChange = { entryName = it },
                    label = { Text("Entry Name (optional)") },
                    placeholder = { Text("e.g., Client meeting, Code review") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val minutes = minutesText.toLongOrNull()
                    if (minutes != null && minutes > 0) {
                        val name = entryName.trim().ifEmpty { null }
                        onAddEntry(minutes, name)
                    }
                },
                enabled = minutesText.toLongOrNull()?.let { it > 0 } == true
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun RenameEntryDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var entryName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name Time Entry") },
        text = {
            OutlinedTextField(
                value = entryName,
                onValueChange = { entryName = it },
                label = { Text("Entry Name (optional)") },
                placeholder = { Text("e.g., Client meeting, Code review") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onRename(entryName.trim())
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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