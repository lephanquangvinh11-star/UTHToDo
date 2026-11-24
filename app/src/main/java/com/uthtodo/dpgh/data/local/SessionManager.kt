package com.uthtodo.dpgh.data.local

import android.content.Context
import android.content.SharedPreferences
import com.uthtodo.dpgh.data.remote.FirebaseManager
import com.uthtodo.dpgh.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSession(
    val displayName: String = "Khách",
    val email: String = "",
    val isGuest: Boolean = true,
    val avatarInitials: String = "K",
    val avatarUrl: String = ""
)

object SessionManager {
    private const val PREFS_NAME = "uth_todo_prefs"
    private const val KEY_NAME = "user_name"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_IS_GUEST = "user_is_guest"
    private const val KEY_HAS_LOGGED_IN = "has_logged_in"
    private const val KEY_AVATAR_URL = "user_avatar_url"

    private lateinit var prefs: SharedPreferences

    private val _session = MutableStateFlow(UserSession())
    val session: StateFlow<UserSession> = _session.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_NAME, "Khách") ?: "Khách"
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val isGuest = prefs.getBoolean(KEY_IS_GUEST, true)
        val avatarUrl = prefs.getString(KEY_AVATAR_URL, "") ?: ""
        _session.value = UserSession(
            displayName = name,
            email = email,
            isGuest = isGuest,
            avatarInitials = computeInitials(name, isGuest),
            avatarUrl = avatarUrl
        )
    }

    fun hasLoggedIn(): Boolean {
        if (!::prefs.isInitialized) return false
        val firebaseUser = FirebaseManager.currentUser()
        if (firebaseUser != null && !firebaseUser.isAnonymous) return true
        return prefs.getBoolean(KEY_HAS_LOGGED_IN, false)
    }

    fun setGoogleUser(name: String, email: String, avatarUrl: String = "") {
        val safeName = if (name.isNotBlank()) name else (email.substringBefore("@").ifBlank { "Người dùng UTH" })
        val finalAvatar = avatarUrl.ifBlank { _session.value.avatarUrl }
        prefs.edit().apply {
            putString(KEY_NAME, safeName)
            putString(KEY_EMAIL, email)
            putBoolean(KEY_IS_GUEST, false)
            putBoolean(KEY_HAS_LOGGED_IN, true)
            putString(KEY_AVATAR_URL, finalAvatar)
            apply()
        }
        _session.value = UserSession(
            displayName = safeName,
            email = email,
            isGuest = false,
            avatarInitials = computeInitials(safeName, false),
            avatarUrl = finalAvatar
        )
        FirebaseManager.syncUserProfile(safeName, email, isGuest = false, avatarUrl = finalAvatar)
    }

    fun setGuestUser() {
        prefs.edit().apply {
            putString(KEY_NAME, "Khách")
            putString(KEY_EMAIL, "")
            putBoolean(KEY_IS_GUEST, true)
            putBoolean(KEY_HAS_LOGGED_IN, true)
            putString(KEY_AVATAR_URL, "")
            apply()
        }
        _session.value = UserSession(
            displayName = "Khách",
            email = "",
            isGuest = true,
            avatarInitials = "K",
            avatarUrl = ""
        )
        FirebaseManager.syncUserProfile("Khách", "", isGuest = true)
    }

    fun updateDisplayName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        val current = _session.value
        val isGuest = current.isGuest
        prefs.edit().putString(KEY_NAME, trimmed).apply()
        _session.value = current.copy(
            displayName = trimmed,
            avatarInitials = computeInitials(trimmed, false)
        )
        FirebaseManager.syncUserProfile(trimmed, current.email, isGuest, current.avatarUrl)
    }

    fun updateAvatarUrl(newUrl: String) {
        val current = _session.value
        prefs.edit().putString(KEY_AVATAR_URL, newUrl).apply()
        _session.value = current.copy(avatarUrl = newUrl)
        FirebaseManager.syncUserProfile(current.displayName, current.email, current.isGuest, newUrl)
    }

    fun logout() {
        prefs.edit().apply {
            putString(KEY_NAME, "Khách")
            putString(KEY_EMAIL, "")
            putBoolean(KEY_IS_GUEST, true)
            putBoolean(KEY_HAS_LOGGED_IN, false)
            putString(KEY_AVATAR_URL, "")
            apply()
        }
        _session.value = UserSession(
            displayName = "Khách",
            email = "",
            isGuest = true,
            avatarInitials = "K",
            avatarUrl = ""
        )
        TaskRepository.clearSessionData()
        FirebaseManager.signOut()
    }

    suspend fun deleteAccountAndData(): Result<Unit> {
        val uid = FirebaseManager.currentUser()?.uid ?: "local_guest"
        TaskRepository.deleteUserCache(uid)
        val res = FirebaseManager.deleteCurrentAccountAndData()
        prefs.edit().clear().apply()
        _session.value = UserSession(
            displayName = "Khách",
            email = "",
            isGuest = true,
            avatarInitials = "K",
            avatarUrl = ""
        )
        return res
    }

    /**
     * Rule: Chữ cái đầu của 2 từ cuối trong tên ("Minh Anh" -> "MA", "Nguyễn Văn A" -> "VA").
     * Khách -> "K".
     */
    private fun computeInitials(name: String, isGuest: Boolean): String {
        if (isGuest || name.isBlank() || name == "Khách") return "K"
        val words = name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        return when {
            words.size >= 2 -> {
                val last1 = words[words.size - 2].first().uppercase()
                val last2 = words[words.size - 1].first().uppercase()
                "$last1$last2"
            }
            words.size == 1 -> words[0].take(2).uppercase()
            else -> "K"
        }
    }
}
