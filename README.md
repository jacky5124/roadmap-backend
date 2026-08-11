# roadmap-backend

A multi-module Kotlin repository for backend projects from [roadmap.sh](https://roadmap.sh/projects).

## Modules

- [`task-tracker`](task-tracker/README.md) — Command-line task tracker with JSON persistence, based on the
  [roadmap.sh Task Tracker project](https://roadmap.sh/projects/task-tracker).
- [`github-user-activity`](github-user-activity/README.md) — Command-line viewer for a GitHub user's recent public
  activity, based on the [roadmap.sh GitHub User Activity project](https://roadmap.sh/projects/github-user-activity).
- `app` — Placeholder application module.
- `utils` — Shared utilities module.

## Build

Use the included Gradle Wrapper:

- Run `./gradlew run` to build and run the application.
- Run `./gradlew build` to only build the application.
- Run `./gradlew check` to run all checks, including tests.
- Run `./gradlew clean` to clean all build outputs.

See each module's README for its usage and implementation details.
