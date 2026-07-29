package dev.roadmap.tasktracker

import java.time.Instant

internal object TaskJson {
    fun encode(tasks: List<Task>): String = buildString {
        append("[\n")
        tasks.forEachIndexed { index, task ->
            append("  {\n")
            append("    \"id\": ${task.id},\n")
            append("    \"description\": \"${escape(task.description)}\",\n")
            append("    \"status\": \"${task.status.value}\",\n")
            append("    \"createdAt\": \"${task.createdAt}\",\n")
            append("    \"updatedAt\": \"${task.updatedAt}\"\n")
            append("  }")
            if (index != tasks.lastIndex) append(',')
            append('\n')
        }
        append("]\n")
    }

    fun decode(json: String): List<Task> {
        val value = JsonParser(json).parse()
        val items = value as? JsonValue.Array
            ?: throw TaskTrackerException("The task file must contain a JSON array")

        return items.values.mapIndexed { index, item ->
            val fields = (item as? JsonValue.Object)?.values
                ?: throw TaskTrackerException("Task ${index + 1} must be a JSON object")
            val id = fields.number("id").toIntExact("id")
            val description = fields.string("description")
            val status = TaskStatus.fromValue(fields.string("status"))
            val createdAt = fields.instant("createdAt")
            val updatedAt = fields.instant("updatedAt")
            Task(id, description, status, createdAt, updatedAt)
        }.also { tasks ->
            if (tasks.any { it.id <= 0 }) throw TaskTrackerException("Task IDs must be positive")
            if (tasks.map { it.id }.distinct().size != tasks.size) {
                throw TaskTrackerException("The task file contains duplicate IDs")
            }
        }
    }

    private fun Map<String, JsonValue>.string(name: String): String =
        (this[name] as? JsonValue.StringValue)?.value
            ?: throw TaskTrackerException("Task property \"$name\" must be a string")

    private fun Map<String, JsonValue>.number(name: String): Long =
        (this[name] as? JsonValue.NumberValue)?.value
            ?: throw TaskTrackerException("Task property \"$name\" must be an integer")

    private fun Map<String, JsonValue>.instant(name: String): Instant =
        try {
            Instant.parse(string(name))
        } catch (exception: Exception) {
            throw TaskTrackerException("Task property \"$name\" must be an ISO-8601 timestamp", exception)
        }

    private fun Long.toIntExact(name: String): Int =
        if (this in Int.MIN_VALUE..Int.MAX_VALUE) toInt()
        else throw TaskTrackerException("Task property \"$name\" is outside the supported range")

    private fun escape(value: String): String = buildString {
        value.forEach { character ->
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20) {
                    append("\\u%04x".format(character.code))
                } else {
                    append(character)
                }
            }
        }
    }
}

private sealed interface JsonValue {
    data class Array(val values: List<JsonValue>) : JsonValue
    data class Object(val values: Map<String, JsonValue>) : JsonValue
    data class StringValue(val value: String) : JsonValue
    data class NumberValue(val value: Long) : JsonValue
    data object Null : JsonValue
    data class BooleanValue(val value: Boolean) : JsonValue
}

private class JsonParser(private val input: String) {
    private var position = 0

    fun parse(): JsonValue {
        skipWhitespace()
        val value = parseValue()
        skipWhitespace()
        if (position != input.length) fail("Unexpected trailing content")
        return value
    }

    private fun parseValue(): JsonValue {
        skipWhitespace()
        if (position >= input.length) fail("Expected a JSON value")
        return when (input[position]) {
            '[' -> parseArray()
            '{' -> parseObject()
            '"' -> JsonValue.StringValue(parseString())
            '-', in '0'..'9' -> JsonValue.NumberValue(parseInteger())
            't' -> parseLiteral("true", JsonValue.BooleanValue(true))
            'f' -> parseLiteral("false", JsonValue.BooleanValue(false))
            'n' -> parseLiteral("null", JsonValue.Null)
            else -> fail("Unexpected character '${input[position]}'")
        }
    }

    private fun parseArray(): JsonValue.Array {
        position++
        skipWhitespace()
        val values = mutableListOf<JsonValue>()
        if (consume(']')) return JsonValue.Array(values)
        while (true) {
            values += parseValue()
            skipWhitespace()
            if (consume(']')) return JsonValue.Array(values)
            expect(',')
        }
    }

    private fun parseObject(): JsonValue.Object {
        position++
        skipWhitespace()
        val values = linkedMapOf<String, JsonValue>()
        if (consume('}')) return JsonValue.Object(values)
        while (true) {
            skipWhitespace()
            if (position >= input.length || input[position] != '"') fail("Expected an object property")
            val name = parseString()
            skipWhitespace()
            expect(':')
            if (values.put(name, parseValue()) != null) fail("Duplicate property \"$name\"")
            skipWhitespace()
            if (consume('}')) return JsonValue.Object(values)
            expect(',')
        }
    }

    private fun parseString(): String {
        expect('"')
        return buildString {
            while (position < input.length) {
                when (val character = input[position++]) {
                    '"' -> return@buildString
                    '\\' -> append(parseEscape())
                    else -> {
                        if (character.code < 0x20) fail("Unescaped control character in string")
                        append(character)
                    }
                }
            }
            fail("Unterminated string")
        }
    }

    private fun parseEscape(): Char {
        if (position >= input.length) fail("Unterminated escape sequence")
        return when (val escaped = input[position++]) {
            '"' -> '"'
            '\\' -> '\\'
            '/' -> '/'
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> {
                if (position + 4 > input.length) fail("Incomplete unicode escape")
                val digits = input.substring(position, position + 4)
                position += 4
                digits.toIntOrNull(16)?.toChar() ?: fail("Invalid unicode escape")
            }
            else -> fail("Invalid escape sequence \\$escaped")
        }
    }

    private fun parseInteger(): Long {
        val start = position
        if (input[position] == '-') position++
        if (position >= input.length) fail("Incomplete number")
        if (input[position] == '0') {
            position++
        } else {
            if (input[position] !in '1'..'9') fail("Invalid number")
            while (position < input.length && input[position].isDigit()) position++
        }
        if (position < input.length && input[position] in ".eE") {
            fail("Only integer numbers are supported")
        }
        return input.substring(start, position).toLongOrNull() ?: fail("Invalid integer")
    }

    private fun <T : JsonValue> parseLiteral(text: String, value: T): T {
        if (!input.startsWith(text, position)) fail("Invalid JSON literal")
        position += text.length
        return value
    }

    private fun skipWhitespace() {
        while (position < input.length && input[position].isWhitespace()) position++
    }

    private fun consume(character: Char): Boolean {
        if (position < input.length && input[position] == character) {
            position++
            return true
        }
        return false
    }

    private fun expect(character: Char) {
        if (!consume(character)) fail("Expected '$character'")
    }

    private fun fail(message: String): Nothing =
        throw TaskTrackerException("Invalid JSON at character ${position + 1}: $message")
}
