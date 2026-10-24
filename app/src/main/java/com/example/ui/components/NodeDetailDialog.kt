package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MindNode
import com.example.data.model.NodeMastery
import com.example.data.model.NodePriority
import com.example.data.model.NodeShape

@Composable
fun NodeDetailDialog(
    node: MindNode,
    allNodes: List<MindNode>,
    onDismiss: () -> Unit,
    onSave: (MindNode) -> Unit,
    onDelete: (MindNode) -> Unit,
    onAddChild: (MindNode) -> Unit,
    onAiExpand: (MindNode) -> Unit,
    onSocraticChallenge: (MindNode) -> Unit
) {
    var title by remember { mutableStateOf(node.title) }
    var notesMarkdown by remember { mutableStateOf(node.notesMarkdown) }
    var selectedColor by remember { mutableStateOf(node.colorHex) }
    var selectedShape by remember { mutableStateOf(node.shape) }
    var selectedIcon by remember { mutableStateOf(node.icon) }
    var selectedMastery by remember { mutableStateOf(node.mastery) }
    var selectedPriority by remember { mutableStateOf(node.priority) }
    var dueDate by remember { mutableStateOf(node.dueDate) }

    var tabIndex by remember { mutableStateOf(0) } // 0: Notes Editor, 1: Markdown Preview, 2: Styling & Links

    val presetColors = listOf(
        "#58A6FF", "#3FB950", "#BD93F9", "#E3B341", "#FF79C6", "#00F0FF", "#F97316", "#EC4899"
    )

    val presetIcons = listOf("💡", "🔬", "📐", "⚡", "🧠", "📚", "🎯", "🧪", "⚙️", "🚀", "📜", "🏷️", "📊", "💻")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .testTag("node_detail_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with icon and action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedIcon, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Concept Editor",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row {
                        IconButton(
                            onClick = { onAiExpand(node) },
                            modifier = Modifier.testTag("ai_expand_btn")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Expand", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { onSocraticChallenge(node) },
                            modifier = Modifier.testTag("socratic_challenge_btn")
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = "Socratic Tutor", tint = Color(0xFFF59E0B))
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Concept Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("node_title_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mastery Status Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NodeMastery.values().forEach { mastery ->
                        val isSelected = selectedMastery == mastery.name
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMastery = mastery.name },
                            label = { Text(mastery.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(android.graphics.Color.parseColor(mastery.colorHex)).copy(alpha = 0.25f),
                                selectedLabelColor = Color(android.graphics.Color.parseColor(mastery.colorHex))
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab selection: Notes, Preview, Styling
                TabRow(selectedTabIndex = tabIndex) {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0 },
                        text = { Text("Notes (Markdown)") }
                    )
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        text = { Text("Preview") }
                    )
                    Tab(
                        selected = tabIndex == 2,
                        onClick = { tabIndex = 2 },
                        text = { Text("Style & Meta") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (tabIndex) {
                    0 -> {
                        // Markdown Editor
                        OutlinedTextField(
                            value = notesMarkdown,
                            onValueChange = { notesMarkdown = it },
                            placeholder = { Text("Enter detailed study notes, markdown lists, checklist items (- [ ]), or formulas ($$ E=mc^2 $$)...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .testTag("node_notes_input")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Tip: Supports Markdown headers (#), bullets (-), checklist (- [ ]), code (`code`), and LaTeX ($$ formula $$)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    1 -> {
                        // Formatted Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (notesMarkdown.isBlank()) {
                                Text("No notes entered yet. Type markdown in the Notes tab.", color = Color.Gray)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val lines = notesMarkdown.lines()
                                    for (line in lines) {
                                        when {
                                            line.startsWith("# ") -> Text(line.removePrefix("# "), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            line.startsWith("## ") -> Text(line.removePrefix("## "), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                            line.startsWith("### ") -> Text(line.removePrefix("### "), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            line.startsWith("- [ ]") -> {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Checkbox(checked = false, onCheckedChange = {})
                                                    Text(line.removePrefix("- [ ]"), style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                            line.startsWith("- [x]") -> {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Checkbox(checked = true, onCheckedChange = {})
                                                    Text(line.removePrefix("- [x]"), style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                            line.startsWith("- ") -> Text("• " + line.removePrefix("- "), style = MaterialTheme.typography.bodyMedium)
                                            line.startsWith("$$") -> Text(line, fontFamily = FontFamily.Monospace, color = Color(0xFFE3B341))
                                            else -> Text(line, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Node Styling & Icons
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Node Accent Color", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                presetColors.forEach { hex ->
                                    val color = Color(android.graphics.Color.parseColor(hex))
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (selectedColor == hex) 3.dp else 1.dp,
                                                color = if (selectedColor == hex) Color.White else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = hex }
                                    )
                                }
                            }

                            Text("Node Shape", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NodeShape.values().forEach { shape ->
                                    FilterChip(
                                        selected = selectedShape == shape.name,
                                        onClick = { selectedShape = shape.name },
                                        label = { Text(shape.displayName) }
                                    )
                                }
                            }

                            Text("Icon / Badge", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presetIcons.forEach { iconEmoji ->
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (selectedIcon == iconEmoji) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { selectedIcon = iconEmoji },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(iconEmoji, fontSize = 20.sp)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = dueDate,
                                    onValueChange = { dueDate = it },
                                    label = { Text("Due / Exam Date") },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("e.g. Nov 15") }
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Priority", style = MaterialTheme.typography.labelSmall)
                                    Row {
                                        NodePriority.values().forEach { prio ->
                                            FilterChip(
                                                selected = selectedPriority == prio.name,
                                                onClick = { selectedPriority = prio.name },
                                                label = { Text(prio.displayName.take(1)) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom actions: Save, Add Child, Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onDelete(node); onDismiss() },
                        modifier = Modifier.testTag("delete_node_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Node", tint = MaterialTheme.colorScheme.error)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onAddChild(node); onDismiss() },
                            modifier = Modifier.testTag("add_child_node_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Child")
                        }

                        Button(
                            onClick = {
                                val updated = node.copy(
                                    title = title.ifBlank { "Untitled" },
                                    notesMarkdown = notesMarkdown,
                                    colorHex = selectedColor,
                                    shape = selectedShape,
                                    icon = selectedIcon,
                                    mastery = selectedMastery,
                                    priority = selectedPriority,
                                    dueDate = dueDate
                                )
                                onSave(updated)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("save_node_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}
