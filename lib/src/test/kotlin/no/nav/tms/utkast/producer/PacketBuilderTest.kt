package no.nav.tms.utkast.producer

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.UUID


internal class PacketBuilderTest {

    @Test
    fun `bygger created map`() {
        val testId = UUID.randomUUID()
        val testLink = "https://test.link"
        val testIdent = "12345678910"
        val testTittel = "Bø på test"
        PacketBuilder.created(
            eventId = testId,
            link = testLink,
            ident = testIdent,
            tittel = testTittel
        ).assert {
            this["@event_name"] shouldBe "created"
            UUID.fromString(this["eventId"]) shouldBe testId
            this["link"] shouldBe testLink
            this["ident"] shouldBe testIdent
            this["tittel"] shouldBe testTittel
        }
    }
}

internal inline fun <T> T.assert(block: T.() -> Unit): T =
    apply {
        block()
    }