package com.example.mountainbiketrainer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(navController: NavHostController, sessionViewModel: SessionViewModel) {
    val sessionFiles by sessionViewModel.sessionFiles.collectAsState()
    val (showDeleteConfirmDialog, setShowDeleteConfirmDialog) = remember { mutableStateOf<SessionFile?>(null) }

    // Load files when the screen is first composed or recomposed after a relevant change
    LaunchedEffect(Unit) {
        sessionViewModel.loadSessionFiles()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recorded Sessions") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back to Main Screen"
                        )
                    }
                },
                actions = {
                    if (sessionFiles.isNotEmpty()) {
                        IconButton(onClick = {
                            sessionViewModel.deleteAllSessionFiles()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete All Sessions"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()) {
            if (sessionFiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No recorded sessions found.")
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(sessionFiles, key = { it.path }) { sessionFile ->
                        SessionFileItem(
                            sessionFile = sessionFile,
                            onDeleteClick = { setShowDeleteConfirmDialog(sessionFile) },
                            onSendClick = { sessionViewModel.sendSessionFileToApi(sessionFile) }
                        )
                        Divider()
                    }
                }
            }
        }
    }

    showDeleteConfirmDialog?.let { fileToDelete ->
        AlertDialog(
            onDismissRequest = { setShowDeleteConfirmDialog(null) },
            title = { Text("Delete Session?") },
            text = { Text("Are you sure you want to delete '${fileToDelete.name}'?") },
            confirmButton = {
                Button(onClick = {
                    sessionViewModel.deleteSessionFile(fileToDelete)
                    setShowDeleteConfirmDialog(null)
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { setShowDeleteConfirmDialog(null) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SessionFileItem(
    sessionFile: SessionFile,
    onDeleteClick: () -> Unit,
    onSendClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(sessionFile.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Size: ${formatFileSize(sessionFile.size)}",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp
            )
            Text(
                "Date: ${dateFormatter.format(Date(sessionFile.lastModified))}",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp
            )
        }
        IconButton(onClick = onSendClick) {
            Icon(Icons.Filled.Send, contentDescription = "Send session data")
        }
        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete session")
        }
    }
}

fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> String.format(Locale.US, "%.2f KB", size / 1024.0)
        size < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.2f MB", size / (1024.0 * 1024.0))
        else -> String.format(Locale.US, "%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
    }
}