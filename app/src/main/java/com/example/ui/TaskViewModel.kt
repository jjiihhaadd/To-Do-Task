package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.api.ThinkingConfig
import com.example.data.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val sharedPrefs = application.getSharedPreferences("local_tasks_pref", android.content.Context.MODE_PRIVATE)
    private var firestoreListener: com.google.firebase.firestore.ListenerRegistration? = null
    
    private val _uiState = MutableStateFlow<List<Task>>(emptyList())
    val uiState: StateFlow<List<Task>> = _uiState.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(sharedPrefs.getBoolean("is_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        val nextValue = !_isDarkTheme.value
        _isDarkTheme.value = nextValue
        sharedPrefs.edit().putBoolean("is_dark_theme", nextValue).apply()
    }

    val categories: StateFlow<List<String>> = _uiState.map { tasks ->
        tasks.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            firestoreListener?.remove()
            firestoreListener = null
            
            if (user != null) {
                val localTasks = loadLocalTasks()
                if (localTasks.isNotEmpty()) {
                    viewModelScope.launch {
                        try {
                            val batch = firestore.batch()
                            for (task in localTasks) {
                                val docId = if (task.id.isNotBlank()) task.id else firestore.collection("users").document(user.uid).collection("tasks").document().id
                                val docRef = firestore.collection("users").document(user.uid).collection("tasks").document(docId)
                                batch.set(docRef, task.copy(id = docId))
                            }
                            batch.commit()
                            saveLocalTasks(emptyList())
                        } catch (e: Exception) {
                            Log.e("TaskViewModel", "Error syncing local tasks to firestore", e)
                        }
                    }
                }

                firestoreListener = firestore.collection("users").document(user.uid).collection("tasks")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("TaskViewModel", "Listen failed", error)
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val tasks = snapshot.toObjects(Task::class.java)
                            _uiState.value = tasks
                        }
                    }
            } else {
                _uiState.value = loadLocalTasks()
            }
        }
    }

    private fun loadLocalTasks(): List<Task> {
        val jsonString = sharedPrefs.getString("tasks_list", null) ?: return emptyList()
        return try {
            Json.decodeFromString<List<Task>>(jsonString)
        } catch (e: Exception) {
            Log.e("TaskViewModel", "Error decoding local tasks", e)
            emptyList()
        }
    }

    private fun saveLocalTasks(tasks: List<Task>) {
        try {
            val jsonString = Json.encodeToString(tasks)
            sharedPrefs.edit().putString("tasks_list", jsonString).apply()
        } catch (e: Exception) {
            Log.e("TaskViewModel", "Error encoding local tasks", e)
        }
    }

    fun addTask(title: String, description: String, category: String, reminderTime: Long? = null, photoBase64: String? = null) {
        val user = auth.currentUser
        if (user != null) {
            val ref = firestore.collection("users").document(user.uid).collection("tasks").document()
            val task = Task(id = ref.id, title = title, description = description, category = category, reminderTime = reminderTime, photoBase64 = photoBase64)
            ref.set(task)
        } else {
            val localTasks = loadLocalTasks().toMutableList()
            val newTask = Task(
                id = java.util.UUID.randomUUID().toString(),
                title = title,
                description = description,
                category = category,
                isCompleted = false,
                timestamp = System.currentTimeMillis(),
                reminderTime = reminderTime,
                photoBase64 = photoBase64
            )
            localTasks.add(0, newTask)
            saveLocalTasks(localTasks)
            _uiState.value = localTasks
        }
    }

    fun updateTask(task: Task) {
        val user = auth.currentUser
        if (user != null) {
            if (task.id.isBlank()) return
            firestore.collection("users").document(user.uid).collection("tasks").document(task.id)
                .set(task)
        } else {
            val localTasks = loadLocalTasks().map {
                if (it.id == task.id) task else it
            }
            saveLocalTasks(localTasks)
            _uiState.value = localTasks
        }
    }

    fun toggleTaskStatus(task: Task) {
        val user = auth.currentUser
        if (user != null) {
            if (task.id.isBlank()) return
            firestore.collection("users").document(user.uid).collection("tasks").document(task.id)
                .update("isCompleted", !task.isCompleted)
        } else {
            val localTasks = loadLocalTasks().map {
                if (it.id == task.id) it.copy(isCompleted = !it.isCompleted) else it
            }
            saveLocalTasks(localTasks)
            _uiState.value = localTasks
        }
    }

    fun deleteTask(task: Task) {
        val user = auth.currentUser
        if (user != null) {
            if (task.id.isBlank()) return
            firestore.collection("users").document(user.uid).collection("tasks").document(task.id)
                .delete()
        } else {
            val localTasks = loadLocalTasks().filter { it.id != task.id }
            saveLocalTasks(localTasks)
            _uiState.value = localTasks
        }
    }

    fun suggestCategory(title: String, description: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val prompt = "Suggest a single short category tag (e.g. Personal, Work, Academic, Project) for this task based on its title and description.\\nTitle: $title\\nDescription: $description"
            val response = generateContent(
                model = "gemini-3.1-flash-lite",
                prompt = prompt,
                systemPrompt = "You are a helpful assistant. Output ONLY the category name, nothing else.",
                useThinking = false
            )
            if (response.startsWith("Error")) {
                onError(response)
            } else if (response.isNotBlank()) {
                onResult(response.trim().replace(Regex("[^a-zA-Z0-9 ]"), ""))
            }
        }
    }

    private suspend fun generateContent(model: String, prompt: String, systemPrompt: String, useThinking: Boolean): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = prompt))
                )
            ),
            generationConfig = GenerationConfig(
                thinkingConfig = if (useThinking) ThinkingConfig(thinkingLevel = "HIGH") else null
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = systemPrompt))
            )
        )
        try {
            val response = RetrofitClient.service.generateContent(model, apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response text"
        } catch (e: retrofit2.HttpException) {
            "Error: HTTP ${e.code()} - ${e.response()?.errorBody()?.string()}"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
