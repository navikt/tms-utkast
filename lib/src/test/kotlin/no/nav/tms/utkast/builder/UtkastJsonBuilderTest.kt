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
        val testTittelEngelsk = "Boo"

        UtkastJsonBuilder()
            .withUtkastId(testId)
            .withIdent(testIdent)
            .withLink(testLink)
            .withTittel(testTittel)
            .withTittel(testTittelEngelsk, Locale.ENGLISH)
            .create()
            .assertJson {
                getText("@event_name") shouldBe EventName.created.name
                getText("utkastId") shouldBe testId
                getText("link") shouldBe testLink
                getText("ident") shouldBe testIdent
                getMap("tittel")?.get("nb") shouldBe testTittel
                getMap("tittel")?.get(Locale.ENGLISH.language) shouldBe testTittelEngelsk
            }
    }

    @Test
    fun `tillater kun en tittel per språk`() {
        val testId = UUID.randomUUID().toString()
        val testLink = "https://test.link"
        val testIdent = "12345678910"
        val testTittel1 = "Bø på taket"
        val testTittel2 = "Bø i kjelleren"

        UtkastJsonBuilder()
            .withUtkastId(testId)
            .withIdent(testIdent)
            .withLink(testLink)
            .withTittel(testTittel1, Locale("nb"))
            .withTittel(testTittel2, Locale("nb"))
            .create()
            .assertJson {
                getText("@event_name") shouldBe EventName.created.name
                getText("utkastId") shouldBe testId
                getText("link") shouldBe testLink
                getText("ident") shouldBe testIdent
                getMap("tittel")?.get("nb") shouldBe testTittel2
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
        val testTittelEngelsk = "Boo"

        UtkastJsonBuilder()
            .withUtkastId(testId)
            .withTittel(testTittel)
            .withTittel(testTittelEngelsk, Locale.ENGLISH)
            .withLink(testLink)
            .update()
            .assertJson {
                getText("@event_name") shouldBe EventName.updated.name
                getText("utkastId") shouldBe testId
                getText("link") shouldBe testLink
                getMap("tittel")?.get("nb") shouldBe testTittel
                getMap("tittel")?.get(Locale.ENGLISH.language) shouldBe testTittelEngelsk
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
                getMap("tittel")?.get("nb") shouldBe testTittel
            }

        UtkastJsonBuilder()
            .withUtkastId(testId)
            .withTittel(testTittelEngelsk, Locale.ENGLISH)
            .update()
            .assertJson {
                getText("@event_name") shouldBe EventName.updated.name
                getText("utkastId") shouldBe testId
                getMap("tittel")?.get(Locale.ENGLISH.language) shouldBe testTittelEngelsk
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

    fun JsonObject.getMap(field: String) = get(field)?.jsonObject?.toMap()?.mapValues { (_,v) -> v.jsonPrimitive.content }
}
