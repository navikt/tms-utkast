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

    @Test
    fun `bygger updated map`() {
        val testId = UUID.randomUUID()
        val testLink = "https://test.link"
        val testTittel = "Bø på test"
        PacketBuilder.updated(
            eventId = testId,
            link = testLink,
            tittel = testTittel
        ).assert {
            this["@event_name"] shouldBe "updated"
            UUID.fromString(this["eventId"]) shouldBe testId
            this["link"] shouldBe testLink
            this["tittel"] shouldBe testTittel
        }

        PacketBuilder.updated(
            eventId = testId,
            link = testLink,
        ).assert {
            this["@event_name"] shouldBe "updated"
            UUID.fromString(this["eventId"]) shouldBe testId
            this["link"] shouldBe testLink
            this["tittel"] shouldBe null
        }

        PacketBuilder.updated(
            eventId = testId,
            tittel = testTittel,
        ).assert {
            this["@event_name"] shouldBe "updated"
            UUID.fromString(this["eventId"]) shouldBe testId
            this["link"] shouldBe null
            this["tittel"] shouldBe testTittel
        }
    }

    @Test
    fun `bygger deleted map`(){
        val testId = UUID.randomUUID()
        PacketBuilder.deleted(testId).apply {
            this["@event_name"] shouldBe "deleted"
            UUID.fromString(this["eventId"]) shouldBe testId
        }
    }
}

internal inline fun <T> T.assert(block: T.() -> Unit): T =
    apply {
        block()
    }