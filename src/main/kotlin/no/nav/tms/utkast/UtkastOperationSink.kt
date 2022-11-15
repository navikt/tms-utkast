package no.nav.tms.utkast

import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.utkast.database.UtkastRepository

class UtkastOperationSink(
    rapidsConnection: RapidsConnection,
    private val utkastRepository: UtkastRepository,
    private val rapidMetricsProbe: RapidMetricsProbe,
    private val operationName: String,
    private val operation: UtkastRepository.(String)->Unit
) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", operationName) }
            validate { it.requireKey("eventId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        utkastRepository.operation(packet["eventId"].asText())
        runBlocking {
            rapidMetricsProbe.countUtkastChanged(operationName)
        }
    }
}


