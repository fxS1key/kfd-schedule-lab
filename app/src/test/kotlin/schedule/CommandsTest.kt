package schedule

import java.time.LocalDate
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.io.TempDir

class CommandsTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun periodFiltersInclusivelyPreservesOrderAndCountsOnlyMatches(@TempDir directory: Path) {
        val file = write(
            directory,
            "schedule.txt",
            listOf(
                "2026-10-01|LECTURE|First|Room 1|Ivanov",
                "2026-10-03|PRACTICE|Middle|Room 2|Petrov",
                "2026-10-05|EXAM|Last|Room 3|Ivanov,Petrov",
                "2026-10-03|LECTURE|Invalid|Room 1|",
            ).joinToString("\n"),
        )
        val output = captureStdout {
            assertEquals(
                1,
                runPeriod(
                    Command.Period(file.toString(), LocalDate.parse("2026-10-03"), LocalDate.parse("2026-10-05")),
                ),
            )
        }

        assertEquals(
            listOf(
                "2026-10-03 | PRACTICE | Middle | Room 2",
                "2026-10-05 | EXAM | Last | Room 3",
                "Total: 2",
            ),
            output,
        )
    }

    @Test
    fun periodPrintsTotalZeroForEmptyFile(@TempDir directory: Path) {
        val file = write(directory, "empty.txt", "")
        val output = captureStdout {
            assertEquals(
                0,
                runPeriod(Command.Period(file.toString(), LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"))),
            )
        }
        assertEquals(listOf("Total: 0"), output)
    }

    @Test
    fun typeSelectsEveryTypeAndPrintsTeachers(@TempDir directory: Path) {
        val file = write(
            directory,
            "types.txt",
            listOf(
                "2026-10-01|LECTURE|Lecture|Room 1|Ivanov",
                "2026-10-02|PRACTICE|Practice|Room 2|Petrov",
                "2026-10-03|EXAM|Exam|Room 3|Ivanov,Petrov",
                "2026-10-04|CANCELLED|Cancelled|-|-",
            ).joinToString("\n"),
        )
        val expected = mapOf(
            SessionType.LECTURE to "2026-10-01 | LECTURE | Lecture | Room 1 | Ivanov",
            SessionType.PRACTICE to "2026-10-02 | PRACTICE | Practice | Room 2 | Petrov",
            SessionType.EXAM to "2026-10-03 | EXAM | Exam | Room 3 | Ivanov, Petrov",
            SessionType.CANCELLED to "2026-10-04 | CANCELLED | Cancelled | - | -",
        )

        for ((type, line) in expected) {
            val output = captureStdout {
                assertEquals(0, runType(Command.ByType(file.toString(), type)))
            }
            assertEquals(listOf(line, "Total: 1"), output)
        }
    }

    @Test
    fun errorsPrintsAllFailuresInFileOrderAndCountsEveryLine(@TempDir directory: Path) {
        val file = write(
            directory,
            "dirty.txt",
            listOf(
                "2026-10-01|LECTURE|Good|Room 1|Ivanov",
                "not a row",
                "2026-13-45|SEMINAR|Bad date|Room 2|Ivanov",
                "2026-10-04|EXAM|One teacher|Room 3|Ivanov",
            ).joinToString("\n"),
        )
        val output = captureStdout {
            assertEquals(1, runErrors(Command.Errors(file.toString())))
        }

        assertEquals(
            listOf(
                "line 2: invalid field separator",
                "line 3: invalid date \"2026-13-45\"",
                "line 4: type \"EXAM\" requires at least 2 teachers, got 1",
                "Rejected: 3 of 4",
            ),
            output,
        )
    }

    @Test
    fun errorsReturnsZeroForEmptyFile(@TempDir directory: Path) {
        val file = write(directory, "empty.txt", "")
        val output = captureStdout {
            assertEquals(0, runErrors(Command.Errors(file.toString())))
        }
        assertEquals(listOf("Rejected: 0 of 0"), output)
    }

    private fun write(directory: Path, name: String, text: String): Path {
        val path = directory.resolve(name)
        Files.writeString(path, text)
        return path
    }

    private fun captureStdout(block: () -> Unit): List<String> {
        val original = System.out
        val bytes = java.io.ByteArrayOutputStream()
        try {
            System.setOut(java.io.PrintStream(bytes, true, Charsets.UTF_8))
            block()
        } finally {
            System.setOut(original)
        }
        return bytes.toString(Charsets.UTF_8).lineSequence().filter { it.isNotEmpty() }.toList()
    }
}
