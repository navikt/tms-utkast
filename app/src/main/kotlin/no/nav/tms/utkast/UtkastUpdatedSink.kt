package no.nav.tms.utkast

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.utkast.config.JsonMessageHelper.keepFields
import no.nav.tms.utkast.database.UtkastRepository

class UtkastUpdatedSink(
    rapidsConnection: RapidsConnection,
    private val utkastRepository: UtkastRepository,
    private val rapidMetricsProbe: RapidMetricsProbe
) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "updated") }
            validate { it.requireKey("eventId") }
            validate { it.interestedIn("tittel", "link") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {


        utkastRepository.updateUtkast(
            eventId = packet["eventId"].asText(),
            update = packet.keepFields("tittel", "link").toString()
        )

        rapidMetricsProbe.countUtkastChanged("updated")
    }
}


