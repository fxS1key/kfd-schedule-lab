package schedule

data class AuditEvent(
    val operation: String,
    val count: Int
)

interface SessionRepository {
    fun add(session: Session)
    fun addAll(sessions: List<Session>)
    fun all(): List<Session>
}


object SessionControl : SessionRepository {         // хранилище как объект
    private val sessions = mutableListOf<Session>()

    override fun add(session: Session) {
        sessions.add(session)
    }

    override fun addAll(sessions: List<Session>) {
        this.sessions.addAll(sessions)
    }

    override fun all(): List<Session> {
        return sessions.toList()
    }
}


class AuditedSessionRepository(                 // обертка для записи в журнал
    private val delegate: SessionRepository
) : SessionRepository by delegate {

    private val journal = mutableListOf<AuditEvent>()

    var totalAdded: Int = 0
        private set

    override fun add(session: Session) {
        delegate.add(session)
        totalAdded++
        journal.add(AuditEvent("add", 1))
    }

    override fun addAll(sessions: List<Session>) {
        delegate.addAll(sessions)
        totalAdded += sessions.size
        journal.add(AuditEvent("addAll", sessions.size))
    }

    fun getJournal(): List<AuditEvent> = journal.toList()
}