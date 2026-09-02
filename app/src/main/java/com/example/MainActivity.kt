package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.JournalType
import com.example.receiver.NotificationUtils
import com.example.ui.components.AiPlannerBottomSheet
import com.example.ui.components.PlannerTopBar
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.FocusAdhdScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.MineScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.TodoPlannerTheme
import com.example.ui.viewmodel.NavScreen
import com.example.ui.viewmodel.PlannerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PlannerViewModel by viewModels()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled gracefully
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationUtils.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val currentTheme by viewModel.currentTheme.collectAsState()

            TodoPlannerTheme(appTheme = currentTheme) {
                PlannerApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PlannerApp(viewModel: PlannerViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val filteredTasks by viewModel.filteredTasks.collectAsState()
    val currentFilterTab by viewModel.currentFilterTab.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiScheduleResult by viewModel.aiResultSchedule.collectAsState()
    val aiMessage by viewModel.aiMessage.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var showAiPlannerSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(aiMessage) {
        aiMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            PlannerTopBar(
                selectedCategory = selectedCategory,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                isSearchActive = isSearchActive,
                onToggleSearch = {
                    isSearchActive = !isSearchActive
                    if (!isSearchActive) viewModel.setSearchQuery("")
                },
                onOpenAiPlanner = { showAiPlannerSheet = true }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavScreen.entries.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setScreen(screen) },
                        icon = {
                            Text(text = screen.icon, fontSize = 20.sp)
                        },
                        label = {
                            Text(
                                text = screen.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("nav_item_${screen.name}")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                NavScreen.TASKS -> {
                    TasksScreen(
                        viewModel = viewModel,
                        tasks = filteredTasks,
                        currentTab = currentFilterTab,
                        selectedCategory = selectedCategory,
                        onOpenAiPlanner = { showAiPlannerSheet = true }
                    )
                }
                NavScreen.CALENDAR -> {
                    CalendarScreen(
                        viewModel = viewModel,
                        onOpenAiPlanner = { showAiPlannerSheet = true }
                    )
                }
                NavScreen.FOCUS_ADHD -> {
                    FocusAdhdScreen(viewModel = viewModel)
                }
                NavScreen.JOURNAL -> {
                    JournalScreen(viewModel = viewModel)
                }
                NavScreen.MINE -> {
                    MineScreen(viewModel = viewModel)
                }
            }
        }
    }

    // AI Planner Sheet
    if (showAiPlannerSheet) {
        AiPlannerBottomSheet(
            selectedDate = selectedDate,
            tasksForDate = allTasks.filter { it.dueDate == selectedDate },
            isLoading = isAiLoading,
            aiScheduleResult = aiScheduleResult,
            onGenerateSchedule = { viewModel.generateAiDaySchedule(selectedDate) },
            onParseBrainDump = { dumpText ->
                viewModel.parseBrainDumpWithAi(dumpText, selectedDate) { count ->
                    // Task parsed callback
                }
            },
            onSaveScheduleAsJournal = { scheduleText ->
                viewModel.saveJournal(
                    title = "AI Schedule: $selectedDate",
                    content = scheduleText,
                    templateType = JournalType.DIARY,
                    mood = "GREAT",
                    tags = "AI Plan, Schedule",
                    date = selectedDate
                )
            },
            onDismiss = {
                showAiPlannerSheet = false
                viewModel.clearAiResult()
            }
        )
    }
}
