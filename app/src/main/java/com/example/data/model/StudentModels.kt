package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey
    val id: String,
    val mindmapId: String,
    val nodeId: String,
    val frontPrompt: String,
    val backAnswer: String,
    val box: Int = 1, // Leitner 1 to 5
    val nextReviewDate: Long = System.currentTimeMillis(),
    val reviewCount: Int = 0
)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val sourceNodeId: String = ""
)

@Entity(tableName = "cornell_notes")
data class CornellNote(
    @PrimaryKey
    val id: String,
    val mindmapId: String,
    val topicTitle: String,
    val cuesColumn: String = "",
    val notesColumn: String = "",
    val summary: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "citations")
data class Citation(
    @PrimaryKey
    val id: String,
    val mindmapId: String,
    val authors: String,
    val title: String,
    val year: String,
    val sourceUrl: String = "",
    val style: String = "APA" // APA, MLA, Harvard, BibTeX
)

@Entity(tableName = "exam_milestones")
data class ExamMilestone(
    @PrimaryKey
    val id: String,
    val mindmapId: String,
    val examTitle: String,
    val examDate: Long, // Epoch ms
    val courseName: String,
    val targetGrade: String = "A",
    val completed: Boolean = false
)

@Entity(tableName = "pomodoro_logs")
data class PomodoroSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mindmapId: String,
    val topicName: String,
    val durationMinutes: Int = 25,
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "course_grades")
data class CourseGrade(
    @PrimaryKey
    val courseCode: String,
    val courseName: String,
    val creditHours: Double = 3.0,
    val currentGrade: Double = 90.0,
    val targetGrade: Double = 95.0,
    val weightedTests: String = "" // JSON list of tests & scores
)

data class AudioLectureBookmark(
    val timestampSeconds: Int,
    val label: String,
    val nodeId: String
)

@Entity(tableName = "lecture_recordings")
data class LectureRecording(
    @PrimaryKey
    val id: String,
    val mindmapId: String,
    val title: String,
    val audioFilePath: String,
    val durationSeconds: Int,
    val bookmarksJson: String = "[]",
    val recordedAt: Long = System.currentTimeMillis()
)

data class SocraticChallenge(
    val prompt: String,
    val counterArgument: String,
    val followUpQuestions: List<String>
)

data class Mnemonic(
    val acronym: String,
    val expansion: List<String>,
    val rhymeOrStory: String
)
