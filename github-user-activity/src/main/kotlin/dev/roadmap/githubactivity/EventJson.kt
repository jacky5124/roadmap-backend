package dev.roadmap.githubactivity

internal object EventJson {
    fun decode(json: String): List<ActivityEvent> {
        val events = JsonParser(json).parse() as? JsonValue.Array
            ?: throw GitHubActivityException("GitHub returned an unexpected response")

        return events.values.mapIndexed { index, value ->
            val event = (value as? JsonValue.Object)?.values
                ?: throw GitHubActivityException("GitHub event ${index + 1} is not an object")
            val repository = event.objectValue("repo").string("name")
            val payload = (event["payload"] as? JsonValue.Object)?.values.orEmpty()
            ActivityEvent(event.string("type"), repository, payload)
        }
    }

    private fun Map<String, JsonValue>.string(name: String): String =
        (this[name] as? JsonValue.StringValue)?.value
            ?: throw GitHubActivityException("GitHub event property \"$name\" is missing or invalid")

    private fun Map<String, JsonValue>.objectValue(name: String): Map<String, JsonValue> =
        (this[name] as? JsonValue.Object)?.values
            ?: throw GitHubActivityException("GitHub event property \"$name\" is missing or invalid")
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
            '-', in '0'..'9' -> JsonValue.NumberValue(parseNumber())
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

    private fun parseNumber(): String {
        val start = position
        consume('-')
        if (position >= input.length) fail("Incomplete number")
        if (consume('0')) {
            // A leading zero must be the entire integer part.
        } else {
            if (input[position] !in '1'..'9') fail("Invalid number")
            while (position < input.length && input[position].isDigit()) position++
        }
        if (consume('.')) {
            if (position >= input.length || !input[position].isDigit()) fail("Invalid number")
            while (position < input.length && input[position].isDigit()) position++
        }
        if (position < input.length && input[position] in "eE") {
            position++
            if (position < input.length && input[position] in "+-") position++
            if (position >= input.length || !input[position].isDigit()) fail("Invalid number")
            while (position < input.length && input[position].isDigit()) position++
        }
        return input.substring(start, position)
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
        throw GitHubActivityException("Invalid JSON at character ${position + 1}: $message")
}
