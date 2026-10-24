package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Mindmap
import com.example.data.model.MindmapTheme
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CollaborationScreen
import com.example.ui.screens.MindmapEditorScreen
import com.example.ui.screens.MindmapListScreen
import com.example.ui.screens.StudyToolsScreen
import com.example.ui.theme.MindNexusTheme
import com.example.ui.viewmodel.MindmapViewModel
import com.example.ui.viewmodel.StudyToolsViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

enum class AppScreen {
    DASHBOARD,
    EDITOR,
    STUDY_TOOLS,
    COLLABORATION
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MindNexusApp()
        }
    }
}

@Composable
fun MindNexusApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isOfflineMode by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null && !isOfflineMode) {
        MindNexusTheme(selectedTheme = MindmapTheme.OBSIDIAN) {
            Surface(modifier = Modifier.fillMaxSize()) {
                AuthScreen(
                    onAuthSuccess = {
                        currentUser = Firebase.auth.currentUser
                    },
                    onContinueOffline = {
                        isOfflineMode = true
                    }
                )
            }
        }
    } else {
        MainContent(
            onSignOut = {
                Firebase.auth.signOut()
                currentUser = null
                isOfflineMode = false
            }
        )
    }
}

@Composable
fun MainContent(
    onSignOut: () -> Unit,
    mindmapViewModel: MindmapViewModel = viewModel(),
    studyToolsViewModel: StudyToolsViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var collabMindmapId by remember { mutableStateOf("") }
    var collabMindmapTitle by remember { mutableStateOf("") }

    val activeMindmap by mindmapViewModel.currentMindmap.collectAsStateWithLifecycle()
    val activeNodes by mindmapViewModel.currentNodes.collectAsStateWithLifecycle()

    val currentTheme = remember(activeMindmap?.theme) {
        try {
            activeMindmap?.theme?.let { MindmapTheme.valueOf(it) } ?: MindmapTheme.OBSIDIAN
        } catch (_: Exception) {
            MindmapTheme.OBSIDIAN
        }
    }

    MindNexusTheme(selectedTheme = currentTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> {
                    MindmapListScreen(
                        viewModel = mindmapViewModel,
                        onSelectMindmap = { map ->
                            currentScreen = AppScreen.EDITOR
                        },
                        onSignOut = onSignOut
                    )
                }
                AppScreen.EDITOR -> {
                    if (activeMindmap != null) {
                        MindmapEditorScreen(
                            mindmap = activeMindmap!!,
                            viewModel = mindmapViewModel,
                            onNavigateBack = {
                                currentScreen = AppScreen.DASHBOARD
                            },
                            onOpenStudyTools = {
                                currentScreen = AppScreen.STUDY_TOOLS
                            },
                            onOpenCollaboration = { mid ->
                                collabMindmapId = mid
                                collabMindmapTitle = activeMindmap?.title ?: "Study Mindmap"
                                currentScreen = AppScreen.COLLABORATION
                            }
                        )
                    } else {
                        currentScreen = AppScreen.DASHBOARD
                    }
                }
                AppScreen.STUDY_TOOLS -> {
                    if (activeMindmap != null) {
                        StudyToolsScreen(
                            mindmap = activeMindmap!!,
                            nodes = activeNodes,
                            studyToolsViewModel = studyToolsViewModel,
                            onNavigateBack = {
                                currentScreen = AppScreen.EDITOR
                            }
                        )
                    } else {
                        currentScreen = AppScreen.DASHBOARD
                    }
                }
                AppScreen.COLLABORATION -> {
                    CollaborationScreen(
                        initialMindmapId = collabMindmapId,
                        mindmapTitle = collabMindmapTitle,
                        repository = mindmapViewModel.repository,
                        onNavigateBack = {
                            currentScreen = AppScreen.EDITOR
                        }
                    )
                }
            }
        }
    }
}
