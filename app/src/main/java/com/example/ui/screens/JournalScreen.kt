package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JournalEntry
import com.example.data.model.JournalType
import com.example.ui.viewmodel.PlannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(viewModel: PlannerViewModel) {
    val journals by viewModel.allJournals.collectAsState()
    var selectedFilterType by remember { mutableStateOf<String?>("ALL") }
    var showNewEntrySheet by remember { mutableStateOf(false) }

    val filteredJournals = remember(journals, selectedFilterType) {
        if (selectedFilterType == null || selectedFilterType == "ALL") {
            journals
        } else {
            journals.filter { it.templateType == selectedFilterType }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📓 Journal, Diary & Notes", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Capture thoughts, reflections, gratitude, and BuJo logs", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Template Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilterType == "ALL",
                        onClick = { selectedFilterType = "ALL" },
                        label = { Text("✨ All Notes") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                items(JournalType.entries) { jt ->
                    FilterChip(
                        selected = selectedFilterType == jt.name,
                        onClick = { selectedFilterType = jt.name },
                        label = { Text("${jt.icon} ${jt.label}") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Entries List
            if (filteredJournals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✍️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No journal entries yet", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Tap + to write a gratitude note, daily diary, or brain dump.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredJournals, key = { it.id }) { entry ->
                        val template = try { JournalType.valueOf(entry.templateType) } catch (_: Exception) { JournalType.DIARY }
                        val moodEmoji = when (entry.mood) {
                            "GREAT" -> "🤩 Great"
                            "GOOD" -> "😊 Good"
                            "CALM" -> "🧘 Calm"
                            "TIRED" -> "😴 Tired"
                            "ANXIOUS" -> "💭 Anxious"
                            else -> "✨"
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "${template.icon} ${template.label}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(text = moodEmoji, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = entry.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        IconButton(
                                            onClick = { viewModel.deleteJournal(entry) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(text = entry.title, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = entry.content,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (entry.tags.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "🏷️ ${entry.tags}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showNewEntrySheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("fab_add_journal"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "New Entry", modifier = Modifier.size(26.dp))
        }

        if (showNewEntrySheet) {
            NewJournalEntrySheet(
                todayDate = viewModel.todayDateString,
                onDismiss = { showNewEntrySheet = false },
                onSave = { title, content, templateType, mood, tags ->
                    viewModel.saveJournal(title, content, templateType, mood, tags)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewJournalEntrySheet(
    todayDate: String,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, templateType: JournalType, mood: String, tags: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTemplate by remember { mutableStateOf(JournalType.GRATITUDE) }
    var title by remember { mutableStateOf(JournalType.GRATITUDE.prompt) }
    var content by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf("GOOD") }
    var tags by remember { mutableStateOf("") }

    val moods = listOf("GREAT" to "🤩 Great", "GOOD" to "😊 Good", "CALM" to "🧘 Calm", "TIRED" to "😴 Tired", "ANXIOUS" to "💭 Anxious")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("New Journal Note", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Template selector
            Text("Template", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(JournalType.entries) { jt ->
                    FilterChip(
                        selected = selectedTemplate == jt,
                        onClick = {
                            selectedTemplate = jt
                            title = jt.prompt
                        },
                        label = { Text("${jt.icon} ${jt.label}", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mood selector
            Text("How are you feeling?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                moods.forEach { (moodKey, label) ->
                    FilterChip(
                        selected = selectedMood == moodKey,
                        onClick = { selectedMood = moodKey },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title / Prompt") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Content
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("Write your thoughts freely...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("journal_content_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tags
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (comma separated, e.g. Focus, Wellness)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (title.isNotBlank() || content.isNotBlank()) {
                        onSave(title, content, selectedTemplate, selectedMood, tags)
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_journal"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save Entry", fontWeight = FontWeight.Bold)
            }
        }
    }
}
