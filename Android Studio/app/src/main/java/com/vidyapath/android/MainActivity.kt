package com.vidyapath.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VidyapathApp() }
    }
}
enum class AppScreen {
    Splash, Welcome, Signup, Login, VerifyEmail, SelectSubjects, Home, Planner, AddTask, TaskSuccess,
    SetReminder, ReminderNotification, RevisionHistory, AddResource, ResourceSuccess, Resources,
    Progress, SubjectProgress, ProgressAnalysis, ExamSetup, ExamPlan, ExamProgress, Profile,
    EditProfile, ManageSubjects, Settings,
}

internal data class PendingEmailCode(
    val name: String,
    val email: String,
    val isSignup: Boolean,
)

@Composable
private fun VidyapathApp() {
    val context = LocalContext.current
    val authClient = remember { NeonAuthClient(context.applicationContext, BuildConfig.NEON_AUTH_BASE_URL) }
    val appStore = remember { LocalAppStore(context.applicationContext) }
    val snackbarHost = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var appData by remember { mutableStateOf(AppData()) }
    var screenStack by remember { mutableStateOf(listOf(AppScreen.Splash)) }
    var pendingCode by remember { mutableStateOf(PendingEmailCode("", "", false)) }
    var isBusy by remember { mutableStateOf(false) }
    var isRestoringSession by remember { mutableStateOf(true) }
    var startupError by remember { mutableStateOf<String?>(null) }
    var selectedSubject by remember { mutableStateOf("Data Structures") }
    val screen = screenStack.last()

    fun notify(message: String) {
        coroutineScope.launch { snackbarHost.showSnackbar(message) }
    }

    fun push(destination: AppScreen) {
        screenStack = screenStack + destination
    }

    fun replace(destination: AppScreen) {
        screenStack = listOf(destination)
    }

    fun back() {
        if (screenStack.size > 1) screenStack = screenStack.dropLast(1)
    }

    fun saveData(next: AppData) {
        appData = next
        if (next.accountId.isNotBlank()) appStore.save(next)
    }

    fun installUser(user: AuthUser) {
        val restored = appStore.load(user.id).copy(accountId = user.id, name = user.name, email = user.email)
        saveData(restored)
        replace(if (restored.onboarded) AppScreen.Home else AppScreen.SelectSubjects)
    }

    LaunchedEffect(authClient) {
        try {
            authClient.restoreSession()?.let(::installUser)
        } catch (error: Exception) {
            startupError = error.message ?: "Unable to restore the saved login."
        } finally {
            isRestoringSession = false
        }
    }

    fun requestEmailCode(name: String, email: String, isSignup: Boolean) {
        val normalizedName = name.trim()
        val normalizedEmail = email.trim().lowercase()
        if (!normalizedEmail.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            notify("Enter a valid email address.")
            return
        }
        if (isSignup && normalizedName.isBlank()) {
            notify("Enter your full name.")
            return
        }
        coroutineScope.launch {
            if (isBusy) return@launch
            isBusy = true
            try {
                authClient.sendEmailCode(normalizedEmail)
                pendingCode = PendingEmailCode(normalizedName, normalizedEmail, isSignup)
                if (screenStack.lastOrNull() != AppScreen.VerifyEmail) push(AppScreen.VerifyEmail)
                notify("A six-digit code was sent to your email.")
            } catch (error: Exception) {
                notify(error.message ?: "Unable to send a verification code.")
            } finally {
                isBusy = false
            }
        }
    }

    fun verifyEmailCode(code: String) {
        if (code.length != 6 || code.any { !it.isDigit() }) {
            notify("Enter the six-digit code from your email.")
            return
        }
        coroutineScope.launch {
            if (isBusy) return@launch
            isBusy = true
            try {
                installUser(authClient.signIn(pendingCode.email, code, pendingCode.name.takeIf { pendingCode.isSignup }))
                notify("Email verified. You are signed in.")
            } catch (error: Exception) {
                notify(error.message ?: "Unable to verify this code.")
            } finally {
                isBusy = false
            }
        }
    }

    fun updateTask(taskId: String) {
        val completedAt = System.currentTimeMillis()
        saveData(appData.copy(tasks = appData.tasks.map { task ->
            if (task.id == taskId) task.copy(done = !task.done, completedAt = if (task.done) null else completedAt) else task
        }))
    }

    suspend fun saveProfile(name: String, avatarFileName: String?) {
        val normalized = name.trim()
        require(normalized.isNotBlank()) { "Enter your name to continue." }
        if (normalized != appData.name) authClient.updateName(normalized)
        val previousPhoto = appData.avatarFileName
        saveData(appData.copy(name = normalized, avatarFileName = avatarFileName))
        if (previousPhoto != avatarFileName) appStore.deletePhoto(appData.accountId, previousPhoto)
    }

    BackHandler(enabled = screenStack.size > 1) { back() }
    VidyapathTheme(darkMode = appData.darkMode) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHost) },
            bottomBar = {
                if (screen in MAIN_TABS) {
                    AppBottomNavigation(screen = screen, navigate = ::replace)
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            if (isRestoringSession) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Top,
                ) {
                    startupError?.let { message ->
                        StartupWarning(message = message, dismiss = { startupError = null })
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AppScreenContent(
                            screen = screen,
                            data = appData,
                            isBusy = isBusy,
                            authClient = authClient,
                            appStore = appStore,
                            pendingCode = pendingCode,
                            selectedSubject = selectedSubject,
                            go = ::push,
                            replace = ::replace,
                            back = ::back,
                            notify = ::notify,
                            requestEmailCode = ::requestEmailCode,
                            verifyEmailCode = ::verifyEmailCode,
                            saveData = ::saveData,
                            updateTask = ::updateTask,
                            saveProfile = ::saveProfile,
                            setSelectedSubject = { selectedSubject = it },
                        )
                    }
                }
            }
        }
    }
}

