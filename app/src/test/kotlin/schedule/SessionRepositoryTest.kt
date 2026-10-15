package schedule

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SessionRepositoryTest {
    @Test
    fun addPreservesOrderAndAllowsDuplicates() {
        val repository = InMemorySessionRepository()
        val first = lecture("First")
        val second = lecture("Second")

        repository.add(first)
        repository.add(second)
        repository.add(first)

        assertEquals(listOf(first, second, first), repository.all())
    }

    @Test
    fun addAllAppendsTheWholeBatchInOrder() {
        val repository = InMemorySessionRepository()
        val before = lecture("Before")
        val first = lecture("First")
        val second = lecture("Second")
        repository.add(before)
        val batch = mutableListOf(first, second)

        repository.addAll(batch)
        batch.clear()

        assertEquals(listOf(before, first, second), repository.all())
    }

    @Test
    fun allReturnsASnapshotThatDoesNotChangeAfterLaterWrites() {
        val repository = InMemorySessionRepository()
        val first = lecture("First")
        val second = lecture("Second")
        val third = lecture("Third")
        repository.addAll(listOf(first, second))

        val snapshot = repository.all()
        repository.add(third)

        assertEquals(listOf(first, second), snapshot)
        assertEquals(listOf(first, second, third), repository.all())

        // A snapshot may be read-only or mutable, but it must never expose storage.
        runCatching { (snapshot as MutableList<Session>).clear() }
        assertEquals(listOf(first, second, third), repository.all())
    }

    @Test
    fun auditRecordsOneEventPerSuccessfulAddOperation() {
        val delegate = InMemorySessionRepository()
        val repository = AuditedSessionRepository(delegate)
        val first = lecture("First")
        val second = lecture("Second")
        val third = lecture("Third")

        repository.add(first)
        repository.addAll(listOf(second, third))
        repository.addAll(emptyList())

        assertEquals(listOf(first, second, third), delegate.all())
        assertEquals(3, repository.totalAdded)
        assertEquals(
            listOf(
                AuditEvent(operation = "add", count = 1),
                AuditEvent(operation = "addAll", count = 2),
                AuditEvent(operation = "addAll", count = 0),
            ),
            repository.getJournal(),
        )
    }

    @Test
    fun readingAllDoesNotCreateAuditEvents() {
        val delegate = InMemorySessionRepository()
        val repository = AuditedSessionRepository(delegate)
        repository.add(lecture("First"))
        val journalBeforeRead = repository.getJournal()

        repository.all()
        repository.all()

        assertEquals(journalBeforeRead, repository.getJournal())
        assertEquals(1, repository.totalAdded)
    }

    @Test
    fun failedDelegateOperationsAreNotAudited() {
        val failingDelegate = object : SessionRepository {
            override fun add(session: Session) {
                throw IllegalStateException("delegate failed")
            }

            override fun addAll(sessions: List<Session>) {
                throw IllegalStateException("delegate failed")
            }

            override fun all(): List<Session> = emptyList()
        }
        val repository = AuditedSessionRepository(failingDelegate)

        assertFailsWith<IllegalStateException> { repository.add(lecture("First")) }
        assertFailsWith<IllegalStateException> {
            repository.addAll(listOf(lecture("Second")))
        }

        assertEquals(0, repository.totalAdded)
        assertEquals(emptyList(), repository.getJournal())
    }

    private fun lecture(topic: String): Session.Lecture =
        Session.Lecture(
            date = LocalDate.of(2026, 10, 15),
            topic = topic,
            location = "Room 101",
            rawTeachers = listOf("Ivanov"),
        )
}

private class InMemorySessionRepository : SessionRepository {
    private val stored = mutableListOf<Session>()

    override fun add(session: Session) {
        stored.add(session)
    }

    override fun addAll(sessions: List<Session>) {
        stored.addAll(sessions)
    }

    override fun all(): List<Session> = stored.toList()
}
