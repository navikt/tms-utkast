package no.nav.tms.utkast.sink

import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotliquery.queryOf
import no.nav.tms.utkast.createUtkastTestPacket
import no.nav.tms.utkast.database.LocalPostgresDatabase
import no.nav.tms.utkast.database.alleUtkast
import no.nav.tms.utkast.deleteUtkastTestPacket
import no.nav.tms.utkast.setupBroadcaster
import no.nav.tms.utkast.updateUtkastTestPacket
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UtkastSinkTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val testFnr = "12345678910"

    private val repository = UtkastRepository(database)
    private val broadcaster = setupBroadcaster(repository)

    @AfterEach
    fun cleanup() {
        database.update {
            queryOf("delete from utkast")
        }
    }

    @Test
    fun `plukker opp created events`() {

        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))

        database.list { alleUtkast }.run {
            size shouldBe 5
            filter { utkast -> utkast.sistEndret != null && utkast.slettet != null }
                .size shouldBe 0
        }
    }

    @Test
    fun `forkaster created events med ugyldig data`() {

        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = "bad utkastId", ident = testFnr))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = randomUUID(), ident = "tooLongIdent"))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr, link = "bad link"))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))

        database.list { alleUtkast }.run {
            size shouldBe 1
        }
    }

    @Test
    fun `plukker opp updated events`()  {
        val testUtkastId1 = randomUUID()
        val testUtkastId2 = randomUUID()
        val nyTittel = "Ny tittel"
        val tittelNo = "En tittel"
        val nyTittelEn = "Other title"


        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = testUtkastId1, ident = testFnr))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = testUtkastId2, ident = testFnr, tittelI18n = mapOf("no" to tittelNo)))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        broadcaster.broadcastJson(
            updateUtkastTestPacket(
            testUtkastId1,
            tittel = nyTittel
        )
        )
        broadcaster.broadcastJson(
            updateUtkastTestPacket(
            testUtkastId2,
            tittelI18n = mapOf("en" to nyTittelEn),
            metrics = mapOf("skjemakode" to "skjemakode","skjemanavn" to "skjemanavn")
        )
        )
        broadcaster.broadcastJson(updateUtkastTestPacket(testUtkastId2, metrics = mapOf("skjemakode" to "skjemakode","skjemanavn" to "skjemanavn")))

        database.list { alleUtkast }.run {
            size shouldBe 3
            find { utkast -> utkast.utkastId == testUtkastId1 }.run {
                require(this != null)
                this.sistEndret shouldNotBe null
                this.slettet shouldBe null
                this.tittel shouldBe nyTittel
                this.tittelI18n.isEmpty() shouldBe true
            }

            find { utkast -> utkast.utkastId == testUtkastId2 }.run {
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


        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = utkastId1, ident = testFnr))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = utkastId2, ident = testFnr))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = utkastId3, ident = testFnr))
        broadcaster.broadcastJson(
            updateUtkastTestPacket(
            utkastId = utkastId1,
            metrics = mapOf("skjemakode" to "skjemakode","skjemanavn" to "skjemanavn")
        )
        )
        broadcaster.broadcastJson(
            updateUtkastTestPacket(
            utkastId = utkastId1,
            link = "Bad link",
            metrics = mapOf("skjemakode" to "skjemakode","skjemanavn" to "skjemanavn")
        )
        )
        database.list { alleUtkast }.run {
            size shouldBe 3

            filter { it.sistEndret != null }.size shouldBe 1
        }
    }

    @Test
    fun `plukker opp deleted events`() {
        val oppdatertUtkastId = randomUUID()
        val slettetUtkastId = randomUUID()

        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = oppdatertUtkastId, ident = testFnr))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = slettetUtkastId, ident = testFnr))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        broadcaster.broadcastJson(
            updateUtkastTestPacket(
            oppdatertUtkastId,
            metrics = mapOf("skjemakode" to "skjemakode","skjemanavn" to "skjemanavn")
        )
        )
        broadcaster.broadcastJson(deleteUtkastTestPacket(slettetUtkastId))

        database.list { alleUtkast }.run {
            size shouldBe 3
            find { it.utkastId == slettetUtkastId }.run {
                require(this != null)
                slettet shouldNotBe null
            }
        }
    }

    private fun randomUUID() = UUID.randomUUID().toString()
}
