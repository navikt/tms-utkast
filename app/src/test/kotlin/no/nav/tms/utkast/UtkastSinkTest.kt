package no.nav.tms.utkast

import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotliquery.queryOf
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.utkast.database.UtkastRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UtkastSinkTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val testRapid = TestRapid()
    private val testFnr = "12345678910"

    @BeforeAll
    fun setup() {
        setupSinks(testRapid, UtkastRepository(database))
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
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))

        database.list { alleUtkast }.assert {
            size shouldBe 1
        }
    }

    @Test
    fun `plukker opp updated events`() {
        val testUtkastId1 = randomUUID()
        val testUtkastId2 = randomUUID()
        val nyTittel = "Ny tittel"
        val tittelNo = "En tittel"
        val nyTittelEn = "Other title"

        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = testUtkastId1, ident = testFnr))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = testUtkastId2, ident = testFnr, tittelI18n = mapOf("no" to tittelNo)))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        testRapid.sendTestMessage(updateUtkastTestPacket(
            testUtkastId1,
            tittel = nyTittel
        ))
        testRapid.sendTestMessage(updateUtkastTestPacket(
            testUtkastId2,
            tittelI18n = mapOf("en" to nyTittelEn),
            metrics = mapOf("skjemakode" to "skjemakode","skjemanavn" to "skjemanavn")
        ))
        testRapid.sendTestMessage(updateUtkastTestPacket(testUtkastId2, metrics = mapOf("skjemakode" to "skjemakode","skjemanavn" to "skjemanavn")))

        database.list { alleUtkast }.assert {
            size shouldBe 3
            find { utkast -> utkast.utkastId == testUtkastId1 }.assert {
                require(this != null)
                this.sistEndret shouldNotBe null
                this.slettet shouldBe null
                this.tittel shouldBe nyTittel
                this.tittelI18n.isEmpty() shouldBe true
            }

            find { utkast -> utkast.utkastId == testUtkastId2 }.assert {
                require(this != null)
                this.sistEndret shouldNotBe null
                this.slettet shouldBe null
                this.tittelI18n shouldContain ("no" to tittelNo)
                this.tittelI18n shouldContain ("en" to nyTittelEn)
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
        testRapid.sendTestMessage(updateUtkastTestPacket(
            utkastId = utkastId1,
            metrics = mapOf("skjemakode" to "skjemakode","skjemanavn" to "skjemanavn")
        ))
        testRapid.sendTestMessage(updateUtkastTestPacket(
            utkastId = utkastId1,
            link = "Bad link",
            metrics = mapOf("skjemakode" to "skjemakode","skjemanavn" to "skjemanavn")
        ))
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
        testRapid.sendTestMessage(updateUtkastTestPacket(
            oppdatertUtkastId,
            metrics = mapOf("skjemakode" to "skjemakode","skjemanavn" to "skjemanavn")
        ))
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

