package dev.roadmap.githubactivity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EventJsonTest {
    @Test
    fun `decodes the fields used by activity output`() {
        val events = EventJson.decode(
            """
            [
              {
                "id": "1",
                "type": "PushEvent",
                "repo": {"id": 42, "name": "octocat/hello-world"},
                "payload": {
                  "size": 2,
                  "commits": [
                    {"sha": "abc", "message": "First"},
                    {"sha": "def", "message": "Second"}
                  ]
                },
                "public": true
              }
            ]
            """.trimIndent(),
        )

        assertEquals(1, events.size)
        assertEquals("PushEvent", events.single().type)
        assertEquals("octocat/hello-world", events.single().repository)
        assertEquals(2, (events.single().payload["commits"] as JsonValue.Array).values.size)
    }

    @Test
    fun `rejects malformed API data with a useful domain error`() {
        assertFailsWith<GitHubActivityException> {
            EventJson.decode("[{\"type\":\"WatchEvent\",\"repo\":null}]")
        }
    }
}
