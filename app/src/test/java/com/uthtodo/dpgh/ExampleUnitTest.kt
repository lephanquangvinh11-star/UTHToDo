package com.uthtodo.dpgh

import com.uthtodo.dpgh.data.local.AppSettings
import com.uthtodo.dpgh.data.model.Subtask
import com.uthtodo.dpgh.data.model.Task
import com.uthtodo.dpgh.data.model.buildNotificationsFromTasks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun task_firestoreMapAndSubtaskSerialization_isConsistent() {
        val original = Task(
            id = 42,
            title = "Kiểm thử Production UTH Todo",
            note = "Đảm bảo tuần tự hóa Firestore Map chính xác",
            category = "study",
            date = "2026-11-20",
            time = "14:30",
            priority = "high",
            remind = true,
            done = false,
            subtasks = listOf(
                Subtask("Viết Firestore Rules", true),
                Subtask("Chạy ADB E2E Test", false)
            ),
            createdAt = 1700000000000L,
            updatedAt = 1700000050000L
        )

        assertEquals("Học tập", original.categoryLabel)
        assertEquals("Cao", original.priorityLabel)

        val firestoreMap = original.toFirestoreMap("uid_test_123")
        assertEquals("uid_test_123", firestoreMap["uid"])
        assertEquals(42, firestoreMap["id"])
        assertEquals("study", firestoreMap["category"])
        assertEquals("high", firestoreMap["priority"])
        assertEquals("2026-11-20", firestoreMap["date"])
        assertEquals("14:30", firestoreMap["time"])

        val rawSubtasks = firestoreMap["subtasks"] as List<*>
        assertEquals(2, rawSubtasks.size)
        val parsedSubtask0 = Subtask.fromMap(rawSubtasks[0] as Map<*, *>)
        val parsedSubtask1 = Subtask.fromMap(rawSubtasks[1] as Map<*, *>)
        assertEquals("Viết Firestore Rules", parsedSubtask0.title)
        assertTrue(parsedSubtask0.done)
        assertEquals("Chạy ADB E2E Test", parsedSubtask1.title)
        assertFalse(parsedSubtask1.done)
    }

    @Test
    fun notifications_generatedFromTasks_respectsReadState() {
        val tasks = listOf(
            Task(id = 1, title = "Bài tập lớn", date = "2099-12-31", time = "09:00", remind = true, done = false),
            Task(id = 2, title = "Việc đã xong", date = "2099-12-31", time = "10:00", remind = false, done = true)
        )
        val initialNotifs = buildNotificationsFromTasks(tasks, emptySet())
        assertEquals(2, initialNotifs.size)
        assertFalse(initialNotifs[0].isRead)

        val afterMarkRead = buildNotificationsFromTasks(tasks, setOf(initialNotifs[0].id))
        assertTrue(afterMarkRead[0].isRead)
        assertFalse(afterMarkRead[1].isRead)
    }

    @Test
    fun appSettings_accentColor_mapsHexCorrectly() {
        val greenSettings = AppSettings(accentColorHex = "#34C759")
        assertEquals(0xFF34C759.toInt(), greenSettings.accentColor.value.shr(32).toInt())
    }
}
