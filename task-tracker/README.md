# Task Tracker CLI

A dependency-free Kotlin command-line application for tracking tasks, based on the
[roadmap.sh Task Tracker project](https://roadmap.sh/projects/task-tracker).

Tasks are stored in a `tasks.json` file in the directory from which the application is run. The file is created
automatically when the first command is executed.

## Requirements

- JDK 25
- No external runtime dependencies

The repository includes the Gradle Wrapper, so a separate Gradle installation is not required.

## Running the CLI

Run commands from the repository root:

```shell
./gradlew :task-tracker:run --args='add "Buy groceries"'
```

To create a reusable local installation:

```shell
./gradlew :task-tracker:installDist
```

Then run the generated executable:

```shell
./task-tracker/build/install/task-tracker/bin/task-tracker list
```

## Commands

Add a task:

```shell
./gradlew :task-tracker:run --args='add "Buy groceries"'
```

Update a task:

```shell
./gradlew :task-tracker:run --args='update 1 "Buy groceries and cook dinner"'
```

Delete a task:

```shell
./gradlew :task-tracker:run --args='delete 1'
```

Change a task's status:

```shell
./gradlew :task-tracker:run --args='mark-in-progress 1'
./gradlew :task-tracker:run --args='mark-done 1'
```

List all tasks:

```shell
./gradlew :task-tracker:run --args='list'
```

Filter tasks by status:

```shell
./gradlew :task-tracker:run --args='list todo'
./gradlew :task-tracker:run --args='list in-progress'
./gradlew :task-tracker:run --args='list done'
./gradlew :task-tracker:run --args='list not-done'
```

Display command help:

```shell
./gradlew :task-tracker:run --args='help'
```

Descriptions containing spaces must be quoted so that they are passed to the CLI as one positional argument.

## Task Data

Each stored task contains:

```json
{
  "id": 1,
  "description": "Buy groceries",
  "status": "todo",
  "createdAt": "2026-07-27T12:00:00Z",
  "updatedAt": "2026-07-27T12:00:00Z"
}
```

Supported statuses are `todo`, `in-progress`, and `done`. IDs are positive integers generated from the largest
existing ID.

Writes use a temporary file followed by an atomic replacement when supported by the filesystem, reducing the chance
of leaving a partially written task file.

## Project Structure

```text
task-tracker/
├── build.gradle.kts
└── src/
    ├── main/kotlin/dev/roadmap/tasktracker/
    │   ├── Main.kt
    │   ├── Task.kt
    │   ├── TaskCli.kt
    │   ├── TaskJson.kt
    │   └── TaskRepository.kt
    └── test/kotlin/dev/roadmap/tasktracker/
        ├── TaskCliTest.kt
        └── TaskRepositoryTest.kt
```

## Testing

Run the module tests:

```shell
./gradlew :task-tracker:test
```

Run all checks across the repository:

```shell
./gradlew check
```
