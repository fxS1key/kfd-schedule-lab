package schedule

import kotlin.test.Test
import kotlin.test.assertEquals

class ParseErrorFormatterTest {
    @Test
    fun formatsRequiredMessagesExactly() {
        assertEquals("line 3: invalid field separator", formatParseError(3, ParseError.InvalidSeparation))
        assertEquals("line 4: got 6 fields, expected 5", formatParseError(4, ParseError.WrongFieldCount(6)))
        assertEquals("line 5: empty required field \"date\"", formatParseError(5, ParseError.EmptyParameterInLine(0)))
        assertEquals("line 6: invalid date \"2026-13-45\"", formatParseError(6, ParseError.InvalidDateFormat("2026-13-45")))
        assertEquals("line 7: unknown session type \"SEMINAR\"", formatParseError(7, ParseError.SessionTypeError("SEMINAR")))
        assertEquals("line 8: type \"EXAM\" requires at least 2 teachers, got 1", formatParseError(8, ParseError.NotEnoughTeachers(listOf("Ivanov"), SessionType.EXAM)))
    }

    @Test
    fun escapesQuotesBackslashesControlsAndNonAsciiInValues() {
        val raw = "bad\"\\\nё"

        // Собираем ожидаемую строку по частям,
        // чтобы Unicode-последовательности остались текстом.
        val escaped = "bad" + "\\\"" + "\\\\" + "\\u000A" + "\\u0451"

        assertEquals(
            "line 1: unknown session type \"$escaped\"",
            formatParseError(1, ParseError.SessionTypeError(raw)),
        )
        assertEquals(
            "line 2: invalid teacher \"$escaped\"",
            formatParseError(2, ParseError.InvalidTeacher(raw)),
        )
        assertEquals(
            "line 3: duplicate teacher \"$escaped\"",
            formatParseError(3, ParseError.DuplicateTeacher(raw)),
        )
        assertEquals(
            "line 4: invalid location \"$escaped\"",
            formatParseError(4, ParseError.InvalidLocation(raw)),
        )
    }

    @Test
    fun teacherCountCountsOnlyNonblankNonDashItems() {
        val error = ParseError.NotEnoughTeachers(listOf("Ivanov", "-", "  "), SessionType.EXAM)
        assertEquals(
            "line 9: type \"EXAM\" requires at least 2 teachers, got 1",
            formatParseError(9, error),
        )
    }

    @Test
    fun everyErrorVariantHasAReadableMessage() {
        val errors = listOf(
            ParseError.InvalidSeparation,
            ParseError.WrongFieldCount(4),
            ParseError.EmptyParameterInLine(4),
            ParseError.BadFormatParameter(" Math "),
            ParseError.InvalidDateFormat("2026-1-01"),
            ParseError.InvalidDateFormat("2026-02-30"),
            ParseError.SessionTypeError("SEMINAR"),
            ParseError.NotEnoughTeachers(listOf("Ivanov"), SessionType.EXAM),
            ParseError.ControlChar("Ma\nth"),
            ParseError.InvalidCancelledField("Room 1|Ivanov"),
            ParseError.InvalidLocation("-"),
            ParseError.InvalidTeacher("-"),
            ParseError.DuplicateTeacher("Ivanov"),
        )
        for (error in errors) {
            val message = formatParseError(12, error)
            kotlin.test.assertTrue(message.startsWith("line 12: "), message)
            kotlin.test.assertTrue(message.length > "line 12: ".length, message)
        }
    }
}