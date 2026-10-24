package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MindNode
import com.example.data.model.Mindmap
import com.example.ui.viewmodel.StudyToolsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyToolsScreen(
    mindmap: Mindmap,
    nodes: List<MindNode>,
    studyToolsViewModel: StudyToolsViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    LaunchedEffect(mindmap.id) {
        studyToolsViewModel.bindMindmap(mindmap.id)
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Flashcards" to Icons.Default.Style,
        "Quiz Arena" to Icons.Default.Quiz,
        "Cornell Notes" to Icons.Default.MenuBook,
        "Citations" to Icons.Default.FormatQuote,
        "Exam Planner" to Icons.Default.Event,
        "GPA Calc" to Icons.Default.Calculate
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Student Study Suite", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(mindmap.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, (label, icon) ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(label)
                            }
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> FlashcardsTab(nodes, studyToolsViewModel)
                    1 -> QuizArenaTab(mindmap, nodes, studyToolsViewModel)
                    2 -> CornellNotesTab(studyToolsViewModel)
                    3 -> CitationsTab(studyToolsViewModel)
                    4 -> ExamPlannerTab(studyToolsViewModel)
                    5 -> GpaCalculatorTab(studyToolsViewModel)
                }
            }
        }
    }
}

@Composable
fun FlashcardsTab(nodes: List<MindNode>, viewModel: StudyToolsViewModel) {
    val flashcards by viewModel.flashcards.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentCardIndex.collectAsStateWithLifecycle()
    val isFlipped by viewModel.isCardFlipped.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🗂️ Leitner Spaced Repetition", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { viewModel.generateFlashcardsFromNodes(nodes) },
                enabled = !isLoading
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate from Map")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (flashcards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No flashcards for this mindmap yet.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap 'Generate from Map' to convert concepts into Leitner review cards!", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            val card = flashcards.getOrNull(currentIndex) ?: flashcards[0]

            Text(
                "Card ${currentIndex + 1} of ${flashcards.size} • Leitner Box ${card.box}/5",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Flippable Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clickable { viewModel.flipCard() }
                    .testTag("flashcard_flip_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFlipped) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = if (isFlipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = if (isFlipped) "ANSWER (Tap to flip)" else "QUESTION (Tap to flip)",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isFlipped) card.backAnswer else card.frontPrompt,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Leitner recall buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = { viewModel.answerFlashcard(correct = false) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Text("❌ Review Again (Box 1)")
                }
                Button(
                    onClick = { viewModel.answerFlashcard(correct = true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("✓ Mastered (Box +1)")
                }
            }
        }
    }
}

