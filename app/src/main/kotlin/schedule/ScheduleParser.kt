package schedule

import java.time.LocalDate
import java.time.format.DateTimeParseException

val REGEXXX = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")


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

    if (!REGEXXX.matches(fields[0])) {  // 4 Неверный формат или значение даты.
        return ParserResult.Failure(
            numberLine = numberLine,
            error = ParseError.InvalidDateFormat(fields[0])
        )
    }

    val date = try {                           // 4 Неверный формат или значение даты.
        LocalDate.parse(fields[0])
    } catch (e: DateTimeParseException) {
        return ParserResult.Failure(
            numberLine = numberLine,
            error = ParseError.InvalidTimeFormat(fields[0])
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
    } catch (e: IllegalArgumentException) {
        return ParserResult.Failure(
            numberLine = numberLine,
            error = ParseError.SessionTypeError(fields[1])
        )
    }

    val topic = fields[2]
    val location = fields[3]
    val teacherRawText = fields[4]

    if (sessionType != SessionType.CANCELLED) {         // 7-5 Недопустимые элементы списка преподавателей.
        val teacherCount = teacherRawText               // 7-6 Повторные имена преподавателей.
            .split(',')
            .count { it.isNotBlank() && it != "-"}

        if (sessionType == SessionType.EXAM) {
            if (teacherCount < 2) {
                return ParserResult.Failure(
                    numberLine = numberLine,
                    error = ParseError.NotEnoughTeachers(teacherRawText, sessionType)
                )
            }
        } else if (teacherCount < 1) {
            return ParserResult.Failure(
                numberLine = numberLine,
                error = ParseError.NotEnoughTeachers(teacherRawText, sessionType)
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
        session = session,
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