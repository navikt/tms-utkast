package no.nav.tms.utkast

import alleUtkast
import io.mockk.mockk
import kotliquery.queryOf
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.utkast.database.UtkastRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import testUtkast

internal class UtkastCreatedSinkTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val testRapid = TestRapid()
    private val testFnr = "12345678910"

    @AfterAll
    fun cleanup() {
        database.update {
            queryOf("delete from utkast")
        }
    }

    @Test
    fun `plukker opp created events`() {
        UtkastCreatedSink(
            rapidsConnection = testRapid,
            utkastRepository = UtkastRepository(database),
            rapidMetricsProbe = mockk(relaxed = true)
        )

        testRapid.sendTestMessage(testUtkast(eventId = "qqeedd1", fnr = testFnr))
        testRapid.sendTestMessage(testUtkast(eventId = "qqeedd2", fnr = testFnr))
        testRapid.sendTestMessage(testUtkast(eventId = "qqeedd3", fnr = testFnr))
        testRapid.sendTestMessage(testUtkast(eventId = "qqeedd3", fnr = testFnr))
        testRapid.sendTestMessage(testUtkast(eventId = "qqeedd3", fnr = testFnr))
        testRapid.sendTestMessage(testUtkast(eventId = "qqeedd3", fnr = testFnr, eventName = "deleted"))
        database.list { alleUtkast }
    }
}

