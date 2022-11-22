package no.nav.tms.utkast

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotliquery.queryOf
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.utkast.database.UtkastRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import java.util.*

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
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        database.list { alleUtkast }.assert {
            size shouldBe 5
            filter { utkast -> utkast.sistEndret != null && utkast.slettet != null }
                .size shouldBe 0
        }
    }

    @Test
    fun `forkaster created events med ugyldig data`() {
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = "bad utkastId", ident = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = randomUUID(), ident = "tooLongIdent"))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr, link = "bad link"))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr, tittel = "Too long tittel".repeat(100)))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        database.list { alleUtkast }.assert {
            size shouldBe 1
        }
    }

    @Test
    fun `plukker opp updated events`() {
        val testUtkastId = randomUUID()

        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = testUtkastId, ident = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        testRapid.sendTestMessage(updateUtkastTestPacket(testUtkastId))
        database.list { alleUtkast }.assert {
            size shouldBe 2
            find { utkast -> utkast.utkastId == testUtkastId }.assert {
                require(this != null)
                this.sistEndret shouldNotBe null
                this.slettet shouldBe null
            }
        }
    }

    @Test
    fun `forkaster updated events med ugyldig data`() {
        val utkastId1 = randomUUID()
        val utkastId2 = randomUUID()
        val utkastId3 = randomUUID()

        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = utkastId1, ident = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = utkastId2, ident = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = utkastId3, ident = testFnr))
        testRapid.sendTestMessage(updateUtkastTestPacket(utkastId = utkastId1))
        testRapid.sendTestMessage(updateUtkastTestPacket(utkastId = utkastId1, tittel = "Too long tittel".repeat(50)))
        testRapid.sendTestMessage(updateUtkastTestPacket(utkastId = utkastId1, link = "Bad link"))
        database.list { alleUtkast }.assert {
            size shouldBe 3

            filter { it.sistEndret != null }.size shouldBe 1
        }
    }

    @Test
    fun `plukker opp deleted events`() {
        val oppdatertUtkastId = randomUUID()
        val slettetUtkastId = randomUUID()

        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = oppdatertUtkastId, ident = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = slettetUtkastId, ident = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        testRapid.sendTestMessage(updateUtkastTestPacket(oppdatertUtkastId))
        testRapid.sendTestMessage(deleteUtkastTestPacket(slettetUtkastId))

        database.list { alleUtkast }.assert {
            size shouldBe 3
            find { it.utkastId == slettetUtkastId }.assert {
                require(this != null)
                this.slettet shouldNotBe null
            }
        }
    }

    private fun randomUUID() = UUID.randomUUID().toString()
}

internal inline fun <T> T.assert(block: T.() -> Unit): T =
    apply {
        block()
    }

