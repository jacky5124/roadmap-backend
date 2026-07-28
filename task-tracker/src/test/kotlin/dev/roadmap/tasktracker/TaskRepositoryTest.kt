package dev.roadmap.tasktracker

import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TaskRepositoryTest {
    private val instant = Instant.parse("2026-07-27T12:00:00Z")
    private val clock = Clock.fixed(instant, ZoneOffset.UTC)

    @Test
    fun `creates storage and supports the full task lifecycle`() {
        val directory = Files.createTempDirectory("task-tracker-test")
        val file = directory.resolve("tasks.json")
        val repository = TaskRepository(file, clock)

        val first = repository.add("Buy groceries")
        val second = repository.add("Write documentation")
        repository.update(first.id, "Buy groceries and cook dinner")
        repository.mark(first.id, TaskStatus.IN_PROGRESS)
        repository.mark(second.id, TaskStatus.DONE)

        assertEquals(listOf(1, 2), repository.list().map(Task::id))
        assertEquals("Buy groceries and cook dinner", repository.list(TaskStatus.IN_PROGRESS).single().description)
        assertEquals(listOf(1), repository.list(notDone = true).map(Task::id))

        repository.delete(first.id)
        assertEquals(listOf(2), repository.list().map(Task::id))
        assertTrue(Files.readString(file).contains("\"status\": \"done\""))
    }

    @Test
    fun `round trips escaped descriptions`() {
        val file = Files.createTempDirectory("task-tracker-test").resolve("tasks.json")
        val repository = TaskRepository(file, clock)
        val description = "Say \"hello\"\\then\nstart"

        repository.add(description)

        assertEquals(description, TaskRepository(file, clock).list().single().description)
    }

    @Test
    fun `rejects invalid operations and malformed storage`() {
        val file = Files.createTempDirectory("task-tracker-test").resolve("tasks.json")
        val repository = TaskRepository(file, clock)

        assertFailsWith<TaskTrackerException> { repository.add("  ") }
        assertFailsWith<TaskTrackerException> { repository.delete(99) }

        Files.writeString(file, "{broken")
        assertFailsWith<TaskTrackerException> { repository.list() }
    }
}
