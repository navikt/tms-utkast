package no.nav.tms.utkast.sink

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.utkast.setup.UtkastMetricsReporter
import no.nav.tms.utkast.setup.withErrorLogging
import observability.traceUtkast

class UtkastDeletedSink(
    rapidsConnection: RapidsConnection,
    private val utkastRepository: UtkastSinkRepository
) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "deleted") }
            validate { it.requireKey("utkastId") }
        }.register(this)
    }

    private val log = KotlinLogging.logger { }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        traceUtkast(id = packet["utkastId"].asText()) {
            log.info { "Utkast deleted" }
            withErrorLogging {
                message = "Feil ved sletting av utkast med id ${packet["utkastId"].asText()}"
                utkastRepository.deleteUtkast(packet["utkastId"].asText())
            }

            UtkastMetricsReporter.countUtkastSlettet()
        }
    }
}
