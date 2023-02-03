package no.nav.tms.utkast

import io.kotest.matchers.shouldBe
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.utkast.builder.UtkastJsonBuilder
import no.nav.tms.utkast.database.UtkastRepository
import org.junit.jupiter.api.Test
import java.util.*

class ProducerLibraryVerification {
    private val utkastRepository = UtkastRepository(LocalPostgresDatabase.cleanDb())
    private val testRapid = TestRapid()
    private val testPersonIdent = "1122334455"

    init {
        setupSinks(testRapid, utkastRepository)
    }

    @Test
    fun `Bruker rikigtige felter i create`() {
        val utkastId = UUID.randomUUID().toString()
        UtkastJsonBuilder()
            .withUtkastId(utkastId)
            .withIdent(testPersonIdent)
            .withTittel("Test tittel")
            .withLink("https://wattevs.test")
            .withMetrics("Skjemanavn", "99/88")
            .create()
            .also { testRapid.sendTestMessage(it) }
        val testUtkast = utkastRepository.getUtkastForIdent(testPersonIdent).first {
            it.utkastId == utkastId
        }

        testUtkast.metrics?.get("skjemanavn") shouldBe "Skjemanavn"
        testUtkast.metrics?.get("skjemakode") shouldBe "99/88"
        testUtkast.tittel shouldBe "Test tittel"
        testUtkast.link shouldBe "https://wattevs.test"
    }

    @Test
    fun `Bruker rikigtige felter i update`() {
        val testId = UUID.randomUUID().toString()
        sendCreatedMelding(testId)
        UtkastJsonBuilder()
            .withUtkastId(testId)
            .withIdent(testPersonIdent)
            .withTittel("Ny tittel")
            .withLink("https://ny.link")
            .update()
            .also {
                testRapid.sendTestMessage(it)
            }

        val testUtkast = utkastRepository.getUtkastForIdent(testPersonIdent).first {
            it.utkastId == testId
        }
        testUtkast.tittel shouldBe "Ny tittel"
        testUtkast.link shouldBe "https://ny.link"
    }

    @Test
    fun `Bruker rikigtige felter i delete`() {
        val testId = UUID.randomUUID().toString()
        sendCreatedMelding(testId)

        UtkastJsonBuilder()
            .withUtkastId(testId)
            .delete()
            .also { testRapid.sendTestMessage(it) }

        utkastRepository.getUtkastForIdent(testPersonIdent).firstOrNull {
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
            .also { testRapid.sendTestMessage(it) }

    }
}
