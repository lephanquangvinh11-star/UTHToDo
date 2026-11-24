package com.uthtodo.dpgh.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.uthtodo.dpgh.data.model.Subtask
import com.uthtodo.dpgh.data.model.Task
import com.uthtodo.dpgh.data.remote.FirebaseManager
import com.uthtodo.dpgh.data.remote.TaskApiClient
import com.uthtodo.dpgh.service.ReminderScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

object TaskRepository {
    private const val PREFS_NAME = "uth_todo_tasks_cache"
    private const val KEY_TASKS_PREFIX = "cached_tasks_"
    private const val KEY_SEEDED_PREFIX = "seeded_user_"

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uid = currentCacheScopeKey()
        val cached = loadTasksFromCache(uid)
        if (cached.isNotEmpty()) {
            _tasks.value = cached
        }
    }

    private fun currentCacheScopeKey(): String {
        return FirebaseManager.currentUser()?.uid ?: "local_guest"
    }

    private fun hasSeededForUser(uid: String): Boolean {
        return prefs?.getBoolean("$KEY_SEEDED_PREFIX$uid", false) ?: false
    }

    private fun markSeededForUser(uid: String) {
        prefs?.edit()?.putBoolean("$KEY_SEEDED_PREFIX$uid", true)?.apply()
    }

    private fun saveTasksToCache(uid: String, list: List<Task>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs?.edit()?.putString("$KEY_TASKS_PREFIX$uid", arr.toString())?.apply()
    }

    private fun loadTasksFromCache(uid: String): List<Task> {
        val raw = prefs?.getString("$KEY_TASKS_PREFIX$uid", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<Task>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(Task.fromJson(obj))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun syncReminders(list: List<Task>) {
        val ctx = appContext ?: return
        ReminderScheduler.syncAllTaskReminders(ctx, list)
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    suspend fun refreshTasks(): Result<List<Task>> {
        _isLoading.value = true
        _errorMessage.value = null

        val firebaseUser = FirebaseManager.ensureAuthenticatedUser()
        if (firebaseUser != null) {
            val uid = firebaseUser.uid
            val cached = loadTasksFromCache(uid)
            if (_tasks.value.isEmpty() && cached.isNotEmpty()) {
                _tasks.value = cached
            } else if (_tasks.value.isEmpty() && !hasSeededForUser(uid)) {
                val defaults = FirebaseManager.buildDefaultSeedTasks()
                _tasks.value = defaults
                saveTasksToCache(uid, defaults)
            }

            FirebaseManager.listenToUserTasks(
                uid = uid,
                shouldSeedIfEmpty = !hasSeededForUser(uid),
                onSeeded = { markSeededForUser(uid) },
                onUpdate = { firestoreTasks ->
                    markSeededForUser(uid)
                    _tasks.value = firestoreTasks
                    _isLoading.value = false
                    _errorMessage.value = null
                    saveTasksToCache(uid, firestoreTasks)
                    syncReminders(firestoreTasks)
                },
                onError = {
                    _isLoading.value = false
                    if (_tasks.value.isEmpty()) {
                        val fallback = FirebaseManager.buildDefaultSeedTasks()
                        _tasks.value = fallback
                        saveTasksToCache(uid, fallback)
                    }
                }
            )
            _isLoading.value = false
            return Result.success(_tasks.value)
        }

        val uid = currentCacheScopeKey()
        val cached = loadTasksFromCache(uid)
        if (cached.isNotEmpty()) {
            _tasks.value = cached
            _isLoading.value = false
            return Result.success(cached)
        }

        val apiResult = TaskApiClient.getTasks()
        _isLoading.value = false
        apiResult.onSuccess { fetchedList ->
            _tasks.value = fetchedList
            saveTasksToCache(uid, fetchedList)
            syncReminders(fetchedList)
        }.onFailure {
            val defaults = FirebaseManager.buildDefaultSeedTasks()
            _tasks.value = defaults
            saveTasksToCache(uid, defaults)
            syncReminders(defaults)
            return Result.success(defaults)
        }
        return apiResult
    }

    suspend fun toggleTaskDone(id: Int): Result<Task> {
        val current = _tasks.value.find { it.id == id }
            ?: return Result.failure(Exception("Không tìm thấy công việc"))
        val now = System.currentTimeMillis()
        val updatedTask = current.copy(done = !current.done, updatedAt = now)

        val updatedList = _tasks.value.map { if (it.id == id) updatedTask else it }
        _tasks.value = updatedList

        val uid = currentCacheScopeKey()
        saveTasksToCache(uid, updatedList)
        syncReminders(updatedList)

        val user = FirebaseManager.currentUser()
        if (user != null) {
            repoScope.launch {
                FirebaseManager.upsertTaskToFirestore(user.uid, updatedTask)
            }
        }
        return Result.success(updatedTask)
    }

    suspend fun addTask(task: Task): Result<Task> {
        _isLoading.value = true
        val now = System.currentTimeMillis()
        val nextId = (_tasks.value.maxOfOrNull { it.id } ?: 0) + 1
        val finalTask = task.copy(
            id = if (task.id > 0) task.id else nextId,
            createdAt = now,
            updatedAt = now
        )

        val updatedList = listOf(finalTask) + _tasks.value
        _tasks.value = updatedList

        val uid = currentCacheScopeKey()
        saveTasksToCache(uid, updatedList)
        syncReminders(updatedList)

        val user = FirebaseManager.ensureAuthenticatedUser()
        if (user != null) {
            repoScope.launch {
                FirebaseManager.upsertTaskToFirestore(user.uid, finalTask)
            }
        }
        _isLoading.value = false
        return Result.success(finalTask)
    }

    suspend fun updateTaskDetails(task: Task): Result<Task> {
        _isLoading.value = true
        val now = System.currentTimeMillis()
        val updatedTask = task.copy(updatedAt = now)

        val updatedList = _tasks.value.map { if (it.id == task.id) updatedTask else it }
        _tasks.value = updatedList

        val uid = currentCacheScopeKey()
        saveTasksToCache(uid, updatedList)
        syncReminders(updatedList)

        val user = FirebaseManager.currentUser()
        if (user != null) {
            repoScope.launch {
                FirebaseManager.upsertTaskToFirestore(user.uid, updatedTask)
            }
        }
        _isLoading.value = false
        return Result.success(updatedTask)
    }

    suspend fun updateSubtasks(taskId: Int, newSubtasks: List<Subtask>): Result<Task> {
        val current = _tasks.value.find { it.id == taskId }
            ?: return Result.failure(Exception("Không tìm thấy công việc"))
        val now = System.currentTimeMillis()
        val updatedTask = current.copy(subtasks = newSubtasks, updatedAt = now)

        val updatedList = _tasks.value.map { if (it.id == taskId) updatedTask else it }
        _tasks.value = updatedList

        val uid = currentCacheScopeKey()
        saveTasksToCache(uid, updatedList)

        val user = FirebaseManager.currentUser()
        if (user != null) {
            repoScope.launch {
                FirebaseManager.upsertTaskToFirestore(user.uid, updatedTask)
            }
        }
        return Result.success(updatedTask)
    }

    suspend fun deleteTask(id: Int): Result<Unit> {
        _isLoading.value = true
        val updatedList = _tasks.value.filterNot { it.id == id }
        _tasks.value = updatedList

        val uid = currentCacheScopeKey()
        saveTasksToCache(uid, updatedList)
        appContext?.let { ReminderScheduler.cancelTaskReminder(it, id) }

        val user = FirebaseManager.currentUser()
        if (user != null) {
            repoScope.launch {
                FirebaseManager.deleteTaskFromFirestore(user.uid, id)
            }
        }
        _isLoading.value = false
        return Result.success(Unit)
    }

    fun getTaskById(id: Int): Task? {
        return _tasks.value.find { it.id == id }
    }

    fun clearSessionData() {
        FirebaseManager.stopListening()
        _tasks.value = emptyList()
        _errorMessage.value = null
        _isLoading.value = false
    }

    fun deleteUserCache(uid: String) {
        _tasks.value.forEach { task ->
            appContext?.let { ReminderScheduler.cancelTaskReminder(it, task.id) }
        }
        prefs?.edit()
            ?.remove("$KEY_TASKS_PREFIX$uid")
            ?.remove("$KEY_SEEDED_PREFIX$uid")
            ?.apply()
        clearSessionData()
    }
}
