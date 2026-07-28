package dev.roadmap.tasktracker

import java.io.PrintStream

class TaskCli(
    private val repository: TaskRepository,
    private val output: PrintStream = System.out,
    private val error: PrintStream = System.err,
) {
    fun run(args: Array<String>): Int = try {
        execute(args)
        0
    } catch (exception: TaskTrackerException) {
        error.println("Error: ${exception.message}")
        1
    }

    private fun execute(args: Array<String>) {
        if (args.isEmpty()) throw TaskTrackerException(usage())
        when (args[0]) {
            "add" -> {
                expectSize(args, 2, "add <description>")
                val task = repository.add(args[1])
                output.println("Task added successfully (ID: ${task.id})")
            }
            "update" -> {
                expectSize(args, 3, "update <id> <description>")
                repository.update(parseId(args[1]), args[2])
                output.println("Task updated successfully")
            }
            "delete" -> {
                expectSize(args, 2, "delete <id>")
                repository.delete(parseId(args[1]))
                output.println("Task deleted successfully")
            }
            "mark-in-progress" -> mark(args, TaskStatus.IN_PROGRESS)
            "mark-done" -> mark(args, TaskStatus.DONE)
            "list" -> list(args)
            "help", "--help", "-h" -> {
                expectSize(args, 1, "help")
                output.println(usage())
            }
            else -> throw TaskTrackerException("Unknown command: ${args[0]}\n\n${usage()}")
        }
    }

    private fun mark(args: Array<String>, status: TaskStatus) {
        expectSize(args, 2, "${args[0]} <id>")
        repository.mark(parseId(args[1]), status)
        output.println("Task marked as ${status.value}")
    }

    private fun list(args: Array<String>) {
        if (args.size > 2) throw TaskTrackerException("Usage: task-cli list [todo|in-progress|done|not-done]")
        val tasks = when (val filter = args.getOrNull(1)) {
            null -> repository.list()
            "not-done" -> repository.list(notDone = true)
            "todo", "in-progress", "done" -> repository.list(TaskStatus.fromValue(filter))
            else -> throw TaskTrackerException("Unknown status: $filter")
        }

        if (tasks.isEmpty()) {
            output.println("No tasks found.")
            return
        }
        output.println("ID\tSTATUS\t\tDESCRIPTION\tUPDATED")
        tasks.forEach { task ->
            output.println("${task.id}\t${task.status.value.padEnd(12)}\t${task.description}\t${task.updatedAt}")
        }
    }

    private fun parseId(value: String): Int {
        val id = value.toIntOrNull()
        if (id == null || id <= 0) throw TaskTrackerException("Task ID must be a positive integer")
        return id
    }

    private fun expectSize(args: Array<String>, expected: Int, commandUsage: String) {
        if (args.size != expected) throw TaskTrackerException("Usage: task-cli $commandUsage")
    }

    private fun usage(): String = """
        Usage: task-cli <command> [arguments]

        Commands:
          add <description>
          update <id> <description>
          delete <id>
          mark-in-progress <id>
          mark-done <id>
          list [todo|in-progress|done|not-done]
          help
    """.trimIndent()
}
