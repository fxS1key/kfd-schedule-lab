package schedule

sealed interface ParseError {
    data object InvalidSeparation : ParseError      // object cause we don't have anything to give like parameter

    data class WrongFieldCount(
        val invalidSize: Int,
    ) : ParseError

    data class EmptyParameterInLine(
        val indexOfLine: Int,
    ) : ParseError

    data class BadFormatParameter(
        val line: String,
    ) : ParseError

    data class InvalidDateFormat(
        val dateFormat: String,
    ) : ParseError

    data class InvalidTimeFormat(
        val timeFormat: String,
    ) : ParseError

    data class SessionTypeError(
        val sessionType: String,
    ) : ParseError

    data class NotEnoughTeachers(
        val teachers: List<String>,
        val type: SessionType,
    ) : ParseError {
        constructor(teacher: String, type: SessionType) : this(
            teachers = teacher.split(","),
            type = type)
    }

    data class ControlChar(
        val field: String,
    ) : ParseError

    data class InvalidCancelledField(
        val field: String,
    ) : ParseError
}