package com.vidyapath.android

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyLogicTest {
    @Test
    fun initialSubjectSelectionMatchesTheAppDefaults() {
        assertEquals(5, DEFAULT_SUBJECTS.size)
        assertFalse("Computer Networks" in DEFAULT_SUBJECTS)
    }

    @Test
    fun progressCountsOnlyCheckedTopicsAndCompletedTasks() {
        val data = AppData(
            subjects = listOf("Data Structures"),
            completedTopics = mapOf("Data Structures" to mapOf("Arrays" to 1L)),
            tasks = listOf(
                StudyTask("done", "Data Structures", "My task", "2026-11-15", 30, "Medium", true, 1L),
                StudyTask("pending", "Data Structures", "Another task", "2026-11-15", 30, "Low"),
            ),
        )

        assertEquals(5, SUBJECT_TOPICS.getValue("Data Structures").size)
        assertEquals(28, calculateSubjectProgress(data, "Data Structures"))
        assertEquals(0, calculateSubjectProgress(AppData(), "unknown"))
    }

    @Test
    fun weeklyActivityUsesCompletionTimestampsAndMondayToSundayWeeks() {
        val monday = LocalDate.of(2026, 11, 9)
        val currentTimestamp = monday.plusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val outsideTimestamp = monday.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val data = AppData(
            tasks = listOf(
                StudyTask("current", "Data Structures", "Arrays", monday.toString(), 45, "High", true, currentTimestamp),
                StudyTask("outside", "Data Structures", "Queues", monday.minusDays(1).toString(), 90, "Low", true, outsideTimestamp),
            ),
            completedTopics = mapOf("Data Structures" to mapOf("Stacks" to currentTimestamp)),
        )

        val weeks = calculateWeeklyActivity(data, monday.plusDays(3))

        assertEquals(4, weeks.size)
        assertEquals(2, weeks.last().activities)
        assertEquals(45, weeks.last().minutes)
        assertTrue(weeks.last().current)
        assertEquals(1, weeks[2].activities)
        assertEquals(90, weeks[2].minutes)
    }
}
