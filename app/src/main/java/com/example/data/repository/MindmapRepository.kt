package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.export.MindmapExporter
import com.example.data.local.AppDatabase
import com.example.data.model.BackupSnapshot
import com.example.data.model.Citation
import com.example.data.model.CollaborativeRoom
import com.example.data.model.CornellNote
import com.example.data.model.CourseGrade
import com.example.data.model.ExamMilestone
import com.example.data.model.Flashcard
import com.example.data.model.LayoutAlgorithm
import com.example.data.model.LectureRecording
import com.example.data.model.MindNode
import com.example.data.model.Mindmap
import com.example.data.model.MindmapTheme
import com.example.data.model.NodeMastery
import com.example.data.model.PomodoroSession
import com.example.data.model.RoomComment
import com.example.data.vector.VectorDatabase
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

class MindmapRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val mindmapDao = db.mindmapDao()
    private val nodeDao = db.nodeDao()
    private val backupDao = db.backupDao()
    private val studentDao = db.studentDao()
    private val vectorDb = VectorDatabase.getInstance()

    // Firestore instance with required custom database ID
    private val firestore: FirebaseFirestore by lazy {
        try {
            val dbId = context.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        } catch (e: Exception) {
            FirebaseFirestore.getInstance()
        }
    }

    private val auth = Firebase.auth

    init {
        // Index all existing nodes in vector database on startup
        CoroutineScope(Dispatchers.IO).launch {
            val nodes = nodeDao.getAllNodesOnce()
            if (nodes.isEmpty()) {
                seedDefaultTemplates()
            } else {
                vectorDb.indexNodes(nodes)
            }
        }
    }

    // --- Mindmaps Flow ---
    val allMindmaps: Flow<List<Mindmap>> = mindmapDao.getAllMindmaps()

    fun getMindmap(id: String): Flow<Mindmap?> = mindmapDao.getMindmapById(id)

    fun getNodes(mindmapId: String): Flow<List<MindNode>> = nodeDao.getNodesForMindmap(mindmapId)

    fun getBackups(mindmapId: String): Flow<List<BackupSnapshot>> = backupDao.getBackupsForMindmap(mindmapId)

    suspend fun saveMindmap(mindmap: Mindmap) {
        mindmapDao.insertMindmap(mindmap)
        syncMindmapToCloudIfAuthenticated(mindmap)
    }

    suspend fun saveNode(node: MindNode, currentMap: Mindmap? = null) {
        nodeDao.insertNode(node)
        vectorDb.indexNode(node, currentMap?.subject ?: "General")
        // Update mindmap node count and trigger backup
        currentMap?.let {
            val nodes = nodeDao.getNodesForMindmapOnce(it.id)
            val updatedMap = it.copy(nodeCount = nodes.size)
            mindmapDao.updateMindmap(updatedMap)
            syncNodeToCloudIfAuthenticated(node)
        }
    }

    suspend fun saveNodes(nodes: List<MindNode>, mindmap: Mindmap) {
        nodeDao.insertNodes(nodes)
        vectorDb.indexNodes(nodes, mindmap.subject)
        mindmapDao.updateMindmap(mindmap.copy(nodeCount = nodes.size))
        for (node in nodes) {
            syncNodeToCloudIfAuthenticated(node)
        }
        createAutoBackup(mindmap.id, mindmap.title, "Auto-saved branch changes")
    }

    suspend fun deleteNode(node: MindNode, currentMap: Mindmap?) {
        nodeDao.deleteNodeById(node.id)
        vectorDb.removeNode(node.id)
        currentMap?.let {
            val nodes = nodeDao.getNodesForMindmapOnce(it.id)
            mindmapDao.updateMindmap(it.copy(nodeCount = nodes.size))
        }
    }

    suspend fun deleteMindmap(mindmapId: String) {
        nodeDao.deleteNodesForMindmap(mindmapId)
        mindmapDao.deleteMindmapById(mindmapId)
    }

    // --- Automatic Backup Snapshots ---
    suspend fun createAutoBackup(mindmapId: String, title: String, note: String = "Auto Backup"): Long {
        val nodes = nodeDao.getNodesForMindmapOnce(mindmapId)
        val json = MindmapExporter.exportToJson(
            Mindmap(id = mindmapId, title = title),
            nodes
        )
        val snapshot = BackupSnapshot(
            mindmapId = mindmapId,
            mindmapTitle = title,
            nodesJson = json,
            note = note
        )
        return backupDao.insertBackup(snapshot)
    }

    suspend fun restoreBackup(snapshot: BackupSnapshot) {
        val uid = auth.currentUser?.uid ?: "local_student"
        val (mindmap, nodes) = MindmapExporter.importFromJson(snapshot.nodesJson, uid)
        mindmapDao.insertMindmap(mindmap)
        nodeDao.deleteNodesForMindmap(mindmap.id)
        nodeDao.insertNodes(nodes)
        vectorDb.indexNodes(nodes, mindmap.subject)
    }

    // --- Auto-Layout Algorithms ---
    suspend fun applyAutoLayout(mindmapId: String, algorithm: LayoutAlgorithm) {
        val nodes = nodeDao.getNodesForMindmapOnce(mindmapId)
        if (nodes.isEmpty()) return

        val root = nodes.firstOrNull { it.parentId.isBlank() } ?: nodes.first()
        val childrenMap = nodes.groupBy { it.parentId }
        val updatedNodes = mutableListOf<MindNode>()

        when (algorithm) {
            LayoutAlgorithm.TREE -> {
                // Hierarchical tree: Left to right or top to bottom
                fun layoutSubtree(node: MindNode, x: Float, yStart: Float): Float {
                    val children = childrenMap[node.id] ?: emptyList()
                    if (children.isEmpty()) {
                        updatedNodes.add(node.copy(posX = x, posY = yStart))
                        return yStart + 90f
                    }
                    var currentY = yStart
                    for (child in children) {
                        currentY = layoutSubtree(child, x + 240f, currentY)
                    }
                    val centerY = (yStart + currentY - 90f) / 2
                    updatedNodes.add(node.copy(posX = x, posY = centerY))
                    return currentY
                }
                layoutSubtree(root, 100f, 100f)
            }
            LayoutAlgorithm.RADIAL -> {
                // Radial layout around center node
                val centerX = 600f
                val centerY = 600f
                updatedNodes.add(root.copy(posX = centerX, posY = centerY))
                val directChildren = childrenMap[root.id] ?: emptyList()
                val step = if (directChildren.isNotEmpty()) (2 * Math.PI / directChildren.size) else 0.0

                for ((idx, child) in directChildren.withIndex()) {
                    val angle = idx * step
                    val radius = 280.0
                    val cx = (centerX + radius * cos(angle)).toFloat()
                    val cy = (centerY + radius * sin(angle)).toFloat()
                    updatedNodes.add(child.copy(posX = cx, posY = cy))

                    // Secondary grandchildren
                    val subChildren = childrenMap[child.id] ?: emptyList()
                    for ((sIdx, sub) in subChildren.withIndex()) {
                        val subAngle = angle + (sIdx - (subChildren.size - 1) / 2.0) * 0.35
                        val subRadius = 480.0
                        val sx = (centerX + subRadius * cos(subAngle)).toFloat()
                        val sy = (centerY + subRadius * sin(subAngle)).toFloat()
                        updatedNodes.add(sub.copy(posX = sx, posY = sy))
                    }
                }
            }
            LayoutAlgorithm.FORCE, LayoutAlgorithm.FREEFORM -> {
                // Keep positions or slightly stagger
                var offset = 100f
                for (n in nodes) {
                    updatedNodes.add(n.copy(posX = n.posX.coerceAtLeast(50f), posY = n.posY.coerceAtLeast(50f)))
                }
            }
        }

        // Save layout
        val map = updatedNodes.associateBy { it.id }
        val finalNodes = nodes.map { map[it.id] ?: it }
        nodeDao.insertNodes(finalNodes)
    }

    // --- Cloud Sync Implementation (Firestore) ---
    private fun syncMindmapToCloudIfAuthenticated(mindmap: Mindmap) {
        val uid = auth.currentUser?.uid ?: return
        val path = "users/$uid/mindmaps/${mindmap.id}"
        val payload = mapOf(
            "userId" to uid,
            "title" to mindmap.title,
            "description" to mindmap.description,
            "theme" to mindmap.theme,
            "subject" to mindmap.subject,
            "tags" to mindmap.tags,
            "nodeCount" to mindmap.nodeCount,
            "isFavorite" to mindmap.isFavorite,
            "layoutType" to mindmap.layoutType,
            "createdAt" to (mindmap.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("users").document(uid).collection("mindmaps").document(mindmap.id)
            .set(payload)
            .addOnFailureListener { e ->
                FirestoreErrorHandler.handleError(e, OperationType.UPDATE, path)
            }
    }

    private fun syncNodeToCloudIfAuthenticated(node: MindNode) {
        val uid = auth.currentUser?.uid ?: return
        val path = "users/$uid/mindmaps/${node.mindmapId}/nodes/${node.id}"
        val payload = mapOf(
            "id" to node.id,
            "mindmapId" to node.mindmapId,
            "userId" to uid,
            "parentId" to node.parentId,
            "title" to node.title,
            "notesMarkdown" to node.notesMarkdown,
            "posX" to node.posX,
            "posY" to node.posY,
            "colorHex" to node.colorHex,
            "shape" to node.shape,
            "icon" to node.icon,
            "mastery" to node.mastery,
            "dueDate" to node.dueDate,
            "priority" to node.priority,
            "tags" to node.tags,
            "attachments" to node.attachments,
            "crossLinks" to node.crossLinks,
            "createdAt" to (node.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("users").document(uid)
            .collection("mindmaps").document(node.mindmapId)
            .collection("nodes").document(node.id)
            .set(payload)
            .addOnFailureListener { e ->
                FirestoreErrorHandler.handleError(e, OperationType.UPDATE, path)
            }
    }

    // --- Real-time Collaborative Rooms ---
    suspend fun createCollaborativeRoom(title: String, mindmapId: String): String {
        val uid = auth.currentUser?.uid ?: "student_${UUID.randomUUID().toString().take(6)}"
        val name = auth.currentUser?.displayName ?: "Student"
        val roomId = "ROOM-${(100000..999999).random()}"
        val nodes = nodeDao.getNodesForMindmapOnce(mindmapId)
        val json = MindmapExporter.exportToJson(Mindmap(id = mindmapId, title = title), nodes)

        val payload = mapOf(
            "roomId" to roomId,
            "title" to title,
            "creatorId" to uid,
            "creatorName" to name,
            "memberUids" to listOf(uid),
            "activeMindmapJson" to json,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("collaborative_rooms").document(roomId).set(payload).await()
        return roomId
    }

    fun observeCollaborativeRoom(roomId: String): Flow<CollaborativeRoom?> = flow {
        val path = "collaborative_rooms/$roomId"
        emitAll(
            firestore.collection("collaborative_rooms").document(roomId)
                .snapshots()
                .map { snap -> snap.toObject(CollaborativeRoom::class.java) }
                .catch { e ->
                    if (e is Exception) FirestoreErrorHandler.handleError(e, OperationType.GET, path)
                    emit(null)
                }
        )
    }

    suspend fun joinCollaborativeRoom(roomId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val docRef = firestore.collection("collaborative_rooms").document(roomId)
        val snap = docRef.get().await()
        if (snap.exists()) {
            docRef.update("memberUids", FieldValue.arrayUnion(uid)).await()
            return true
        }
        return false
    }

    suspend fun postRoomComment(roomId: String, content: String, nodeId: String = "") {
        val uid = auth.currentUser?.uid ?: return
        val name = auth.currentUser?.displayName ?: "Student Peer"
        val commentId = UUID.randomUUID().toString()
        val payload = mapOf(
            "id" to commentId,
            "roomId" to roomId,
            "authorId" to uid,
            "authorName" to name,
            "content" to content,
            "nodeId" to nodeId,
            "createdAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("collaborative_rooms").document(roomId)
            .collection("comments").document(commentId)
            .set(payload).await()
    }

    fun observeRoomComments(roomId: String): Flow<List<RoomComment>> = flow {
        val path = "collaborative_rooms/$roomId/comments"
        emitAll(
            firestore.collection("collaborative_rooms").document(roomId)
                .collection("comments")
                .orderBy("createdAt")
                .snapshots()
                .map { snap -> snap.toObjects(RoomComment::class.java) }
                .catch { e ->
                    if (e is Exception) FirestoreErrorHandler.handleError(e, OperationType.LIST, path)
                    emit(emptyList())
                }
        )
    }

    // --- Student Tools DAOs Access ---
    fun getFlashcards(mindmapId: String) = studentDao.getFlashcards(mindmapId)
    suspend fun saveFlashcards(cards: List<Flashcard>) = studentDao.insertFlashcards(cards)
    suspend fun updateFlashcard(card: Flashcard) = studentDao.updateFlashcard(card)

    fun getCornellNotes(mindmapId: String) = studentDao.getCornellNotes(mindmapId)
    suspend fun saveCornellNote(note: CornellNote) = studentDao.saveCornellNote(note)

    fun getCitations(mindmapId: String) = studentDao.getCitations(mindmapId)
    suspend fun saveCitation(citation: Citation) = studentDao.insertCitation(citation)

    fun getExams() = studentDao.getAllExams()
    suspend fun saveExam(exam: ExamMilestone) = studentDao.saveExam(exam)

    fun getPomodoroSessions() = studentDao.getPomodoroSessions()
    suspend fun logPomodoro(session: PomodoroSession) = studentDao.logPomodoro(session)

    fun getGrades() = studentDao.getAllGrades()
    suspend fun saveGrade(grade: CourseGrade) = studentDao.saveGrade(grade)

    fun getRecordings(mindmapId: String) = studentDao.getRecordings(mindmapId)
    suspend fun saveRecording(rec: LectureRecording) = studentDao.saveRecording(rec)

    // --- Default Preloaded Student Templates ---
    private suspend fun seedDefaultTemplates() {
        val uid = auth.currentUser?.uid ?: "local_student"

        // Template 1: Biology
        val bioMap = Mindmap(
            id = "template_bio",
            userId = uid,
            title = "Biology: Cellular Respiration",
            description = "Glycolysis, Krebs Cycle, and Oxidative Phosphorylation pathways",
            theme = MindmapTheme.CYBERPUNK.name,
            subject = "BIO-101",
            tags = listOf("Biology", "Metabolism", "Exam-Prep"),
            nodeCount = 6,
            isFavorite = true
        )
        val bioNodes = listOf(
            MindNode(id = "bio_root", mindmapId = bioMap.id, userId = uid, parentId = "", title = "Cellular Respiration", notesMarkdown = "# Cellular Respiration\nC6H12O6 + 6O2 -> 6CO2 + 6H2O + 36 ATP\nCatabolic pathways that produce ATP from biochemical energy.", posX = 100f, posY = 300f, colorHex = "#00F0FF", icon = "🔬", mastery = NodeMastery.MASTERED.name),
            MindNode(id = "bio_glyco", mindmapId = bioMap.id, userId = uid, parentId = "bio_root", title = "1. Glycolysis", notesMarkdown = "### Anaerobic breakdown in cytoplasm\n- Net yield: 2 ATP + 2 NADH\n- Glucose split into 2 Pyruvate\n- Key enzyme: Phosphofructokinase (PFK)", posX = 380f, posY = 150f, colorHex = "#00FF9F", icon = "⚡", mastery = NodeMastery.LEARNING.name),
            MindNode(id = "bio_krebs", mindmapId = bioMap.id, userId = uid, parentId = "bio_root", title = "2. Citric Acid Cycle", notesMarkdown = "### Matrix of Mitochondria\n- Acetyl-CoA joins Oxaloacetate\n- Produces 2 ATP, 6 NADH, 2 FADH2 per glucose\n- Releases CO2 waste", posX = 380f, posY = 300f, colorHex = "#FF0055", icon = "🔄", mastery = NodeMastery.LEARNING.name),
            MindNode(id = "bio_etc", mindmapId = bioMap.id, userId = uid, parentId = "bio_root", title = "3. Electron Transport Chain", notesMarkdown = "### Inner mitochondrial cristae\n- Proton gradient across membrane\n- ATP Synthase chemiosmosis\n- Oxygen is the terminal electron acceptor", posX = 380f, posY = 450f, colorHex = "#BD93F9", icon = "🔋", mastery = NodeMastery.UNLEARNED.name),
            MindNode(id = "bio_atp", mindmapId = bioMap.id, userId = uid, parentId = "bio_etc", title = "ATP Synthase Mechanism", notesMarkdown = "Rotary motor powered by proton motive force (H+ gradient).", posX = 660f, posY = 450f, colorHex = "#FF79C6", icon = "⚙️", mastery = NodeMastery.UNLEARNED.name),
            MindNode(id = "bio_ferment", mindmapId = bioMap.id, userId = uid, parentId = "bio_glyco", title = "Fermentation (Anaerobic)", notesMarkdown = "Lactate or ethanol production when O2 is absent to regenerate NAD+.", posX = 660f, posY = 150f, colorHex = "#F1FA8C", icon = "🧪", mastery = NodeMastery.LEARNING.name)
        )

        // Template 2: Computer Science
        val csMap = Mindmap(
            id = "template_cs",
            userId = uid,
            title = "CS: Data Structures & Graphs",
            description = "Trees, Graph Algorithms, Complexity & Big-O",
            theme = MindmapTheme.OBSIDIAN.name,
            subject = "CS-201",
            tags = listOf("Coding", "Algorithms", "Interview"),
            nodeCount = 5,
            isFavorite = true
        )
        val csNodes = listOf(
            MindNode(id = "cs_root", mindmapId = csMap.id, userId = uid, parentId = "", title = "Graph Theory & Trees", notesMarkdown = "# Graph Data Structures\nG = (V, E)\nRepresentations: Adjacency List vs Adjacency Matrix.", posX = 100f, posY = 250f, colorHex = "#58A6FF", icon = "🧠", mastery = NodeMastery.MASTERED.name),
            MindNode(id = "cs_bfs", mindmapId = csMap.id, userId = uid, parentId = "cs_root", title = "Breadth-First Search (BFS)", notesMarkdown = "### Queue-based Level Order Traversal\n- Time: O(V + E)\n- Space: O(V)\n- Shortest path in unweighted graphs", posX = 380f, posY = 160f, colorHex = "#3FB950", icon = "🌊", mastery = NodeMastery.MASTERED.name),
            MindNode(id = "cs_dfs", mindmapId = csMap.id, userId = uid, parentId = "cs_root", title = "Depth-First Search (DFS)", notesMarkdown = "### Stack / Recursive Backtracking\n- Topological Sort\n- Cycle detection in DAG\n- Connected components", posX = 380f, posY = 340f, colorHex = "#D29922", icon = "⛏️", mastery = NodeMastery.LEARNING.name),
            MindNode(id = "cs_dijkstra", mindmapId = csMap.id, userId = uid, parentId = "cs_bfs", title = "Dijkstra Shortest Path", notesMarkdown = "PriorityQueue min-heap implementation for non-negative weights: O((V+E) log V).", posX = 660f, posY = 160f, colorHex = "#F778BA", icon = "🧭", mastery = NodeMastery.LEARNING.name),
            MindNode(id = "cs_tree", mindmapId = csMap.id, userId = uid, parentId = "cs_dfs", title = "AVL & Red-Black Trees", notesMarkdown = "Self-balancing binary search trees guaranteeing O(log N) lookup, insertion, and deletion.", posX = 660f, posY = 340f, colorHex = "#BC8CFF", icon = "🌲", mastery = NodeMastery.UNLEARNED.name)
        )

        mindmapDao.insertMindmaps(listOf(bioMap, csMap))
        nodeDao.insertNodes(bioNodes + csNodes)
        vectorDb.indexNodes(bioNodes, bioMap.subject)
        vectorDb.indexNodes(csNodes, csMap.subject)
    }
}
