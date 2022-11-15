package no.nav.tms.utkast

import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.utkast.database.UtkastRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class UtkastCreatedSink(
    rapidsConnection: RapidsConnection,
    private val utkastRepository: UtkastRepository,
    private val rapidMetricsProbe: RapidMetricsProbe,
) :
    River.PacketListener {
    private val log: Logger = LoggerFactory.getLogger(UtkastCreatedSink::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "created") }
            validate { it.requireKey("link","tittel","ident", "eventId")}
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        utkastRepository.createUtkast(packet.toJson())
        runBlocking {
            rapidMetricsProbe.countUtkastReceived()
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error(problems.toString())
    }
}

