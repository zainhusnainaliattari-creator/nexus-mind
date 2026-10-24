package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MindmapTheme(val displayName: String, val darkBg: Long, val cardBg: Long, val primaryColor: Long, val accentColor: Long, val textColor: Long) {
    OBSIDIAN("Obsidian Dark", 0xFF0F1117, 0xFF161B22, 0xFF58A6FF, 0xFF3FB950, 0xFFE6EDF3),
    DRACULA("Dracula", 0xFF282A36, 0xFF44475A, 0xFFBD93F9, 0xFFFF79C6, 0xFFF8F8F2),
    CYBERPUNK("Cyberpunk Neon", 0xFF0D0221, 0xFF190934, 0xFF00F0FF, 0xFFFF0055, 0xFF00FF9F),
    NORD("Nord Frost", 0xFF2E3440, 0xFF3B4252, 0xFF88C0D0, 0xFF81A1C1, 0xFFECEFF4),
    SOLARIZED("Solarized Dark", 0xFF002B36, 0xFF073642, 0xFF268BD2, 0xFF2AA198, 0xFF93A1A1),
    CLEAN_LIGHT("Clean Light", 0xFFF8FAFC, 0xFFFFFFFF, 0xFF2563EB, 0xFF0D9488, 0xFF1E293B)
}

enum class NodeShape(val displayName: String) {
    ROUNDED("Rounded Box"),
    PILL("Pill"),
    RECTANGLE("Rectangle"),
    DIAMOND("Diamond"),
    HEXAGON("Hexagon"),
    CLOUD("Cloud")
}

enum class NodeMastery(val displayName: String, val colorHex: String) {
    UNLEARNED("Unlearned", "#EF4444"),
    LEARNING("Reviewing", "#F59E0B"),
    MASTERED("Mastered", "#10B981")
}

enum class NodePriority(val displayName: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

enum class LayoutAlgorithm(val displayName: String) {
    TREE("Hierarchical Tree"),
    RADIAL("Radial Mindmap"),
    FORCE("Organic Force"),
    FREEFORM("Freeform Canvas")
}

@Entity(tableName = "mindmaps")
@IgnoreExtraProperties
data class Mindmap(
    @PrimaryKey
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val title: String = "Untitled Mindmap",
    val description: String = "",
    val theme: String = MindmapTheme.OBSIDIAN.name,
    val subject: String = "General",
    val tags: List<String> = emptyList(),
    val nodeCount: Int = 1,
    val isFavorite: Boolean = false,
    val layoutType: String = LayoutAlgorithm.TREE.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Entity(tableName = "nodes")
@IgnoreExtraProperties
data class MindNode(
    @PrimaryKey
    @DocumentId
    val id: String = "",
    val mindmapId: String = "",
    val userId: String = "",
    val parentId: String = "",
    val title: String = "New Concept",
    val notesMarkdown: String = "",
    val posX: Float = 0f,
    val posY: Float = 0f,
    val colorHex: String = "#58A6FF",
    val shape: String = NodeShape.ROUNDED.name,
    val icon: String = "💡",
    val mastery: String = NodeMastery.UNLEARNED.name,
    val dueDate: String = "",
    val priority: String = NodePriority.MEDIUM.name,
    val tags: List<String> = emptyList(),
    val attachments: List<String> = emptyList(),
    val crossLinks: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Entity(tableName = "backup_snapshots")
data class BackupSnapshot(
    @PrimaryKey(autoGenerate = true)
    val snapshotId: Long = 0,
    val mindmapId: String,
    val mindmapTitle: String,
    val timestamp: Long = System.currentTimeMillis(),
    val nodesJson: String,
    val note: String = "Automatic Backup"
)

@IgnoreExtraProperties
data class CollaborativeRoom(
    @DocumentId
    val roomId: String = "",
    val title: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val memberUids: List<String> = emptyList(),
    val activeMindmapJson: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RoomComment(
    @DocumentId
    val id: String = "",
    val roomId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val nodeId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
