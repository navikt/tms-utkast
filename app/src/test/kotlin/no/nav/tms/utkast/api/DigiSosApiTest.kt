package no.nav.tms.utkast.api

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.token.support.tokenx.validation.mock.LevelOfAssurance
import no.nav.tms.token.support.tokenx.validation.mock.tokenXMock
import no.nav.tms.utkast.asLocalDateTime
import no.nav.tms.utkast.sink.assert
import no.nav.tms.utkast.digisosExternalRouting
import no.nav.tms.utkast.setup.configureJackson
import no.nav.tms.utkast.shouldBeCaSameAs
import no.nav.tms.utkast.sink.LocalDateTimeHelper
import no.nav.tms.utkast.testUtkastData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DigiSosApiTest {

    private val tokendingsMockk = mockk<TokendingsService>().also {
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
        externalServices {
            hosts(digisosTestHost) {
                digisosExternalRouting(expextedUtkastData)
            }
        }

        api(createClient { configureJackson() })

        client.get("/utkast/digisos").assert {
            status shouldBe HttpStatusCode.OK
            val content = objectMapper.readTree(bodyAsText()).toList()
            content.size shouldBe 2
            expextedUtkastData.forEach { expected ->
                content.find { it["utkastId"].asText() == expected.utkastId }.assert {
                    requireNotNull(this)
                    this["tittel"].asText() shouldBe expected.tittel
                    this["link"].asText() shouldBe expected.link
                    this["opprettet"]?.asLocalDateTime() shouldBeCaSameAs expected.opprettet
                    this["sistEndret"]?.asLocalDateTime() shouldBeCaSameAs expected.sistEndret
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
        externalServices {
            hosts(digisosTestHost) {
                digisosExternalRouting(expextedUtkastData)
            }
        }
        api(createClient { configureJackson() })
        client.get("/utkast/digisos/antall").assert {
            status shouldBe HttpStatusCode.OK
            objectMapper.readTree(bodyAsText())["antall"].asInt() shouldBe 4
        }


    }

    private fun ApplicationTestBuilder.api(client: HttpClient) =
        application {
            utkastApi(
                utkastRepository = mockk(),
                utkastFetcher = UtkastFetcher(
                    digiSosBaseUrl = digisosTestHost,
                    httpClient = client,
                    digisosClientId = "dummyid",
                    tokendingsService = tokendingsMockk,
                    aapClientId = "dummyAapId"
                ),
                installAuthenticatorsFunction = {
                    authentication {
                        tokenXMock {
                            alwaysAuthenticated = true
                            setAsDefault = true
                            staticUserPid = testFnr
                            staticLevelOfAssurance = LevelOfAssurance.LEVEL_4
                        }
                    }
                })
        }
}
