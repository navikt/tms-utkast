package no.nav.tms.utkast

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
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
        val eventId = packet["eventId"].asText()

        val update = packet.withFields("tittel", "link")

        utkastRepository.updateUtkast(eventId, update)
        rapidMetricsProbe.countUtkastChanged("updated")
    }

    private val objectMapper = ObjectMapper()

    private fun JsonMessage.withFields(vararg fields: String): JsonNode {
        val objectNode = objectMapper.createObjectNode()

        fields.forEach { field ->
            get(field)
                .takeUnless { it.isNull }
                ?.let { objectNode.replace(field, it) }
        }

        return objectNode
    }
}


