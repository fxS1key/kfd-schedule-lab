package schedule

interface SessionRepository {
    fun addSession(session: Session)
    fun addAllSessions(sessions: List<Session>)
    fun allSessions(): List<Session>
}

object SessionControl : SessionRepository {
    private val sessionList = mutableListOf<Session>()

    override fun addSession(session: Session) {
        sessionList.add(session)
    }

    override fun addAllSessions(sessions: List<Session>) {
        for (session in sessions) {
            sessionList.add(session)
        }
    }

    override fun allSessions(): List<Session> {
        return sessionList.toList()
    }
}