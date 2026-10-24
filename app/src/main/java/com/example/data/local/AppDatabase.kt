package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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

@Database(
    entities = [
        Mindmap::class,
        MindNode::class,
        BackupSnapshot::class,
        Flashcard::class,
        CornellNote::class,
        Citation::class,
        ExamMilestone::class,
        PomodoroSession::class,
        CourseGrade::class,
        LectureRecording::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mindmapDao(): MindmapDao
    abstract fun nodeDao(): NodeDao
    abstract fun backupDao(): BackupDao
    abstract fun studentDao(): StudentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mindnexus_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
