package com.vidyapath.android

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun AppScreenContent(
    screen: AppScreen,
    data: AppData,
    isBusy: Boolean,
    accountStore: LocalAccountStore,
    appStore: LocalAppStore,
    selectedSubject: String,
    go: (AppScreen) -> Unit,
    replace: (AppScreen) -> Unit,
    back: () -> Unit,
    notify: (String) -> Unit,
    createAccount: (String, String, String, String) -> Unit,
    signIn: (String, String) -> Unit,
    saveData: (AppData) -> Unit,
    updateTask: (String) -> Unit,
    saveProfile: suspend (String, String?) -> Unit,
    setSelectedSubject: (String) -> Unit,
) {
    when (screen) {
        AppScreen.Splash -> SplashScreen { go(if (data.accountId.isBlank()) AppScreen.Welcome else AppScreen.Home) }
        AppScreen.Welcome -> WelcomeScreen { go(AppScreen.Signup) }
        AppScreen.Signup -> SignupScreen(isBusy, createAccount, { go(AppScreen.Login) }, back)
        AppScreen.Login -> LoginScreen(isBusy, signIn, { go(AppScreen.Signup) }, back)
        AppScreen.SelectSubjects -> SubjectsScreen(
            data = data,
            onChange = saveData,
            onDone = {
                if (data.subjects.isEmpty()) notify("Choose at least one subject.")
                else {
                    saveData(data.copy(onboarded = true))
                    replace(AppScreen.Home)
                }
            },
            back = back,
            title = "Select Subjects",
            buttonText = "Continue",
        )
        AppScreen.ManageSubjects -> SubjectsScreen(
            data = data,
            onChange = saveData,
            onDone = {
                if (data.subjects.isEmpty()) notify("Choose at least one subject.")
                else {
                    saveData(data)
                    notify("Subjects updated.")
                    back()
                }
            },
            back = back,
            title = "Manage Subjects",
            buttonText = "Save Changes",
        )
        AppScreen.Home -> HomeScreen(data, go, replace, updateTask)
        AppScreen.Planner -> PlannerScreen(data, go, updateTask)
        AppScreen.AddTask -> AddTaskScreen(data, back, notify, { task ->
            saveData(data.copy(tasks = data.tasks + task))
            go(AppScreen.TaskSuccess)
        })
        AppScreen.TaskSuccess -> SuccessScreen(
            title = "Task Added Successfully!",
            firstButton = "Add Another Task",
            onFirst = { replace(AppScreen.AddTask) },
            secondButton = "Go to Planner",
            onSecond = { replace(AppScreen.Planner) },
        )
        AppScreen.SetReminder -> ReminderFormScreen(data, back, notify, { reminder ->
            saveData(data.copy(reminders = data.reminders + reminder))
            go(AppScreen.ReminderNotification)
        })
        AppScreen.ReminderNotification -> ReminderNotificationScreen(data, notify, saveData, replace)
        AppScreen.RevisionHistory -> RevisionHistoryScreen(data, back, go, saveData)
        AppScreen.AddResource -> AddResourceScreen(data, appStore, notify, back, { resource ->
            saveData(data.copy(resources = listOf(resource) + data.resources))
            go(AppScreen.ResourceSuccess)
        })
        AppScreen.ResourceSuccess -> SuccessScreen(
            title = "Resource Saved!",
            firstButton = "Add Another",
            onFirst = { replace(AppScreen.AddResource) },
            secondButton = "View Resources",
            onSecond = { replace(AppScreen.Resources) },
        )
        AppScreen.Resources -> ResourcesScreen(data, go, appStore, notify)
        AppScreen.Progress -> ProgressScreen(data, go, setSelectedSubject)
        AppScreen.SubjectProgress -> SubjectProgressScreen(data, selectedSubject, back, saveData)
        AppScreen.ProgressAnalysis -> ProgressAnalysisScreen(data, back)
        AppScreen.ExamSetup -> ExamSetupScreen(data, back, notify, { subject, date, plan ->
            saveData(data.copy(examSubject = subject, examDate = date, examPlan = plan))
            go(AppScreen.ExamPlan)
        })
        AppScreen.ExamPlan -> ExamPlanScreen(data, back, {
            val newTasks = data.examPlan.dropLast(1).mapIndexed { index, item ->
                StudyTask(
                    subject = data.examSubject,
                    topic = item.topic,
                    date = LocalDate.now().plusDays((index * 2L)).toString(),
                    minutes = 60,
                    priority = "High",
                )
            }
            saveData(data.copy(tasks = data.tasks + newTasks))
            notify("Study plan added to planner.")
            go(AppScreen.ExamProgress)
        })
        AppScreen.ExamProgress -> ExamProgressScreen(data, back, replace, saveData)
        AppScreen.Profile -> ProfileScreen(data, go, back, replace, accountStore, saveData, notify)
        AppScreen.EditProfile -> EditProfileScreen(data, appStore, saveProfile, back, notify)
        AppScreen.Settings -> SettingsScreen(data, back, saveData, notify)
    }
}

