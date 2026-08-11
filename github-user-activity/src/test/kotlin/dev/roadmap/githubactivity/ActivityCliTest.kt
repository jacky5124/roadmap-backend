package dev.roadmap.githubactivity

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActivityCliTest {
    @Test
    fun `fetches and prints activity for the supplied username`() {
        val output = ByteArrayOutputStream()
        val error = ByteArrayOutputStream()
        val fetcher = ActivityFetcher { username ->
            assertEquals("octocat", username)
            listOf(ActivityEvent("WatchEvent", "octocat/hello-world", emptyMap()))
        }

        val exitCode = ActivityCli(fetcher, PrintStream(output), PrintStream(error)).run(arrayOf("octocat"))

        assertEquals(0, exitCode)
        assertEquals("- Starred octocat/hello-world\n", output.toString())
        assertEquals("", error.toString())
    }

    @Test
    fun `reports usage and API failures without a stack trace`() {
        val output = ByteArrayOutputStream()
        val error = ByteArrayOutputStream()
        val fetcher = ActivityFetcher { throw GitHubActivityException("GitHub user 'missing' was not found") }
        val cli = ActivityCli(fetcher, PrintStream(output), PrintStream(error))

        assertEquals(1, cli.run(emptyArray()))
        assertTrue(error.toString().contains("Usage: github-activity <username>"))

        error.reset()
        assertEquals(1, cli.run(arrayOf("missing")))
        assertEquals("Error: GitHub user 'missing' was not found\n", error.toString())
    }

    @Test
    fun `prints a message when the user has no public events`() {
        val output = ByteArrayOutputStream()
        val cli = ActivityCli(ActivityFetcher { emptyList() }, PrintStream(output), PrintStream(ByteArrayOutputStream()))

        assertEquals(0, cli.run(arrayOf("octocat")))
        assertEquals("No recent public activity found for octocat.\n", output.toString())
    }
}
