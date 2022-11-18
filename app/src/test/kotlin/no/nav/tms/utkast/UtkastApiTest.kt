package no.nav.tms.utkast

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.asOptionalLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.token.support.authentication.installer.mock.installMockedAuthenticators
import no.nav.tms.token.support.tokenx.validation.mock.SecurityLevel
import no.nav.tms.utkast.config.LocalDateTimeHelper
import no.nav.tms.utkast.database.UtkastRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.UUID

class UtkastApiTest {
    private val objectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
    }

    private val utkastRepository = UtkastRepository(LocalPostgresDatabase.cleanDb())
    private val testRapid = TestRapid()
    private val testFnr = "19873569100"
    private val startTestTime = LocalDateTimeHelper.nowAtUtc()

    private val utkastForTestFnr = mutableListOf(
        testUtkastData(),
        testUtkastData(),
        testUtkastData(),
        testUtkastData()
    )


    @BeforeAll
    fun populate() {
        setupSinks(testRapid,utkastRepository)
        utkastForTestFnr.forEach {
            testRapid.sendTestMessage(it.toTestMessage())
        }
        testRapid.sendTestMessage(updateUtkastTestPacket(utkastForTestFnr[0].eventId))
        utkastForTestFnr[0] = utkastForTestFnr[0].copy(sistEndret = LocalDateTimeHelper.nowAtUtc())
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = UUID.randomUUID().toString(), ident = "9988776655"))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = UUID.randomUUID().toString(), ident = "9988776655"))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = UUID.randomUUID().toString(), ident = "9988776655"))
    }

    @Test
    fun `henter utkast for bruker med ident`() {
        testApplication {
            application { utkastApi(utkastRepository = utkastRepository, installAuthenticatorsFunction = {
                installMockedAuthenticators {
                    installTokenXAuthMock {
                        alwaysAuthenticated = true
                        setAsDefault = true
                        staticUserPid = testFnr
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
                        val eventId = jsonNode["eventId"].asText()
                        val forventedeVerdier =
                            utkastForTestFnr.find { it.eventId == eventId }
                                ?: throw AssertionError("Fant utkast som ikke tilhører ident, eventid : $eventId")
                        jsonNode["link"].asText() shouldBe forventedeVerdier.link
                        jsonNode["opprettet"].asLocalDateTime() shouldNotBe null
                        jsonNode["sistEndret"].asOptionalLocalDateTime() shouldBeCaSameAs forventedeVerdier.sistEndret
                    }
                }
            }
        }
    }

    private fun UtkastData.toTestMessage(ident: String = testFnr) = createUtkastTestPacket(
        eventId = eventId,
        ident = ident,
        link = link,
        tittel = tittel
    )

    private fun testUtkastData() = UtkastData(
        eventId = UUID.randomUUID().toString(),
        tittel = "Test tittel",
        link = "https://test.link",
        opprettet = startTestTime,
        sistEndret = null,
        slettet = null
    )
}