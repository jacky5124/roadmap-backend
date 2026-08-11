package dev.roadmap.githubactivity

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val exitCode = ActivityCli(GitHubActivityClient()).run(args)
    if (exitCode != 0) exitProcess(exitCode)
}
