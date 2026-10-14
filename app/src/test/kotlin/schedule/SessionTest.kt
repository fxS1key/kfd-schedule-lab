package schedule

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import java.time.LocalDate

class SessionTest {

    @Test
    fun rejectBlankTopic() {
        assertFailsWith<IllegalArgumentException> {
            Session.Lecture(
                date = LocalDate.of(2026, 10, 15),
                topic = " ",
                location = "Room 101",
                rawTeachers = listOf("Ivan")
            )
        }
    }

    @Test
    fun rejectInvalidYear() {
        for (year in listOf(0, 10000)) {
            assertFailsWith<IllegalArgumentException> {
                Session.Lecture(
                    date = LocalDate.of(year, 1, 1),
                    topic = "Math",
                    location = "Room 101",
                    rawTeachers = listOf("Ivan")
                )
            }
        }
    }

    @Test
    fun rejectWithoutTeachers() {
        assertFailsWith<IllegalArgumentException> {
            Session.Lecture(
                date = LocalDate.of(2026, 10, 15),
                topic = "Math",
                location = "Room 101",
                rawTeachers = emptyList()
            )
        }
    }

    @Test
    fun rejectPassLessonWithOneTeacher() {
        assertFailsWith<IllegalArgumentException> {
            Session.PassLesson(
                date = LocalDate.of(2026, 10, 15),
                topic = "Math",
                location = "Room 101",
                rawTeachers = listOf("Ivan")
            )
        }
    }

    @Test
    fun rejectLessonWithDuplicateTeachers() {
        assertFailsWith<IllegalArgumentException> {
            Session.PassLesson(
                date = LocalDate.of(2026, 10, 15),
                topic = "Math",
                location = "Room 101",
                rawTeachers = listOf("Ivan", "Ivan")
            )
        }
    }

    @Test
    fun protectTeachersFromChanges() {
        val originalTeachers = mutableListOf("Ivan", "Maria")

        val session = Session.PassLesson(
            date = LocalDate.of(2026, 10, 15),
            topic = "Math",
            location = "Room 101",
            rawTeachers = originalTeachers
        )

        originalTeachers.clear()

        assertEquals(listOf("Ivan", "Maria"), session.teachers)
    }

    @Test
    fun rejectModificationsTeachers() {
        val session = Session.Lecture(
            date = LocalDate.of(2026, 10, 15),
            topic = "Math",
            location = "Room 101",
            rawTeachers = listOf("Ivan")
        )

        assertFailsWith<UnsupportedOperationException> {
            (session.teachers as MutableList<String>).add("Maria")
        }

        assertEquals(listOf("Ivan"), session.teachers)
    }

    @Test
    fun rejectTrickyShitWithCopy() {
        val session = Session.Lecture(
            date = LocalDate.of(2026, 10, 15),
            topic = "Math",
            location = "Room 101",
            rawTeachers = listOf("Ivan")
        )

        assertFailsWith<IllegalArgumentException> {
            session.copy(topic = " ")
        }

        assertFailsWith<IllegalArgumentException> {
            session.copy(rawTeachers = emptyList())
        }
    }
}