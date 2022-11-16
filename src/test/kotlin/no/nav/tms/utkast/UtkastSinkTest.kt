package no.nav.tms.utkast

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kotliquery.queryOf
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.utkast.database.UtkastRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll

internal class UtkastSinkTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val testRapid = TestRapid()
    private val testFnr = "12345678910"

    @BeforeAll
    fun setup() {
        UtkastCreatedSink(
            rapidsConnection = testRapid,
            utkastRepository = UtkastRepository(database),
            rapidMetricsProbe = mockk(relaxed = true)
        )
        UtkastUpdatedSink(
            rapidsConnection = testRapid,
            utkastRepository = UtkastRepository(database),
            rapidMetricsProbe = mockk(relaxed = true),
        )
        UtkastDeletedSink(
            rapidsConnection = testRapid,
            utkastRepository = UtkastRepository(database),
            rapidMetricsProbe = mockk(relaxed = true),
            operationName = "deleted",
            operation = { eventId: String -> deleteUtkast(eventId) }
        )
    }

    @AfterEach
    fun cleanup() {
        database.update {
            queryOf("delete from utkast")
        }
    }

    @Test
    fun `plukker opp created events`() {
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = "qqeedd1", fnr = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = "qqeedd2", fnr = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = "qqeedd3", fnr = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = "qqeedd4", fnr = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = "qqeedd5", fnr = testFnr))
        database.list { alleUtkast }.assert {
            size shouldBe 5
            filter { utkast -> utkast.sistEndret != null && utkast.slettet != null }
                .size shouldBe 0
        }
    }

    @Test
    fun `plukker opp updated events`() {
        val testEventId = "qqeedd1"

        testRapid.sendTestMessage(createUtkastTestPacket(eventId = testEventId, fnr = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = "qqeedd2", fnr = testFnr))
        testRapid.sendTestMessage(updateUtkastTestPacket(testEventId))
        database.list { alleUtkast }.assert {
            size shouldBe 2
            find { utkast -> utkast.eventId == testEventId }.assert {
                require(this != null)
                this.sistEndret shouldNotBe null
                this.slettet shouldBe null
            }
        }
    }

    @Test
    fun `plukker opp deleted events`() {
        val oppdatertEventId = "qqeedd1"
        val slettetEventId = "qqeedd99"

        testRapid.sendTestMessage(createUtkastTestPacket(eventId = oppdatertEventId, fnr = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = slettetEventId, fnr = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = "qqeedd2", fnr = testFnr))
        testRapid.sendTestMessage(updateUtkastTestPacket(oppdatertEventId))
        testRapid.sendTestMessage(deleteUtkastTestPacket(slettetEventId))

        database.list { alleUtkast }.assert {
            size shouldBe 3
            find { it.eventId == slettetEventId }.assert {
                require(this != null)
                this.slettet shouldNotBe null
            }
        }
    }
}

internal inline fun <T> T.assert(block: T.() -> Unit): T =
    apply {
        block()
    }

