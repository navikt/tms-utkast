package no.nav.tms.utkast

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.utkast.database.UtkastRepository
import no.nav.tms.utkast.builder.UtkastJsonBuilder
import org.junit.jupiter.api.Test
import java.util.UUID

class ProducerLibraryVerification {
    private val repositoryMock: UtkastRepository = mockk(relaxed = true)
    private val testRapid = TestRapid()

    init {
        setupSinks(testRapid, repositoryMock)
    }

    @Test
    fun `Bruker rikigtige felter i create`() {
        UtkastJsonBuilder()
            .withUtkastId(UUID.randomUUID().toString())
            .withIdent("1122334455")
            .withTittel("Test tittel")
            .withLink("https://wattevs")
            .create()
            .also { testRapid.sendTestMessage(it) }

        verify { repositoryMock.createUtkast(any()) }
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

        verify { repositoryMock.updateUtkast(testId, any()) }
    }

    @Test
    fun `Bruker rikigtige felter i delete`() {
        val testId = UUID.randomUUID().toString()

        UtkastJsonBuilder()
            .withUtkastId(testId)
            .delete()
            .also { testRapid.sendTestMessage(it) }


        verify { repositoryMock.deleteUtkast(testId) }
    }

}
