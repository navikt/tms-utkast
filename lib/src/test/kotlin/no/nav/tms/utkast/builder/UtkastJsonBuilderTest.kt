package no.nav.tms.utkast.builder

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import java.util.UUID


internal class UtkastJsonBuilderTest {

    @Test
    fun `bygger created json-objekt`() {
        val testId = UUID.randomUUID().toString()
        val testLink = "https://test.link"
        val testIdent = "12345678910"
        val testTittel = "Bø på test"

        UtkastJsonBuilder.newBuilder()
            .withUtkastId(testId)
            .withIdent(testIdent)
            .withLink(testLink)
            .withTittel(testTittel)
            .create()
            .assertJson {
                getText("@event_name") shouldBe EventName.created.name
                getText("utkastId") shouldBe testId
                getText("link") shouldBe testLink
                getText("ident") shouldBe testIdent
                getText("tittel") shouldBe testTittel
            }
    }

    @Test
    fun `krever alle felt for created`() {
        val testId = UUID.randomUUID().toString()
        val testLink = "https://test.link"
        val testIdent = "12345678910"
        val testTittel = "Bø på test"


        shouldThrow<IllegalArgumentException> {
            UtkastJsonBuilder.newBuilder()
                .create()
        }

        shouldThrow<IllegalArgumentException> {
            UtkastJsonBuilder.newBuilder()
                .withUtkastId(testId)
                .create()
        }

        shouldThrow<IllegalArgumentException> {
            UtkastJsonBuilder.newBuilder()
                .withUtkastId(testId)
                .withIdent(testIdent)
                .create()
        }

        shouldThrow<IllegalArgumentException> {
            UtkastJsonBuilder.newBuilder()
                .withUtkastId(testId)
                .withIdent(testIdent)
                .withLink(testLink)
                .create()
        }

        shouldNotThrowAny{
            UtkastJsonBuilder.newBuilder()
                .withUtkastId(testId)
                .withIdent(testIdent)
                .withLink(testLink)
                .withTittel(testTittel)
                .create()
        }
    }

    @Test
    fun `bygger updated map json-objekt`() {
        val testId = UUID.randomUUID().toString()
        val testLink = "https://test.link"
        val testTittel = "Bø på test"

        UtkastJsonBuilder.newBuilder()
            .withUtkastId(testId)
            .withTittel(testTittel)
            .withLink(testLink)
            .update()
            .assertJson {
                getText("@event_name") shouldBe EventName.updated.name
                getText("utkastId") shouldBe testId
                getText("link") shouldBe testLink
                getText("tittel") shouldBe testTittel
            }

        UtkastJsonBuilder.newBuilder()
            .withUtkastId(testId)
            .withLink(testLink)
            .update()
            .assertJson {
                getText("@event_name") shouldBe EventName.updated.name
                getText("utkastId") shouldBe testId
                getText("link") shouldBe testLink
            }

        UtkastJsonBuilder.newBuilder()
            .withUtkastId(testId)
            .withTittel(testTittel)
            .update()
            .assertJson {
                getText("@event_name") shouldBe EventName.updated.name
                getText("utkastId") shouldBe testId
                getText("tittel") shouldBe testTittel
            }
    }

    @Test
    fun `krever utkastId felt for updated`() {
        val testId = UUID.randomUUID().toString()


        shouldThrow<IllegalArgumentException> {
            UtkastJsonBuilder.newBuilder()
                .update()
        }

        shouldNotThrowAny {
            UtkastJsonBuilder.newBuilder()
                .withUtkastId(testId)
                .update()
        }
    }

    @Test
    fun `bygger deleted map json-objekt`(){
        val testId = UUID.randomUUID().toString()
        UtkastJsonBuilder.newBuilder()
            .withUtkastId(testId)
            .delete()
            .assertJson {
                getText("@event_name") shouldBe EventName.deleted.name
                getText("utkastId") shouldBe testId
            }
    }

    @Test
    fun `krever utkastId for deleted`() {

        val testId = UUID.randomUUID().toString()

        shouldThrow<IllegalArgumentException> {
            UtkastJsonBuilder.newBuilder()
                .delete()
        }

        shouldNotThrowAny {
            UtkastJsonBuilder.newBuilder()
                .withUtkastId(testId)
                .delete()
        }
    }

    inline fun String.assertJson(block: JsonObject.() -> Unit) =
        apply {
            block(Json.parseToJsonElement(this).jsonObject)
        }

    fun JsonObject.getText(field: String) = get(field)?.jsonPrimitive?.content
}
