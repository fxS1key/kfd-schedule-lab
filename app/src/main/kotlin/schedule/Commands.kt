package schedule

interface CommandHandlers {
    fun period(command: Command.Period): Int
    fun byType(command: Command.ByType): Int
    fun errors(command: Command.Errors): Int
}

object StudentCommandHandlers : CommandHandlers {
    override fun period(command: Command.Period): Int = runPeriod(command)
    override fun byType(command: Command.ByType): Int = runType(command)
    override fun errors(command: Command.Errors): Int = runErrors(command)
}

fun runPeriod(command: Command.Period): Int {
    val sessions = mutableListOf<Session>()
    var hasErrors = false

    forEachScheduleLine(command.file) {number, line ->
        when (val result = parseLine(number, line)) {
            is ParserResult.Success -> {
                if (result.session.date in command.from..command.to) {
                    sessions.add(result.session)
                }
            }
            is ParserResult.Failure -> hasErrors = true
        }
    }

    sessions.forEach {
        println(formatSession(it, includeTeachers = false))
    }
    println("Total: ${sessions.size}")
    return if (hasErrors) 1 else 0
}

fun runType(command: Command.ByType): Int {
    val sessions = mutableListOf<Session>()
    var hasErrors = false

    forEachScheduleLine(command.file) {number, line ->
        when (val result = parseLine(number, line)) {
            is ParserResult.Success -> {
                val isCorrectType = when (command.type) {
                    SessionType.PRACTICE -> result.session is Session.Practice
                    SessionType.LECTURE -> result.session is Session.Lecture
                    SessionType.EXAM -> result.session is Session.Exam
                    SessionType.CANCELLED -> result.session is Session.Cancelled
                }

                if (isCorrectType) {
                    sessions.add(result.session)
                }
            }
            is ParserResult.Failure -> hasErrors = true
        }
    }

    sessions.forEach {
        println(formatSession(it, includeTeachers = true))
    }
    println("Total: ${sessions.size}")
    return if (hasErrors) 1 else 0
}

fun runErrors(command: Command.Errors): Int {
    var total = 0L
    var rejected = 0L

    forEachScheduleLine(command.file) {number, line ->
        total++

        when (parseLine(number, line)) {
            is ParserResult.Success -> {}
            is ParserResult.Failure -> {
                rejected++
            }
        }
    }

    println("Rejected: $rejected of $total")

    return if (rejected > 0) 1 else 0
}


fun formatSession(session: Session, includeTeachers: Boolean): String {
    return when (session) {
        is Session.Lecture ->
            if (includeTeachers) {
                "${session.date} | LECTURE | ${session.topic} | ${session.location} | ${session.teachers.joinToString(", ")}"
            } else {
                "${session.date} | LECTURE | ${session.topic} | ${session.location}"
            }

        is Session.Practice ->
            if (includeTeachers) {
                "${session.date} | PRACTICE | ${session.topic} | ${session.location} | ${session.teachers.joinToString(", ")}"
            } else {
                "${session.date} | PRACTICE | ${session.topic} | ${session.location}"
            }

        is Session.Exam ->
            if (includeTeachers) {
                "${session.date} | EXAM | ${session.topic} | ${session.location} | ${session.teachers.joinToString(", ")}"
            } else {
                "${session.date} | EXAM | ${session.topic} | ${session.location}"
            }

        is Session.Cancelled ->
            if (includeTeachers) {
                "${session.date} | CANCELLED | ${session.topic} | - | -"
            } else {
                "${session.date} | CANCELLED | ${session.topic} | -"
            }
    }
}