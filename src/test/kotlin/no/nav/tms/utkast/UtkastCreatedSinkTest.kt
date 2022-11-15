package no.nav.tms.utkast

import alleUtkast
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotliquery.sessionOf
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.utkast.database.UtkastRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import testUtkast

internal class UtkastCreatedSinkTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val ds = database.dataSource
    private val testRapid = TestRapid()
    private val testFnr = "12345678910"

    @Test
    fun `plukker opp created events`() {
        UtkastCreatedSink(
            rapidsConnection = testRapid,
            utkastRepository = UtkastRepository(ds),
            rapidMetricsProbe = mockk(relaxed = true)
        )

        testRapid.sendTestMessage(testUtkast(eventId = "qqeedd1", fnr = testFnr))
        testRapid.sendTestMessage(testUtkast(eventId = "qqeedd2", fnr = testFnr))
        testRapid.sendTestMessage(testUtkast(eventId = "qqeedd3", fnr = testFnr))
        testRapid.sendTestMessage(testUtkast(eventId = "qqeedd3", fnr = testFnr))
        testRapid.sendTestMessage(testUtkast(eventId = "qqeedd3", fnr = testFnr))
        testRapid.sendTestMessage(testUtkast(eventId = "qqeedd3", fnr = testFnr, eventName = "deleted"))

        sessionOf(ds).alleUtkast().size shouldBe 5

    }
}