@Composable
private fun SplashScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(
            androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.background),
            ),
        ).padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Logo()
            Text("Vidyapath", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)
            Text("Plan. Learn. Revise. Stay on Track.", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
        }
        PrimaryButton("Get Started", onGetStarted)
    }
}

@Composable
private fun WelcomeScreen(next: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Welcome to\nVidyapath", textAlign = TextAlign.Center, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Image(
            painter = painterResource(R.drawable.welcome_study),
            contentDescription = "Student studying at a laptop",
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp).aspectRatio(1.35f).clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop,
        )
        val benefits = listOf(
            Icons.Default.CalendarMonth to "Organize your study",
            Icons.Default.BarChart to "Track your progress",
            Icons.Default.Spa to "Stay consistent",
            Icons.Default.Book to "Save study resources",
            Icons.Default.Notifications to "Get revision reminders",
            Icons.Default.CheckCircle to "Prepare for exams",
            Icons.Default.FolderOpen to "Manage your subjects",
        )
        benefits.forEach { (icon, label) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp), tint = MaterialTheme.colorScheme.primary)
                Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton("Next", next)
    }
}

@Composable
private fun SignupScreen(
    busy: Boolean,
    createAccount: (String, String, String, String) -> Unit,
    goToLogin: () -> Unit,
    back: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        AppHeader("Create Account", back)
        Logo(large = true)
        Text("Create Account", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        AppTextField(name, { name = it }, "Full Name")
        AppTextField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
        AppTextField(password, { password = it }, "Password", keyboardType = KeyboardType.Password, password = true)
        AppTextField(confirmation, { confirmation = it }, "Confirm Password", keyboardType = KeyboardType.Password, password = true)
        PrimaryButton(if (busy) "Creating account..." else "Create Account", {
            createAccount(name, email, password, confirmation)
        }, enabled = !busy)
        TextButton(onClick = goToLogin) { Text("Already have an account? Sign In") }
    }
}

@Composable
private fun LoginScreen(
    busy: Boolean,
    signIn: (String, String) -> Unit,
    goToSignup: () -> Unit,
    back: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        AppHeader("Login", back)
        Logo(large = true)
        Text("Sign In", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        AppTextField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
        AppTextField(password, { password = it }, "Password", keyboardType = KeyboardType.Password, password = true)
        PrimaryButton(if (busy) "Signing in..." else "Sign In", { signIn(email, password) }, enabled = !busy)
        TextButton(onClick = goToSignup) { Text("Don't have an account? Sign Up") }
    }
}

@Composable
private fun SubjectsScreen(
    data: AppData,
    onChange: (AppData) -> Unit,
    onDone: () -> Unit,
    back: () -> Unit,
    title: String,
    buttonText: String,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(title, back)
        Text("Choose the subjects you want to manage", modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            ALL_SUBJECTS.forEach { subject ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        val updated = if (subject in data.subjects) data.subjects - subject else data.subjects + subject
                        onChange(data.copy(subjects = updated))
                    }.padding(vertical = 7.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = subject in data.subjects, onCheckedChange = {
                        val updated = if (it) data.subjects + subject else data.subjects - subject
                        onChange(data.copy(subjects = updated))
                    })
                    Text(subject, modifier = Modifier.weight(1f), fontSize = 15.sp)
                    Divider(modifier = Modifier.width(0.dp))
                }
            }
        }
        PrimaryButton(buttonText, onDone, modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp))
    }
}

