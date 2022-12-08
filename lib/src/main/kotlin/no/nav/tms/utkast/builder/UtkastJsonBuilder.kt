package no.nav.tms.utkast.builder

import kotlinx.serialization.json.*
import no.nav.tms.utkast.builder.UtkastValidator.validateUtkastId
import no.nav.tms.utkast.builder.UtkastValidator.validateIdent
import no.nav.tms.utkast.builder.UtkastValidator.validateLink
import java.util.*

//Dummy chang
class UtkastJsonBuilder {
    private var utkastId: String? = null
    private var ident: String? = null
    private var link: String? = null
    private var eventName: EventName? = null
    private var origin: String = this::class.qualifiedName!!
    private var defaultTittel: String? = null
    private val tittelByLanguage = mutableMapOf<String, String>()

    fun withUtkastId(utkastId: String) = apply {
        this.utkastId = validateUtkastId(utkastId)
    }

    fun withIdent(ident: String) = apply {
        this.ident = validateIdent(ident)
    }

    fun withTittel(tittel: String) = apply {
        this.defaultTittel = tittel
    }

    fun withTittelI18n(tittel: String, locale: Locale) = apply {
        tittelByLanguage[locale.language] = tittel
    }

    fun withLink(link: String) = apply {
        this.link = validateLink(link)
    }

    fun create(): String {
        requireNotNull(utkastId)
        requireNotNull(ident)
        requireNotNull(link)
        requireNotNull(defaultTittel)

        this.eventName = EventName.created

        return serializeToJson()
    }

    fun update(): String {
        requireNotNull(utkastId)

        this.eventName = EventName.updated

        return serializeToJson()
    }

    fun delete(): String {
        requireNotNull(utkastId)

        this.eventName = EventName.deleted

        return serializeToJson()
    }

    private fun serializeToJson(): String {

        val tittelObject = tittelByLanguage.toJsonObject()

        val fields: MutableMap<String, Any?> = mutableMapOf(
            "utkastId" to utkastId,
            "ident" to ident,
            "tittel" to defaultTittel,
            "tittel_i18n" to tittelObject,
            "link" to link,
            "@event_name" to eventName?.name,
            "@origin" to origin
        )

        return fields.toJsonObject().toString()
    }

    private fun Map<String, Any?>.toJsonObject(): JsonObject? {
        return filterValues { it != null }
            .takeIf { it.isNotEmpty() }
            ?.mapValues { (_, v) ->
                when (v) {
                    is String -> JsonPrimitive(v)
                    is JsonObject -> v
                    else -> JsonPrimitive(v.toString())
                }
            }?.let { JsonObject(it) }
    }
}
