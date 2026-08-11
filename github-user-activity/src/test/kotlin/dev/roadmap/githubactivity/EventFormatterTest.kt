package dev.roadmap.githubactivity

import kotlin.test.Test
import kotlin.test.assertEquals

class EventFormatterTest {
    @Test
    fun `formats common GitHub event types`() {
        assertEquals(
            "Pushed 2 commits to octocat/hello-world",
            EventFormatter.format(
                event(
                    "PushEvent",
                    mapOf("commits" to JsonValue.Array(listOf(JsonValue.Null, JsonValue.Null))),
                ),
            ),
        )
        assertEquals(
            "Opened an issue in octocat/hello-world",
            EventFormatter.format(event("IssuesEvent", mapOf("action" to JsonValue.StringValue("opened")))),
        )
        assertEquals("Starred octocat/hello-world", EventFormatter.format(event("WatchEvent")))
        assertEquals(
            "Created branch main in octocat/hello-world",
            EventFormatter.format(
                event(
                    "CreateEvent",
                    mapOf(
                        "ref_type" to JsonValue.StringValue("branch"),
                        "ref" to JsonValue.StringValue("main"),
                    ),
                ),
            ),
        )
    }

    private fun event(type: String, payload: Map<String, JsonValue> = emptyMap()) =
        ActivityEvent(type, "octocat/hello-world", payload)
}
