package no.nav.tms.utkast

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.asOptionalLocalDateTime
import no.nav.tms.token.support.authentication.installer.mock.installMockedAuthenticators
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.token.support.tokenx.validation.mock.LevelOfAssurance
import no.nav.tms.utkast.config.LocalDateTimeHelper
import no.nav.tms.utkast.config.configureJackson
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DigiSosApiTest {

    val tokendingsMockk = mockk<TokendingsService>().also {
        coEvery { it.exchangeToken(any(), any()) } returns "<dummytoken>"
    }

    private val objectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
    }
    private val digisosTestHost = "http://www.digisos.test"
    private val testFnr = "88776655"


    @Test
    fun `henter beskjeder fra digisos og presenterer dem som utkast`() = testApplication {
        val expextedUtkastData = listOf(
            testUtkastData(startTestTime = LocalDateTimeHelper.nowAtUtc()),
            testUtkastData(startTestTime = LocalDateTimeHelper.nowAtUtc())
        )
        digisosService(digisosTestHost, expextedUtkastData)
        api(createClient { configureJackson() })

        client.get("/utkast/digisos").assert {
            status shouldBe HttpStatusCode.OK
            val content = objectMapper.readTree(bodyAsText()).toList()
            content.size shouldBe 2
            expextedUtkastData.forEach { expected ->
                content.find { it["utkastId"].asText() == expected.utkastId }.assert {
                    require(this != null)
                    this["tittel"].asText() shouldBe expected.tittel
                    this["link"].asText() shouldBe expected.link
                    this["opprettet"].asLocalDateTime() shouldBeCaSameAs expected.opprettet
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
            testUtkastData(startTestTime = LocalDateTimeHelper.nowAtUtc()),
            testUtkastData(startTestTime = LocalDateTimeHelper.nowAtUtc()),
            testUtkastData(startTestTime = LocalDateTimeHelper.nowAtUtc()),
        )
        digisosService(digisosTestHost, expextedUtkastData)
        api(createClient { configureJackson() })
        client.get("/utkast/digisos/antall").assert {
            status shouldBe HttpStatusCode.OK
            objectMapper.readTree(bodyAsText())["antall"].asInt() shouldBe 4
        }


    }

    private fun ApplicationTestBuilder.digisosService(testHost: String, expextedUtkastData: List<UtkastData>) =
        externalServices {
            hosts(testHost) {
                routing {
                    get("/dittnav/pabegynte/aktive") {
                        val digisosResp = expextedUtkastData.joinToString(
                            prefix = "[",
                            postfix = "]",
                            separator = ","
                        ) { it.toDigisosResponse() }
                        call.respondBytes(
                            contentType = ContentType.Application.Json,
                            provider = { digisosResp.toByteArray() })
                    }
                }
            }
        }

    private fun ApplicationTestBuilder.api(client: HttpClient) =
        application {
            utkastApi(
                utkastRepository = mockk(),
                digisosHttpClient = DigisosHttpClient(digisosTestHost, client, "dummyid", tokendingsMockk),
                installAuthenticatorsFunction = {
                    installMockedAuthenticators {
                        installTokenXAuthMock {
                            alwaysAuthenticated = true
                            setAsDefault = true
                            staticUserPid = testFnr
                            staticLevelOfAssurance = LevelOfAssurance.LEVEL_4
                        }
                    }
                })
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
        "link": "$link",
        "tekst": "$tittel",
        "sistOppdatert": null,
        "isAktiv": true
        }
    """.trimIndent()

private fun testUtkastData(tittelI18n: Map<String, String> = emptyMap(), startTestTime: LocalDateTime) = UtkastData(
    utkastId = UUID.randomUUID().toString(),
    tittel = "testTittel ${Random.nextInt(0, 10)}",
    tittelI18n = tittelI18n,
    link = "https://test.link",
    opprettet = startTestTime,
    sistEndret = null,
    slettet = null
)