@Composable
private fun HomeScreen(data: AppData, go: (AppScreen) -> Unit, replace: (AppScreen) -> Unit, updateTask: (String) -> Unit) {
    val today = LocalDate.now().toString()
    val tasks = data.tasks.filter { it.date == today }
    val greeting = when (java.time.LocalTime.now().hour) {
        in 0..11 -> "Morning"
        in 12..16 -> "Afternoon"
        else -> "Evening"
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Logo()
            Text("Vidyapath", modifier = Modifier.weight(1f), fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
            IconButton(onClick = { go(AppScreen.RevisionHistory) }) { Icon(Icons.Default.Notifications, "Notifications") }
            IconButton(onClick = { go(AppScreen.Profile) }) { Icon(Icons.Default.Person, "Profile", tint = MaterialTheme.colorScheme.primary) }
        }
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Good $greeting,", fontWeight = FontWeight.Bold)
                    Text("${data.name.substringBefore(' ').ifBlank { "Student" }}!", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text("Stay consistent and keep learning", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.Spa, null, modifier = Modifier.size(34.dp), tint = Color(0xFF29985E))
            }
        }
        SectionTitle("Today's Tasks", "View All") { replace(AppScreen.Planner) }
        if (tasks.isEmpty()) {
            EmptyMessage("No tasks today.", button = "Add one") { go(AppScreen.AddTask) }
        } else {
            tasks.forEach { task -> TaskRow(task, { updateTask(task.id) }) }
        }
        SectionTitle("Subject Progress", "View All") { replace(AppScreen.Progress) }
        data.subjects.take(3).forEachIndexed { index, subject ->
            val value = calculateSubjectProgress(data, subject)
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(subject, modifier = Modifier.weight(1f), fontSize = 12.sp)
                    Text("$value%", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                ProgressBar(value, if (index == 0) Color(0xFF33A76F) else MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun PlannerScreen(data: AppData, go: (AppScreen) -> Unit, updateTask: (String) -> Unit) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val dates = (-2L..2L).map { LocalDate.now().plusDays(it) }
    val tasks = data.tasks.filter { it.date == selectedDate.toString() }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            AppHeader("Study Planner")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                dates.forEach { date ->
                    val active = date == selectedDate
                    Column(
                        modifier = Modifier.clip(RoundedCornerShape(14.dp))
                            .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedDate = date }.padding(horizontal = 11.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())), color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(date.dayOfMonth.toString(), color = if (active) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(formatDate(selectedDate), modifier = Modifier.padding(top = 16.dp), fontWeight = FontWeight.Bold)
            if (tasks.isEmpty()) EmptyMessage("No tasks for this day.") else tasks.forEach { TaskRow(it) { updateTask(it.id) } }
        }
        androidx.compose.material3.FloatingActionButton(onClick = { go(AppScreen.AddTask) }, modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp)) {
            Icon(Icons.Default.Add, "Add task")
        }
    }
}

@Composable
private fun AddTaskScreen(data: AppData, back: () -> Unit, notify: (String) -> Unit, save: (StudyTask) -> Unit) {
    var subject by remember(data.subjects) { mutableStateOf(data.subjects.firstOrNull() ?: ALL_SUBJECTS.first()) }
    var topic by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var minutes by remember { mutableStateOf("45") }
    var priority by remember { mutableStateOf("High") }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppHeader("Study Planner", back)
        ChoiceField("Subject", subject, data.subjects.ifEmpty { ALL_SUBJECTS }) { subject = it }
        AppTextField(topic, { topic = it }, "Topic")
        AppTextField(date, { date = it }, "Date (YYYY-MM-DD)")
        ChoiceField("Study Time", minutes, listOf("15", "30", "45", "60", "90", "120")) { minutes = it }
        ChoiceField("Priority", priority, listOf("High", "Medium", "Low")) { priority = it }
        PrimaryButton("Add Task", {
            if (topic.isBlank()) {
                notify("Enter a topic.")
                return@PrimaryButton
            }
            val validDate = runCatching { LocalDate.parse(date.trim()) }.getOrNull()
            if (validDate == null) {
                notify("Enter a valid date in YYYY-MM-DD format.")
                return@PrimaryButton
            }
            save(StudyTask(subject = subject, topic = topic.trim(), date = date.trim(), minutes = minutes.toIntOrNull() ?: 45, priority = priority))
        })
    }
}

@Composable
private fun SuccessScreen(title: String, firstButton: String, onFirst: () -> Unit, secondButton: String, onSecond: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(78.dp), tint = Color(0xFF2DA66A))
        Text(title, modifier = Modifier.padding(vertical = 22.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        PrimaryButton(firstButton, onFirst)
        Spacer(Modifier.height(12.dp))
        SecondaryButton(secondButton, onSecond)
    }
}

@Composable
private fun ReminderFormScreen(data: AppData, back: () -> Unit, notify: (String) -> Unit, save: (StudyReminder) -> Unit) {
    var subject by remember(data.subjects) { mutableStateOf(data.subjects.firstOrNull() ?: ALL_SUBJECTS.first()) }
    var topic by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var interval by remember { mutableStateOf("3") }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppHeader("Revision Reminder", back)
        ChoiceField("Subject", subject, data.subjects.ifEmpty { ALL_SUBJECTS }) { subject = it }
        AppTextField(topic, { topic = it }, "Topic")
        AppTextField(date, { date = it }, "First Revision Date (YYYY-MM-DD)")
        ChoiceField("Repeat Interval", interval, listOf("1", "3", "7", "14")) { interval = it }
        Text("Repeats every $interval days", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PrimaryButton("Save Reminder", {
            if (topic.isBlank()) {
                notify("Enter a topic.")
                return@PrimaryButton
            }
            if (runCatching { LocalDate.parse(date.trim()) }.isFailure) {
                notify("Enter a valid date in YYYY-MM-DD format.")
                return@PrimaryButton
            }
            save(StudyReminder(subject = subject, topic = topic.trim(), date = date.trim(), intervalDays = interval.toIntOrNull() ?: 3))
        })
    }
}

