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

        is ParseError.InvalidDateFormat ->
            "invalid date \"${error.dateFormat}\""

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

        is ParseError.InvalidTeacher ->
            "invalid teacher \"${escapeValue(error.teacher)}\""

        is ParseError.DuplicateTeacher ->
            "duplicate teacher \"${escapeValue(error.teacher)}\""

        is ParseError.InvalidLocation ->
            "invalid location \"${escapeValue(error.location)}\""
    }

    return "line $numberLine: $message"
}

private fun fieldName(index: Int): String =
    listOf("date", "type", "topic", "location", "teachers")
        .get(index)

private fun escapeValue(value: String): String {
    var newString: String = ""
    for (char in value) {
        newString += when {
            char == '\\' -> "\\\\"
            char == '"' -> "\\\""
            char.code !in 0x20..0x7E ->
                "\\u%04X".format(char.code)
            else -> char
        }
    }
    return newString
}
