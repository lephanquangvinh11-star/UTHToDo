package com.uthtodo.dpgh.ui.screens

import com.uthtodo.dpgh.data.local.AppPreferences
import com.uthtodo.dpgh.data.local.SessionManager
import com.uthtodo.dpgh.data.model.AppNotification
import com.uthtodo.dpgh.data.model.Subtask
import com.uthtodo.dpgh.data.model.Task
import com.uthtodo.dpgh.data.model.buildNotificationsFromTasks
import com.uthtodo.dpgh.data.remote.FirebaseManager
import com.uthtodo.dpgh.data.repository.TaskRepository

import com.uthtodo.dpgh.service.ReminderScheduler
import com.uthtodo.dpgh.ui.components.AppBottomNavigationBar
import com.uthtodo.dpgh.ui.components.LifeIcon
import com.uthtodo.dpgh.ui.components.PrivacyPolicyDialog
import com.uthtodo.dpgh.ui.components.StudyIcon
import com.uthtodo.dpgh.util.formatRelativeDate

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

private fun getWebClientId(context: Context): String {
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    return if (resId != 0) context.getString(resId) else ""
}

@Composable
fun LoginScreen(
    onGoogleClick: () -> Unit = {},
    onGuestClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary

    // Google Sign-In Legacy Intent Fallback
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = false
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                val email = account.email.orEmpty()
                val name = account.displayName ?: email.substringBefore("@").ifBlank { "Người dùng UTH" }
                val photoUrl = account.photoUrl?.toString().orEmpty()
                if (idToken != null) {
                    isLoading = true
                    FirebaseManager.signInWithGoogle(
                        idToken = idToken,
                        onSuccess = { firebaseUser ->
                            isLoading = false
                            val finalEmail = firebaseUser.email ?: email
                            val finalName = firebaseUser.displayName ?: name
                            val finalPhoto = firebaseUser.photoUrl?.toString() ?: photoUrl
                            SessionManager.setGoogleUser(finalName, finalEmail, finalPhoto)
                            coroutineScope.launch { TaskRepository.refreshTasks() }
                            Toast.makeText(context, "Đã đăng nhập: $finalEmail", Toast.LENGTH_SHORT).show()
                            onGoogleClick()
                        },
                        onError = { err ->
                            isLoading = false
                            authError = err
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    authError = "Không nhận được Google ID Token từ thiết bị."
                    Toast.makeText(context, authError, Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                authError = "Đăng nhập Google không thành công (mã ${e.statusCode}). Bạn có thể dùng chế độ Khách nếu chưa đăng nhập tài khoản Google trên thiết bị."
                Toast.makeText(context, authError, Toast.LENGTH_LONG).show()
            }
        } else {
            authError = "Đã hủy đăng nhập Google."
        }
    }

    fun triggerGoogleSignIn() {
        val webClientId = getWebClientId(context)
        if (webClientId.isBlank()) {
            authError = "Chưa cấu hình Web Client ID cho Google Sign-In."
            return
        }
        isLoading = true
        authError = null

        coroutineScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val rawNonce = UUID.randomUUID().toString()
                val bytes = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray())
                val nonce = bytes.fold("") { str, it -> str + "%02x".format(it) }

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setNonce(nonce)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    FirebaseManager.signInWithGoogle(
                        idToken = idToken,
                        onSuccess = { user ->
                            isLoading = false
                            val email = user.email ?: googleIdTokenCredential.id
                            val name = user.displayName ?: googleIdTokenCredential.displayName ?: email.substringBefore("@")
                            val photo = user.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString().orEmpty()
                            SessionManager.setGoogleUser(name, email, photo)
                            coroutineScope.launch { TaskRepository.refreshTasks() }
                            Toast.makeText(context, "Đã đăng nhập: $email", Toast.LENGTH_SHORT).show()
                            onGoogleClick()
                        },
                        onError = { err ->
                            isLoading = false
                            authError = err
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        }
                    )
                    return@launch
                }
            } catch (_: GetCredentialCancellationException) {
                isLoading = false
                authError = "Đã hủy chọn tài khoản Google."
                return@launch
            } catch (_: Exception) {
                // Fallback to legacy GoogleSignInClient below
            }

            try {
                val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestIdToken(webClientId)
                val googleSignInClient = GoogleSignIn.getClient(context, gsoBuilder.build())
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            } catch (e: Exception) {
                isLoading = false
                authError = e.localizedMessage ?: "Không thể khởi chạy Google Sign-In trên thiết bị này."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp)
    ) {
        Spacer(modifier = Modifier.weight(0.9f))

        // UTH Todo App Icon with soft shadow
        Surface(
            modifier = Modifier
                .size(86.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = primaryColor.copy(alpha = 0.5f),
                    ambientColor = primaryColor.copy(alpha = 0.2f)
                ),
            shape = RoundedCornerShape(24.dp),
            color = primaryColor
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoginUthLogoIcon()
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Heading
        Text(
            text = "Chào mừng đến với\nUTH Todo",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 38.sp,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Subtitle
        Text(
            text = "Đăng nhập để lưu công việc và đồng bộ\ntrên mọi thiết bị của bạn.",
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        if (authError != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = authError.orEmpty(),
                fontSize = 13.sp,
                color = Color(0xFFE5484D),
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.weight(1.1f))

        // Continue with Google Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = Color(0xFFE5E5EA),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable {
                    if (!isLoading) triggerGoogleSignIn()
                },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = primaryColor,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Đang kết nối…",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = primaryColor
                    )
                } else {
                    GoogleIcon()
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Tiếp tục với Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Divider with "hoặc"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Color(0xFFEEEEF0))
            )
            Text(
                text = "hoặc",
                fontSize = 13.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Color(0xFFEEEEF0))
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Try without account Button (Anonymous Firebase Auth)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    if (!isLoading) {
                        isLoading = true
                        authError = null
                        FirebaseManager.signInAnonymously(
                            onSuccess = {
                                isLoading = false
                                SessionManager.setGuestUser()
                                coroutineScope.launch { TaskRepository.refreshTasks() }
                                onGuestClick()
                            },
                            onError = {
                                isLoading = false
                                SessionManager.setGuestUser()
                                coroutineScope.launch { TaskRepository.refreshTasks() }
                                onGuestClick()
                            }
                        )
                    }
                }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Dùng thử không cần tài khoản",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Terms and Privacy disclaimer footer (Clickable per Google Play policy)
        Text(
            text = "Khi tiếp tục, bạn đồng ý với Điều khoản sử dụng và Chính sách quyền riêng tư của UTH Todo.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = primaryColor,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { showPrivacyDialog = true }
                .padding(bottom = 24.dp, top = 4.dp, start = 8.dp, end = 8.dp)
        )
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }
}

@Composable
private fun LoginUthLogoIcon() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(11.dp)) {
                    val path = Path().apply {
                        moveTo(size.width * 0.18f, size.height * 0.5f)
                        lineTo(size.width * 0.42f, size.height * 0.76f)
                        lineTo(size.width * 0.86f, size.height * 0.24f)
                    }
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(
                            width = 2.2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(8.dp)
                    .background(Color.White, RoundedCornerShape(4.dp))
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.75f),
                    radius = (size.minDimension / 2) - (1.2.dp.toPx()),
                    style = Stroke(width = 2.2.dp.toPx())
                )
            }

            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(8.dp)
                    .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun GoogleIcon() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(1.dp, Color(0xFFD1D1D6), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "G",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5F6368)
        )
    }
}


