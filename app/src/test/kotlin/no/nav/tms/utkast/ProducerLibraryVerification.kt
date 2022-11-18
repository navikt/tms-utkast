package no.nav.tms.utkast

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.utkast.database.UtkastRepository
import no.nav.tms.utkast.producer.PacketBuilder
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
        JsonMessage.newMessage(
            PacketBuilder.created(
                eventId = UUID.randomUUID(),
                tittel = "Test tittel",
                link = "https://wattevs",
                ident = "1122334455"
            )
        ).apply {
            testRapid.sendTestMessage(this.toJson())
        }

        verify { repositoryMock.createUtkast(any()) }
    }

    @Test
    fun `Bruker rikigtige felter i update`() {
        val testId = UUID.randomUUID()
        JsonMessage.newMessage(
            PacketBuilder.updated(
                eventId = testId,
                link = "https://wattevs",
            )
        ).apply {
            testRapid.sendTestMessage(this.toJson())
        }
        verify { repositoryMock.updateUtkast(testId.toString(), any()) }
    }

    @Test
    fun `Bruker rikigtige felter i delete`() {
        val testId = UUID.randomUUID()
        JsonMessage.newMessage(
            PacketBuilder.deleted(
                eventId = testId,
            )
        ).apply {
            testRapid.sendTestMessage(this.toJson())
        }
        verify { repositoryMock.deleteUtkast(testId.toString()) }
    }

}