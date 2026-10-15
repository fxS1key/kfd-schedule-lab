
package schedule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ParserTest {

    private fun failure(
        line: String,
        numberLine: Long = 1L,
    ): ParserResult.Failure {
        val result = parseLine(numberLine, line)
        return assertIs<ParserResult.Failure>(result)
    }

    @Test
    fun invalidSeparation() {
        val result = failure("2026-10-15")

        assertIs<ParseError.InvalidSeparation>(result.error)
    }

    @Test
    fun invalidSeparationHasPriorityOverOtherErrors() {
        val result = failure("not a valid line")

        assertIs<ParseError.InvalidSeparation>(result.error)
    }

    @Test
    fun tooFewFields() {
        val result = failure("2026-10-15|LECTURE|Math")

        val error = assertIs<ParseError.WrongFieldCount>(result.error)
        assertEquals(3, error.invalidSize)
    }

    @Test
    fun tooManyFields() {
        val result = failure(
            "2026-10-15|LECTURE|Math|Room 1|Ivanov|Extra"
        )

        val error = assertIs<ParseError.WrongFieldCount>(result.error)
        assertEquals(6, error.invalidSize)
    }

    @Test
    fun wrongFieldCountHasPriorityOverEmptyFields() {
        val result = failure("2026-10-15||Math")

        val error = assertIs<ParseError.WrongFieldCount>(result.error)
        assertEquals(3, error.invalidSize)
    }

    @Test
    fun emptyDate() {
        val result = failure("|LECTURE|Math|Room 1|Ivanov")

        val error = assertIs<ParseError.EmptyParameterInLine>(result.error)
        assertEquals(0, error.indexOfLine)
    }

    @Test
    fun emptySessionType() {
        val result = failure("2026-10-15||Math|Room 1|Ivanov")

        val error = assertIs<ParseError.EmptyParameterInLine>(result.error)
        assertEquals(1, error.indexOfLine)
    }

    @Test
    fun emptyTopic() {
        val result = failure("2026-10-15|LECTURE||Room 1|Ivanov")

        val error = assertIs<ParseError.EmptyParameterInLine>(result.error)
        assertEquals(2, error.indexOfLine)
    }

    @Test
    fun emptyLocation() {
        val result = failure("2026-10-15|LECTURE|Math||Ivanov")

        val error = assertIs<ParseError.EmptyParameterInLine>(result.error)
        assertEquals(3, error.indexOfLine)
    }

    @Test
    fun emptyTeachers() {
        val result = failure("2026-10-15|LECTURE|Math|Room 1|")

        val error = assertIs<ParseError.EmptyParameterInLine>(result.error)
        assertEquals(4, error.indexOfLine)
    }

    @Test
    fun whitespaceOnlyTopicIsEmpty() {
        val result = failure("2026-10-15|LECTURE|   |Room 1|Ivanov")

        val error = assertIs<ParseError.EmptyParameterInLine>(result.error)
        assertEquals(2, error.indexOfLine)
    }

    @Test
    fun firstEmptyFieldHasPriority() {
        val result = failure("2026-10-15||Math||Ivanov")

        val error = assertIs<ParseError.EmptyParameterInLine>(result.error)
        assertEquals(1, error.indexOfLine)
    }

    @Test
    fun malformedDateFormat() {
        val result = failure("15-10-2026|LECTURE|Math|Room 1|Ivanov")

        val error = assertIs<ParseError.InvalidDateFormat>(result.error)
        assertEquals("15-10-2026", error.dateFormat)
    }

    @Test
    fun impossibleCalendarDate() {
        val result = failure("2026-13-45|LECTURE|Math|Room 1|Ivanov")

        val error = assertIs<ParseError.InvalidTimeFormat>(result.error)
        assertEquals("2026-13-45", error.timeFormat)
    }

    @Test
    fun invalidLeapDay() {
        val result = failure("2025-02-29|LECTURE|Math|Room 1|Ivanov")

        val error = assertIs<ParseError.InvalidTimeFormat>(result.error)
        assertEquals("2025-02-29", error.timeFormat)
    }

    @Test
    fun validLeapDay() {
        val result = parseLine(
            1L,
            "2024-02-29|LECTURE|Math|Room 1|Ivanov"
        )

        assertIs<ParserResult.Success>(result)
    }

    @Test
    fun yearZeroIsRejected() {
        val result = failure("0000-01-01|LECTURE|Math|Room 1|Ivanov")

        assertIs<ParseError.InvalidDateFormat>(result.error)
    }

    @Test
    fun invalidDateHasPriorityOverUnknownType() {
        val result = failure("2026-13-45|SEMINAR|Math|Room 1|Ivanov")

        assertIs<ParseError.InvalidTimeFormat>(result.error)
    }

    @Test
    fun unknownSessionType() {
        val result = failure("2026-10-15|SEMINAR|Math|Room 1|Ivanov")

        val error = assertIs<ParseError.SessionTypeError>(result.error)
        assertEquals("SEMINAR", error.sessionType)
    }

    @Test
    fun examWithOneTeacherIsRejected() {
        val result = failure("2026-10-15|EXAM|Math|Room 1|Ivanov")

        val error = assertIs<ParseError.NotEnoughTeachers>(result.error)
        assertEquals(SessionType.EXAM, error.type)
        assertEquals(listOf("Ivanov"), error.teachers)
    }

    @Test
    fun examWithNoValidTeachersIsRejected() {
        val result = failure("2026-10-15|EXAM|Math|Room 1|, ")

        assertIs<ParseError.NotEnoughTeachers>(result.error)
    }

    @Test
    fun examWithTwoTeachersIsAccepted() {
        val result = parseLine(
            1L,
            "2026-10-15|EXAM|Math|Room 1|Ivanov,Petrov"
        )

        assertIs<ParserResult.Success>(result)
    }

    @Test
    fun lectureWithOneTeacherIsAccepted() {
        val result = parseLine(
            1L,
            "2026-10-15|LECTURE|Math|Room 1|Ivanov"
        )

        assertIs<ParserResult.Success>(result)
    }

    @Test
    fun unknownTypeHasPriorityOverTeacherCount() {
        val result = failure("2026-10-15|SEMINAR|Math|Room 1|")

        assertIs<ParseError.EmptyParameterInLine>(result.error)
    }

    @Test
    fun controlCharacterInTopic() {
        val result = failure("2026-10-15|LECTURE|Ma\nth|Room 1|Ivanov")

        val error = assertIs<ParseError.ControlChar>(result.error)
        assertEquals("Ma\nth", error.field)
    }

    @Test
    fun controlCharacterHasPriorityOverWhitespace() {
        val result = failure(
            "2026-10-15|LECTURE|Ma\nth|Room 1|Ivanov"
        )

        val error = assertIs<ParseError.ControlChar>(result.error)
        assertEquals("Ma\nth", error.field)
    }

    @Test
    fun leadingWhitespaceIsRejected() {
        val result = failure("2026-10-15|LECTURE| Math|Room 1|Ivanov")

        val error = assertIs<ParseError.BadFormatParameter>(result.error)
        assertEquals(" Math", error.line)
    }

    @Test
    fun trailingWhitespaceIsRejected() {
        val result = failure("2026-10-15|LECTURE|Math |Room 1|Ivanov")

        val error = assertIs<ParseError.BadFormatParameter>(result.error)
        assertEquals("Math ", error.line)
    }

    @Test
    fun controlCharacterInLaterFieldHasPriorityOverEarlierWhitespace() {
        val result = failure(
            "2026-10-15|LECTURE| Math|Room\n1|Ivanov"
        )

        val error = assertIs<ParseError.ControlChar>(result.error)
        assertEquals("Room\n1", error.field)
    }

    @Test
    fun cancelledSessionWithRequiredMarkersIsAccepted() {
        val result = parseLine(
            1L,
            "2026-10-15|CANCELLED|Math|-|-"
        )

        assertIs<ParserResult.Success>(result)
    }

    @Test
    fun cancelledSessionWithInvalidLocationIsRejected() {
        val result = failure(
            "2026-10-15|CANCELLED|Math|Room 1|-"
        )

        val error = assertIs<ParseError.InvalidCancelledField>(result.error)
        assertEquals("Room 1|-", error.field)
    }

    @Test
    fun cancelledSessionWithInvalidTeachersIsRejected() {
        val result = failure(
            "2026-10-15|CANCELLED|Math|-|Ivanov"
        )

        val error = assertIs<ParseError.InvalidCancelledField>(result.error)
        assertEquals("-|Ivanov", error.field)
    }

    // MARK: Line number

    @Test
    fun failurePreservesLineNumber() {
        val result = failure(
            line = "2026-10-15",
            numberLine = 42L
        )

        assertEquals(42L, result.numberLine)
        assertIs<ParseError.InvalidSeparation>(result.error)
    }
}