@Composable
fun QuizArenaTab(mindmap: Mindmap, nodes: List<MindNode>, viewModel: StudyToolsViewModel) {
    val questions by viewModel.quizQuestions.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentQuizIndex.collectAsStateWithLifecycle()
    val score by viewModel.quizScore.collectAsStateWithLifecycle()
    val selectedAnswer by viewModel.selectedAnswer.collectAsStateWithLifecycle()
    val isSubmitted by viewModel.isAnswerSubmitted.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎯 Practice Quiz Arena", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { viewModel.generateQuiz(mindmap.title, nodes) },
                enabled = !isLoading
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate Quiz")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (questions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No practice questions yet.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap 'Generate Quiz' to create active recall test questions from this mindmap!", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            val q = questions[currentIndex]

            Text(
                "Question ${currentIndex + 1} of ${questions.size} • Score: $score",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = q.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            q.options.forEachIndexed { idx, opt ->
                val isSelected = selectedAnswer == idx
                val isCorrect = idx == q.correctIndex

                val btnBg = when {
                    isSubmitted && isCorrect -> Color(0xFF10B981).copy(alpha = 0.25f)
                    isSubmitted && isSelected && !isCorrect -> Color(0xFFEF4444).copy(alpha = 0.25f)
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(enabled = !isSubmitted) { viewModel.selectQuizAnswer(idx) },
                    colors = CardDefaults.cardColors(containerColor = btnBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${('A'.code + idx).toChar()}.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(opt, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (isSubmitted) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Explanation:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text(q.explanation, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isSubmitted) {
                    Button(
                        onClick = { viewModel.submitQuizAnswer() },
                        enabled = selectedAnswer != null
                    ) {
                        Text("Submit Answer")
                    }
                } else {
                    Button(onClick = { viewModel.nextQuizQuestion() }) {
                        Text(if (currentIndex + 1 < questions.size) "Next Question →" else "Finish Quiz")
                    }
                }
            }
        }
    }
}

@Composable
fun CornellNotesTab(viewModel: StudyToolsViewModel) {
    val cornellNotes by viewModel.cornellNotes.collectAsStateWithLifecycle()
    var topic by remember { mutableStateOf("") }
    var cues by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("📝 Cornell Notes System", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            label = { Text("Topic / Chapter") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = cues,
                onValueChange = { cues = it },
                label = { Text("Cues & Keywords") },
                placeholder = { Text("Key questions\nFormulas\nVocabulary") },
                modifier = Modifier
                    .weight(0.4f)
                    .height(180.dp)
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Main Lecture Notes") },
                placeholder = { Text("Core explanations, diagrams, step-by-step logic...") },
                modifier = Modifier
                    .weight(0.6f)
                    .height(180.dp)
            )
        }

        OutlinedTextField(
            value = summary,
            onValueChange = { summary = it },
            label = { Text("Summary (Bottom Row)") },
            placeholder = { Text("Brief synthesis of the entire lesson in 2-3 sentences...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )

        Button(
            onClick = {
                viewModel.saveCornellNote(topic, cues, notes, summary)
                topic = ""
                cues = ""
                notes = ""
                summary = ""
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Save Cornell Note")
        }

        if (cornellNotes.isNotEmpty()) {
            Text("Saved Cornell Notes (${cornellNotes.size}):", fontWeight = FontWeight.Bold)
            cornellNotes.forEach { cn ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(cn.topicTitle, fontWeight = FontWeight.Bold)
                        Text("Cues: ${cn.cuesColumn.take(40)}...", style = MaterialTheme.typography.bodySmall)
                        Text("Summary: ${cn.summary}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun CitationsTab(viewModel: StudyToolsViewModel) {
    val citations by viewModel.citations.collectAsStateWithLifecycle()
    var authors by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2024") }
    var url by remember { mutableStateOf("") }
    var style by remember { mutableStateOf("APA") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("📖 Academic Bibliography & Citations", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("APA", "MLA", "BibTeX", "Harvard").forEach { s ->
                FilterChip(
                    selected = style == s,
                    onClick = { style = s },
                    label = { Text(s) }
                )
            }
        }

        OutlinedTextField(
            value = authors,
            onValueChange = { authors = it },
            label = { Text("Author(s)") },
            placeholder = { Text("e.g. Watson, J. D., & Crick, F. H.") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title of Paper / Book") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = year,
                onValueChange = { year = it },
                label = { Text("Year") },
                modifier = Modifier.weight(0.4f)
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Source DOI / URL") },
                modifier = Modifier.weight(0.6f)
            )
        }

        Button(
            onClick = {
                if (title.isNotBlank()) {
                    viewModel.addCitation(authors, title, year, url, style)
                    title = ""
                    authors = ""
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Add Citation")
        }

        Text("Saved Citations (${citations.size}):", fontWeight = FontWeight.Bold)
        citations.forEach { cit ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val formatted = when (cit.style) {
                        "MLA" -> "${cit.authors}. \"${cit.title}.\" (${cit.year})."
                        "BibTeX" -> "@article{ref,\n  author = {${cit.authors}},\n  title = {${cit.title}},\n  year = {${cit.year}}\n}"
                        else -> "${cit.authors} (${cit.year}). ${cit.title}. ${cit.sourceUrl}"
                    }
                    Text(formatted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ExamPlannerTab(viewModel: StudyToolsViewModel) {
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var daysUntil by remember { mutableStateOf("14") }
    var targetGrade by remember { mutableStateOf("A") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("📅 Exam Countdown & Milestones", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Exam Name (e.g. Midterm 1)") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = course,
                onValueChange = { course = it },
                label = { Text("Course Code") },
                modifier = Modifier.weight(0.5f)
            )
            OutlinedTextField(
                value = daysUntil,
                onValueChange = { daysUntil = it },
                label = { Text("Days Left") },
                modifier = Modifier.weight(0.3f)
            )
            OutlinedTextField(
                value = targetGrade,
                onValueChange = { targetGrade = it },
                label = { Text("Target") },
                modifier = Modifier.weight(0.2f)
            )
        }

        Button(
            onClick = {
                val days = daysUntil.toLongOrNull() ?: 7L
                val futureEpoch = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days)
                viewModel.addExam(title, course, futureEpoch, targetGrade)
                title = ""
                course = ""
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("+ Add Milestone")
        }

        Text("Active Exam Countdowns:", fontWeight = FontWeight.Bold)
        exams.forEach { ex ->
            val remainingDays = ((ex.examDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(ex.examTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(ex.courseName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (remainingDays < 5) Color(0xFFEF4444).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "$remainingDays Days",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            color = if (remainingDays < 5) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GpaCalculatorTab(viewModel: StudyToolsViewModel) {
    val grades by viewModel.courseGrades.collectAsStateWithLifecycle()
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("3") }
    var currentScore by remember { mutableStateOf("92") }

    val calculatedGpa = remember(grades) {
        if (grades.isEmpty()) 3.85
        else {
            val totalPoints = grades.sumOf {
                val point = when {
                    it.currentGrade >= 93 -> 4.0
                    it.currentGrade >= 90 -> 3.7
                    it.currentGrade >= 87 -> 3.3
                    it.currentGrade >= 83 -> 3.0
                    it.currentGrade >= 80 -> 2.7
                    else -> 2.0
                }
                point * it.creditHours
            }
            val totalCredits = grades.sumOf { it.creditHours }
            if (totalCredits > 0) totalPoints / totalCredits else 4.0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("📊 GPA & Syllabus Weight Calculator", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Projected Cumulative GPA", style = MaterialTheme.typography.labelMedium)
                Text(
                    String.format("%.2f", calculatedGpa),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Course Code") },
                modifier = Modifier.weight(0.4f)
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Course Title") },
                modifier = Modifier.weight(0.6f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = credits,
                onValueChange = { credits = it },
                label = { Text("Credits") },
                modifier = Modifier.weight(0.5f)
            )
            OutlinedTextField(
                value = currentScore,
                onValueChange = { currentScore = it },
                label = { Text("Current Score (%)") },
                modifier = Modifier.weight(0.5f)
            )
        }

        Button(
            onClick = {
                if (code.isNotBlank()) {
                    viewModel.saveCourseGrade(
                        code,
                        name.ifBlank { code },
                        credits.toDoubleOrNull() ?: 3.0,
                        currentScore.toDoubleOrNull() ?: 90.0,
                        95.0
                    )
                    code = ""
                    name = ""
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("+ Add Course")
        }

        Text("Course Grades List (${grades.size}):", fontWeight = FontWeight.Bold)
        grades.forEach { cg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(cg.courseCode, fontWeight = FontWeight.Bold)
                        Text(cg.courseName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${cg.currentGrade}% (${cg.creditHours} cr)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
