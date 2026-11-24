package com.uthtodo.dpgh.data.remote

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import com.uthtodo.dpgh.data.model.Subtask
import com.uthtodo.dpgh.data.model.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object FirebaseManager {

    private const val TAG = "FirebaseManager"

    // Services
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    val remoteConfig: FirebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }
    val crashlytics: FirebaseCrashlytics by lazy { FirebaseCrashlytics.getInstance() }

    private var analytics: FirebaseAnalytics? = null
    private var tasksListenerRegistration: ListenerRegistration? = null
    private var activeListeningUid: String? = null

    fun init(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context)

        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(60)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(
            mapOf("welcome_slogan" to "Việc học, việc nhà — gọn gàng mỗi ngày")
        )
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Remote config fetched: ${remoteConfig.getString("welcome_slogan")}")
                }
            }
    }

    fun getWelcomeSlogan(): String {
        val slogan = remoteConfig.getString("welcome_slogan")
        return if (slogan.isNotBlank()) slogan else "Việc học, việc nhà — gọn gàng mỗi ngày"
    }

    // --- Authentication ---
    fun currentUser(): FirebaseUser? = auth.currentUser

    fun signInWithGoogle(
        idToken: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val current = auth.currentUser
        val authTask = if (current != null && current.isAnonymous) {
            current.linkWithCredential(credential)
        } else {
            auth.signInWithCredential(credential)
        }

        authTask
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    logEvent("login_google_success", Bundle().apply {
                        putString("user_id", user.uid)
                    })
                    onSuccess(user)
                } else {
                    onError("Không tìm thấy thông tin tài khoản.")
                }
            }
            .addOnFailureListener {
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { fallbackResult ->
                        val user = fallbackResult.user
                        if (user != null) {
                            logEvent("login_google_success", Bundle().apply {
                                putString("user_id", user.uid)
                            })
                            onSuccess(user)
                        } else {
                            onError("Không tìm thấy thông tin tài khoản.")
                        }
                    }
                    .addOnFailureListener { finalErr ->
                        crashlytics.recordException(finalErr)
                        onError(finalErr.localizedMessage ?: "Đăng nhập Google thất bại.")
                    }
            }
    }

    fun signInAnonymously(
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val existing = auth.currentUser
        if (existing != null) {
            onSuccess(existing)
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                result.user?.let { user ->
                    logEvent("login_guest_success", Bundle().apply {
                        putString("user_id", user.uid)
                    })
                    onSuccess(user)
                } ?: onError("Lỗi đăng nhập ẩn danh.")
            }
            .addOnFailureListener { e ->
                crashlytics.recordException(e)
                onError(e.localizedMessage ?: "Đăng nhập ẩn danh thất bại.")
            }
    }

    suspend fun ensureAuthenticatedUser(): FirebaseUser? = suspendCancellableCoroutine { cont ->
        val current = auth.currentUser
        if (current != null) {
            cont.resume(current)
            return@suspendCancellableCoroutine
        }
        auth.signInAnonymously()
            .addOnSuccessListener { res ->
                if (cont.isActive) cont.resume(res.user)
            }
            .addOnFailureListener { e ->
                crashlytics.recordException(e)
                if (cont.isActive) cont.resume(null)
            }
    }

    fun syncUserProfile(
        displayName: String,
        email: String,
        isGuest: Boolean,
        avatarUrl: String = ""
    ) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val profileData = mutableMapOf<String, Any>(
            "uid" to uid,
            "displayName" to displayName.trim().ifBlank { "Khách" }.take(100),
            "isGuest" to isGuest,
            "updatedAt" to System.currentTimeMillis()
        )
        if (email.isNotBlank()) {
            profileData["email"] = email.trim().take(254)
        }
        if (avatarUrl.isNotBlank()) {
            profileData["avatarUrl"] = avatarUrl.trim().take(1024)
        }
        firestore.collection("users").document(uid)
            .set(profileData)
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to sync user profile: ${e.message}")
            }
    }

    fun signOut() {
        stopListening()
        auth.signOut()
        logEvent("user_signed_out", Bundle())
    }

    suspend fun deleteCurrentAccountAndData(): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            stopListening()
            val user = auth.currentUser
            if (user == null) {
                if (cont.isActive) cont.resume(Result.success(Unit))
                return@suspendCancellableCoroutine
            }
            val uid = user.uid
            val userDocRef = firestore.collection("users").document(uid)
            val tasksColRef = userDocRef.collection("tasks")

            tasksColRef.get()
                .addOnCompleteListener { snapshotTask ->
                    val batch = firestore.batch()
                    if (snapshotTask.isSuccessful) {
                        snapshotTask.result?.documents?.forEach { doc ->
                            batch.delete(doc.reference)
                        }
                    }
                    batch.delete(userDocRef)
                    batch.commit()
                        .addOnCompleteListener {
                            user.delete()
                                .addOnCompleteListener {
                                    auth.signOut()
                                    logEvent("account_deleted", Bundle().apply {
                                        putString("user_id", uid)
                                    })
                                    if (cont.isActive) cont.resume(Result.success(Unit))
                                }
                        }
                }
        }

    // --- Cloud Firestore (User-scoped Tasks: /users/{uid}/tasks) ---
    fun listenToUserTasks(
        uid: String,
        shouldSeedIfEmpty: Boolean,
        onSeeded: () -> Unit,
        onUpdate: (List<Task>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        if (activeListeningUid == uid && tasksListenerRegistration != null) {
            return
        }
        stopListening()
        activeListeningUid = uid

        val userTasksCol = firestore.collection("users").document(uid).collection("tasks")
        var didSeed = false

        tasksListenerRegistration = userTasksCol.orderBy("id").addSnapshotListener { snapshot, error ->
            if (error != null) {
                crashlytics.recordException(error)
                onError(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                if (snapshot.isEmpty && shouldSeedIfEmpty && !didSeed) {
                    didSeed = true
                    onSeeded()
                    seedInitialUserTasks(uid)
                } else {
                    val taskList = snapshot.documents.mapNotNull { doc ->
                        Task.fromFirestoreDoc(doc)
                    }
                    onUpdate(taskList)
                }
            }
        }
    }

    fun buildDefaultSeedTasks(): List<Task> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calToday = Calendar.getInstance()
        val todayStr = sdf.format(calToday.time)
        val calTomorrow = (calToday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowStr = sdf.format(calTomorrow.time)
        val now = System.currentTimeMillis()

        return listOf(
            Task(
                id = 1,
                title = "Hoàn thành báo cáo Lập trình Thiết bị Di động",
                note = "Kiểm tra lại phần tích hợp Firebase Firestore, Auth và Remote Config.",
                category = "study",
                date = todayStr,
                time = "14:00",
                priority = "high",
                remind = true,
                done = false,
                subtasks = listOf(
                    Subtask("Cấu hình Firestore Security Rules", true),
                    Subtask("Kiểm thử đồng bộ thời gian thực trên máy ảo", false)
                ),
                createdAt = now - 3600_000L * 4,
                updatedAt = now - 3600_000L * 2
            ),
            Task(
                id = 2,
                title = "Ôn tập chương 4 – Cấu trúc dữ liệu & Giải thuật",
                note = "Làm bài tập cây nhị phân tìm kiếm và bảng băm.",
                category = "study",
                date = todayStr,
                time = "19:30",
                priority = "medium",
                remind = true,
                done = true,
                subtasks = listOf(
                    Subtask("Đọc slide bài giảng", true),
                    Subtask("Giải 5 bài trên hệ thống UTH", true)
                ),
                createdAt = now - 3600_000L * 10,
                updatedAt = now - 3600_000L * 1
            ),
            Task(
                id = 3,
                title = "Tập thể dục buổi chiều 30 phút",
                note = "Chạy bộ quanh công viên hoặc tập cardio tại nhà.",
                category = "life",
                date = todayStr,
                time = "17:30",
                priority = "medium",
                remind = false,
                done = false,
                subtasks = emptyList(),
                createdAt = now - 3600_000L * 6,
                updatedAt = now - 3600_000L * 6
            ),
            Task(
                id = 4,
                title = "Mua nhu yếu phẩm cho tuần mới",
                note = "Sữa tươi, trái cây, rau xanh và đồ dùng học tập.",
                category = "life",
                date = tomorrowStr,
                time = "09:00",
                priority = "low",
                remind = true,
                done = false,
                subtasks = listOf(
                    Subtask("Lên danh sách đồ cần mua", true),
                    Subtask("Ghé siêu thị gần trường", false)
                ),
                createdAt = now - 3600_000L * 12,
                updatedAt = now - 3600_000L * 12
            )
        )
    }

    private fun seedInitialUserTasks(uid: String) {
        val defaults = buildDefaultSeedTasks()
        val col = firestore.collection("users").document(uid).collection("tasks")
        val batch = firestore.batch()
        defaults.forEach { task ->
            val docRef = col.document("task_${task.id}")
            batch.set(docRef, task.toFirestoreMap(uid))
        }
        batch.commit().addOnFailureListener { e ->
            Log.w(TAG, "Failed to seed initial tasks to Firestore: ${e.message}")
        }
    }

    suspend fun upsertTaskToFirestore(uid: String, task: Task): Result<Task> =
        suspendCancellableCoroutine { cont ->
            val docRef = firestore.collection("users")
                .document(uid)
                .collection("tasks")
                .document("task_${task.id}")

            docRef.set(task.toFirestoreMap(uid))
                .addOnSuccessListener {
                    logEvent("task_upserted", Bundle().apply {
                        putInt("task_id", task.id)
                        putString("category", task.category)
                    })
                    if (cont.isActive) cont.resume(Result.success(task))
                }
                .addOnFailureListener { e ->
                    crashlytics.recordException(e)
                    if (cont.isActive) cont.resume(Result.failure(e))
                }
        }

    suspend fun deleteTaskFromFirestore(uid: String, taskId: Int): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val docRef = firestore.collection("users")
                .document(uid)
                .collection("tasks")
                .document("task_$taskId")

            docRef.delete()
                .addOnSuccessListener {
                    logEvent("task_deleted", Bundle().apply {
                        putInt("task_id", taskId)
                    })
                    if (cont.isActive) cont.resume(Result.success(Unit))
                }
                .addOnFailureListener { e ->
                    crashlytics.recordException(e)
                    if (cont.isActive) cont.resume(Result.failure(e))
                }
        }

    // --- Cloud Storage (Avatar Upload) ---
    fun uploadAvatar(
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: "anonymous_${System.currentTimeMillis()}"
        val ref = storage.reference.child("avatars/$userId.jpg")

        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    logEvent("avatar_uploaded", Bundle().apply {
                        putString("user_id", userId)
                    })
                    onSuccess(downloadUri.toString())
                }
            }
            .addOnFailureListener { e ->
                crashlytics.recordException(e)
                onError(e.localizedMessage ?: "Tải ảnh đại diện lên Storage thất bại.")
            }
    }

    // --- Analytics ---
    fun logEvent(name: String, params: Bundle = Bundle()) {
        analytics?.logEvent(name, params)
    }

    fun stopListening() {
        tasksListenerRegistration?.remove()
        tasksListenerRegistration = null
        activeListeningUid = null
    }
}
