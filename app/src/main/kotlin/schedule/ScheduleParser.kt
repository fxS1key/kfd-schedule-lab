package schedule

import java.time.LocalDate
import java.time.format.DateTimeParseException

val CODE_REGEX = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")


/** Priority
 * 1 Нет | — неверный разделитель.
 * 2 Полей не пять — неверное количество полей.
 * 3 Есть пустое или полностью пробельное поле — первое такое поле в порядке следования.
 * 4 Неверный формат или значение даты.
 * 5 Неизвестный тип занятия.
 * 6 У сдачи менее двух непустых и непробельных элементов списка, отличных от -.
 * 7 Остальные нарушения домена.
 * Внутри пункта 7 проверяйте последовательно:
 *
 * 7-1 Управляющие символы.
 * 7-2 Внешние пробельные символы.
 * 7-3 Неверные поля отмены.
 * 7-4 Недопустимое место действующего занятия.
 * 7-5 Недопустимые элементы списка преподавателей.
 * 7-6 Повторные имена преподавателей.
 *
 */
fun parseLine(
    numberLine: Long,
    line: String,
) : ParserResult {      // все проверки
    if ('|' !in line) {         // 1 Нет | — неверный разделитель.
        return ParserResult.Failure(
            numberLine = numberLine,
            error = ParseError.InvalidSeparation
        )
    }

    val fields = line.split('|', limit = Int.MAX_VALUE) // limit for "a|b|c|d|"
    if (fields.size != 5) {         // 2 Полей не пять — неверное количество полей.
        return ParserResult.Failure(
            numberLine = numberLine,
            error = ParseError.WrongFieldCount(fields.size)
        )
    }


    fields.forEachIndexed { index, field ->     // 3 Есть пустое или полностью пробельное поле
        if (field.isBlank()) {                  // — первое такое поле в порядке следования.
            return ParserResult.Failure(
                numberLine = numberLine,
                error = ParseError.EmptyParameterInLine(index)
            )
        }
    }

    if (!CODE_REGEX.matches(fields[0])) {  // 4 Неверный формат или значение даты.
        return ParserResult.Failure(
            numberLine = numberLine,
            error = ParseError.InvalidDateFormat(fields[0])
        )
    }

    val date = try {                           // 4 Неверный формат или значение даты.
        LocalDate.parse(fields[0])
    } catch (_: DateTimeParseException) {
        return ParserResult.Failure(
            numberLine = numberLine,
            error = ParseError.InvalidDateFormat(fields[0])
        )
    }

    if (date.year !in 1..9999) {        // 4 Неверный формат или значение даты.
        return ParserResult.Failure(
            numberLine = numberLine,
            error = ParseError.InvalidDateFormat(fields[0])
        )
    }

    val sessionType = try {                 // 5 Неизвестный тип занятия.
        SessionType.valueOf(fields[1])
    } catch (_: IllegalArgumentException) {
        return ParserResult.Failure(
            numberLine = numberLine,
            error = ParseError.SessionTypeError(fields[1])
        )
    }

    val topic = fields[2]
    val location = fields[3]
    val teacherRawText = fields[4]

    if (sessionType == SessionType.EXAM) {
        val teachers = teacherRawText.split(',')
        val validCount = teachers.count {
            it.isNotBlank() && it != "-"
        }

        if (validCount < 2) {
            return ParserResult.Failure(
                numberLine = numberLine,
                error = ParseError.NotEnoughTeachers(
                    teachers = teachers,
                    type = sessionType
                )
            )
        }
    }


    fields.forEach { field ->               // 7-1 Управляющие символы.
        if (field.any { it.isISOControl() }) {
            return ParserResult.Failure(
                numberLine = numberLine,
                error = ParseError.ControlChar(field)
            )
        }
    }

    fields.forEach { field ->
        if (field.trim() != field) {
            return ParserResult.Failure(
                numberLine = numberLine,
                error = ParseError.BadFormatParameter(field)
            )
        }
    }

    if (sessionType == SessionType.CANCELLED) {
        if (location != "-" || teacherRawText != "-") {
            return ParserResult.Failure(
                numberLine = numberLine,
                error = ParseError.InvalidCancelledField("$location|$teacherRawText")
            )
        }
    }


    if (sessionType != SessionType.CANCELLED) {
        // 7-4: место занятия
        if (location == "-") {
            return ParserResult.Failure(
                numberLine = numberLine,
                error = ParseError.InvalidLocation(location)
            )
        }

        val teachers = teacherRawText.split(',')

        // 7-5: отдельные элементы списка преподавателей
        for (teacher in teachers) {
            if (teacher.isBlank() || teacher == "-" || teacher.trim() != teacher) {
                return ParserResult.Failure(
                    numberLine = numberLine,
                    error = ParseError.InvalidTeacher(teacher)
                )
            }
        }

        // 7-6: повторные имена
        val seen = mutableSetOf<String>()

        for (teacher in teachers) {
            if (!seen.add(teacher)) {
                return ParserResult.Failure(
                    numberLine = numberLine,
                    error = ParseError.DuplicateTeacher(teacher)
                )
            }
        }
    }


    val session: Session = when (sessionType) {
        SessionType.LECTURE -> Session.Lecture(
            date = date,
            topic = topic,
            location = location,
            rawTeachers = teacherRawText.split(',')
        )
        SessionType.PRACTICE -> Session.Practice(
            date = date,
            topic = topic,
            location = location,
            rawTeachers = teacherRawText.split(',')
        )
        SessionType.EXAM -> Session.Exam(
            date = date,
            topic = topic,
            location = location,
            rawTeachers = teacherRawText.split(',')
        )
        SessionType.CANCELLED -> Session.Cancelled(
            date = date,
            topic = topic,
        )
    }

    return ParserResult.Success(
        session = session
    )
}


fun parseScheduleFile(
    file: String,
) : List<ParserResult> {
    val results = mutableListOf<ParserResult>()

    forEachScheduleLine(file) {number, line ->
        results.add(parseLine(number, line))
    }

    return results
}