package no.nav.tms.utkast

import io.kotest.matchers.shouldBe
import no.nav.tms.utkast.api.UtkastApiRepository
import no.nav.tms.utkast.builder.UtkastJsonBuilder
import no.nav.tms.utkast.database.LocalPostgresDatabase
import no.nav.tms.utkast.sink.UtkastRepository
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*

class ProducerLibraryVerification {
    private val database = LocalPostgresDatabase.getCleanInstance()

    private val utkastApiRepository = UtkastApiRepository(database)
    private val utkastSinkRepository = UtkastRepository(database)
    private val testPersonIdent = "1122334455"

    private val broadcaster = setupBroadcaster(utkastSinkRepository)


    @Test
    fun `Bruker riktige felter i create`() {
        val utkastId = UUID.randomUUID().toString()
        UtkastJsonBuilder()
            .withUtkastId(utkastId)
            .withIdent(testPersonIdent)
            .withTittel("Test tittel")
            .withLink("https://wattevs.test")
            .withSlettesEtter(ZonedDateTime.parse("2026-01-01T02:00:00Z"))
            .withMetrics("Skjemanavn", "99/88")
            .create()
            .also { broadcaster.broadcastJson(it) }

        val testUtkast = utkastApiRepository.getUtkastForIdent(testPersonIdent).first {
            it.utkastId == utkastId
        }

        testUtkast.metrics?.get("skjemanavn") shouldBe "Skjemanavn"
        testUtkast.metrics?.get("skjemakode") shouldBe "99/88"
        testUtkast.tittel shouldBe "Test tittel"
        testUtkast.slettesEtter?.toEpochSecond() shouldBe ZonedDateTime.parse("2026-01-01T02:00:00Z").toEpochSecond()
        testUtkast.link shouldBe "https://wattevs.test"
    }

    @Test
    fun `Bruker riktige felter i update`() {
        val testId = UUID.randomUUID().toString()
        sendCreatedMelding(testId)
        UtkastJsonBuilder()
            .withUtkastId(testId)
            .withIdent(testPersonIdent)
            .withTittel("Ny tittel")
            .withLink("https://ny.link")
            .withSlettesEtter(ZonedDateTime.parse("2026-01-01T02:00:00Z"))
            .update()
            .also {
                broadcaster.broadcastJson(it)
            }

        val testUtkast = utkastApiRepository.getUtkastForIdent(testPersonIdent).first {
            it.utkastId == testId
        }
        testUtkast.tittel shouldBe "Ny tittel"
        testUtkast.link shouldBe "https://ny.link"
        testUtkast.slettesEtter?.toEpochSecond() shouldBe ZonedDateTime.parse("2026-01-01T02:00:00Z").toEpochSecond()
    }

    @Test
    fun `Bruker riktige felter i delete`() {
        val testId = UUID.randomUUID().toString()
        sendCreatedMelding(testId)

        UtkastJsonBuilder()
            .withUtkastId(testId)
            .delete()
            .also { broadcaster.broadcastJson(it) }

        utkastApiRepository.getUtkastForIdent(testPersonIdent).firstOrNull {
            it.utkastId == testId
        } shouldBe null

    }

    private fun sendCreatedMelding(utkastId: String) {

        UtkastJsonBuilder()
            .withUtkastId(utkastId)
            .withIdent(testPersonIdent)
            .withTittel("Test tittel")
            .withLink("https://wattevs.test")
            .withMetrics("Skjemanavn", "99/88")
            .create()
            .also { broadcaster.broadcastJson(it) }

    }
}
