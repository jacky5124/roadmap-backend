package dev.roadmap.githubactivity

object EventFormatter {
    fun format(event: ActivityEvent): String = with(event) {
        when (type) {
            "PushEvent" -> {
                val count = payload.arraySize("commits")
                "Pushed $count ${plural(count, "commit")} to $repository"
            }
            "IssuesEvent" -> "${payload.actionPastTense()} an issue in $repository"
            "IssueCommentEvent" -> "${payload.actionPastTense()} an issue comment in $repository"
            "PullRequestEvent" -> "${payload.actionPastTense()} a pull request in $repository"
            "PullRequestReviewEvent" -> "${payload.actionPastTense()} a pull request review in $repository"
            "PullRequestReviewCommentEvent" -> "${payload.actionPastTense()} a pull request review comment in $repository"
            "WatchEvent" -> "Starred $repository"
            "ForkEvent" -> "Forked $repository"
            "CreateEvent" -> formatReference("Created", event)
            "DeleteEvent" -> formatReference("Deleted", event)
            "ReleaseEvent" -> "${payload.actionPastTense()} a release in $repository"
            "MemberEvent" -> "${payload.actionPastTense()} a repository collaborator in $repository"
            "CommitCommentEvent" -> "Created a commit comment in $repository"
            "GollumEvent" -> "Updated the wiki in $repository"
            "PublicEvent" -> "Made $repository public"
            else -> "Performed ${type.removeSuffix("Event")} activity in $repository"
        }
    }

    private fun formatReference(verb: String, event: ActivityEvent): String {
        val kind = event.payload.string("ref_type") ?: "repository"
        val reference = event.payload.string("ref")
        return if (reference == null) "$verb $kind in ${event.repository}"
        else "$verb $kind $reference in ${event.repository}"
    }

    private fun Map<String, JsonValue>.arraySize(name: String): Int =
        (this[name] as? JsonValue.Array)?.values?.size ?: 0

    private fun Map<String, JsonValue>.string(name: String): String? =
        (this[name] as? JsonValue.StringValue)?.value

    private fun Map<String, JsonValue>.actionPastTense(): String = when (val action = string("action")) {
        "open", "opened" -> "Opened"
        "close", "closed" -> "Closed"
        "reopen", "reopened" -> "Reopened"
        "create", "created" -> "Created"
        "delete", "deleted" -> "Deleted"
        "publish", "published" -> "Published"
        "add", "added" -> "Added"
        "remove", "removed" -> "Removed"
        null -> "Updated"
        else -> action.replaceFirstChar(Char::uppercaseChar)
    }

    private fun plural(count: Int, singular: String): String = if (count == 1) singular else "${singular}s"
}
