# GitHub User Activity CLI

A dependency-free Kotlin command-line application that fetches a GitHub user's recent public events and displays a
short, readable activity summary in the terminal.

Project page: [roadmap.sh — GitHub User Activity](https://roadmap.sh/projects/github-user-activity)

## Requirements

- JDK 25
- Internet access to `api.github.com`

The repository includes the Gradle Wrapper, so a separate Gradle installation is not required. The application uses
the JDK HTTP client and has no external runtime dependencies.

## Run

From the repository root, pass a GitHub username as the only argument:

```shell
./gradlew :github-user-activity:run --args='octocat'
```

Example output:

```text
- Pushed 3 commits to octocat/hello-world
- Opened an issue in octocat/hello-world
- Starred octocat/hello-world
```

To create a reusable local installation:

```shell
./gradlew :github-user-activity:installDist
./github-user-activity/build/install/github-activity/bin/github-activity octocat
```

Display command help:

```shell
./gradlew :github-user-activity:run --args='--help'
```

GitHub permits unauthenticated API requests but applies a relatively low rate limit. To authenticate and receive a
higher limit, provide a token through the environment; the token is never printed:

```shell
GITHUB_TOKEN=your_token ./gradlew :github-user-activity:run --args='octocat'
```

## Errors

The CLI prints concise messages and exits with status `1` for invalid arguments, missing users, connection failures,
unexpected API responses, and API rate-limit errors. A successful request exits with status `0`.

Only public activity returned by GitHub's `/users/{username}/events` endpoint is displayed.

## Test

Run this module's tests:

```shell
./gradlew :github-user-activity:test
```

Run all repository checks:

```shell
./gradlew check
```
