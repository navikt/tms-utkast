package no.nav.tms.utkast

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kotliquery.queryOf
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.utkast.database.UtkastRepository
import no.nav.tms.utkast.producer.PacketBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll

internal class UtkastSinkTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val testRapid = TestRapid()
    private val testFnr = "12345678910"

    @BeforeAll
    fun setup() {
        setupSinks(testRapid,UtkastRepository(database))
    }

    @AfterEach
    fun cleanup() {
        database.update {
            queryOf("delete from utkast")
        }
    }

    @Test
    fun `plukker opp created events`() {
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = "qqeedd1", ident = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = "qqeedd2", ident = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = "qqeedd3", ident = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = "qqeedd4", ident = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = "qqeedd5", ident = testFnr))
        database.list { alleUtkast }.assert {
            size shouldBe 5
            filter { utkast -> utkast.sistEndret != null && utkast.slettet != null }
                .size shouldBe 0
        }
    }

    @Test
    fun `plukker opp updated events`() {
        val testEventId = "qqeedd1"

        testRapid.sendTestMessage(createUtkastTestPacket(eventId = testEventId, ident = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = "qqeedd2", ident = testFnr))
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

        testRapid.sendTestMessage(createUtkastTestPacket(eventId = oppdatertEventId, ident = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = slettetEventId, ident = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = "qqeedd2", ident = testFnr))
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

