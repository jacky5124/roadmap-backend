package dev.roadmap.tasktracker

import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Clock
import java.time.Instant

class TaskRepository(
    private val file: Path,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun add(description: String): Task {
        requireDescription(description)
        val tasks = load().toMutableList()
        val nextId = (tasks.maxOfOrNull(Task::id) ?: 0)
            .let { if (it == Int.MAX_VALUE) throw TaskTrackerException("No more task IDs are available") else it + 1 }
        val now = Instant.now(clock)
        val task = Task(nextId, description, TaskStatus.TODO, now, now)
        tasks += task
        save(tasks)
        return task
    }

    fun update(id: Int, description: String): Task {
        requireDescription(description)
        return replace(id) { task ->
            task.copy(description = description, updatedAt = Instant.now(clock))
        }
    }

    fun mark(id: Int, status: TaskStatus): Task = replace(id) { task ->
        task.copy(status = status, updatedAt = Instant.now(clock))
    }

    fun delete(id: Int) {
        val tasks = load().toMutableList()
        if (!tasks.removeIf { it.id == id }) throw TaskTrackerException("Task $id was not found")
        save(tasks)
    }

    fun list(status: TaskStatus? = null, notDone: Boolean = false): List<Task> =
        load().filter { task ->
            when {
                notDone -> task.status != TaskStatus.DONE
                status != null -> task.status == status
                else -> true
            }
        }

    private fun replace(id: Int, transform: (Task) -> Task): Task {
        val tasks = load().toMutableList()
        val index = tasks.indexOfFirst { it.id == id }
        if (index == -1) throw TaskTrackerException("Task $id was not found")
        return transform(tasks[index]).also {
            tasks[index] = it
            save(tasks)
        }
    }

    private fun load(): List<Task> {
        ensureFile()
        return try {
            TaskJson.decode(Files.readString(file, StandardCharsets.UTF_8))
        } catch (exception: TaskTrackerException) {
            throw exception
        } catch (exception: Exception) {
            throw TaskTrackerException("Could not read task file ${file.toAbsolutePath()}", exception)
        }
    }

    private fun save(tasks: List<Task>) {
        ensureFile()
        val absoluteFile = file.toAbsolutePath()
        val parent = absoluteFile.parent
        val temporary = try {
            Files.createTempFile(parent, ".tasks-", ".json")
        } catch (exception: Exception) {
            throw TaskTrackerException("Could not create a temporary task file", exception)
        }

        try {
            Files.writeString(temporary, TaskJson.encode(tasks), StandardCharsets.UTF_8)
            try {
                Files.move(
                    temporary,
                    absoluteFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, absoluteFile, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (exception: Exception) {
            Files.deleteIfExists(temporary)
            throw TaskTrackerException("Could not save task file $absoluteFile", exception)
        }
    }

    private fun ensureFile() {
        try {
            val absoluteFile = file.toAbsolutePath()
            absoluteFile.parent?.let(Files::createDirectories)
            if (Files.notExists(absoluteFile)) {
                try {
                    Files.writeString(
                        absoluteFile,
                        "[]\n",
                        StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE_NEW,
                    )
                } catch (_: java.nio.file.FileAlreadyExistsException) {
                    // Another process created it first.
                }
            }
        } catch (exception: Exception) {
            throw TaskTrackerException("Could not initialize task file ${file.toAbsolutePath()}", exception)
        }
    }

    private fun requireDescription(description: String) {
        if (description.isBlank()) throw TaskTrackerException("Description must not be blank")
    }
}
