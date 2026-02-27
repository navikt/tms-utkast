package no.nav.tms.utkast.sink

import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotliquery.queryOf
import no.nav.tms.common.postgres.JsonbHelper.toJsonb
import no.nav.tms.utkast.createUtkastTestPacket
import no.nav.tms.utkast.database.LocalPostgresDatabase
import no.nav.tms.utkast.database.LocalPostgresDatabase.getUtkast
import no.nav.tms.utkast.deleteUtkastTestPacket
import no.nav.tms.utkast.setupBroadcaster
import no.nav.tms.utkast.updateUtkastTestPacket
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UtkastSubscriberTest {
    private val database = LocalPostgresDatabase.getCleanInstance()
    private val testFnr = "12345678910"

    private val repository = UtkastRepository(database)
    private val broadcaster = setupBroadcaster(repository)

    @AfterEach
    fun cleanup() {
        LocalPostgresDatabase.resetInstance()
    }

    @Test
    fun `plukker opp og lagrer utkast fra kafka basert på opprettet-event`() {
        val utkastId = randomUUID()
        val defaultTittel = "Norsk tittel"
        val engelskTittel = "Engelsk tittel"
        val link = "https://link.nav.no"
        val slettesEtter = ZonedDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MILLIS)

        createUtkastTestPacket(
            utkastId = utkastId,
            ident = testFnr,
            tittel = defaultTittel,
            tittelI18n = mapOf("en" to engelskTittel),
            link = link,
            slettesEtter = slettesEtter
        ).let {
            broadcaster.broadcastJson(it)
        }

        getUtkast(utkastId).let {
            it.shouldNotBeNull()
            it.utkastId shouldBe utkastId
            it.tittel shouldBe defaultTittel
            it.tittelI18n shouldBe mapOf("en" to engelskTittel)
            it.link shouldBe link
            it.opprettet.shouldNotBeNull()
            it.sistEndret.shouldBeNull()
            it.slettesEtter shouldBe slettesEtter
        }
    }

    @Test
    fun `plukker opp created events`() {

        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))

        LocalPostgresDatabase.alleUtkast().run {
            size shouldBe 5
            filter { utkast -> utkast.sistEndret != null }
                .size shouldBe 0
        }
    }

    @Test
    fun `forkaster created events med ugyldig data`() {

        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = "bad utkastId", ident = testFnr))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = randomUUID(), ident = "tooLongIdent"))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr, link = "bad link"))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = randomUUID(), ident = testFnr))

        LocalPostgresDatabase.alleUtkast().run {
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

        LocalPostgresDatabase.alleUtkast().run {
            size shouldBe 3
            find { utkast -> utkast.utkastId == testUtkastId1 }.run {
                require(this != null)
                this.sistEndret shouldNotBe null
                this.tittel shouldBe nyTittel
                this.tittelI18n.isEmpty() shouldBe true
            }

            find { utkast -> utkast.utkastId == testUtkastId2 }.run {
                require(this != null)
                this.sistEndret shouldNotBe null
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
        LocalPostgresDatabase.alleUtkast().run {
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

        LocalPostgresDatabase.alleUtkast().run {
            size shouldBe 2
            find { it.utkastId == slettetUtkastId }.shouldBeNull()
        }
    }

    @Test
    fun `tillater å endre men ikke fjerne slettesEtter-tidspunkt`() {
        val utkastId = randomUUID()
        val slettesEtter = ZonedDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MILLIS)
        val slettesEtterSenere = ZonedDateTime.now().plusDays(2).truncatedTo(ChronoUnit.MILLIS)

        createUtkastTestPacket(
            utkastId = utkastId,
            ident = testFnr,
            slettesEtter = slettesEtter
        ).let(broadcaster::broadcastJson)

        getUtkast(utkastId).let {
            it.shouldNotBeNull()
            it.slettesEtter shouldBe slettesEtter
        }

        updateUtkastTestPacket(utkastId, slettesEtter = slettesEtterSenere)
            .let(broadcaster::broadcastJson)

        getUtkast(utkastId).let {
            it.shouldNotBeNull()
            it.slettesEtter shouldBe slettesEtterSenere
        }

        updateUtkastTestPacket(utkastId, slettesEtter = null)
            .let(broadcaster::broadcastJson)

        getUtkast(utkastId).let {
            it.shouldNotBeNull()
            it.slettesEtter shouldBe slettesEtterSenere
        }
    }

    private fun randomUUID() = UUID.randomUUID().toString()
}
