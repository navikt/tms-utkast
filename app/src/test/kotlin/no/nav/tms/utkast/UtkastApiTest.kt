package no.nav.tms.utkast

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.asOptionalLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.token.support.authentication.installer.mock.installMockedAuthenticators
import no.nav.tms.token.support.tokenx.validation.mock.SecurityLevel
import no.nav.tms.utkast.config.LocalDateTimeHelper
import no.nav.tms.utkast.database.UtkastRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UtkastApiTest {
    private val objectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
    }

    private val utkastRepository = UtkastRepository(LocalPostgresDatabase.cleanDb())
    private val testRapid = TestRapid()
    private val testFnr1 = "19873569100"
    private val testFnr2 = "19873100100"
    private val startTestTime = LocalDateTimeHelper.nowAtUtc()

    private val utkastForTestFnr1 = mutableListOf(
        testUtkastData(),
        testUtkastData(),
        testUtkastData(),
        testUtkastData()
    )

    private val utkastForTestFnr2 = testUtkastData(tittelI18n = mapOf("en" to "English title", "nn" to "Nynorsk tittel"))


    @BeforeAll
    fun populate() {
        setupSinks(testRapid,utkastRepository)
        utkastForTestFnr1.forEach {
            testRapid.sendTestMessage(it.toTestMessage(testFnr1))
        }
        testRapid.sendTestMessage(utkastForTestFnr2.toTestMessage(testFnr2))
        testRapid.sendTestMessage(updateUtkastTestPacket(
            utkastForTestFnr1[0].utkastId,
            metrics = mapOf("skjemakode" to "skjemakode","skjemanavn" to "skjemanavn")
        ))
        utkastForTestFnr1[0] = utkastForTestFnr1[0].copy(sistEndret = LocalDateTimeHelper.nowAtUtc())
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = UUID.randomUUID().toString(), ident = "9988776655"))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = UUID.randomUUID().toString(), ident = "9988776655"))
        testRapid.sendTestMessage(createUtkastTestPacket(utkastId = UUID.randomUUID().toString(), ident = "9988776655"))
    }

    @Test
    fun `henter utkast for bruker med ident`() = testApplication {
        application { utkastApi(utkastRepository = utkastRepository, installAuthenticatorsFunction = {
            installMockedAuthenticators {
                installTokenXAuthMock {
                    alwaysAuthenticated = true
                    setAsDefault = true
                    staticUserPid = testFnr1
                    staticSecurityLevel = SecurityLevel.LEVEL_4
                }
            }
        } ) }

        client.get("/utkast/antall").assert{
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
                    jsonNode["link"].asText() shouldBe forventedeVerdier.link
                    jsonNode["opprettet"].asLocalDateTime() shouldNotBe null
                    jsonNode["sistEndret"].asOptionalLocalDateTime() shouldBeCaSameAs forventedeVerdier.sistEndret
                    jsonNode["metrics"]?.get("skjemakode")?.asText() shouldBe forventedeVerdier.metrics?.get("skjemakode")
                    jsonNode["metrics"]?.get("skjemanavn")?.asText() shouldBe forventedeVerdier.metrics?.get("skjemanavn")
                }
            }
        }
    }

    @Test
    fun `forsøker å hente tittel på ønsket språk`() = testApplication {
        application { utkastApi(utkastRepository = utkastRepository, installAuthenticatorsFunction = {
            installMockedAuthenticators {
                installTokenXAuthMock {
                    alwaysAuthenticated = true
                    setAsDefault = true
                    staticUserPid = testFnr2
                    staticSecurityLevel = SecurityLevel.LEVEL_4
                }
            }
        } ) }

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

    private fun testUtkastData(tittelI18n: Map<String, String> = emptyMap()) = UtkastData(
        utkastId = UUID.randomUUID().toString(),
        tittel = "testTittel",
        tittelI18n = tittelI18n,
        link = "https://test.link",
        opprettet = startTestTime,
        sistEndret = null,
        slettet = null
    )
}