@Composable
private fun ReminderNotificationScreen(
    data: AppData,
    notify: (String) -> Unit,
    saveData: (AppData) -> Unit,
    replace: (AppScreen) -> Unit,
) {
    val reminder = data.reminders.lastOrNull()
    Column(modifier = Modifier.fillMaxSize().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Notifications, null, modifier = Modifier.size(54.dp), tint = Color(0xFFF2A900))
        Text("Revision Reminder", modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.Bold)
        Text("Time to revise\n${reminder?.topic.orEmpty()}\n(${reminder?.subject.orEmpty()})", modifier = Modifier.padding(vertical = 16.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PrimaryButton("Mark as Done", {
            if (reminder != null) {
                val nextDate = runCatching { LocalDate.parse(reminder.date).plusDays(reminder.intervalDays.toLong()).toString() }.getOrDefault(reminder.date)
                saveData(data.copy(reminders = data.reminders.map { if (it.id == reminder.id) it.copy(done = true) else it } + reminder.copy(id = java.util.UUID.randomUUID().toString(), date = nextDate, done = false)))
            }
            replace(AppScreen.RevisionHistory)
        })
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Snooze (1 Hour)", { notify("Reminder snoozed for one hour."); replace(AppScreen.RevisionHistory) })
    }
}

@Composable
private fun RevisionHistoryScreen(data: AppData, back: () -> Unit, go: (AppScreen) -> Unit, saveData: (AppData) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
        AppHeader("Revision History", back, trailing = { IconButton(onClick = { go(AppScreen.SetReminder) }) { Icon(Icons.Default.Add, "Add reminder") } })
        data.reminders.sortedBy { it.date }.forEach { reminder ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    saveData(data.copy(reminders = data.reminders.map { if (it.id == reminder.id) it.copy(done = !it.done) else it }))
                }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (reminder.done) Icons.Default.CheckCircle else Icons.Default.Snooze, null, tint = if (reminder.done) Color(0xFF29A267) else Color(0xFFE0A12E))
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(formatDateOrRaw(reminder.date), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("${reminder.topic} · ${reminder.subject}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Text(if (reminder.done) "Completed" else "Pending ›", fontSize = 11.sp, color = if (reminder.done) Color(0xFF29A267) else Color(0xFFE0A12E))
            }
            Divider()
        }
        if (data.reminders.isEmpty()) EmptyMessage("No reminders yet.") else Unit
    }
}

@Composable
private fun AddResourceScreen(
    data: AppData,
    appStore: LocalAppStore,
    notify: (String) -> Unit,
    back: () -> Unit,
    save: (StudyResource) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf("Link") }
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var subject by remember(data.subjects) { mutableStateOf(data.subjects.firstOrNull() ?: ALL_SUBJECTS.first()) }
    var topic by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri
        selectedName = uri?.let { context.contentResolver.queryDisplayName(it) }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppHeader("Add Resource", back, trailing = { Icon(Icons.Default.Search, "Search") })
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Link", "PDF", "Note", "Video").forEach { option ->
                androidx.compose.material3.FilterChip(
                    selected = type == option,
                    onClick = { type = option; selectedUri = null; selectedName = null; url = "" },
                    label = { Text(option) },
                )
            }
        }
        AppTextField(title, { title = it }, "Title")
        if (type == "PDF" || type == "Note") {
            SecondaryButton(
                selectedName?.let { "Selected: $it" } ?: if (type == "PDF") "Choose PDF" else "Choose note file",
                { filePicker.launch(if (type == "PDF") "application/pdf" else "*/*") },
            )
            Text("Files stay in this app on this device. Maximum file size: 25 MB.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            AppTextField(url, { url = it }, if (type == "Video") "Video URL" else "URL")
        }
        ChoiceField("Subject", subject, data.subjects.ifEmpty { ALL_SUBJECTS }) { subject = it }
        AppTextField(topic, { topic = it }, "Topic")
        PrimaryButton(if (isSaving) "Saving..." else "Save Resource", {
            if (title.isBlank()) {
                notify("Enter a title.")
                return@PrimaryButton
            }
            if ((type == "PDF" || type == "Note") && selectedUri == null) {
                notify("Choose a file to upload.")
                return@PrimaryButton
            }
            if (type == "PDF" && selectedUri != null && context.contentResolver.getType(selectedUri!!)?.let { it != "application/pdf" } == true) {
                notify("Choose a PDF file.")
                return@PrimaryButton
            }
            val safeUrl = url.trim()
            if (type == "Link" || type == "Video") {
                val parsed = runCatching { Uri.parse(safeUrl) }.getOrNull()
                if (parsed == null || parsed.scheme !in listOf("http", "https") || parsed.host.isNullOrBlank()) {
                    notify("Enter a valid http or https URL.")
                    return@PrimaryButton
                }
            }
            scope.launch {
                if (isSaving) return@launch
                isSaving = true
                try {
                    val stored = selectedUri?.let { uri ->
                        withContext(Dispatchers.IO) { appStore.saveResource(data.accountId, context.contentResolver, uri) }
                    }
                    save(
                        StudyResource(
                            type = type,
                            title = title.trim(),
                            url = if (stored == null) safeUrl else "",
                            subject = subject,
                            topic = topic.trim(),
                            fileName = stored?.first,
                            originalFileName = stored?.second,
                        ),
                    )
                } catch (error: Exception) {
                    notify(error.message ?: "Unable to save this resource.")
                } finally {
                    isSaving = false
                }
            }
        }, enabled = !isSaving)
    }
}

