package schedule

data class AuditEvent(
    val operation: String,
    val count: Int
)

class AuditedSessionRepository (
    private val delegate: SessionRepository
): SessionRepository by delegate {

    private val journal = mutableListOf<AuditEvent>()

    var totalAdded: Int = 0
        private set

    override fun addSession(session: Session) {
        delegate.addSession(session)                // НЕ SessionControl !!!
        totalAdded++
        journal.add(AuditEvent("addSession", 1))
    }

    override fun addAllSessions(sessions: List<Session>) {
        delegate.addAllSessions(sessions)
        totalAdded += sessions.size
        journal.add(AuditEvent("addAllSessions", sessions.size))
    }

    fun getJournal(): List<AuditEvent> {
        return journal.toList()
    }
}