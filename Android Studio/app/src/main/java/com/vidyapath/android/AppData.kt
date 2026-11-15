package com.vidyapath.android

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

data class StudyTask(
    val id: String = UUID.randomUUID().toString(),
    val subject: String,
    val topic: String,
    val date: String,
    val minutes: Int,
    val priority: String,
    val done: Boolean = false,
    val completedAt: Long? = null,
)

data class StudyReminder(
    val id: String = UUID.randomUUID().toString(),
    val subject: String,
    val topic: String,
    val date: String,
    val intervalDays: Int,
    val done: Boolean = false,
)

data class StudyResource(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val title: String,
    val url: String,
    val subject: String,
    val topic: String,
    val fileName: String? = null,
    val originalFileName: String? = null,
)

data class ExamItem(val range: String, val topic: String, val done: Boolean = false)

data class AppData(
    val accountId: String = "",
    val name: String = "",
    val email: String = "",
    val onboarded: Boolean = false,
    val subjects: List<String> = DEFAULT_SUBJECTS,
    val tasks: List<StudyTask> = emptyList(),
    val reminders: List<StudyReminder> = emptyList(),
    val resources: List<StudyResource> = emptyList(),
    val completedTopics: Map<String, Map<String, Long>> = emptyMap(),
    val examSubject: String = "",
    val examDate: String = "",
    val examPlan: List<ExamItem> = emptyList(),
    val notifications: Boolean = true,
    val darkMode: Boolean = false,
    val language: String = "English",
    val avatarFileName: String? = null,
)

val ALL_SUBJECTS = listOf(
    "Data Structures",
    "Database Management",
    "Operating Systems",
    "Computer Networks",
    "Mathematics",
    "Software Engineering",
)

val DEFAULT_SUBJECTS = ALL_SUBJECTS.filterNot { it == "Computer Networks" }

val SUBJECT_TOPICS = mapOf(
    "Data Structures" to listOf("Introduction", "Arrays", "Linked Lists", "Stacks", "Queues"),
    "Operating Systems" to listOf("Process Management", "CPU Scheduling", "Memory Management", "File Systems"),
    "Database Management" to listOf("Introduction", "ER Model", "SQL", "Normalization"),
    "Computer Networks" to listOf("OSI Model", "TCP/IP", "Routing", "Security"),
    "Mathematics" to listOf("Calculus", "Linear Algebra", "Probability"),
    "Software Engineering" to listOf("SDLC", "Agile", "Testing", "UML"),
)

class LocalAppStore(context: Context) {
    private val preferences = context.getSharedPreferences("vidyapath_study_data", Context.MODE_PRIVATE)
    private val filesRoot = File(context.filesDir, "account_files").apply { mkdirs() }

    fun load(accountId: String): AppData {
        val raw = preferences.getString(stateKey(accountId), null) ?: return AppData(accountId = accountId)
        return decodeState(JSONObject(raw), accountId)
    }

    fun save(data: AppData) {
        if (data.accountId.isBlank()) return
        preferences.edit().putString(stateKey(data.accountId), encodeState(data).toString()).apply()
    }

    fun photoFile(accountId: String, fileName: String?): File? {
        if (fileName.isNullOrBlank()) return null
        val file = File(accountDirectory(accountId), fileName)
        return file.takeIf { it.isFile && it.canonicalFile.parentFile == accountDirectory(accountId).canonicalFile }
    }

    fun newCameraPhotoFile(accountId: String): File =
        File(accountDirectory(accountId), "capture-${UUID.randomUUID()}.jpg")

    fun deletePhoto(accountId: String, fileName: String?) {
        photoFile(accountId, fileName)?.delete()
    }

