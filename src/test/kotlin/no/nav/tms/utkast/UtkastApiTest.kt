package no.nav.tms.utkast

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.utkast.config.LocalDateTimeHelper
import no.nav.tms.utkast.database.Utkast
import no.nav.tms.utkast.database.UtkastRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.UUID

class UtkastApiTest {
    private val objectMapper = jacksonObjectMapper()

    private val utkastRepository = UtkastRepository(LocalPostgresDatabase.cleanDb())
    private val testRapid = TestRapid()
    private val metricksMock = mockk<RapidMetricsProbe>(relaxed = true)
    private val testFnr = "19873569100"
    private val startTestTime = LocalDateTimeHelper.nowAtUtc()

    private val utkastForTestFnr = listOf(
        testUtkast(),
        testUtkast(),
        testUtkast(),
        testUtkast()
    )


    @BeforeAll
    fun populate() {
        setupSinks()
        utkastForTestFnr.forEach {
            testRapid.sendTestMessage(it.toTestMessage())
        }
        testRapid.sendTestMessage(updateUtkastTestPacket(utkastForTestFnr[0].eventId))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = UUID.randomUUID().toString(), ident = "9988776655"))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = UUID.randomUUID().toString(), ident = "9988776655"))
        testRapid.sendTestMessage(createUtkastTestPacket(eventId = UUID.randomUUID().toString(), ident = "9988776655"))
    }

    @Test
    fun `henter alle utkast for bruker med ident`() {
        testApplication {
            application { utkastApi(utkastRepository) }
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
                        jsonNode["opprettet"].asLocalDateTime() shouldBeCaSameAs startTestTime
                    }
                }
            }
        }
    }

    private fun setupSinks() {
        UtkastUpdatedSink(
            rapidsConnection = testRapid,
            utkastRepository = utkastRepository,
            rapidMetricsProbe = metricksMock
        )
        UtkastDeletedSink(
            rapidsConnection = testRapid,
            utkastRepository = utkastRepository,
            rapidMetricsProbe = metricksMock
        )
        UtkastCreatedSink(
            rapidsConnection = testRapid,
            utkastRepository = utkastRepository,
            rapidMetricsProbe = metricksMock
        )
    }

    private fun Utkast.toTestMessage(ident: String = testFnr) = createUtkastTestPacket(
        eventId = eventId,
        ident = ident,
        link = link,
        tittel = tittel
    )

    private fun testUtkast() = Utkast(
        eventId = UUID.randomUUID().toString(),
        tittel = "Test tittel",
        link = "https://test.link",
        opprettet = startTestTime,
        sistEndret = null,
    )
}