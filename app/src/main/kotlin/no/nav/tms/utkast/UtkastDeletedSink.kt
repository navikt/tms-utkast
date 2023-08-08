package no.nav.tms.utkast

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.utkast.config.withErrorLogging
import no.nav.tms.utkast.database.UtkastRepository

class UtkastDeletedSink(
    rapidsConnection: RapidsConnection,
    private val utkastRepository: UtkastRepository
) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "deleted") }
            validate { it.requireKey("utkastId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withErrorLogging {
            message = "Feil ved sletting av utkast med id ${packet["utkastId"].asText()}"
            utkastRepository.deleteUtkast(packet["utkastId"].asText())
        }

        UtkastMetricsReporter.countUtkastSlettet()
    }
}