    fun savePhoto(accountId: String, resolver: ContentResolver, uri: Uri): String {
        val sourceSize = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
        require(sourceSize == null || sourceSize <= MAX_PHOTO_BYTES) { "Choose an image smaller than 5 MB." }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: error("Unable to read the selected photo.")
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "The selected file is not a readable image." }
        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Unable to decode the selected photo.")
        return savePhotoBitmap(accountId, bitmap)
    }

    fun savePhotoFile(accountId: String, source: File): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "The camera did not return a readable photo." }
        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
        val bitmap = BitmapFactory.decodeFile(source.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: error("Unable to decode the camera photo.")
        return savePhotoBitmap(accountId, bitmap)
    }

    fun saveResource(accountId: String, resolver: ContentResolver, uri: Uri): Pair<String, String> {
        val originalName = resolver.queryDisplayName(uri)?.take(120) ?: "resource"
        val extension = originalName.substringAfterLast('.', "").filter { it.isLetterOrDigit() }.take(8)
        val fileName = UUID.randomUUID().toString() + if (extension.isBlank()) "" else ".$extension"
        val destination = File(accountDirectory(accountId), fileName)
        var total = 0L
        try {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_RESOURCE_BYTES) { "Choose a file smaller than 25 MB." }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: error("Unable to open the selected file.")
        } catch (error: Exception) {
            destination.delete()
            throw error
        }
        if (total == 0L) {
            destination.delete()
            error("The selected file is empty.")
        }
        return fileName to originalName
    }

    fun resourceFile(accountId: String, fileName: String?): File? {
        if (fileName.isNullOrBlank()) return null
        val file = File(accountDirectory(accountId), fileName)
        return file.takeIf { it.isFile && it.canonicalFile.parentFile == accountDirectory(accountId).canonicalFile }
    }

    private fun savePhotoBitmap(accountId: String, bitmap: Bitmap): String {
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        val square = Bitmap.createBitmap(bitmap, x, y, size, size)
        val scaled = Bitmap.createScaledBitmap(square, 512, 512, true)
        val name = "profile-${UUID.randomUUID()}.jpg"
        val output = File(accountDirectory(accountId), name)
        FileOutputStream(output).use { stream ->
            check(scaled.compress(Bitmap.CompressFormat.JPEG, 86, stream)) { "Unable to save the profile photo." }
        }
        if (scaled !== square) scaled.recycle()
        if (square !== bitmap) square.recycle()
        bitmap.recycle()
        return name
    }

    private fun accountDirectory(accountId: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(accountId.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(filesRoot, digest).apply { mkdirs() }
    }

    private fun stateKey(accountId: String) = "state:${Base64.encodeToString(accountId.toByteArray(), Base64.NO_WRAP)}"

    private fun encodeState(state: AppData) = JSONObject().apply {
        put("accountId", state.accountId)
        put("name", state.name)
        put("email", state.email)
        put("onboarded", state.onboarded)
        put("subjects", JSONArray(state.subjects))
        put("tasks", JSONArray().apply {
            state.tasks.forEach { task ->
                put(JSONObject().apply {
                    put("id", task.id); put("subject", task.subject); put("topic", task.topic)
                    put("date", task.date); put("minutes", task.minutes); put("priority", task.priority)
                    put("done", task.done); put("completedAt", task.completedAt ?: JSONObject.NULL)
                })
            }
        })
        put("reminders", JSONArray().apply {
            state.reminders.forEach { reminder ->
                put(JSONObject().apply {
                    put("id", reminder.id); put("subject", reminder.subject); put("topic", reminder.topic)
                    put("date", reminder.date); put("intervalDays", reminder.intervalDays); put("done", reminder.done)
                })
            }
        })
        put("resources", JSONArray().apply {
            state.resources.forEach { resource ->
                put(JSONObject().apply {
                    put("id", resource.id); put("type", resource.type); put("title", resource.title)
                    put("url", resource.url); put("subject", resource.subject); put("topic", resource.topic)
                    put("fileName", resource.fileName ?: JSONObject.NULL)
                    put("originalFileName", resource.originalFileName ?: JSONObject.NULL)
                })
            }
        })
        put("completedTopics", JSONObject().apply {
            state.completedTopics.forEach { (subject, topics) ->
                put(subject, JSONObject().apply { topics.forEach { (topic, timestamp) -> put(topic, timestamp) } })
            }
        })
        put("examSubject", state.examSubject)
        put("examDate", state.examDate)
        put("examPlan", JSONArray().apply {
            state.examPlan.forEach { item -> put(JSONObject().put("range", item.range).put("topic", item.topic).put("done", item.done)) }
        })
        put("notifications", state.notifications)
        put("darkMode", state.darkMode)
        put("language", state.language)
        put("avatarFileName", state.avatarFileName ?: JSONObject.NULL)
    }

    private fun decodeState(json: JSONObject, accountId: String) = AppData(
        accountId = accountId,
        name = json.optString("name"),
        email = json.optString("email"),
        onboarded = json.optBoolean("onboarded"),
        subjects = json.optJSONArray("subjects").toStringList().ifEmpty { DEFAULT_SUBJECTS },
        tasks = json.optJSONArray("tasks").toObjectList { item ->
            StudyTask(
                id = item.optString("id", UUID.randomUUID().toString()),
                subject = item.optString("subject"), topic = item.optString("topic"),
                date = item.optString("date"), minutes = item.optInt("minutes", 45),
                priority = item.optString("priority", "Medium"), done = item.optBoolean("done"),
                completedAt = item.optLongOrNull("completedAt"),
            )
        },
        reminders = json.optJSONArray("reminders").toObjectList { item ->
            StudyReminder(
                id = item.optString("id", UUID.randomUUID().toString()),
                subject = item.optString("subject"), topic = item.optString("topic"),
                date = item.optString("date"), intervalDays = item.optInt("intervalDays", 3),
                done = item.optBoolean("done"),
            )
        },
        resources = json.optJSONArray("resources").toObjectList { item ->
            StudyResource(
                id = item.optString("id", UUID.randomUUID().toString()), type = item.optString("type"),
                title = item.optString("title"), url = item.optString("url"),
                subject = item.optString("subject"), topic = item.optString("topic"),
                fileName = item.optNullableString("fileName"),
                originalFileName = item.optNullableString("originalFileName"),
            )
        },
        completedTopics = json.optJSONObject("completedTopics").let { subjects ->
            if (subjects == null) emptyMap() else subjects.keys().asSequence().associateWith { subject ->
                val topics = subjects.optJSONObject(subject)
                if (topics == null) emptyMap() else topics.keys().asSequence().associateWith { topic ->
                    topics.optLong(topic)
                }
            }
        },
        examSubject = json.optString("examSubject"),
        examDate = json.optString("examDate"),
        examPlan = json.optJSONArray("examPlan").toObjectList { item ->
            ExamItem(item.optString("range"), item.optString("topic"), item.optBoolean("done"))
        },
        notifications = json.optBoolean("notifications", true),
        darkMode = json.optBoolean("darkMode"),
        language = json.optString("language", "English"),
        avatarFileName = json.optNullableString("avatarFileName"),
    )

    private companion object {
        const val MAX_PHOTO_BYTES = 5L * 1024 * 1024
        const val MAX_RESOURCE_BYTES = 25L * 1024 * 1024
    }
}

private fun JSONArray?.toStringList(): List<String> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }

private inline fun <T> JSONArray?.toObjectList(transform: (JSONObject) -> T): List<T> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { index -> optJSONObject(index)?.let(transform) }

private fun JSONObject.optLongOrNull(name: String): Long? =
    if (!has(name) || isNull(name)) null else optLong(name)

private fun JSONObject.optNullableString(name: String): String? =
    if (!has(name) || isNull(name)) null else optString(name).takeIf(String::isNotBlank)

internal fun ContentResolver.queryDisplayName(uri: Uri): String? = query(
    uri,
    arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
    null,
    null,
    null,
)?.use { cursor ->
    if (cursor.moveToFirst()) cursor.getString(0) else null
}
