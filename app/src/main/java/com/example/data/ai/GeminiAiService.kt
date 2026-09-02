package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.Priority
import com.example.data.model.SubTask
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiRestApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: okhttp3.RequestBody
    ): okhttp3.ResponseBody
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    val api: GeminiRestApi by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiRestApi::class.java)
    }
}

class AiPlannerEngine {

    suspend fun askGemini(prompt: String, systemInstruction: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("API key not configured"))
        }

        try {
            val jsonRoot = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            jsonRoot.put("contents", contentsArray)

            if (!systemInstruction.isNullOrBlank()) {
                val sysObj = JSONObject()
                val sysParts = JSONArray()
                val sysPart = JSONObject()
                sysPart.put("text", systemInstruction)
                sysParts.put(sysPart)
                sysObj.put("parts", sysParts)
                jsonRoot.put("systemInstruction", sysObj)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonRoot.toString().toRequestBody(mediaType)

            val responseBody = GeminiClient.api.generateContent(apiKey, requestBody)
            val responseString = responseBody.string()
            val resJson = JSONObject(responseString)

            val candidates = resJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    return@withContext Result.success(text)
                }
            }
            Result.failure(Exception("Empty candidate response"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 1. Smart Task Breakdown: Decomposes a task into 4-6 actionable checklist subtasks
    suspend fun breakdownTask(taskTitle: String, taskDesc: String): List<SubTask> = withContext(Dispatchers.IO) {
        val prompt = """
            Break down the following task into 4 to 6 clear, actionable, concise subtasks for a high-productivity to-do list:
            Task: "$taskTitle"
            Details: "$taskDesc"
            
            Return ONLY a raw JSON array of strings, without code fences or markdown. Example: ["Review requirements", "Draft outline", "Write initial draft", "Proofread and polish"]
        """.trimIndent()

        val result = askGemini(prompt)
        if (result.isSuccess) {
            val text = result.getOrNull()?.trim() ?: ""
            val cleaned = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            try {
                val array = JSONArray(cleaned)
                val subtasks = mutableListOf<SubTask>()
                for (i in 0 until array.length()) {
                    val subTitle = array.getString(i)
                    if (subTitle.isNotBlank()) {
                        subtasks.add(SubTask(title = subTitle.trim()))
                    }
                }
                if (subtasks.isNotEmpty()) return@withContext subtasks
            } catch (_: Exception) {}
        }

        // High quality algorithmic fallback
        generateHeuristicSubtasks(taskTitle)
    }

    // 2. Brain Dump to Structured Tasks
    suspend fun parseBrainDump(brainDumpText: String, targetDate: String): List<TaskItem> = withContext(Dispatchers.IO) {
        val prompt = """
            Convert the following raw brain dump notes into a structured JSON list of actionable to-do items.
            Brain Dump:
            "$brainDumpText"
            
            Target Date: $targetDate
            Categories to choose from: Work, Personal, Study, Fitness, Health, Wishlist, Shopping, Routine, Anniversary, Other.
            Priorities: HIGH, MEDIUM, LOW, NONE.
            
            Output ONLY raw JSON format:
            [
              {
                "title": "Task title",
                "category": "Work",
                "priority": "HIGH",
                "dueTime": "10:00",
                "durationMinutes": 45,
                "sticker": "🎯",
                "subtasks": ["Step 1", "Step 2"]
              }
            ]
        """.trimIndent()

        val result = askGemini(prompt)
        if (result.isSuccess) {
            val text = result.getOrNull()?.trim() ?: ""
            val cleaned = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            try {
                val array = JSONArray(cleaned)
                val tasks = mutableListOf<TaskItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val subArray = obj.optJSONArray("subtasks")
                    val subList = mutableListOf<SubTask>()
                    if (subArray != null) {
                        for (j in 0 until subArray.length()) {
                            subList.add(SubTask(title = subArray.getString(j)))
                        }
                    }

                    val task = TaskItem(
                        title = obj.optString("title", "Task"),
                        category = obj.optString("category", TaskCategory.WORK.label),
                        priority = obj.optString("priority", Priority.MEDIUM.name),
                        dueDate = targetDate,
                        dueTime = obj.optString("dueTime", "10:00").takeIf { it.isNotBlank() },
                        durationMinutes = obj.optInt("durationMinutes", 30),
                        sticker = obj.optString("sticker", "⭐️")
                    ).copyWithSubtasks(subList)

                    tasks.add(task)
                }
                if (tasks.isNotEmpty()) return@withContext tasks
            } catch (_: Exception) {}
        }

        // Heuristic fallback for brain dump
        parseBrainDumpHeuristic(brainDumpText, targetDate)
    }

    // 3. AI Day Schedule Planner: Takes current tasks and returns an optimized time-blocked plan
    suspend fun generateDailySchedule(existingTasks: List<TaskItem>, targetDate: String): String = withContext(Dispatchers.IO) {
        val taskDescriptions = existingTasks.joinToString("\n") {
            "- ${it.title} (${it.category}, Priority: ${it.priority}, Est: ${it.durationMinutes}m, Scheduled: ${it.dueTime ?: "Flexible"})"
        }

        val prompt = """
            You are an expert AI Schedule Planner & ADHD Time-Blocking Coach.
            Create an optimized, realistic, energizing hour-by-hour daily schedule planner for $targetDate based on these tasks:
            $taskDescriptions
            
            Schedule guidelines:
            1. Include morning routine (e.g. 07:30 - 08:30)
            2. High-priority Deep Work focus sprint in the morning
            3. Buffer breaks, midday lunch & hydration check
            4. Afternoon batching for meetings/chores
            5. Evening wind-down & reflection
            6. Add encouraging dopamine-friendly tips for ADHD focus.
            
            Format clearly with time blocks (e.g. 08:00 AM - 09:00 AM) and emoji bullet points.
        """.trimIndent()

        val result = askGemini(prompt)
        if (result.isSuccess) {
            return@withContext result.getOrNull() ?: fallbackDailySchedule(existingTasks, targetDate)
        }
        fallbackDailySchedule(existingTasks, targetDate)
    }

    private fun generateHeuristicSubtasks(title: String): List<SubTask> {
        val lower = title.lowercase()
        return when {
            lower.contains("presentation") || lower.contains("slide") || lower.contains("pitch") -> listOf(
                SubTask(title = "Define core message & target audience"),
                SubTask(title = "Draft 5-key slide outline"),
                SubTask(title = "Gather supporting data & visual charts"),
                SubTask(title = "Design slide layouts & talking points"),
                SubTask(title = "Run 1 timed practice rehearsal")
            )
            lower.contains("clean") || lower.contains("tidy") || lower.contains("room") -> listOf(
                SubTask(title = "Clear surface clutter & gather trash"),
                SubTask(title = "Wipe down desks and counters"),
                SubTask(title = "Organize loose papers & cables"),
                SubTask(title = "Sweep / vacuum floor"),
                SubTask(title = "Light a candle / fresh room spray")
            )
            lower.contains("study") || lower.contains("exam") || lower.contains("read") -> listOf(
                SubTask(title = "Review syllabus & chapter key terms"),
                SubTask(title = "Active recall on core formulas/concepts"),
                SubTask(title = "Work through 3 practice problems"),
                SubTask(title = "Summarize 1-page cheat sheet")
            )
            lower.contains("workout") || lower.contains("gym") || lower.contains("fitness") -> listOf(
                SubTask(title = "5-min dynamic warm-up & joint mobility"),
                SubTask(title = "Main compound exercise sets"),
                SubTask(title = "Core / secondary muscle circuits"),
                SubTask(title = "Cool down stretching & log hydration")
            )
            lower.contains("code") || lower.contains("app") || lower.contains("bug") || lower.contains("feature") -> listOf(
                SubTask(title = "Analyze requirements & edge cases"),
                SubTask(title = "Implement data models & business logic"),
                SubTask(title = "Build UI components with Compose"),
                SubTask(title = "Run unit verification & test edge cases")
            )
            else -> listOf(
                SubTask(title = "Clarify main goal and deliverable"),
                SubTask(title = "Gather required materials & open tools"),
                SubTask(title = "Execute initial focus draft / 25min sprint"),
                SubTask(title = "Review quality & complete final polish")
            )
        }
    }

    private fun parseBrainDumpHeuristic(text: String, date: String): List<TaskItem> {
        val lines = text.lines().map { it.trim().removePrefix("-").removePrefix("•").removePrefix("*").trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            return listOf(
                TaskItem(title = "Organize daily priorities", dueDate = date, category = TaskCategory.WORK.label, priority = Priority.HIGH.name)
            )
        }

        val times = listOf("09:00", "11:00", "14:00", "16:30", "18:00", "19:30")
        return lines.mapIndexed { idx, line ->
            val lower = line.lowercase()
            val category = when {
                lower.contains("buy") || lower.contains("shop") || lower.contains("milk") || lower.contains("grocery") -> TaskCategory.SHOPPING.label
                lower.contains("call") || lower.contains("email") || lower.contains("meet") || lower.contains("project") || lower.contains("work") -> TaskCategory.WORK.label
                lower.contains("gym") || lower.contains("run") || lower.contains("exercise") || lower.contains("water") -> TaskCategory.FITNESS.label
                lower.contains("doctor") || lower.contains("med") || lower.contains("dentist") -> TaskCategory.HEALTH.label
                lower.contains("study") || lower.contains("read") || lower.contains("book") || lower.contains("course") -> TaskCategory.STUDY.label
                lower.contains("gift") || lower.contains("birthday") || lower.contains("anniversary") -> TaskCategory.BIRTHDAY.label
                else -> TaskCategory.PERSONAL.label
            }

            val priority = if (idx == 0 || lower.contains("urgent") || lower.contains("important") || lower.contains("asap")) Priority.HIGH.name
            else if (idx <= 2) Priority.MEDIUM.name else Priority.LOW.name

            val sticker = when (category) {
                TaskCategory.WORK.label -> "💼"
                TaskCategory.SHOPPING.label -> "🛒"
                TaskCategory.FITNESS.label -> "🏃"
                TaskCategory.STUDY.label -> "📚"
                TaskCategory.BIRTHDAY.label -> "🎉"
                else -> "⭐️"
            }

            TaskItem(
                title = line.capitalizeWords(),
                category = category,
                priority = priority,
                dueDate = date,
                dueTime = times.getOrNull(idx % times.size),
                durationMinutes = 30,
                sticker = sticker
            )
        }
    }

    private fun fallbackDailySchedule(tasks: List<TaskItem>, date: String): String {
        val taskItems = if (tasks.isEmpty()) "• Deep work session\n• Daily workout\n• Evening reading"
        else tasks.joinToString("\n") { "• ${it.title} (${it.category}, ${it.dueTime ?: "flexible"})" }

        return """
            📅 AI Day Schedule Plan ($date)
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            ☀️ 07:30 AM - 08:30 AM | Morning Momentum
            • Hydrate with 500ml water
            • 10-min sunlight stretch & breakfast
            • Review top 3 outcomes for the day
            
            ⚡️ 09:00 AM - 11:30 AM | Peak Focus Block
            $taskItems
            
            🥗 12:30 PM - 01:30 PM | Recharge & Nourish
            • Healthy lunch away from screens
            • 15-min mindfulness walk
            
            💼 02:00 PM - 04:30 PM | Collaborative & Routine Batching
            • Clear action items & fast tasks
            • Quick 5-min pomodoro breaks
            
            🏃 05:30 PM - 06:30 PM | Movement & Wellness
            • Physical activity / cardio / stretching
            
            🌙 08:30 PM - 10:00 PM | Evening Wind-Down
            • Gratitude journal entry
            • Set up tomorrow's to-do list!
        """.trimIndent()
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }
}
