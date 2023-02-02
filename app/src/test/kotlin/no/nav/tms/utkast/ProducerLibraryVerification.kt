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

    init {
        setupSinks(testRapid, utkastRepository)
    }

    @Test
    fun `Bruker rikigtige felter i create`() {
        val utkastId = UUID.randomUUID().toString()
        UtkastJsonBuilder()
            .withUtkastId(utkastId)
            .withIdent("1122334455")
            .withTittel("Test tittel")
            .withLink("https://wattevs")
            .withMetrics("Skjemanavn", "99/88")
            .create()
            .also { testRapid.sendTestMessage(it) }
        val testUtkast = utkastRepository.getUtkast("1122334455").first {
            it.utkastId == utkastId
        }

        testUtkast.metrics?.get("skjemanavn") shouldBe "Skjemanavn"
        testUtkast.metrics?.get("skjemakode") shouldBe "99/88"
    }

    @Test
    fun `Bruker rikigtige felter i update`() {
        val testId = UUID.randomUUID().toString()

        UtkastJsonBuilder()
            .withUtkastId(testId)
            .withIdent("1122334455")
            .withLink("https://wattevs")
            .update()
            .also { testRapid.sendTestMessage(it) }

    }

    @Test
    fun `Bruker rikigtige felter i delete`() {
        val testId = UUID.randomUUID().toString()

        UtkastJsonBuilder()
            .withUtkastId(testId)
            .delete()
            .also { testRapid.sendTestMessage(it) }

    }

}
