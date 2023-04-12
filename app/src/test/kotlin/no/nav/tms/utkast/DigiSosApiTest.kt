package no.nav.tms.utkast

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.asOptionalLocalDateTime
import no.nav.tms.utkast.config.LocalDateTimeHelper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DigiSosApiTest {

    private val objectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
    }
    private val digisosTestHost = "https://digisos.test"
    private val testFnr = "88776655"


    @Test
    fun `henter beskjeder fra digisos og presenterer dem som utkast`() = testApplication {
        val expextedUtkastData = listOf(
            testUtkastData(startTestTime = LocalDateTimeHelper.nowAtUtc()),
            testUtkastData(startTestTime = LocalDateTimeHelper.nowAtUtc())
        )

        digisosService(digisosTestHost, expextedUtkastData)

        client.get("/utkast/digisos").assert {
            status shouldBe HttpStatusCode.OK
            val content = objectMapper.readTree(bodyAsText()).toList()
            content.size shouldBe 2
            expextedUtkastData.forEach { expected ->
                content.find { it["utkastId"].asText() == expected.utkastId }.assert {
                    require(this != null)
                    this["tittel"].asText() shouldBe expected.tittel
                    this["link"].asText() shouldBe expected.link
                    this["opprettet"].asLocalDateTime() shouldNotBe null
                    this["sistEndret"].asOptionalLocalDateTime() shouldBeCaSameAs expected.sistEndret
                    this["metrics"]?.get("skjemakode")?.asText() shouldBe expected.metrics?.get("skjemakode")
                    this["metrics"]?.get("skjemanavn")?.asText() shouldBe expected.metrics?.get("skjemanavn")
                }
            }

        }
    }

    @Test
    fun `henter riktig antall 'utkast' fra digisos`() = testApplication {
        val expextedUtkastData = listOf(
            testUtkastData(startTestTime = LocalDateTimeHelper.nowAtUtc()),
            testUtkastData(startTestTime = LocalDateTimeHelper.nowAtUtc())
        )
        digisosService(digisosTestHost, expextedUtkastData)
        client.get("/utkast/digisos/antall").assert {
            status shouldBe HttpStatusCode.OK
            objectMapper.readTree(bodyAsText())["antall"] shouldBe 2
        }


    }

    private fun ApplicationTestBuilder.digisosService(testHost: String, expextedUtkastData: List<UtkastData>) =
        externalServices {
            require(expextedUtkastData.size == 2)
            hosts(testHost) {
                routing {
                    get("/dittnav/pabegynte/aktive") {
                        if ("hfajhfak" == testFnr) {
                            val digisosResp = """
                            [
                                ${expextedUtkastData.first().toDigisosResponse()},
                                ${expextedUtkastData.last().toDigisosResponse()}
                            ]
                        """.trimIndent()
                            call.respondBytes(
                                contentType = ContentType.Application.Json,
                                provider = { digisosResp.toByteArray() })
                        } else {
                            call.respondBytes(
                                contentType = ContentType.Application.Json,
                                provider = { "[]".toByteArray() })
                        }
                    }
                }
            }
        }
}


@Language("JSON")
private fun UtkastData.toDigisosResponse() =
    """
        {
        "eventTidspunkt" : "$opprettet",
        "eventId":"$utkastId",
        "grupperingsId":"tadda",
        "sikkerhetsniva": "3",
        "link": "$link"
        "tekst": "dummytekst",
        "sistOppdatert": "${opprettet.plusMinutes(3)}"
        "isAktiv": true
        }
    """

private fun testUtkastData(tittelI18n: Map<String, String> = emptyMap(), startTestTime: LocalDateTime) = UtkastData(
    utkastId = UUID.randomUUID().toString(),
    tittel = "testTittel ${Random.nextInt(0, 10)}",
    tittelI18n = tittelI18n,
    link = "https://test.link",
    opprettet = startTestTime,
    sistEndret = null,
    slettet = null
)