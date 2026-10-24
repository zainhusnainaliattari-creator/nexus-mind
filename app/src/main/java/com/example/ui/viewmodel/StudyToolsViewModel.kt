package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiService
import com.example.data.model.Citation
import com.example.data.model.CornellNote
import com.example.data.model.CourseGrade
import com.example.data.model.ExamMilestone
import com.example.data.model.Flashcard
import com.example.data.model.LectureRecording
import com.example.data.model.MindNode
import com.example.data.model.PomodoroSession
import com.example.data.model.QuizQuestion
import com.example.data.repository.MindmapRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class StudyToolsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MindmapRepository(application)
    private val aiService = AiService.getInstance()

    // Flashcards
    private val _flashcards = MutableStateFlow<List<Flashcard>>(emptyList())
    val flashcards: StateFlow<List<Flashcard>> = _flashcards.asStateFlow()

    private val _currentCardIndex = MutableStateFlow(0)
    val currentCardIndex: StateFlow<Int> = _currentCardIndex.asStateFlow()

    private val _isCardFlipped = MutableStateFlow(false)
    val isCardFlipped: StateFlow<Boolean> = _isCardFlipped.asStateFlow()

    // Quizzes
    private val _quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val quizQuestions: StateFlow<List<QuizQuestion>> = _quizQuestions.asStateFlow()

    private val _quizScore = MutableStateFlow(0)
    val quizScore: StateFlow<Int> = _quizScore.asStateFlow()

    private val _currentQuizIndex = MutableStateFlow(0)
    val currentQuizIndex: StateFlow<Int> = _currentQuizIndex.asStateFlow()

    private val _selectedAnswer = MutableStateFlow<Int?>(null)
    val selectedAnswer: StateFlow<Int?> = _selectedAnswer.asStateFlow()

    private val _isAnswerSubmitted = MutableStateFlow(false)
    val isAnswerSubmitted: StateFlow<Boolean> = _isAnswerSubmitted.asStateFlow()

    // Cornell Notes
    private val _cornellNotes = MutableStateFlow<List<CornellNote>>(emptyList())
    val cornellNotes: StateFlow<List<CornellNote>> = _cornellNotes.asStateFlow()

    // Citations
    private val _citations = MutableStateFlow<List<Citation>>(emptyList())
    val citations: StateFlow<List<Citation>> = _citations.asStateFlow()

    // Exams
    val exams: StateFlow<List<ExamMilestone>> = repository.getExams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pomodoro Logs
    val pomodoroLogs: StateFlow<List<PomodoroSession>> = repository.getPomodoroSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Grades
    val courseGrades: StateFlow<List<CourseGrade>> = repository.getGrades()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Status / Loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _activeMindmapId = MutableStateFlow("")
    val activeMindmapId: StateFlow<String> = _activeMindmapId.asStateFlow()

    fun bindMindmap(mindmapId: String) {
        _activeMindmapId.value = mindmapId
        viewModelScope.launch {
            repository.getFlashcards(mindmapId).collect {
                _flashcards.value = it
            }
        }
        viewModelScope.launch {
            repository.getCornellNotes(mindmapId).collect {
                _cornellNotes.value = it
            }
        }
        viewModelScope.launch {
            repository.getCitations(mindmapId).collect {
                _citations.value = it
            }
        }
    }

    // --- Flashcard Leitner Spaced Repetition Logic ---
    fun flipCard() {
        _isCardFlipped.value = !_isCardFlipped.value
    }

    fun answerFlashcard(correct: Boolean) {
        val cards = _flashcards.value
        if (cards.isEmpty()) return
        val currentCard = cards[_currentCardIndex.value]
        val newBox = if (correct) (currentCard.box + 1).coerceAtMost(5) else 1
        val updated = currentCard.copy(
            box = newBox,
            reviewCount = currentCard.reviewCount + 1,
            nextReviewDate = System.currentTimeMillis() + (newBox * 86400000L) // box days interval
        )
        viewModelScope.launch {
            repository.updateFlashcard(updated)
            _isCardFlipped.value = false
            _currentCardIndex.value = (_currentCardIndex.value + 1) % cards.size
        }
    }

    fun generateFlashcardsFromNodes(nodes: List<MindNode>) {
        if (nodes.isEmpty()) return
        _isLoading.value = true
        viewModelScope.launch {
            val newCards = nodes.mapIndexed { idx, n ->
                Flashcard(
                    id = "card_${UUID.randomUUID().toString().take(8)}",
                    mindmapId = n.mindmapId,
                    nodeId = n.id,
                    frontPrompt = "What are the core concepts and mechanisms of '${n.title}'?",
                    backAnswer = if (n.notesMarkdown.isNotBlank()) n.notesMarkdown else "Key node '${n.title}' with priority ${n.priority}."
                )
            }
            repository.saveFlashcards(newCards)
            _isLoading.value = false
        }
    }

    // --- Quiz Arena Logic ---
    fun generateQuiz(topic: String, nodes: List<MindNode>) {
        _isLoading.value = true
        viewModelScope.launch {
            val titles = nodes.map { it.title }
            val questions = aiService.generateQuiz(topic, titles)
            _quizQuestions.value = questions
            _currentQuizIndex.value = 0
            _quizScore.value = 0
            _selectedAnswer.value = null
            _isAnswerSubmitted.value = false
            _isLoading.value = false
        }
    }

    fun selectQuizAnswer(index: Int) {
        if (!_isAnswerSubmitted.value) {
            _selectedAnswer.value = index
        }
    }

    fun submitQuizAnswer() {
        val selected = _selectedAnswer.value ?: return
        val questions = _quizQuestions.value
        if (questions.isEmpty()) return
        val q = questions[_currentQuizIndex.value]
        _isAnswerSubmitted.value = true
        if (selected == q.correctIndex) {
            _quizScore.value += 1
        }
    }

    fun nextQuizQuestion() {
        val questions = _quizQuestions.value
        if (_currentQuizIndex.value + 1 < questions.size) {
            _currentQuizIndex.value += 1
            _selectedAnswer.value = null
            _isAnswerSubmitted.value = false
        }
    }

    // --- Cornell Notes ---
    fun saveCornellNote(topic: String, cues: String, notes: String, summary: String) {
        val mid = _activeMindmapId.value.ifBlank { "general" }
        val note = CornellNote(
            id = "cornell_${UUID.randomUUID().toString().take(8)}",
            mindmapId = mid,
            topicTitle = topic.ifBlank { "Lecture Notes" },
            cuesColumn = cues,
            notesColumn = notes,
            summary = summary
        )
        viewModelScope.launch {
            repository.saveCornellNote(note)
        }
    }

    // --- Citations ---
    fun addCitation(authors: String, title: String, year: String, url: String, style: String) {
        val mid = _activeMindmapId.value.ifBlank { "general" }
        val cit = Citation(
            id = "cit_${UUID.randomUUID().toString().take(8)}",
            mindmapId = mid,
            authors = authors,
            title = title,
            year = year,
            sourceUrl = url,
            style = style
        )
        viewModelScope.launch {
            repository.saveCitation(cit)
        }
    }

    // --- Exams ---
    fun addExam(title: String, course: String, examDate: Long, targetGrade: String) {
        val mid = _activeMindmapId.value.ifBlank { "general" }
        val ex = ExamMilestone(
            id = "exam_${UUID.randomUUID().toString().take(8)}",
            mindmapId = mid,
            examTitle = title,
            courseName = course,
            examDate = examDate,
            targetGrade = targetGrade
        )
        viewModelScope.launch {
            repository.saveExam(ex)
        }
    }

    // --- Grades & GPA ---
    fun saveCourseGrade(code: String, name: String, credits: Double, currentScore: Double, targetScore: Double) {
        val grade = CourseGrade(
            courseCode = code,
            courseName = name,
            creditHours = credits,
            currentGrade = currentScore,
            targetGrade = targetScore
        )
        viewModelScope.launch {
            repository.saveGrade(grade)
        }
    }

    // --- Pomodoro ---
    fun logPomodoro(topic: String, durationMin: Int = 25) {
        val mid = _activeMindmapId.value.ifBlank { "general" }
        viewModelScope.launch {
            repository.logPomodoro(
                PomodoroSession(
                    mindmapId = mid,
                    topicName = topic,
                    durationMinutes = durationMin
                )
            )
        }
    }
}
