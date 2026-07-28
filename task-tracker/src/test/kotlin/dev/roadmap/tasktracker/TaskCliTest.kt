package dev.roadmap.tasktracker

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskCliTest {
    @Test
    fun `executes commands and reports user errors`() {
        val repository = TaskRepository(Files.createTempDirectory("task-cli-test").resolve("tasks.json"))
        val standardOutput = ByteArrayOutputStream()
        val standardError = ByteArrayOutputStream()
        val cli = TaskCli(repository, PrintStream(standardOutput), PrintStream(standardError))

        assertEquals(0, cli.run(arrayOf("add", "Ship it")))
        assertEquals(0, cli.run(arrayOf("mark-in-progress", "1")))
        assertEquals(0, cli.run(arrayOf("list", "in-progress")))
        assertTrue(standardOutput.toString().contains("Ship it"))

        assertEquals(1, cli.run(arrayOf("delete", "abc")))
        assertTrue(standardError.toString().contains("positive integer"))
    }
}
