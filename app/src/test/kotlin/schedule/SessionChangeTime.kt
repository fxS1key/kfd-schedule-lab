package schedule

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SessionChangeTime {

    private val oldDate = LocalDate.of(2026, 10, 15)
    private val newDate = LocalDate.of(2026, 10, 20)

    @Test
    fun reschedulesLectureAndPreservesOriginal() {
        val original = Session.Lecture(
            date = oldDate,
            topic = "Kotlin",
            location = "Room 101",
            rawTeachers = listOf("Ivanov")
        )

        val result = original.changeTime(newDate)

        val newSession = assertIs<Session.Lecture>(
            assertIs<ChangeTimeResult.Success>(result).session
        )

        assertEquals(newDate, newSession.date)
        assertEquals(original.topic, newSession.topic)
        assertEquals(original.location, newSession.location)
        assertEquals(original.teachers, newSession.teachers)
        assertEquals(oldDate, original.date)
    }

    @Test
    fun reschedulesPractice() {
        val original = Session.Practice(
            date = oldDate,
            topic = "Algorithms",
            location = "Room 202",
            rawTeachers = listOf("Ivanov", "Petrov")
        )

        val result = original.changeTime(newDate)

        val newSession = assertIs<Session.Practice>(
            assertIs<ChangeTimeResult.Success>(result).session
        )

        assertEquals(newDate, newSession.date)
        assertEquals(original.topic, newSession.topic)
        assertEquals(original.location, newSession.location)
        assertEquals(original.teachers, newSession.teachers)
        assertEquals(oldDate, original.date)
    }

    @Test
    fun reschedulesExam() {
        val original = Session.Exam(
            date = oldDate,
            topic = "Probability",
            location = "Room 303",
            rawTeachers = listOf("Ivanov", "Petrov")
        )

        val result = original.changeTime(newDate)

        val moved = assertIs<Session.Exam>(
            assertIs<ChangeTimeResult.Success>(result).session
        )

        assertEquals(newDate, moved.date)
        assertEquals(original.topic, moved.topic)
        assertEquals(original.location, moved.location)
        assertEquals(original.teachers, moved.teachers)
        assertEquals(oldDate, original.date)
    }

    @Test
    fun cancelledSessionCannotBeRescheduled() {
        val original = Session.Cancelled(
            date = oldDate,
            topic = "Kotlin"
        )

        val result = original.changeTime(newDate)

        assertEquals(
            ChangeTimeResult.CancelledCannotBeRescheduled,
            result
        )
        assertEquals(oldDate, original.date)
    }

    @Test
    fun invalidYearIsRejected() {
        val original = Session.Lecture(
            date = oldDate,
            topic = "Kotlin",
            location = "Room 101",
            rawTeachers = listOf("Ivanov")
        )
        val invalidDate = LocalDate.of(10000, 1, 1)

        val result = original.changeTime(invalidDate)

        assertEquals(
            ChangeTimeResult.InvalidDate(invalidDate),
            result
        )
        assertEquals(oldDate, original.date)
    }
}
