package com.vidyapath.android

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class WeekActivity(val range: String, val activities: Int, val minutes: Int, val current: Boolean)

fun calculateSubjectProgress(data: AppData, subject: String): Int {
    val topicCount = SUBJECT_TOPICS[subject].orEmpty().size
    val taskCount = data.tasks.count { it.subject == subject }
    val total = topicCount + taskCount
    if (total == 0) return 0
    val done = data.tasks.count { it.subject == subject && it.done } +
        SUBJECT_TOPICS[subject].orEmpty().count { it in data.completedTopics[subject].orEmpty() }
    return done * 100 / total
}

fun calculateWeeklyActivity(data: AppData, today: LocalDate = LocalDate.now()): List<WeekActivity> {
    val currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (-3L..0L).map { offset ->
        val start = currentMonday.plusWeeks(offset)
        val end = start.plusDays(6)
        val completedTasks = data.tasks.filter { task ->
            task.done && task.completedAt?.let { timestamp ->
                Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().let { it in start..end }
            } == true
        }
        val completedTopics = data.completedTopics.values.flatMap { it.values }.count { timestamp ->
            Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().let { it in start..end }
        }
        WeekActivity(
            range = "${start.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))}–${end.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))}",
            activities = completedTasks.size + completedTopics,
            minutes = completedTasks.sumOf { it.minutes },
            current = offset == 0L,
        )
    }
}