@Composable
private fun ResourcesScreen(data: AppData, go: (AppScreen) -> Unit, appStore: LocalAppStore, notify: (String) -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf("All") }
    val filtered = data.resources.filter { tab == "All" || it.type == tab }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
        AppHeader("Resource Vault", trailing = { IconButton(onClick = { go(AppScreen.AddResource) }) { Icon(Icons.Default.Add, "Add resource") } })
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("All", "Note", "PDF", "Link", "Video").forEach { option ->
                androidx.compose.material3.FilterChip(selected = tab == option, onClick = { tab = option }, label = { Text(if (option == "Note") "Notes" else if (option == "Video") "Videos" else option) })
            }
        }
        if (filtered.isEmpty()) {
            EmptyMessage("Nothing here yet.", button = "Add Resource") { go(AppScreen.AddResource) }
        } else {
            filtered.forEach { resource ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        try {
                            val intent = if (resource.fileName != null) {
                                val file = appStore.resourceFile(data.accountId, resource.fileName)
                                    ?: error("This file is not available on this device.")
                                Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(
                                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file),
                                        when (resource.type) {
                                            "PDF" -> "application/pdf"
                                            "Note" -> "*/*"
                                            else -> "application/octet-stream"
                                        },
                                    )
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            } else {
                                val uri = Uri.parse(resource.url)
                                require(uri.scheme == "http" || uri.scheme == "https") { "This resource URL is invalid." }
                                Intent(Intent.ACTION_VIEW, uri)
                            }
                            context.startActivity(intent)
                        } catch (error: Exception) {
                            notify(error.message ?: "Unable to open this resource.")
                        }
                    }.padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(resourceIcon(resource.type), null, modifier = Modifier.size(25.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(resource.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                        Text(resource.originalFileName ?: resource.topic.ifBlank { resource.url }, fontSize = 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Divider()
            }
        }
    }
}

