package no.nav.tms.utkast

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.utkast.config.JsonMessageHelper.keepFields
import no.nav.tms.utkast.database.UtkastRepository


class UtkastCreatedSink(
    rapidsConnection: RapidsConnection,
    private val utkastRepository: UtkastRepository,
    private val rapidMetricsProbe: RapidMetricsProbe,
) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "created") }
            validate { it.requireKey("link", "eventId", "tittel", "ident")}
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {

        packet.keepFields("eventId", "ident", "link", "tittel")
            .toString()
            .let { utkastRepository.createUtkast(it) }

        rapidMetricsProbe.countUtkastReceived()
    }
}

