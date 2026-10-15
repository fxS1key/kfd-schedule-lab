package schedule

fun formatParseError(
    numberLine: Long,
    error: ParseError,
): String {
    val message = when (error) {
        ParseError.InvalidSeparation ->
            "invalid field separator"

        is ParseError.WrongFieldCount ->
            "got ${error.invalidSize} fields, expected 5"

        is ParseError.EmptyParameterInLine ->
            "empty required field \"${fieldName(error.indexOfLine)}\""

        is ParseError.BadFormatParameter ->
            "field has leading or trailing whitespace: \"${escapeValue(error.line)}\""

        is ParseError.InvalidDateFormat,
        is ParseError.InvalidTimeFormat ->
            "invalid date \"${escapeValue(errorDate(error))}\""

        is ParseError.SessionTypeError ->
            "unknown session type \"${escapeValue(error.sessionType)}\""

        is ParseError.NotEnoughTeachers -> {
            val actual = error.teachers.count {
                it.isNotBlank() && it != "-"
            }
            val required = if (error.type == SessionType.EXAM) 2 else 1

            "type \"${error.type.name}\" requires at least $required " +
                    "teachers, got $actual"
        }

        is ParseError.ControlChar ->
            "field contains control characters: \"${escapeValue(error.field)}\""

        is ParseError.InvalidCancelledField ->
            "invalid fields for type \"CANCELLED\": \"${escapeValue(error.field)}\""
    }

    return "line $numberLine: $message"
}

private fun fieldName(index: Int): String =
    listOf("date", "type", "topic", "location", "teachers")
        .getOrElse(index) { "unknown" }

private fun errorDate(error: ParseError): String =
    when (error) {
        is ParseError.InvalidDateFormat -> error.dateFormat
        is ParseError.InvalidTimeFormat -> error.timeFormat
        else -> error.toString()
    }

private fun escapeValue(value: String): String = buildString {
    for (char in value) {
        when {
            char == '\\' -> append("\\\\")
            char == '"' -> append("\\\"")
            char.code < 0x20 || char.code > 0x7E ->
                append("\\u%04X".format(char.code))
            else -> append(char)
        }
    }
}
