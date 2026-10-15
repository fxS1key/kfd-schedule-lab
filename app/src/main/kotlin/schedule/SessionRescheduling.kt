package schedule

import java.time.LocalDate

sealed interface ChangeTimeResult {
    data class Success(val session: Session) : ChangeTimeResult

    data object CancelledCannotBeRescheduled : ChangeTimeResult

    data class InvalidDate(val date: LocalDate) : ChangeTimeResult
}

fun Session.changeTime(newDate: LocalDate): ChangeTimeResult {
    if (this is Session.Cancelled) {
        return ChangeTimeResult.CancelledCannotBeRescheduled
    }

    if (newDate.year !in 1..9999) {
        return ChangeTimeResult.InvalidDate(newDate)
    }

    val movedSession: Session = when (this) {
        is Session.Lecture -> Session.Lecture(
            date = newDate,
            topic = topic,
            location = location,
            rawTeachers = teachers
        )

        is Session.Practice -> Session.Practice(
            date = newDate,
            topic = topic,
            location = location,
            rawTeachers = teachers
        )

        is Session.Exam -> Session.Exam(
            date = newDate,
            topic = topic,
            location = location,
            rawTeachers = teachers
        )

        is Session.Cancelled ->
            return ChangeTimeResult.CancelledCannotBeRescheduled
    }

    return ChangeTimeResult.Success(movedSession)
}