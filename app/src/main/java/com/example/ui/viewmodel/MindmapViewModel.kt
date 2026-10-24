package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiProvider
import com.example.data.ai.AiService
import com.example.data.ai.GeneratedNodePlan
import com.example.data.export.MindmapExporter
import com.example.data.model.BackupSnapshot
import com.example.data.model.LayoutAlgorithm
import com.example.data.model.MindNode
import com.example.data.model.Mindmap
import com.example.data.model.MindmapTheme
import com.example.data.model.NodeMastery
import com.example.data.model.SocraticChallenge
import com.example.data.repository.MindmapRepository
import com.example.data.vector.VectorDatabase
import com.example.data.vector.VectorSearchResult
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MindmapViewModel(application: Application) : AndroidViewModel(application) {
    val repository = MindmapRepository(application)
    val aiService = AiService.getInstance()
    val vectorDb = VectorDatabase.getInstance()

    // All mindmaps
    val allMindmaps: StateFlow<List<Mindmap>> = repository.allMindmaps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active mindmap and nodes
    private val _currentMindmap = MutableStateFlow<Mindmap?>(null)
    val currentMindmap: StateFlow<Mindmap?> = _currentMindmap.asStateFlow()

    private val _currentNodes = MutableStateFlow<List<MindNode>>(emptyList())
    val currentNodes: StateFlow<List<MindNode>> = _currentNodes.asStateFlow()

    private val _selectedNode = MutableStateFlow<MindNode?>(null)
    val selectedNode: StateFlow<MindNode?> = _selectedNode.asStateFlow()

    private val _backups = MutableStateFlow<List<BackupSnapshot>>(emptyList())
    val backups: StateFlow<List<BackupSnapshot>> = _backups.asStateFlow()

    // Vector Search
    private val _vectorQuery = MutableStateFlow("")
    val vectorQuery: StateFlow<String> = _vectorQuery.asStateFlow()

    private val _vectorResults = MutableStateFlow<List<VectorSearchResult>>(emptyList())
    val vectorResults: StateFlow<List<VectorSearchResult>> = _vectorResults.asStateFlow()

    // Presentation / Walkthrough mode
    private val _isPresentationMode = MutableStateFlow(false)
    val isPresentationMode: StateFlow<Boolean> = _isPresentationMode.asStateFlow()

    private val _presentationStep = MutableStateFlow(0)
    val presentationStep: StateFlow<Int> = _presentationStep.asStateFlow()

    // AI & Dialog states
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _socraticChallenge = MutableStateFlow<SocraticChallenge?>(null)
    val socraticChallenge: StateFlow<SocraticChallenge?> = _socraticChallenge.asStateFlow()

    private val _exportOutput = MutableStateFlow<Pair<String, String>?>(null) // format -> content
    val exportOutput: StateFlow<Pair<String, String>?> = _exportOutput.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun openMindmap(mindmap: Mindmap) {
        _currentMindmap.value = mindmap
        viewModelScope.launch {
            repository.getNodes(mindmap.id).collect { nodes ->
                _currentNodes.value = nodes
                // If in presentation mode, clamp step
                if (_presentationStep.value >= nodes.size) {
                    _presentationStep.value = 0
                }
            }
        }
        viewModelScope.launch {
            repository.getBackups(mindmap.id).collect {
                _backups.value = it
            }
        }
    }

    fun closeMindmap() {
        _currentMindmap.value = null
        _currentNodes.value = emptyList()
        _selectedNode.value = null
        _isPresentationMode.value = false
    }

    fun selectNode(node: MindNode?) {
        _selectedNode.value = node
    }

    fun updateNodePosition(node: MindNode, newX: Float, newY: Float) {
        val updated = node.copy(posX = newX, posY = newY)
        viewModelScope.launch {
            repository.saveNode(updated, _currentMindmap.value)
        }
    }

    fun saveNode(node: MindNode) {
        viewModelScope.launch {
            repository.saveNode(node, _currentMindmap.value)
            _selectedNode.value = node
            _statusMessage.value = "Saved '${node.title}'"
        }
    }

    fun deleteNode(node: MindNode) {
        viewModelScope.launch {
            repository.deleteNode(node, _currentMindmap.value)
            if (_selectedNode.value?.id == node.id) {
                _selectedNode.value = null
            }
        }
    }

    fun addChildNode(parent: MindNode) {
        val map = _currentMindmap.value ?: return
        val uid = Firebase.auth.currentUser?.uid ?: "local_student"
        val newChild = MindNode(
            id = "node_${UUID.randomUUID().toString().take(8)}",
            mindmapId = map.id,
            userId = uid,
            parentId = parent.id,
            title = "New Sub-concept",
            posX = parent.posX + 240f,
            posY = parent.posY + 40f,
            colorHex = parent.colorHex,
            icon = "🔹"
        )
        viewModelScope.launch {
            repository.saveNode(newChild, map)
            _selectedNode.value = newChild
        }
    }

    fun addSiblingNode(sibling: MindNode) {
        val map = _currentMindmap.value ?: return
        val uid = Firebase.auth.currentUser?.uid ?: "local_student"
        val newSibling = MindNode(
            id = "node_${UUID.randomUUID().toString().take(8)}",
            mindmapId = map.id,
            userId = uid,
            parentId = sibling.parentId,
            title = "Related Concept",
            posX = sibling.posX,
            posY = sibling.posY + 90f,
            colorHex = sibling.colorHex,
            icon = sibling.icon
        )
        viewModelScope.launch {
            repository.saveNode(newSibling, map)
            _selectedNode.value = newSibling
        }
    }

    fun addRootNode(title: String, notes: String = "") {
        val map = _currentMindmap.value ?: return
        val uid = Firebase.auth.currentUser?.uid ?: "local_student"
        val newNode = MindNode(
            id = "node_${UUID.randomUUID().toString().take(8)}",
            mindmapId = map.id,
            userId = uid,
            parentId = "",
            title = title,
            notesMarkdown = notes,
            posX = 120f,
            posY = 120f,
            colorHex = "#58A6FF",
            icon = "💡"
        )
        viewModelScope.launch {
            repository.saveNode(newNode, map)
            _selectedNode.value = newNode
        }
    }

    fun applyAutoLayout(algorithm: LayoutAlgorithm) {
        val map = _currentMindmap.value ?: return
        viewModelScope.launch {
            repository.applyAutoLayout(map.id, algorithm)
            _statusMessage.value = "Applied ${algorithm.displayName}"
        }
    }

    fun updateTheme(theme: MindmapTheme) {
        val map = _currentMindmap.value ?: return
        val updated = map.copy(theme = theme.name)
        _currentMindmap.value = updated
        viewModelScope.launch {
            repository.saveMindmap(updated)
        }
    }

    // --- AI Generation Actions ---
    fun createMindmapFromAi(topic: String, subject: String = "General") {
        _isAiLoading.value = true
        val uid = Firebase.auth.currentUser?.uid ?: "local_student"
        viewModelScope.launch {
            try {
                val plan = aiService.generateMindmap(topic)
                val mapId = "map_${UUID.randomUUID().toString().take(8)}"
                val newMap = Mindmap(
                    id = mapId,
                    userId = uid,
                    title = plan.title,
                    description = "AI-generated study mindmap for $topic",
                    subject = subject,
                    theme = MindmapTheme.OBSIDIAN.name
                )
                repository.saveMindmap(newMap)

                val generatedNodes = mutableListOf<MindNode>()
                val rootNode = MindNode(
                    id = "root_$mapId",
                    mindmapId = mapId,
                    userId = uid,
                    parentId = "",
                    title = plan.title,
                    notesMarkdown = plan.notes,
                    posX = 100f,
                    posY = 300f,
                    colorHex = plan.colorHex,
                    icon = plan.icon
                )
                generatedNodes.add(rootNode)

                fun addChildren(parent: MindNode, children: List<GeneratedNodePlan>, startX: Float, yStart: Float): Float {
                    var curY = yStart
                    for (ch in children) {
                        val childNode = MindNode(
                            id = "node_${UUID.randomUUID().toString().take(8)}",
                            mindmapId = mapId,
                            userId = uid,
                            parentId = parent.id,
                            title = ch.title,
                            notesMarkdown = ch.notes,
                            posX = startX,
                            posY = curY,
                            colorHex = ch.colorHex,
                            icon = ch.icon
                        )
                        generatedNodes.add(childNode)
                        if (ch.children.isNotEmpty()) {
                            curY = addChildren(childNode, ch.children, startX + 240f, curY)
                        } else {
                            curY += 90f
                        }
                    }
                    return curY
                }

                addChildren(rootNode, plan.children, 360f, 150f)
                repository.saveNodes(generatedNodes, newMap)
                openMindmap(newMap)
                _statusMessage.value = "Generated mindmap for '$topic'!"
            } catch (e: Exception) {
                _statusMessage.value = "AI generation failed: ${e.message}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun aiExpandNode(node: MindNode) {
        val map = _currentMindmap.value ?: return
        _isAiLoading.value = true
        val uid = Firebase.auth.currentUser?.uid ?: "local_student"
        viewModelScope.launch {
            try {
                val plans = aiService.expandNode(node.title, map.subject)
                val newNodes = mutableListOf<MindNode>()
                for ((idx, p) in plans.withIndex()) {
                    newNodes.add(
                        MindNode(
                            id = "exp_${UUID.randomUUID().toString().take(8)}",
                            mindmapId = map.id,
                            userId = uid,
                            parentId = node.id,
                            title = p.title,
                            notesMarkdown = p.notes,
                            posX = node.posX + 240f,
                            posY = node.posY + (idx * 80f) - 60f,
                            colorHex = p.colorHex,
                            icon = p.icon
                        )
                    )
                }
                repository.saveNodes(_currentNodes.value + newNodes, map)
                _statusMessage.value = "Added ${plans.size} sub-concepts to '${node.title}'"
            } catch (e: Exception) {
                _statusMessage.value = "Auto-expand error: ${e.message}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun requestSocraticChallenge(node: MindNode) {
        _isAiLoading.value = true
        viewModelScope.launch {
            try {
                val challenge = aiService.generateSocraticChallenge(node.title)
                _socraticChallenge.value = challenge
            } catch (e: Exception) {
                _statusMessage.value = "Socratic tutor unavailable"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun dismissSocraticChallenge() {
        _socraticChallenge.value = null
    }

    // --- Vector Database Search ---
    fun searchVectorDatabase(query: String) {
        _vectorQuery.value = query
        if (query.isBlank()) {
            _vectorResults.value = emptyList()
            return
        }
        val results = vectorDb.search(query, topK = 12)
        _vectorResults.value = results
    }

    // --- Export Options ---
    fun exportCurrentMap(format: String) {
        val map = _currentMindmap.value ?: return
        val nodes = _currentNodes.value
        val output = when (format.uppercase()) {
            "HTML" -> MindmapExporter.exportToHtml(map, nodes)
            "SVG" -> MindmapExporter.exportToSvg(map, nodes)
            "MARKDOWN" -> MindmapExporter.exportToMarkdown(map, nodes)
            "JSON" -> MindmapExporter.exportToJson(map, nodes)
            else -> MindmapExporter.exportToMarkdown(map, nodes)
        }
        _exportOutput.value = Pair(format.uppercase(), output)
    }

    fun dismissExport() {
        _exportOutput.value = null
    }

    fun importMindmapFromJson(jsonText: String) {
        val uid = Firebase.auth.currentUser?.uid ?: "local_student"
        try {
            val (importedMap, importedNodes) = MindmapExporter.importFromJson(jsonText, uid)
            viewModelScope.launch {
                repository.saveMindmap(importedMap)
                repository.saveNodes(importedNodes, importedMap)
                openMindmap(importedMap)
                _statusMessage.value = "Imported '${importedMap.title}' successfully!"
            }
        } catch (e: Exception) {
            _statusMessage.value = "Import failed: ${e.message}"
        }
    }

    // --- Backups & Restore ---
    fun createManualBackup(note: String = "Manual Snapshot") {
        val map = _currentMindmap.value ?: return
        viewModelScope.launch {
            repository.createAutoBackup(map.id, map.title, note)
            _statusMessage.value = "Snapshot saved!"
        }
    }

    fun restoreSnapshot(snapshot: BackupSnapshot) {
        viewModelScope.launch {
            repository.restoreBackup(snapshot)
            _statusMessage.value = "Restored snapshot from ${snapshot.note}"
        }
    }

    // --- Presentation Mode ---
    fun togglePresentationMode() {
        _isPresentationMode.value = !_isPresentationMode.value
        _presentationStep.value = 0
        if (_isPresentationMode.value && _currentNodes.value.isNotEmpty()) {
            _selectedNode.value = _currentNodes.value[0]
        }
    }

    fun nextPresentationNode() {
        val nodes = _currentNodes.value
        if (nodes.isEmpty()) return
        val next = (_presentationStep.value + 1) % nodes.size
        _presentationStep.value = next
        _selectedNode.value = nodes[next]
    }

    fun prevPresentationNode() {
        val nodes = _currentNodes.value
        if (nodes.isEmpty()) return
        val prev = if (_presentationStep.value - 1 < 0) nodes.size - 1 else _presentationStep.value - 1
        _presentationStep.value = prev
        _selectedNode.value = nodes[prev]
    }

    fun deleteMindmap(mindmapId: String) {
        viewModelScope.launch {
            repository.deleteMindmap(mindmapId)
            if (_currentMindmap.value?.id == mindmapId) {
                closeMindmap()
            }
        }
    }

    fun createBlankMindmap(title: String, subject: String, onCreated: (Mindmap) -> Unit) {
        val uid = Firebase.auth.currentUser?.uid ?: "local_student"
        val mapId = "map_${UUID.randomUUID().toString().take(8)}"
        val newMap = Mindmap(
            id = mapId,
            userId = uid,
            title = title,
            subject = subject
        )
        val rootNode = MindNode(
            id = "root_$mapId",
            mindmapId = mapId,
            userId = uid,
            title = title,
            posX = 150f,
            posY = 250f
        )
        viewModelScope.launch {
            repository.saveMindmap(newMap)
            repository.saveNode(rootNode, newMap)
            openMindmap(newMap)
            onCreated(newMap)
        }
    }

    fun clearStatus() {
        _statusMessage.value = null
    }
}

