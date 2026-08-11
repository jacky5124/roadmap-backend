package dev.roadmap.githubactivity

import java.io.PrintStream

class ActivityCli(
    private val fetcher: ActivityFetcher,
    private val output: PrintStream = System.out,
    private val error: PrintStream = System.err,
) {
    fun run(args: Array<String>): Int {
        if (args.size == 1 && args[0] in setOf("help", "--help", "-h")) {
            output.println(usage())
            return 0
        }
        if (args.size != 1) {
            error.println("Error: expected one GitHub username\n\n${usage()}")
            return 1
        }

        val username = args[0]
        if (!USERNAME.matches(username)) {
            error.println("Error: invalid GitHub username")
            return 1
        }

        return try {
            val events = fetcher.fetch(username)
            if (events.isEmpty()) {
                output.println("No recent public activity found for $username.")
            } else {
                events.forEach { output.println("- ${EventFormatter.format(it)}") }
            }
            0
        } catch (exception: GitHubActivityException) {
            error.println("Error: ${exception.message}")
            1
        }
    }

    private fun usage(): String = "Usage: github-activity <username>"

    private companion object {
        val USERNAME = Regex("[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})")
    }
}
