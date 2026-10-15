package schedule

import java.time.LocalDate
import java.util.Collections

private fun validateText(text: String, name: String) {
    require(text.isNotBlank()) {
        "$name must not be blank"
    }
    require(text.trim() == text) {
        "$name must not contain spaces on sides"
    }
    require(text.none() { it.isISOControl() }) {
        "$name must not contain special characters"
    }
    require(text != "-") {
        "$name cannot be '-'"
    }
}


private fun validateSession(date: LocalDate, topic: String) {
    require(date.year in 1..9999) {
        "Invalid date"
    }
    validateText(topic, "Topic")
}


private fun validateActiveSession(
    date: LocalDate, topic: String,
    location: String, teachers: List<String>,
    minTeachers: Int
) {
    validateText(location, "Location")

    validateSession(date, topic)



    require(teachers.isNotEmpty()) {
        "Teachers must not be empty"
    }
    require(teachers.size >= minTeachers) {
        "Teachers must be at least $minTeachers teachers"
    }
    require(teachers.size == teachers.distinct().size) {
        "Teachers must not contain duplicate teachers"
    }
    for (teacher in teachers) {
        validateText(teacher, "Teacher name")
    }
}


sealed interface Session {
    val date: LocalDate
    val topic: String

    data class Lecture (
        override val date: LocalDate,
        override val topic: String,
        val location: String,
        val rawTeachers: List<String>,
    ) : Session {
        val teachers: List<String> = Collections.unmodifiableList(rawTeachers.toList())

        init {
            validateActiveSession(date, topic, location, this.teachers, 1)
        }
    }

    data class Practice (
        override val date: LocalDate,
        override val topic: String,
        val location: String,
        val rawTeachers: List<String>
    ) : Session {
        val teachers: List<String> = Collections.unmodifiableList(rawTeachers.toList())

        init {
            validateActiveSession(date, topic, location, this.teachers, 1)
        }
    }

    data class Exam (
        override val date: LocalDate,
        override val topic: String,
        val location: String,
        val rawTeachers: List<String>
    ) : Session {
        val teachers: List<String> = Collections.unmodifiableList(rawTeachers.toList())

        init {
            validateActiveSession(date, topic, location, this.teachers, 2)
        }
    }

    data class Cancelled (
        override val date: LocalDate,
        override val topic: String,
    ) : Session {
        init {
            validateSession(date, topic)
        }
    }

}