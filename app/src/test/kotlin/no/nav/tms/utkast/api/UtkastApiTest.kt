package no.nav.tms.utkast.api

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.asOptionalLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.token.support.tokenx.validation.mock.LevelOfAssurance
import no.nav.tms.token.support.tokenx.validation.mock.tokenXMock
import no.nav.tms.utkast.*
import no.nav.tms.utkast.UtkastData
import no.nav.tms.utkast.createUtkastTestPacket
import no.nav.tms.utkast.database.LocalPostgresDatabase
import no.nav.tms.utkast.setup.configureJackson
import no.nav.tms.utkast.setupSinks
import no.nav.tms.utkast.sink.LocalDateTimeHelper
import no.nav.tms.utkast.sink.UtkastSinkRepository
import no.nav.tms.utkast.sink.assert
import no.nav.tms.utkast.updateUtkastTestPacket
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UtkastApiTest {
    private val objectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
    }

    private val repository = UtkastApiRepository(LocalPostgresDatabase.cleanDb())
    private val testRapid = TestRapid()
    private val testFnr1 = "19873569100"
    private val testFnr2 = "19873100100"
    private val startTestTime = LocalDateTimeHelper.nowAtUtc()
    private val digisosTestHost = "http://www.digisos.test"
    private val aapTestHost = "http://innsending.aap"
    private val tokendingsMockk = mockk<TokendingsService>().also {
        coEvery { it.exchangeToken(any(), any()) } returns "<dummytoken>"
    }

    private val utkastForTestFnr1 = mutableListOf(
        testUtkastData(),
        testUtkastData(),
        testUtkastData(),
        testUtkastData()
    )

    private val utkastForTestFnr2 =
        testUtkastData(tittelI18n = mapOf("en" to "English title", "nn" to "Nynorsk tittel"))


    @BeforeAll
    fun populate() {
        setupSinks(testRapid, UtkastSinkRepository(LocalPostgresDatabase.cleanDb()))
        utkastForTestFnr1.forEach {
            testRapid.sendTestMessage(it.toTestMessage(testFnr1))
        }
        testRapid.sendTestMessage(utkastForTestFnr2.toTestMessage(testFnr2))
        testRapid.sendTestMessage(
            updateUtkastTestPacket(
                utkastForTestFnr1[0].utkastId,
                metrics = mapOf("skjemakode" to "skjemakode", "skjemanavn" to "skjemanavn")
            )
        )
        utkastForTestFnr1[0] = utkastForTestFnr1[0].copy(sistEndret = LocalDateTimeHelper.nowAtUtc())
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = UUID.randomUUID().toString(), ident = "9988776655"))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = UUID.randomUUID().toString(), ident = "9988776655"))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = UUID.randomUUID().toString(), ident = "9988776655"))
    }

    @Test
    fun `henter utkast for bruker med ident`() = testApplication {
        application {
            utkastApi(
                utkastRepository = repository,
                installAuthenticatorsFunction = {
                    authentication {
                        tokenXMock {
                            alwaysAuthenticated = true
                            setAsDefault = true
                            staticUserPid = testFnr1
                            staticLevelOfAssurance = LevelOfAssurance.LEVEL_4
                        }
                    }
                },
                utkastFetcher = mockk()
            )
        }

        client.get("/utkast/antall").assert {
            status shouldBe HttpStatusCode.OK
            objectMapper.readTree(bodyAsText())["antall"].asInt() shouldBe 4
        }

        client.get("/utkast").assert {
            status.shouldBe(HttpStatusCode.OK)
            objectMapper.readTree(bodyAsText()).assert {
                size() shouldBe 4
                forEach { jsonNode ->
                    val utkastId = jsonNode["utkastId"].asText()
                    val forventedeVerdier =
                        utkastForTestFnr1.find { it.utkastId == utkastId }
                            ?: throw AssertionError("Fant utkast som ikke tilhører ident, utkastId : $utkastId")
                    jsonNode["tittel"].asText() shouldBe forventedeVerdier.tittel
                    jsonNode["link"].asText() shouldBe forventedeVerdier.link
                    jsonNode["opprettet"].asLocalDateTime() shouldNotBe null
                    jsonNode["sistEndret"].asOptionalLocalDateTime() shouldBeCaSameAs forventedeVerdier.sistEndret
                    jsonNode["metrics"]?.get("skjemakode")
                        ?.asText() shouldBe forventedeVerdier.metrics?.get("skjemakode")
                    jsonNode["metrics"]?.get("skjemanavn")
                        ?.asText() shouldBe forventedeVerdier.metrics?.get("skjemanavn")
                }
            }
        }
    }

    @Test
    fun `v2 henter utkast fra flere kilder`() {
        val digisosUtkast = testUtkastData(
            opprettet = LocalDateTime.now().minusDays(2)
        )
        val aapUtkast = testUtkastData(
            opprettet = LocalDateTime.now().plusHours(1),
            id = "AAP"
        )

        val alleForventedeUtkast = (utkastForTestFnr1 + listOf(digisosUtkast, aapUtkast))
            .sortedBy { data -> data.sistEndret ?: data.opprettet }

        testApplication {
            val applicationClient = createClient { configureJackson() }
            application {
                utkastApi(
                    utkastRepository = repository,
                    installAuthenticatorsFunction = {
                        authentication {
                            tokenXMock {
                                alwaysAuthenticated = true
                                setAsDefault = true
                                staticUserPid = testFnr1
                                staticLevelOfAssurance = LevelOfAssurance.LEVEL_4
                            }
                        }
                    },
                    utkastFetcher = UtkastFetcher(
                        digiSosBaseUrl = digisosTestHost,
                        httpClient = applicationClient,
                        digisosClientId = "dummyid",
                        tokendingsService = tokendingsMockk,
                        aapClientId = "dummyAAp"
                    )
                )
            }

            externalServices {
                hosts(digisosTestHost, aapTestHost) {
                    install(ExternalServicesDebug)
                    digisosExternalRouting(listOf(digisosUtkast))
                    aapExternalRouting(aapUtkast)
                }
            }

            client.get("v2/utkast/antall").assert {
                status shouldBe HttpStatusCode.OK
                objectMapper.readTree(bodyAsText())["antall"].asInt() shouldBe 6
            }

            client.get("v2/utkast").assert {
                status.shouldBe(HttpStatusCode.OK)
                objectMapper.readTree(bodyAsText()).assert {
                    map {
                        it["sistEndret"].asOptionalLocalDateTime()?.toLocalDate() ?: it["opprettet"].asLocalDateTime()
                            .toLocalDate()
                    }.sortedByDescending { it } shouldBe alleForventedeUtkast.map {
                        it.sistEndret?.toLocalDate() ?: it.opprettet.toLocalDate()
                    }
                        .sortedByDescending { it }

                    size() shouldBe 6
                    forEach { jsonNode ->
                        val utkastId = jsonNode["utkastId"].asText()
                        val forventedeVerdier =
                            alleForventedeUtkast.find { it.utkastId == utkastId }
                                ?: throw AssertionError("Fant utkast som ikke tilhører ident, utkastId : $utkastId")
                        jsonNode["tittel"].asText() shouldBe forventedeVerdier.tittel
                        jsonNode["link"].asText() shouldBe forventedeVerdier.link
                        jsonNode["opprettet"].asLocalDateTime() shouldNotBe null
                        jsonNode["metrics"]?.get("skjemakode")
                            ?.asText() shouldBe forventedeVerdier.metrics?.get("skjemakode")
                        jsonNode["metrics"]?.get("skjemanavn")
                            ?.asText() shouldBe forventedeVerdier.metrics?.get("skjemanavn")
                    }

                }
            }
        }
    }

    @Test
    fun `v2 henter utkast når det ikke finnes noen eksterne`() {

        testApplication {
            val applicationClient = createClient { configureJackson() }
            application {
                utkastApi(
                    utkastRepository = repository,
                    installAuthenticatorsFunction = {
                        authentication {
                            tokenXMock {
                                alwaysAuthenticated = true
                                setAsDefault = true
                                staticUserPid = testFnr1
                                staticLevelOfAssurance = LevelOfAssurance.LEVEL_4
                            }
                        }
                    },
                    utkastFetcher = UtkastFetcher(
                        digiSosBaseUrl = digisosTestHost,
                        httpClient = applicationClient,
                        digisosClientId = "dummyid",
                        tokendingsService = tokendingsMockk,
                        aapClientId = "dummyAAp"
                    )
                )
            }

            externalServices {
                hosts(digisosTestHost, aapTestHost) {
                    install(ExternalServicesDebug)
                    digisosExternalRouting(listOf())
                    aapExternalRouting(null)
                }
            }

            client.get("v2/utkast/antall").assert {
                status shouldBe HttpStatusCode.OK
                objectMapper.readTree(bodyAsText())["antall"].asInt() shouldBe 4
            }

            client.get("v2/utkast").assert {
                status.shouldBe(HttpStatusCode.OK)
                objectMapper.readTree(bodyAsText()).assert {
                    size() shouldBe 4
                    forEach { jsonNode ->
                        val utkastId = jsonNode["utkastId"].asText()
                        val forventedeVerdier =
                            utkastForTestFnr1.find { it.utkastId == utkastId }
                                ?: throw AssertionError("Fant utkast som ikke tilhører ident, utkastId : $utkastId")
                        jsonNode["tittel"].asText() shouldBe forventedeVerdier.tittel
                        jsonNode["link"].asText() shouldBe forventedeVerdier.link
                        jsonNode["opprettet"].asLocalDateTime() shouldNotBe null
                        jsonNode["sistEndret"].asOptionalLocalDateTime() shouldBeCaSameAs forventedeVerdier.sistEndret
                        jsonNode["metrics"]?.get("skjemakode")
                            ?.asText() shouldBe forventedeVerdier.metrics?.get("skjemakode")
                        jsonNode["metrics"]?.get("skjemanavn")
                            ?.asText() shouldBe forventedeVerdier.metrics?.get("skjemanavn")
                    }
                }
            }
        }
    }

    @Test
    fun `v2 håndterer feil fra eksterne tjenester`() {

        testApplication {
            val applicationClient = createClient { configureJackson() }
            application {
                utkastApi(
                    utkastRepository = repository,
                    installAuthenticatorsFunction = {
                        authentication {
                            tokenXMock {
                                alwaysAuthenticated = true
                                setAsDefault = true
                                staticUserPid = testFnr1
                                staticLevelOfAssurance = LevelOfAssurance.LEVEL_4
                            }
                        }
                    },
                    utkastFetcher = UtkastFetcher(
                        digiSosBaseUrl = digisosTestHost,
                        httpClient = applicationClient,
                        digisosClientId = "dummyid",
                        tokendingsService = tokendingsMockk,
                        aapClientId = "dummyAAp"
                    )
                )
            }

            externalServices {
                hosts(digisosTestHost, aapTestHost) {
                    install(ExternalServicesDebug)
                    routing {
                        get("/dittnav/pabegynte/aktive") {
                            call.respond(HttpStatusCode.InternalServerError)
                        }
                    }
                    aapExternalRouting(
                        testUtkastData(
                            opprettet = LocalDateTime.now().plusHours(1),
                            id = "AAP"
                        )
                    )
                }
            }

            client.get("v2/utkast/antall").assert {
                status shouldBe HttpStatusCode.MultiStatus
                objectMapper.readTree(bodyAsText())["antall"].asInt() shouldBe 5
            }

            client.get("v2/utkast").assert {
                status.shouldBe(HttpStatusCode.MultiStatus)
                objectMapper.readTree(bodyAsText()).size() shouldBe 5
            }
        }
    }

    @Test
    fun `forsøker å hente tittel på ønsket språk`() = testApplication {
        application {
            utkastApi(
                utkastRepository = repository, installAuthenticatorsFunction = {
                    authentication {
                        tokenXMock {
                            alwaysAuthenticated = true
                            setAsDefault = true
                            staticUserPid = testFnr2
                            staticLevelOfAssurance = LevelOfAssurance.LEVEL_4
                        }
                    }
                }, utkastFetcher = mockk()
            )
        }

        client.get("/utkast").assert {
            status.shouldBe(HttpStatusCode.OK)
            objectMapper.readTree(bodyAsText()).assert {
                size() shouldBe 1
                get(0)["tittel"].textValue() shouldBe utkastForTestFnr2.tittel
            }
        }

        client.get("/utkast?la=en").assert {
            status.shouldBe(HttpStatusCode.OK)
            objectMapper.readTree(bodyAsText()).assert {
                size() shouldBe 1
                get(0)["tittel"].textValue() shouldBe utkastForTestFnr2.tittelI18n["en"]
            }
        }

        client.get("/utkast?la=nn").assert {
            status.shouldBe(HttpStatusCode.OK)
            objectMapper.readTree(bodyAsText()).assert {
                size() shouldBe 1
                get(0)["tittel"].textValue() shouldBe utkastForTestFnr2.tittelI18n["nn"]
            }
        }

        client.get("/utkast?la=se").assert {
            status.shouldBe(HttpStatusCode.OK)
            objectMapper.readTree(bodyAsText()).assert {
                size() shouldBe 1

                val tittel = get(0).get("tittel").textValue()

                tittel shouldBe utkastForTestFnr2.tittel
            }
        }
    }

    private fun UtkastData.toTestMessage(ident: String) = createUtkastTestPacket(
        utkastId = utkastId,
        ident = ident,
        link = link,
        tittel = tittel,
        tittelI18n = tittelI18n
    )

    private fun testUtkastData(
        tittelI18n: Map<String, String> = emptyMap(),
        opprettet: LocalDateTime = startTestTime,
        id: String = UUID.randomUUID().toString()
    ) =
        UtkastData(
            utkastId = id,
            tittel = "testTittel",
            tittelI18n = tittelI18n,
            link = "https://test.link",
            opprettet = opprettet,
            sistEndret = null,
            slettet = null
        )
}
