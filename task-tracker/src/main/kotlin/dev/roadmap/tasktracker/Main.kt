package dev.roadmap.tasktracker

import java.nio.file.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val repository = TaskRepository(Path.of("tasks.json"))
    val exitCode = TaskCli(repository).run(args)
    if (exitCode != 0) exitProcess(exitCode)
}
