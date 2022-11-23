package no.nav.tms.utkast.builder

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import java.util.*


internal class UtkastJsonBuilderTest {

    @Test
    fun `bygger created json-objekt`() {
        val testId = UUID.randomUUID().toString()
        val testLink = "https://test.link"
        val testIdent = "12345678910"
        val testTittel = "Bø på test"

        UtkastJsonBuilder()
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
            UtkastJsonBuilder()
                .create()
        }

        shouldThrow<IllegalArgumentException> {
            UtkastJsonBuilder()
                .withUtkastId(testId)
                .create()
        }

        shouldThrow<IllegalArgumentException> {
            UtkastJsonBuilder()
                .withUtkastId(testId)
                .withIdent(testIdent)
                .create()
        }

        shouldThrow<IllegalArgumentException> {
            UtkastJsonBuilder()
                .withUtkastId(testId)
                .withIdent(testIdent)
                .withLink(testLink)
                .create()
        }

        shouldNotThrowAny{
            UtkastJsonBuilder()
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

        UtkastJsonBuilder()
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

        UtkastJsonBuilder()
            .withUtkastId(testId)
            .withLink(testLink)
            .update()
            .assertJson {
                getText("@event_name") shouldBe EventName.updated.name
                getText("utkastId") shouldBe testId
                getText("link") shouldBe testLink
            }

        UtkastJsonBuilder()
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
            UtkastJsonBuilder()
                .update()
        }

        shouldNotThrowAny {
            UtkastJsonBuilder()
                .withUtkastId(testId)
                .update()
        }
    }

    @Test
    fun `bygger deleted map json-objekt`(){
        val testId = UUID.randomUUID().toString()
        UtkastJsonBuilder()
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
            UtkastJsonBuilder()
                .delete()
        }

        shouldNotThrowAny {
            UtkastJsonBuilder()
                .withUtkastId(testId)
                .delete()
        }
    }

    inline fun String.assertJson(block: JsonObject.() -> Unit) =
        apply {
            block(Json.parseToJsonElement(this).jsonObject)
        }

    fun JsonObject.getText(field: String) = get(field)?.jsonPrimitive?.content

    fun JsonObject.getMap(field: String) = get(field)?.jsonObject?.toMap()
}