@Composable
private fun ProgressScreen(data: AppData, go: (AppScreen) -> Unit, setSelectedSubject: (String) -> Unit) {
    val values = data.subjects.associateWith { calculateSubjectProgress(data, it) }
    val overall = if (values.isEmpty()) 0 else values.values.average().toInt()
    val done = data.tasks.count { it.done }
    val pending = data.tasks.size - done
    val due = data.reminders.count { !it.done }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
        AppHeader("Progress Overview", trailing = { IconButton(onClick = { go(AppScreen.ProgressAnalysis) }) { Icon(Icons.Default.BarChart, "Progress analysis") } })
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Overall Progress", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("$overall%", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        ProgressBar(overall, Color(0xFF2CA16A), Modifier.padding(top = 8.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatValue(done, "Completed")
                StatValue(pending, "Pending")
                StatValue(due, "Revision Due")
            }
        }
        Text("Subject Progress", modifier = Modifier.padding(top = 18.dp, bottom = 8.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        data.subjects.forEach { subject ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { setSelectedSubject(subject); go(AppScreen.SubjectProgress) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(subject, modifier = Modifier.weight(1.2f), fontSize = 12.sp)
                ProgressBar(values[subject] ?: 0, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                Text("${values[subject] ?: 0}%", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
        SecondaryButton("Exam Preparation Mode", { go(AppScreen.ExamSetup) }, Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun SubjectProgressScreen(data: AppData, subject: String, back: () -> Unit, saveData: (AppData) -> Unit) {
    val topics = SUBJECT_TOPICS[subject].orEmpty()
    val checked = data.completedTopics[subject].orEmpty()
    val value = calculateSubjectProgress(data, subject)
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
        AppHeader("Subject Progress", back)
        Text(subject, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator(progress = value / 100f, modifier = Modifier.size(146.dp), strokeWidth = 12.dp, strokeCap = StrokeCap.Round)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$value%", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("Completed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Topics", fontWeight = FontWeight.Bold)
        topics.forEach { topic ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    val updated = checked.toMutableMap()
                    if (topic in updated) updated.remove(topic) else updated[topic] = System.currentTimeMillis()
                    saveData(data.copy(completedTopics = data.completedTopics + (subject to updated)))
                }.padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = topic in checked, onCheckedChange = {
                    val updated = checked.toMutableMap()
                    if (it) updated[topic] = System.currentTimeMillis() else updated.remove(topic)
                    saveData(data.copy(completedTopics = data.completedTopics + (subject to updated)))
                })
                Text(topic, fontSize = 14.sp)
            }
            Divider()
        }
    }
}

@Composable
private fun ProgressAnalysisScreen(data: AppData, back: () -> Unit) {
    var tab by remember { mutableStateOf("Completed") }
    val weeks = calculateWeeklyActivity(data)
    val current = weeks.lastOrNull()
    val maximum = maxOf(1, weeks.maxOfOrNull { if (tab == "Completed") it.activities else it.minutes } ?: 0)
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
        AppHeader("Progress Analysis", back)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(modifier = Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("THIS WEEK · ${current?.range.orEmpty()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("${current?.activities ?: 0} activities completed", fontSize = 13.sp)
                }
                Text("%.1fh".format(Locale.getDefault(), (current?.minutes ?: 0) / 60f), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
        }
        Row(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Completed", "Study Time").forEach { option ->
                androidx.compose.material3.FilterChip(selected = tab == option, onClick = { tab = option }, label = { Text(option) })
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth().padding(15.dp)) {
                Text(if (tab == "Completed") "Weekly activity" else "Study time by week", fontWeight = FontWeight.Bold)
                Text(if (tab == "Completed") "Completed tasks and checked topics" else "Hours from completed tasks", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                if (tab == "Completed") {
                    Row(modifier = Modifier.fillMaxWidth().height(190.dp).padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                        weeks.forEach { week ->
                            val count = week.activities
                            Column(modifier = Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                Text(count.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier.fillMaxWidth(0.65f).height((maxOf(4, (count * 112 / maximum))).dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(if (week.current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                )
                                Text(week.range, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                Text(if (week.current) "This week" else " ", fontSize = 8.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                } else {
                    StudyHoursChart(weeks)
                }
            }
        }
        if (weeks.none { it.activities > 0 }) EmptyMessage("Complete a task or check a topic to start your weekly chart.")
    }
}

@Composable
private fun StudyHoursChart(weeks: List<WeekActivity>) {
    val maxMinutes = maxOf(1, weeks.maxOfOrNull { it.minutes } ?: 0)
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp).padding(vertical = 12.dp)) {
        val left = 20.dp.toPx()
        val right = size.width - 20.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = size.height - 18.dp.toPx()
        val step = if (weeks.size > 1) (right - left) / (weeks.size - 1) else 0f
        repeat(3) { index ->
            val y = top + index * (bottom - top) / 2f
            drawLine(Color.LightGray.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(left, y), androidx.compose.ui.geometry.Offset(right, y), strokeWidth = 1.dp.toPx())
        }
        val points = weeks.mapIndexed { index, week ->
            val ratio = week.minutes.toFloat() / maxMinutes
            androidx.compose.ui.geometry.Offset(left + index * step, bottom - ratio * (bottom - top))
        }
        points.zipWithNext().forEach { (start, end) ->
            drawLine(lineColor, start, end, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        points.forEach { drawCircle(lineColor, radius = 5.dp.toPx(), center = it) }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        weeks.forEach { week ->
            Text("%.1fh\n%s".format(Locale.getDefault(), week.minutes / 60f, week.range), fontSize = 9.sp, textAlign = TextAlign.Center)
        }
    }
}

    @Composable
    private fun ExamSetupScreen(data: AppData, back: () -> Unit, notify: (String) -> Unit, generate: (String, String, List<ExamItem>) -> Unit) {
        var subject by remember(data.examSubject, data.subjects) {
            mutableStateOf(data.examSubject.ifBlank { data.subjects.firstOrNull() ?: ALL_SUBJECTS.first() })
        }
        var date by remember(data.examDate) { mutableStateOf(data.examDate.ifBlank { LocalDate.now().plusDays(20).toString() }) }
        val daysLeft = runCatching { ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(date)).coerceAtLeast(0).toInt() }.getOrDefault(0)
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppHeader("Exam Preparation", back)
            ChoiceField("Select Subject", subject, data.subjects.ifEmpty { ALL_SUBJECTS }) { subject = it }
            AppTextField(date, { date = it }, "Exam Date (YYYY-MM-DD)")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.fillMaxWidth().padding(15.dp)) {
                    Text("Days Left", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$daysLeft days", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            PrimaryButton("Generate Study Plan", {
                val examDate = runCatching { LocalDate.parse(date.trim()) }.getOrNull()
                if (examDate == null || examDate.isBefore(LocalDate.now())) {
                    notify("Enter an exam date today or later.")
                    return@PrimaryButton
                }
                val topics = SUBJECT_TOPICS[subject].orEmpty() + "Revision & Mock Test"
                val days = (ChronoUnit.DAYS.between(LocalDate.now(), examDate).toInt() + 1).coerceAtLeast(1)
                val perTopic = (days / topics.size).coerceAtLeast(1)
                val plan = topics.mapIndexed { index, topic ->
                    val start = LocalDate.now().plusDays(index * perTopic.toLong())
                    val end = start.plusDays((perTopic - 1).toLong())
                    val range = if (perTopic > 1) "${shortDate(start)} – ${shortDate(end)}" else shortDate(start)
                    ExamItem(range, topic)
                }
                generate(subject, date.trim(), plan)
            })
        }
    }

    @Composable
    private fun ExamPlanScreen(data: AppData, back: () -> Unit, addToPlanner: () -> Unit) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            AppHeader("Study Plan (${data.examSubject.split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString("")})", back)
            data.examPlan.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text(item.range, modifier = Modifier.width(95.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.topic, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Divider()
            }
            PrimaryButton("Add to Planner", addToPlanner, Modifier.padding(top = 18.dp))
        }
    }

    @Composable
    private fun ExamProgressScreen(data: AppData, back: () -> Unit, replace: (AppScreen) -> Unit, saveData: (AppData) -> Unit) {
        val progress = if (data.examPlan.isEmpty()) 0 else data.examPlan.count { it.done } * 100 / data.examPlan.size
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            AppHeader("Exam Preparation", back)
            Text(data.examSubject, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(progress = progress / 100f, modifier = Modifier.size(130.dp), strokeWidth = 11.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$progress%", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Completed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            data.examPlan.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        saveData(data.copy(examPlan = data.examPlan.mapIndexed { i, plan -> if (i == index) plan.copy(done = !plan.done) else plan }))
                    }.padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(item.done, onCheckedChange = {
                        saveData(data.copy(examPlan = data.examPlan.mapIndexed { i, plan -> if (i == index) plan.copy(done = it) else plan }))
                    })
                    Text(item.topic, fontSize = 13.sp)
                }
            }
            PrimaryButton("View Resources", { replace(AppScreen.Resources) }, Modifier.padding(top = 16.dp))
        }
    }

    @Composable
    private fun ProfileScreen(
        data: AppData,
        go: (AppScreen) -> Unit,
        back: () -> Unit,
        replace: (AppScreen) -> Unit,
        accountStore: LocalAccountStore,
        saveData: (AppData) -> Unit,
        notify: (String) -> Unit,
    ) {
        val scope = rememberCoroutineScope()
        var signingOut by remember { mutableStateOf(false) }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            AppHeader("Profile", back, trailing = { IconButton(onClick = { go(AppScreen.Settings) }) { Icon(Icons.Default.Settings, "Settings") } })
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ProfileAvatar(data, Modifier.size(82.dp))
                Text(data.name.ifBlank { "Student Name" }, modifier = Modifier.padding(top = 7.dp), fontWeight = FontWeight.Bold)
                Text(data.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ProfileMenu(Icons.Default.Edit, "Edit Profile") { go(AppScreen.EditProfile) }
            ProfileMenu(Icons.Default.MenuBook, "Manage Subjects") { go(AppScreen.ManageSubjects) }
            ProfileMenu(Icons.Default.Notifications, "Notifications") { go(AppScreen.RevisionHistory) }
            ProfileMenu(Icons.Default.Tune, "App Theme") { go(AppScreen.Settings) }
            ProfileMenu(Icons.Default.Info, "Help & Support") { notify("Email us at support@vidyapath.app") }
            Row(
                modifier = Modifier.fillMaxWidth().clickable(enabled = !signingOut) {
                    scope.launch {
                        signingOut = true
                        try {
                            accountStore.signOut()
                            saveData(AppData())
                            replace(AppScreen.Login)
                        } catch (error: Exception) {
                            notify(error.message ?: "Unable to log out.")
                        } finally {
                            signingOut = false
                        }
                    }
                }.padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error)
                Text(if (signingOut) "Signing out..." else "Logout", modifier = Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }
        }
    }

    @Composable
    private fun EditProfileScreen(
        data: AppData,
        appStore: LocalAppStore,
        saveProfile: suspend (String, String?) -> Unit,
        back: () -> Unit,
        notify: (String) -> Unit,
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var name by remember(data.name) { mutableStateOf(data.name) }
        var photoName by remember(data.avatarFileName) { mutableStateOf(data.avatarFileName) }
        var saving by remember { mutableStateOf(false) }
        var pendingCameraFile by remember { mutableStateOf<java.io.File?>(null) }

        val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val source = pendingCameraFile
            if (success && source != null) {
                scope.launch {
                    try {
                        photoName = withContext(Dispatchers.IO) { appStore.savePhotoFile(data.accountId, source) }
                        notify("Camera photo selected.")
                    } catch (error: Exception) {
                        notify(error.message ?: "Unable to save the camera photo.")
                    } finally {
                        source.delete()
                        pendingCameraFile = null
                    }
                }
            } else {
                source?.delete()
                pendingCameraFile = null
            }
        }
        val uploader = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        photoName = withContext(Dispatchers.IO) { appStore.savePhoto(data.accountId, context.contentResolver, uri) }
                        notify("Profile photo selected.")
                    } catch (error: Exception) {
                        notify(error.message ?: "Unable to save this photo.")
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppHeader("Edit Profile", back)
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                ProfileAvatar(data.copy(avatarFileName = photoName), Modifier.size(96.dp))
                Text("Add a profile photo", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Take Photo", {
                        try {
                            val file = appStore.newCameraPhotoFile(data.accountId).apply { createNewFile() }
                            pendingCameraFile = file
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            camera.launch(uri)
                        } catch (error: Exception) {
                            notify(error.message ?: "Unable to open the camera.")
                        }
                    }, Modifier.weight(1f), icon = Icons.Default.CameraAlt)
                    SecondaryButton("Upload Photo", { uploader.launch("image/*") }, Modifier.weight(1f), icon = Icons.Default.FolderOpen)
                }
                if (photoName != null) {
                    TextButton(onClick = { photoName = null }) {
                        Icon(Icons.Default.DeleteOutline, null)
                        Text("Remove photo")
                    }
                }
            }
            AppTextField(name, { name = it }, "Full Name")
            OutlinedTextField(
                value = data.email,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                readOnly = true,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
            Text("Profile photos are saved privately on this device.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrimaryButton(if (saving) "Saving..." else "Save Changes", {
                scope.launch {
                    if (saving) return@launch
                    saving = true
                    try {
                        saveProfile(name, photoName)
                        notify("Profile updated.")
                        back()
                    } catch (error: Exception) {
                        notify(error.message ?: "Unable to update your profile.")
                    } finally {
                        saving = false
                    }
                }
            }, enabled = !saving)
        }
    }

    @Composable
    private fun SettingsScreen(data: AppData, back: () -> Unit, saveData: (AppData) -> Unit, notify: (String) -> Unit) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            AppHeader("Settings", back)
            SettingSwitchRow("Notifications", Icons.Default.Notifications, data.notifications) {
                saveData(data.copy(notifications = it))
                notify(if (it) "Notifications enabled." else "Notifications disabled.")
            }
            SettingSwitchRow("Dark Mode", Icons.Default.Tune, data.darkMode) { saveData(data.copy(darkMode = it)) }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Language", modifier = Modifier.weight(1f).padding(start = 12.dp), fontSize = 14.sp)
                ChoiceField("", data.language, listOf("English", "Hindi", "Tamil", "Telugu")) { saveData(data.copy(language = it)) }
            }
            ProfileMenu(Icons.Default.Info, "About Vidyapath") { notify("Vidyapath v1.0 · Plan. Learn. Revise. Stay on Track.") }
        }
    }

    @Composable
    private fun SettingSwitchRow(label: String, icon: ImageVector, checked: Boolean, onChecked: (Boolean) -> Unit) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, modifier = Modifier.weight(1f).padding(start = 12.dp), fontSize = 14.sp)
            Switch(checked = checked, onCheckedChange = onChecked)
        }
        Divider()
    }

    @Composable
    private fun AppHeader(title: String, back: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (back != null) {
                IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Back") }
            } else {
                Spacer(Modifier.size(48.dp))
            }
            Text(title, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                trailing?.invoke()
            }
        }
    }

    @Composable
    private fun Logo(large: Boolean = false) {
        val size = if (large) 78.dp else 38.dp
        Box(
            modifier = Modifier.size(size).clip(RoundedCornerShape(if (large) 22.dp else 12.dp)).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.School, contentDescription = "Vidyapath", modifier = Modifier.size(if (large) 44.dp else 22.dp), tint = Color.White)
        }
    }

    @Composable
    private fun PrimaryButton(
        label: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.fillMaxWidth().heightIn(min = 50.dp),
            shape = RoundedCornerShape(13.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 13.dp),
            colors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)),
        ) {
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    }

    @Composable
    private fun SecondaryButton(
        label: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        icon: ImageVector? = null,
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(13.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 11.dp),
        ) {
            if (icon != null) {
                Icon(icon, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }

    @Composable
    private fun AppTextField(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        keyboardType: KeyboardType = KeyboardType.Text,
        password: Boolean = false,
    ) {
        var passwordVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (password && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            trailingIcon = if (password) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        )
                    }
                }
            } else {
                null
            },
            shape = RoundedCornerShape(13.dp),
        )
    }

    @Composable
    private fun ChoiceField(label: String, value: String, options: List<String>, onSelected: (String) -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        Column {
            if (label.isNotBlank()) Text(label, modifier = Modifier.padding(bottom = 4.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                OutlinedTextField(
                    value = value,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp),
                    trailingIcon = { Icon(Icons.Default.ArrowForward, null, modifier = Modifier.clickable { expanded = true }.size(18.dp)) },
                )
                Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onSelected(option)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TaskRow(task: StudyTask, onToggle: () -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = task.done, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(task.topic, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                Text("${task.minutes} minutes · ${task.priority}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Divider()
    }

    @Composable
    private fun SectionTitle(title: String, action: String, onAction: () -> Unit) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onAction) { Text(action, fontSize = 11.sp) }
        }
    }

    @Composable
    private fun EmptyMessage(text: String, button: String? = null, onButtonClick: () -> Unit = {}) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            if (button != null) TextButton(onClick = onButtonClick) { Text(button) }
        }
    }

    @Composable
    private fun ProgressBar(value: Int, color: Color, modifier: Modifier = Modifier) {
        Box(modifier = modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(modifier = Modifier.fillMaxWidth(value.coerceIn(0, 100) / 100f).fillMaxHeight().clip(CircleShape).background(color))
        }
    }

    @Composable
    private fun StatValue(value: Int, label: String) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    @Composable
    private fun ProfileMenu(icon: ImageVector, label: String, onClick: () -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, modifier = Modifier.weight(1f).padding(start = 12.dp), fontSize = 14.sp)
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Divider()
    }

    @Composable
    private fun ProfileAvatar(data: AppData, modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val file = remember(data.accountId, data.avatarFileName) { LocalAppStore(context).photoFile(data.accountId, data.avatarFileName) }
        val bitmap = remember(file?.absolutePath) { file?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() } }
        Box(modifier = modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(bitmap, contentDescription = "${data.name}'s profile photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }

    private fun resourceIcon(type: String): ImageVector = when (type) {
        "PDF" -> Icons.Default.Book
        "Note" -> Icons.Default.StickyNote2
        "Video" -> Icons.Default.PlayCircle
        else -> Icons.Default.Link
    }

    private fun formatDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))

    private fun formatDateOrRaw(date: String): String =
        runCatching { formatDate(LocalDate.parse(date)) }.getOrDefault(date)

    private fun shortDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
