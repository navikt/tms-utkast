package no.nav.tms.utkast.producer

import no.nav.tms.utkast.producer.PacketBuilder.UtkastOperations.CREATED
import no.nav.tms.utkast.producer.PacketBuilder.UtkastOperations.DELETED
import no.nav.tms.utkast.producer.PacketBuilder.UtkastOperations.UPDATED
import java.util.UUID

object PacketBuilder {

    enum class UtkastOperations(val eventName: String) {
        CREATED("created"), UPDATED("updated"), DELETED("deleted")
    }

    fun created(eventId: UUID, tittel: String, link: String, ident: String) = mapOf(
        "@event_name" to CREATED.eventName,
        "tittel" to tittel,
        "link" to link,
        "ident" to ident,
        "eventId" to eventId.toString()
    )

    fun updated(eventId: UUID, tittel: String? = null, link: String? = null) = mutableMapOf(
        "@event_name" to UPDATED.eventName,
        "eventId" to eventId.toString()
    ).apply {
        if (!tittel.isNullOrEmpty()) { this["tittel"] = tittel }
        if(!link.isNullOrEmpty()) this["link"] = link
    }

    fun deleted(eventId: UUID) = mutableMapOf(
        "@event_name" to DELETED.eventName,
        "eventId" to eventId.toString()
    )
}
