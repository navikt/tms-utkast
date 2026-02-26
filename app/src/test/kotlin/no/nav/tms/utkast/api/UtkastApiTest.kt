package no.nav.tms.utkast.api

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.get
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import kotliquery.queryOf
import no.nav.tms.kafka.application.MessageBroadcaster
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.token.support.tokenx.validation.mock.LevelOfAssurance
import no.nav.tms.token.support.tokenx.validation.mock.tokenXMock
import no.nav.tms.utkast.*
import no.nav.tms.utkast.UtkastData
import no.nav.tms.utkast.createUtkastTestPacket
import no.nav.tms.utkast.database.LocalPostgresDatabase
import no.nav.tms.utkast.setup.configureClient
import no.nav.tms.utkast.setupBroadcaster
import no.nav.tms.utkast.sink.LocalDateTimeHelper
import no.nav.tms.utkast.sink.UtkastRepository
import no.nav.tms.utkast.sink.ZonedDateTimeHelper
import no.nav.tms.utkast.sink.utkastIdParam
import no.nav.tms.utkast.updateUtkastTestPacket
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UtkastApiTest {
    private val objectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
    }

    private val database = LocalPostgresDatabase.getCleanInstance()
    private val repository = UtkastApiRepository(LocalPostgresDatabase.getCleanInstance())
    private val testFnr1 = "12345600001"
    private val testFnr2 = "12345600022"
    private val testFnr3 = "12345600333"
    private val testFnr4 = "12345604444"
    private val startTestTime = LocalDateTimeHelper.nowAtUtc()
    private val externalServiceHost = "http://externalhost.test"
    private val tokendingsMockk = mockk<TokendingsService>().also {
        coEvery { it.exchangeToken(any(), any()) } returns "<dummytoken>"
    }

    private lateinit var broadcaster: MessageBroadcaster

    val utkastForTestFnr1 = mutableListOf(
        testUtkastData(),
        testUtkastData(),
        testUtkastData(),
        testUtkastData()
    )

    private val utkastForTestFnr2 =
        testUtkastData(tittelI18n = mapOf("en" to "English title", "nn" to "Nynorsk tittel"))

    private val slettesEtter = ZonedDateTimeHelper.nowAtUtc().plusDays(7)
    private val utkastForTestFnr3 =
        testUtkastData(slettesEtter = slettesEtter)

    private val utkastForTestFnr4 = listOf(
        testUtkastData(),
        testUtkastData()
    )

    private val digisosErrorRoute = HttpRouteConfig(
        path = "/dittnav/pabegynte/aktive",
        statusCode = HttpStatusCode.InternalServerError
    )

    @BeforeAll
    fun populate() {
        broadcaster = setupBroadcaster(UtkastRepository(database))
        utkastForTestFnr1.forEach {
            broadcaster.broadcastJson(it.toTestMessage(testFnr1))
        }
        broadcaster.broadcastJson(utkastForTestFnr2.toTestMessage(testFnr2))
        broadcaster.broadcastJson(
            updateUtkastTestPacket(
                utkastForTestFnr1[0].utkastId,
                metrics = mapOf("skjemakode" to "skjemakode", "skjemanavn" to "skjemanavn")
            )
        )
        utkastForTestFnr4.forEach {
            broadcaster.broadcastJson(it.toTestMessage(testFnr4))
        }
        utkastForTestFnr1[0] = utkastForTestFnr1[0].copy(sistEndret = LocalDateTimeHelper.nowAtUtc())
        broadcaster.broadcastJson(utkastForTestFnr3.toTestMessage(testFnr3))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = UUID.randomUUID().toString(), ident = "9988776655"))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = UUID.randomUUID().toString(), ident = "9988776655"))
        broadcaster.broadcastJson(createUtkastTestPacket(utkastId = UUID.randomUUID().toString(), ident = "9988776655"))
    }

    @Test
    fun `henter utkast for bruker med ident`() = utkastTestApplication(testFnr1) {

        initExternalServices(externalServiceHost, digisosRouteConfig(), aapRouteConfig())

        client.get("v2/utkast/antall").run {
            status shouldBe HttpStatusCode.OK
            objectMapper.readTree(bodyAsText())["antall"].asInt() shouldBe 4
        }

        client.get("v2/utkast").run {
            status.shouldBe(HttpStatusCode.OK)
            objectMapper.readTree(bodyAsText()).run {
                size() shouldBe 4
                forEach { jsonNode ->
                    val utkastId = jsonNode["utkastId"].asText()
                    val forventedeVerdier =
                        utkastForTestFnr1.find { it.utkastId == utkastId }
                            ?: throw AssertionError("Fant utkast som ikke tilhører ident, utkastId : $utkastId")
                    jsonNode["tittel"].asText() shouldBe forventedeVerdier.tittel
                    jsonNode["link"].asText() shouldBe forventedeVerdier.link
                    jsonNode["opprettet"].asLocalDateTime() shouldNotBe null
                    jsonNode["sistEndret"]?.asLocalDateTime() shouldBeCaSameAs forventedeVerdier.sistEndret
                    jsonNode["slettesEtter"].isNull shouldBe true
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

        utkastTestApplication(testFnr1) {
            initExternalServices(
                externalServiceHost,
                digisosRouteConfig(listOf(digisosUtkast)),
                aapRouteConfig(aapUtkast)
            )

            client.get("v2/utkast/antall").run {
                status shouldBe HttpStatusCode.OK
                objectMapper.readTree(bodyAsText())["antall"].asInt() shouldBe 6
            }

            client.get("v2/utkast").run {
                status.shouldBe(HttpStatusCode.OK)
                objectMapper.readTree(bodyAsText()).run {
                    map {
                        it["sistEndret"]?.asLocalDateTime()?.toLocalDate() ?: it["opprettet"].asLocalDateTime()
                            ?.toLocalDate()
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
                        jsonNode["slettesEtter"].asText()
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

        utkastTestApplication(testFnr1) {
            initExternalServices(
                externalServiceHost,
                digisosRouteConfig(),
                aapRouteConfig()
            )

            client.get("v2/utkast/antall").run {
                status shouldBe HttpStatusCode.OK
                objectMapper.readTree(bodyAsText())["antall"].asInt() shouldBe 4
            }

            client.get("v2/utkast").run {
                status.shouldBe(HttpStatusCode.OK)
                objectMapper.readTree(bodyAsText()).run {
                    size() shouldBe 4
                    forEach { jsonNode ->
                        val utkastId = jsonNode["utkastId"].asText()
                        val forventedeVerdier =
                            utkastForTestFnr1.find { it.utkastId == utkastId }
                                ?: throw AssertionError("Fant utkast som ikke tilhører ident, utkastId : $utkastId")
                        jsonNode["tittel"].asText() shouldBe forventedeVerdier.tittel
                        jsonNode["link"].asText() shouldBe forventedeVerdier.link
                        jsonNode["opprettet"].asLocalDateTime() shouldNotBe null
                        jsonNode["sistEndret"]?.asLocalDateTime() shouldBeCaSameAs forventedeVerdier.sistEndret
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
        utkastTestApplication(testFnr1) {
            initExternalServices(
                externalServiceHost,
                digisosErrorRoute,
                aapRouteConfig(   testUtkastData(
                    opprettet = LocalDateTime.now().plusHours(1),
                    id = "AAP"
                ))
            )

            client.get("v2/utkast/antall").run {
                status shouldBe HttpStatusCode.MultiStatus
                objectMapper.readTree(bodyAsText())["antall"].asInt() shouldBe 5
            }

            client.get("v2/utkast").run {
                status.shouldBe(HttpStatusCode.MultiStatus)
                objectMapper.readTree(bodyAsText()).size() shouldBe 5
            }
        }
    }

    @Test
    fun `forsøker å hente tittel på ønsket språk`() = utkastTestApplication(testFnr2) {

        initExternalServices(externalServiceHost, digisosRouteConfig(), aapRouteConfig())

        client.get("v2/utkast").run {
            status.shouldBe(HttpStatusCode.OK)
            objectMapper.readTree(bodyAsText()).run {
                size() shouldBe 1
                get(0)["tittel"].textValue() shouldBe utkastForTestFnr2.tittel
            }
        }

        client.get("v2/utkast?la=en").run {
            status.shouldBe(HttpStatusCode.OK)
            objectMapper.readTree(bodyAsText()).run {
                size() shouldBe 1
                get(0)["tittel"].textValue() shouldBe utkastForTestFnr2.tittelI18n["en"]
            }
        }

        client.get("v2/utkast?la=nn").run {
            status.shouldBe(HttpStatusCode.OK)
            objectMapper.readTree(bodyAsText()).run {
                size() shouldBe 1
                get(0)["tittel"].textValue() shouldBe utkastForTestFnr2.tittelI18n["nn"]
            }
        }

        client.get("v2/utkast?la=se").run {
            status.shouldBe(HttpStatusCode.OK)
            objectMapper.readTree(bodyAsText()).run {
                size() shouldBe 1

                val tittel = get(0).get("tittel").textValue()

                tittel shouldBe utkastForTestFnr2.tittel
            }
        }
    }

    @Test
    fun `leverer info om når utkast slettes automatisk`() = utkastTestApplication(testFnr3) {
        initExternalServices(externalServiceHost, digisosRouteConfig(), aapRouteConfig())

        client.get("v2/utkast").run {
            status.shouldBe(HttpStatusCode.OK)
            objectMapper.readTree(bodyAsText()).run {
                size() shouldBe 1
                first().let { jsonNode ->
                    val slettesEtterResponse = jsonNode["slettesEtter"].asText().let(ZonedDateTime::parse)

                    slettesEtterResponse.toEpochSecond() shouldBe slettesEtter.toEpochSecond()
                }
            }
        }
    }

    @Test
    fun `viser ikke gamle utkast som er markert slettet, men fortsatt ligger i basen`() = utkastTestApplication(testFnr4) {
        initExternalServices(externalServiceHost, digisosRouteConfig(), aapRouteConfig())

        val response = client.get("v2/utkast").run {
            status.shouldBe(HttpStatusCode.OK)
            objectMapper.readTree(bodyAsText())
        }

        response.size() shouldBe 2

        val markeresSlettet = utkastForTestFnr4.first().utkastId

        database.update {
            queryOf(
                "update utkast set slettet = now() where packet @> :utkastId",
                mapOf("utkastId" to utkastIdParam(markeresSlettet))
            )
        }

        val responseAfterSlettet = client.get("v2/utkast").run {
            status.shouldBe(HttpStatusCode.OK)
            objectMapper.readTree(bodyAsText())
        }

        responseAfterSlettet.size() shouldBe 1

        responseAfterSlettet.map { it["utkastId"].asText() } shouldNotContain markeresSlettet
    }

    private fun UtkastData.toTestMessage(ident: String) = createUtkastTestPacket(
        utkastId = utkastId,
        ident = ident,
        link = link,
        tittel = tittel,
        tittelI18n = tittelI18n,
        slettesEtter = slettesEtter
    )

    private fun testUtkastData(
        tittelI18n: Map<String, String> = emptyMap(),
        opprettet: LocalDateTime = startTestTime,
        id: String = UUID.randomUUID().toString(),
        slettesEtter: ZonedDateTime? = null
    ) =
        UtkastData(
            utkastId = id,
            tittel = "testTittel",
            tittelI18n = tittelI18n,
            link = "https://test.link",
            opprettet = opprettet,
            sistEndret = null,
            slettesEtter = slettesEtter,
        )

    private fun ApplicationTestBuilder.utkastFetcher() =
        UtkastFetcher(
            aapBaseUrl = externalServiceHost,
            digiSosBaseUrl = externalServiceHost,
            httpClient = createClient {
                configureClient()
            },
            digisosClientId = "dummyid",
            tokendingsService = tokendingsMockk,
            aapClientId = "dummyAAp"
        )

    private fun utkastTestApplication(testfnr: String, config: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                utkastApi(
                    utkastRepository = repository,
                    installAuthenticatorsFunction = {
                        authentication {
                            tokenXMock {
                                alwaysAuthenticated = true
                                setAsDefault = true
                                staticUserPid = testfnr
                                staticLevelOfAssurance = LevelOfAssurance.LEVEL_4
                            }
                        }
                    },
                    utkastFetcher = this@testApplication.utkastFetcher()
                )
            }
            config()
        }
}

fun digisosRouteConfig(expextedUtkastData: List<UtkastData> = emptyList()) = HttpRouteConfig(
    path = "/dittnav/pabegynte/aktive",
    responseContent = expextedUtkastData.joinToString(
        prefix = "[",
        postfix = "]",
        separator = ","
    ) { it.toDigisosResponse() }
)

fun aapRouteConfig(expextedUtkastData: UtkastData? = null) = if (expextedUtkastData != null) {
    HttpRouteConfig(
        path = "/mellomlagring/søknad/finnes",
        responseContent = expextedUtkastData.toAapResponse()
    )
} else {
    HttpRouteConfig(
        path = "/mellomlagring/søknad/finnes",
        statusCode = HttpStatusCode.NoContent,
        responseContent = ""
    )
}

