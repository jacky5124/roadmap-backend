package dev.roadmap.tasktracker

import java.time.Instant

enum class TaskStatus(val value: String) {
    TODO("todo"),
    IN_PROGRESS("in-progress"),
    DONE("done");

    companion object {
        fun fromValue(value: String): TaskStatus =
            entries.firstOrNull { it.value == value }
                ?: throw TaskTrackerException("Unknown task status: $value")
    }
}

data class Task(
    val id: Int,
    val description: String,
    val status: TaskStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

class TaskTrackerException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
