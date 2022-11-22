package no.nav.tms.utkast.builder

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.tms.utkast.builder.UtkastValidator.validateUtkastId
import no.nav.tms.utkast.builder.UtkastValidator.validateIdent
import no.nav.tms.utkast.builder.UtkastValidator.validateLink
import no.nav.tms.utkast.builder.UtkastValidator.validateTittel


@Serializable
class UtkastJsonBuilder internal constructor() {
    private var utkastId: String? = null
    private var ident: String? = null
    private var tittel: String? = null
    private var link: String? = null
    @SerialName("@event_name") private var eventName: EventName? = null
    @SerialName("@origin") private var origin: String = this::class.qualifiedName!!

    companion object {
        fun newBuilder() = UtkastJsonBuilder()
    }

    fun withUtkastId(utkastId: String) = apply {
        this.utkastId = validateUtkastId(utkastId)
    }

    fun withIdent(ident: String) = apply {
        this.ident = validateIdent(ident)
    }

    fun withTittel(tittel: String) = apply {
        this.tittel = validateTittel(tittel)
    }

    fun withLink(link: String) = apply {
        this.link = validateLink(link)
    }

    fun create(): String {
        requireNotNull(utkastId)
        requireNotNull(ident)
        requireNotNull(tittel)
        requireNotNull(link)

        this.eventName = EventName.created

        return Json.encodeToString(this)
    }

    fun update(): String {
        requireNotNull(utkastId)

        this.eventName = EventName.updated

        return Json.encodeToString(this)
    }

    fun delete(): String {
        requireNotNull(utkastId)

        this.eventName = EventName.deleted

        return Json.encodeToString(this)
    }
}
