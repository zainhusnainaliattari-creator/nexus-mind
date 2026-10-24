package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BackupSnapshot
import com.example.data.model.Citation
import com.example.data.model.CornellNote
import com.example.data.model.CourseGrade
import com.example.data.model.ExamMilestone
import com.example.data.model.Flashcard
import com.example.data.model.LectureRecording
import com.example.data.model.MindNode
import com.example.data.model.Mindmap
import com.example.data.model.PomodoroSession
import kotlinx.coroutines.flow.Flow

@Dao
interface MindmapDao {
    @Query("SELECT * FROM mindmaps ORDER BY isFavorite DESC, updatedAt DESC")
    fun getAllMindmaps(): Flow<List<Mindmap>>

    @Query("SELECT * FROM mindmaps WHERE id = :id LIMIT 1")
    fun getMindmapById(id: String): Flow<Mindmap?>

    @Query("SELECT * FROM mindmaps WHERE id = :id LIMIT 1")
    suspend fun getMindmapByIdOnce(id: String): Mindmap?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMindmap(mindmap: Mindmap)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMindmaps(mindmaps: List<Mindmap>)

    @Update
    suspend fun updateMindmap(mindmap: Mindmap)

    @Query("DELETE FROM mindmaps WHERE id = :id")
    suspend fun deleteMindmapById(id: String)
}

@Dao
interface NodeDao {
    @Query("SELECT * FROM nodes WHERE mindmapId = :mindmapId ORDER BY id ASC")
    fun getNodesForMindmap(mindmapId: String): Flow<List<MindNode>>

    @Query("SELECT * FROM nodes WHERE mindmapId = :mindmapId")
    suspend fun getNodesForMindmapOnce(mindmapId: String): List<MindNode>

    @Query("SELECT * FROM nodes")
    suspend fun getAllNodesOnce(): List<MindNode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: MindNode)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<MindNode>)

    @Update
    suspend fun updateNode(node: MindNode)

    @Query("DELETE FROM nodes WHERE id = :id")
    suspend fun deleteNodeById(id: String)

    @Query("DELETE FROM nodes WHERE mindmapId = :mindmapId")
    suspend fun deleteNodesForMindmap(mindmapId: String)
}

@Dao
interface BackupDao {
    @Query("SELECT * FROM backup_snapshots WHERE mindmapId = :mindmapId ORDER BY timestamp DESC")
    fun getBackupsForMindmap(mindmapId: String): Flow<List<BackupSnapshot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: BackupSnapshot): Long

    @Query("DELETE FROM backup_snapshots WHERE snapshotId = :id")
    suspend fun deleteBackup(id: Long)
}

@Dao
interface StudentDao {
    // Flashcards
    @Query("SELECT * FROM flashcards WHERE mindmapId = :mindmapId")
    fun getFlashcards(mindmapId: String): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<Flashcard>)

    @Update
    suspend fun updateFlashcard(flashcard: Flashcard)

    // Cornell Notes
    @Query("SELECT * FROM cornell_notes WHERE mindmapId = :mindmapId")
    fun getCornellNotes(mindmapId: String): Flow<List<CornellNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCornellNote(note: CornellNote)

    // Citations
    @Query("SELECT * FROM citations WHERE mindmapId = :mindmapId")
    fun getCitations(mindmapId: String): Flow<List<Citation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCitation(citation: Citation)

    // Exam Milestones
    @Query("SELECT * FROM exam_milestones ORDER BY examDate ASC")
    fun getAllExams(): Flow<List<ExamMilestone>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveExam(exam: ExamMilestone)

    // Pomodoro Sessions
    @Query("SELECT * FROM pomodoro_logs ORDER BY completedAt DESC")
    fun getPomodoroSessions(): Flow<List<PomodoroSession>>

    @Insert
    suspend fun logPomodoro(session: PomodoroSession)

    // Course Grades
    @Query("SELECT * FROM course_grades")
    fun getAllGrades(): Flow<List<CourseGrade>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGrade(grade: CourseGrade)

    // Lecture Recordings
    @Query("SELECT * FROM lecture_recordings WHERE mindmapId = :mindmapId ORDER BY recordedAt DESC")
    fun getRecordings(mindmapId: String): Flow<List<LectureRecording>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRecording(recording: LectureRecording)
}