private val MAIN_TABS = setOf(AppScreen.Home, AppScreen.Planner, AppScreen.Resources, AppScreen.Progress)

@Composable
private fun AppBottomNavigation(screen: AppScreen, navigate: (AppScreen) -> Unit) {
    val items = listOf(
        Triple(AppScreen.Home, Icons.Default.Home, "Home"),
        Triple(AppScreen.Planner, Icons.Default.CalendarMonth, "Planner"),
        Triple(AppScreen.Resources, Icons.Default.FolderOpen, "Resources"),
        Triple(AppScreen.Progress, Icons.Default.BarChart, "Progress"),
    )
    NavigationBar {
        items.forEach { (destination, icon, label) ->
            NavigationBarItem(
                selected = screen == destination,
                onClick = { navigate(destination) },
                icon = { androidx.compose.material3.Icon(icon, contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun StartupWarning(message: String, dismiss: () -> Unit) {
    androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.errorContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            androidx.compose.material3.TextButton(onClick = dismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun VidyapathTheme(darkMode: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (darkMode) {
        androidx.compose.material3.darkColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF92B6FF),
            background = androidx.compose.ui.graphics.Color(0xFF111827),
            surface = androidx.compose.ui.graphics.Color(0xFF1E293B),
            secondary = androidx.compose.ui.graphics.Color(0xFF9CCBF5),
            error = androidx.compose.ui.graphics.Color(0xFFFFB4AB),
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF1763E8),
            onPrimary = androidx.compose.ui.graphics.Color.White,
            secondary = androidx.compose.ui.graphics.Color(0xFF1E3A8A),
            background = androidx.compose.ui.graphics.Color(0xFFF7F9FE),
            surface = androidx.compose.ui.graphics.Color.White,
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE9F1FF),
            error = androidx.compose.ui.graphics.Color(0xFFB42318),
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
