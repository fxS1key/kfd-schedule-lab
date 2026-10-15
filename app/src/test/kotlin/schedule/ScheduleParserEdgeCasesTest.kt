package schedule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ScheduleParserEdgeCasesTest {
    private fun failure(line: String): ParserResult.Failure =
        assertIs<ParserResult.Failure>(parseLine(17, line))

    @Test
    fun preservesTrailingEmptyFieldsWhenCountingColumns() {
        val result = failure("2026-10-15|LECTURE|Topic|Room 1|")
        val error = assertIs<ParseError.EmptyParameterInLine>(result.error)
        assertEquals(4, error.indexOfLine)
    }

    @Test
    fun unknownTypeIsCheckedBeforeExamTeacherCount() {
        val result = failure("2026-10-15|SEMINAR|Topic|Room 1|Ivanov")
        assertIs<ParseError.SessionTypeError>(result.error)
    }

    @Test
    fun examTeacherCountPrecedesDomainValidation() {
        val result = failure("2026-10-15|EXAM|Topic|Room 1|-")
        assertIs<ParseError.NotEnoughTeachers>(result.error)
    }

    @Test
    fun duplicateExamTeachersAreRejectedAfterMinimumCount() {
        val result = failure("2026-10-15|EXAM|Topic|Room 1|Ivanov,Ivanov")
        val error = assertIs<ParseError.DuplicateTeacher>(result.error)
        assertEquals("Ivanov", error.teacher)
    }

    @Test
    fun firstInvalidTeacherWins() {
        val result = failure("2026-10-15|LECTURE|Topic|Room 1|Ivanov,-,Petrov")
        val error = assertIs<ParseError.InvalidTeacher>(result.error)
        assertEquals("-", error.teacher)
    }

    @Test
    fun firstInvalidFieldWinsWithinTeacherList() {
        val result = failure("2026-10-15|LECTURE|Topic|Room 1|-,Ivanov")
        val error = assertIs<ParseError.InvalidTeacher>(result.error)
        assertEquals("-", error.teacher)
    }

    @Test
    fun caseSensitiveTeacherNamesAreDistinct() {
        val result = parseLine(1, "2026-10-15|EXAM|Topic|Room 1|Ivanov,ivanov")
        assertIs<ParserResult.Success>(result)
    }

    @Test
    fun internalSpacesAreAllowedInTopicLocationAndTeacherName() {
        val result = parseLine(1, "2026-10-15|LECTURE|Object oriented Kotlin|Room 101 A|Ivanov Petrov")
        assertIs<ParserResult.Success>(result)
    }

    @Test
    fun whitespaceBeforeDelimiterIsRejected() {
        val result = failure("2026-10-15|LECTURE|Topic |Room 1|Ivanov")
        val error = assertIs<ParseError.BadFormatParameter>(result.error)
        assertEquals("Topic ", error.line)
    }

    @Test
    fun cancellationRequiresBothMarkersExactly() {
        val result = failure("2026-10-15|CANCELLED|Topic| -|-")
        val error = assertIs<ParseError.BadFormatParameter>(result.error)
        assertEquals(" -", error.line)
    }

    @Test
    fun dateYearMustHaveFourDigits() {
        val result = failure("2026-1-01|LECTURE|Topic|Room 1|Ivanov")
        val error = assertIs<ParseError.InvalidDateFormat>(result.error)
        assertEquals("2026-1-01", error.dateFormat)
    }

    @Test
    fun lineNumberIsPreservedOnSuccessfulParseOnlyThroughFailureContract() {
        val result = failure("2026-10-15")
        assertEquals(17L, result.numberLine)
    }
}