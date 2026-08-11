package dev.roadmap.githubactivity

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

fun interface ActivityFetcher {
    fun fetch(username: String): List<ActivityEvent>
}

class GitHubActivityClient(
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build(),
    private val token: String? = System.getenv("GITHUB_TOKEN")?.takeIf { it.isNotBlank() },
) : ActivityFetcher {
    override fun fetch(username: String): List<ActivityEvent> {
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/users/$username/events"))
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "roadmap-github-activity-cli")
            .GET()
        token?.let { requestBuilder.header("Authorization", "Bearer $it") }

        val response = try {
            httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw GitHubActivityException("The request to GitHub was interrupted", exception)
        } catch (exception: IOException) {
            throw GitHubActivityException("Could not connect to GitHub: ${exception.message}", exception)
        } catch (exception: IllegalArgumentException) {
            throw GitHubActivityException("Invalid GitHub username", exception)
        }

        return when (response.statusCode()) {
            200 -> EventJson.decode(response.body())
            404 -> throw GitHubActivityException("GitHub user '$username' was not found")
            403, 429 -> throw GitHubActivityException(
                "GitHub API rate limit exceeded; wait and try again or set GITHUB_TOKEN",
            )
            else -> throw GitHubActivityException("GitHub API request failed (HTTP ${response.statusCode()})")
        }
    }
}
