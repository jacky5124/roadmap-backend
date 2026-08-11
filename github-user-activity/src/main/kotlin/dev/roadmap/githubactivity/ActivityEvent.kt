package dev.roadmap.githubactivity

data class ActivityEvent(
    val type: String,
    val repository: String,
    val payload: Map<String, JsonValue>,
)

sealed interface JsonValue {
    data class Array(val values: List<JsonValue>) : JsonValue
    data class Object(val values: Map<String, JsonValue>) : JsonValue
    data class StringValue(val value: String) : JsonValue
    data class NumberValue(val value: String) : JsonValue
    data class BooleanValue(val value: Boolean) : JsonValue
    data object Null : JsonValue
}

class GitHubActivityException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
