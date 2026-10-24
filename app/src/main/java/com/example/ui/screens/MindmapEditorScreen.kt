package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BackupSnapshot
import com.example.data.model.LayoutAlgorithm
import com.example.data.model.MindNode
import com.example.data.model.Mindmap
import com.example.data.model.MindmapTheme
import com.example.ui.components.AudioLectureRecorderWidget
import com.example.ui.components.CanvasMindmapView
import com.example.ui.components.NodeDetailDialog
import com.example.ui.components.PomodoroTimerWidget
import com.example.ui.viewmodel.MindmapViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindmapEditorScreen(
    mindmap: Mindmap,
    viewModel: MindmapViewModel,
    onNavigateBack: () -> Unit,
    onOpenStudyTools: () -> Unit,
    onOpenCollaboration: (String) -> Unit
) {
    BackHandler {
        viewModel.closeMindmap()
        onNavigateBack()
    }

    val context = LocalContext.current
    val nodes by viewModel.currentNodes.collectAsStateWithLifecycle()
    val selectedNode by viewModel.selectedNode.collectAsStateWithLifecycle()
    val isPresentationMode by viewModel.isPresentationMode.collectAsStateWithLifecycle()
    val presentationStep by viewModel.presentationStep.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val socraticChallenge by viewModel.socraticChallenge.collectAsStateWithLifecycle()
    val exportOutput by viewModel.exportOutput.collectAsStateWithLifecycle()
    val backups by viewModel.backups.collectAsStateWithLifecycle()

    var showDetailDialog by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showLayoutMenu by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showBackupsDialog by remember { mutableStateOf(false) }
    var showScratchpadDialog by remember { mutableStateOf(false) }
    var scratchpadText by remember { mutableStateOf("") }
    var showAddNodeDialog by remember { mutableStateOf(false) }
    var newNodeTitle by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = mindmap.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${mindmap.subject} • ${nodes.size} Nodes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.closeMindmap()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("back_to_dashboard_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Walkthrough Presentation Mode
                    IconButton(
                        onClick = { viewModel.togglePresentationMode() },
                        modifier = Modifier.testTag("presentation_mode_btn")
                    ) {
                        Icon(
                            if (isPresentationMode) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Presentation Mode",
                            tint = if (isPresentationMode) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Theme selector
                    Box {
                        IconButton(onClick = { showThemeMenu = true }) {
                            Icon(Icons.Default.Palette, contentDescription = "Theme")
                        }
                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            MindmapTheme.values().forEach { theme ->
                                DropdownMenuItem(
                                    text = { Text(theme.displayName) },
                                    onClick = {
                                        viewModel.updateTheme(theme)
                                        showThemeMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Auto-Layout
                    Box {
                        IconButton(onClick = { showLayoutMenu = true }) {
                            Icon(Icons.Default.AccountTree, contentDescription = "Auto Layout")
                        }
                        DropdownMenu(
                            expanded = showLayoutMenu,
                            onDismissRequest = { showLayoutMenu = false }
                        ) {
                            LayoutAlgorithm.values().forEach { algo ->
                                DropdownMenuItem(
                                    text = { Text(algo.displayName) },
                                    onClick = {
                                        viewModel.applyAutoLayout(algo)
                                        showLayoutMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Export
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Default.Download, contentDescription = "Export")
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            listOf("HTML", "SVG", "Markdown", "JSON").forEach { fmt ->
                                DropdownMenuItem(
                                    text = { Text("Export as $fmt") },
                                    onClick = {
                                        viewModel.exportCurrentMap(fmt)
                                        showExportMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Backup snapshots history
                    IconButton(onClick = { showBackupsDialog = true }) {
                        Icon(Icons.Default.Backup, contentDescription = "Backups")
                    }

                    // Study Tools Hub
                    IconButton(
                        onClick = onOpenStudyTools,
                        modifier = Modifier.testTag("open_study_tools_btn")
                    ) {
                        Icon(Icons.Default.School, contentDescription = "Study Tools", tint = Color(0xFFE3B341))
                    }

                    // Peer Collaboration Room
                    IconButton(
                        onClick = { onOpenCollaboration(mindmap.id) },
                        modifier = Modifier.testTag("open_collab_btn")
                    ) {
                        Icon(Icons.Default.Group, contentDescription = "Collaborate", tint = MaterialTheme.colorScheme.secondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Interactive Canvas
            CanvasMindmapView(
                nodes = nodes,
                selectedNodeId = selectedNode?.id,
                onNodeSelected = { node ->
                    viewModel.selectNode(node)
                },
                onNodeMoved = { node, x, y ->
                    viewModel.updateNodePosition(node, x, y)
                },
                onBackgroundTapped = {
                    viewModel.selectNode(null)
                },
                onNodeDoubleTapped = { node ->
                    viewModel.selectNode(node)
                    showDetailDialog = true
                }
            )

            // Presentation Mode Banner
            if (isPresentationMode && nodes.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = { viewModel.prevPresentationNode() }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Walkthrough Mode (${presentationStep + 1}/${nodes.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                nodes.getOrNull(presentationStep)?.title ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = { viewModel.nextPresentationNode() }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next")
                        }
                        IconButton(onClick = { viewModel.togglePresentationMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit Walkthrough")
                        }
                    }
                }
            }

            // Floating Bottom-Left: Pomodoro Timer & Audio Memo
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AudioLectureRecorderWidget(
                    mindmapId = mindmap.id,
                    onRecordingSaved = { title, duration ->
                        Toast.makeText(context, "Saved lecture recording ($duration s)", Toast.LENGTH_SHORT).show()
                    }
                )
                PomodoroTimerWidget(
                    currentTopic = selectedNode?.title ?: mindmap.title,
                    onSessionCompleted = { mins, topic ->
                        Toast.makeText(context, "Completed $mins min focus on '$topic'!", Toast.LENGTH_LONG).show()
                    }
                )
            }

            // Floating Bottom-Right: Quick Actions (Add Node, Scratchpad, AI expand)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Scratchpad
                FloatingActionButton(
                    onClick = { showScratchpadDialog = true },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("scratchpad_btn")
                ) {
                    Text("📝", fontSize = 18.sp)
                }

                // AI Auto Expand on selected node
                if (selectedNode != null) {
                    FloatingActionButton(
                        onClick = { viewModel.aiExpandNode(selectedNode!!) },
                        containerColor = Color(0xFFF59E0B),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("floating_ai_expand_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Expand", tint = Color.Black)
                    }
                }

                // Add Node
                FloatingActionButton(
                    onClick = {
                        if (selectedNode != null) {
                            viewModel.addChildNode(selectedNode!!)
                        } else {
                            showAddNodeDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("add_node_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Node")
                }
            }

            // AI Loading indicator
            if (isAiLoading) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(16.dp)),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Text("AI generating study concepts...", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    // Node Detail Dialog
    if (showDetailDialog && selectedNode != null) {
        NodeDetailDialog(
            node = selectedNode!!,
            allNodes = nodes,
            onDismiss = { showDetailDialog = false },
            onSave = { updated ->
                viewModel.saveNode(updated)
                showDetailDialog = false
            },
            onDelete = { nodeToDelete ->
                viewModel.deleteNode(nodeToDelete)
                showDetailDialog = false
            },
            onAddChild = { parent ->
                viewModel.addChildNode(parent)
            },
            onAiExpand = { target ->
                viewModel.aiExpandNode(target)
            },
            onSocraticChallenge = { target ->
                viewModel.requestSocraticChallenge(target)
            }
        )
    }

    // Add Root Node Dialog
    if (showAddNodeDialog) {
        AlertDialog(
            onDismissRequest = { showAddNodeDialog = false },
            title = { Text("Add Concept Node") },
            text = {
                OutlinedTextField(
                    value = newNodeTitle,
                    onValueChange = { newNodeTitle = it },
                    label = { Text("Node Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNodeTitle.isNotBlank()) {
                            viewModel.addRootNode(newNodeTitle)
                            newNodeTitle = ""
                            showAddNodeDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddNodeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Quick Scratchpad Dialog
    if (showScratchpadDialog) {
        AlertDialog(
            onDismissRequest = { showScratchpadDialog = false },
            title = { Text("📝 Quick Capture Scratchpad") },
            text = {
                Column {
                    Text(
                        "Jot down lecture thoughts. Tap 'Convert to Node' to instantly attach to this mindmap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = scratchpadText,
                        onValueChange = { scratchpadText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = { Text("Rapid lecture notes...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (scratchpadText.isNotBlank()) {
                            viewModel.addRootNode("Note: " + scratchpadText.take(24), scratchpadText)
                            scratchpadText = ""
                            showScratchpadDialog = false
                        }
                    }
                ) {
                    Text("Convert to Node")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showScratchpadDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Socratic Challenge Dialog
    if (socraticChallenge != null) {
        val sc = socraticChallenge!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissSocraticChallenge() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏛️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Socratic Tutor")
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = sc.prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Counter-Perspective:\n${sc.counterArgument}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Questions to consider:", style = MaterialTheme.typography.labelMedium)
                    sc.followUpQuestions.forEach { q ->
                        Text("• $q", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissSocraticChallenge() }) {
                    Text("I Understand")
                }
            }
        )
    }

    // Export Output Dialog
    if (exportOutput != null) {
        val (format, content) = exportOutput!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissExport() },
            title = { Text("$format Export Bundle") },
            text = {
                Column {
                    Text(
                        "Your $format export is ready! You can copy to clipboard or paste into browser/viewer.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = content,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("$format Export", content))
                        Toast.makeText(context, "$format copied to clipboard!", Toast.LENGTH_SHORT).show()
                        viewModel.dismissExport()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissExport() }) {
                    Text("Close")
                }
            }
        )
    }

    // Backup Snapshots Dialog
    if (showBackupsDialog) {
        AlertDialog(
            onDismissRequest = { showBackupsDialog = false },
            title = { Text("⏱️ Revision Time Machine") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Automatic and manual backups", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(
                            onClick = { viewModel.createManualBackup() }
                        ) {
                            Text("+ Snapshot")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (backups.isEmpty()) {
                        Text("No backups found yet. Auto-backups are created on changes.")
                    } else {
                        LazyColumn(modifier = Modifier.height(220.dp)) {
                            items(backups) { snap ->
                                val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(snap.timestamp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(snap.note, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.restoreSnapshot(snap)
                                                showBackupsDialog = false
                                            }
                                        ) {
                                            Text("Restore")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showBackupsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
