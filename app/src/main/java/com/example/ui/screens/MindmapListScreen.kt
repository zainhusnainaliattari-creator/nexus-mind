package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Mindmap
import com.example.data.model.MindmapTheme
import com.example.ui.viewmodel.MindmapViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindmapListScreen(
    viewModel: MindmapViewModel,
    onSelectMindmap: (Mindmap) -> Unit,
    onSignOut: () -> Unit
) {
    val mindmaps by viewModel.allMindmaps.collectAsStateWithLifecycle()
    val vectorQuery by viewModel.vectorQuery.collectAsStateWithLifecycle()
    val vectorResults by viewModel.vectorResults.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    var selectedSubjectFilter by remember { mutableStateOf("All") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createMode by remember { mutableStateOf(0) } // 0: AI Generator, 1: Blank, 2: Template
    var newTopicPrompt by remember { mutableStateOf("") }
    var newSubjectCode by remember { mutableStateOf("CS-101") }

    val distinctSubjects = remember(mindmaps) {
        listOf("All") + mindmaps.map { it.subject }.filter { it.isNotBlank() }.distinct()
    }

    val filteredMindmaps = remember(mindmaps, selectedSubjectFilter) {
        if (selectedSubjectFilter == "All") mindmaps
        else mindmaps.filter { it.subject.equals(selectedSubjectFilter, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧠", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("MindNexus", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text("Student Knowledge Graph", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Text("Sign Out", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("create_mindmap_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Mindmap")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Semantic Vector Search Bar
            OutlinedTextField(
                value = vectorQuery,
                onValueChange = { viewModel.searchVectorDatabase(it) },
                placeholder = { Text("Vector Search concepts across all notes & maps...") },
                leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = "Vector Search", tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (vectorQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.searchVectorDatabase("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vector_search_input"),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            // Vector Search Results Preview
            if (vectorQuery.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Semantic Vector Matches (${vectorResults.size} concepts found):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(modifier = Modifier.height(180.dp)) {
                    if (vectorResults.isEmpty()) {
                        item {
                            Text("No vector matches above similarity threshold.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    } else {
                        items(vectorResults) { res ->
                            val percent = (res.similarityScore * 100).toInt()
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        val targetMap = mindmaps.find { it.id == res.record.mindmapId }
                                        if (targetMap != null) {
                                            viewModel.openMindmap(targetMap)
                                            onSelectMindmap(targetMap)
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(res.record.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(res.snippet, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (percent > 60) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            "$percent% match",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (percent > 60) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Student Stats Overview Banner
            val totalNodes = remember(mindmaps) { mindmaps.sumOf { it.nodeCount } }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${mindmaps.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Mindmaps", style = MaterialTheme.typography.bodySmall)
                    }
                    Box(modifier = Modifier.size(1.dp, 30.dp).background(MaterialTheme.colorScheme.outline))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$totalNodes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Text("Concepts", style = MaterialTheme.typography.bodySmall)
                    }
                    Box(modifier = Modifier.size(1.dp, 30.dp).background(MaterialTheme.colorScheme.outline))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔥 5 Day", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                        Text("Study Streak", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Course Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                distinctSubjects.forEach { sub ->
                    FilterChip(
                        selected = selectedSubjectFilter == sub,
                        onClick = { selectedSubjectFilter = sub },
                        label = { Text(sub) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mindmaps List
            if (filteredMindmaps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📚", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No mindmaps found in $selectedSubjectFilter", fontWeight = FontWeight.SemiBold)
                        Text("Tap + to brainstorm with Pollinations AI or pick a template!", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMindmaps) { map ->
                        MindmapCard(
                            mindmap = map,
                            onClick = {
                                viewModel.openMindmap(map)
                                onSelectMindmap(map)
                            },
                            onDelete = {
                                viewModel.deleteMindmap(map.id)
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Create Mindmap Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Study Mindmap")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = createMode == 0,
                            onClick = { createMode = 0 },
                            label = { Text("AI Auto-Map") }
                        )
                        FilterChip(
                            selected = createMode == 1,
                            onClick = { createMode = 1 },
                            label = { Text("Blank") }
                        )
                    }

                    if (createMode == 0) {
                        OutlinedTextField(
                            value = newTopicPrompt,
                            onValueChange = { newTopicPrompt = it },
                            label = { Text("Topic to Brainstorm") },
                            placeholder = { Text("e.g. Organic Chemistry, Quantum Physics, French Revolution") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_topic_prompt_input")
                        )
                        Text(
                            "Powered by default free Pollinations AI (no API key required!)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        OutlinedTextField(
                            value = newTopicPrompt,
                            onValueChange = { newTopicPrompt = it },
                            label = { Text("Mindmap Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = newSubjectCode,
                        onValueChange = { newSubjectCode = it },
                        label = { Text("Subject / Course Code") },
                        placeholder = { Text("e.g. BIO-101, HIST-200") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val topic = newTopicPrompt.ifBlank { "Untitled Mindmap" }
                        val subject = newSubjectCode.ifBlank { "General" }
                        if (createMode == 0) {
                            viewModel.createMindmapFromAi(topic, subject)
                        } else {
                            viewModel.createBlankMindmap(topic, subject) { created ->
                                onSelectMindmap(created)
                            }
                        }
                        showCreateDialog = false
                    },
                    modifier = Modifier.testTag("confirm_create_mindmap_btn")
                ) {
                    Text(if (createMode == 0) "Generate with AI" else "Create")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MindmapCard(
    mindmap: Mindmap,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val themeEnum = try { MindmapTheme.valueOf(mindmap.theme) } catch (_: Exception) { MindmapTheme.OBSIDIAN }
    val accentColor = Color(themeEnum.primaryColor)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("mindmap_card_${mindmap.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = mindmap.subject,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mindmap.isFavorite) {
                        Icon(Icons.Default.Star, contentDescription = "Favorite", tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = mindmap.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (mindmap.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mindmap.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌿 ${mindmap.nodeCount} Concepts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "🎨 ${themeEnum.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
