package no.nav.tms.utkast.producer

import no.nav.tms.utkast.producer.PacketBuilder.UtkastOperations.CREATED
import java.util.UUID

object PacketBuilder {

    enum class UtkastOperations(val eventName: String) {
        CREATED("created"), UPDATED("updated"), DELETED("deleted")
    }

    fun created(eventId: UUID, tittel: String, link: String, ident: String) = mapOf<String, String>(
        "@event_name" to CREATED.eventName,
        "tittel" to tittel,
        "link" to link,
        "ident" to ident,
        "eventId" to eventId.toString()
    )
}