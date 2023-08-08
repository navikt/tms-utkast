package no.nav.tms.utkast

import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.date.shouldNotBeAfter
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tms.utkast.database.UtkastRepository
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime

internal fun setupSinks(
    rapidsConnection: RapidsConnection,
    utkastRepository: UtkastRepository,
) {
    UtkastUpdatedSink(
        rapidsConnection = rapidsConnection,
        utkastRepository = utkastRepository
    )
    UtkastDeletedSink(
        rapidsConnection = rapidsConnection,
        utkastRepository = utkastRepository
    )
    UtkastCreatedSink(
        rapidsConnection = rapidsConnection,
        utkastRepository = utkastRepository
    )
}

@Language("JSON")
internal fun createUtkastTestPacket(
    utkastId: String,
    ident: String,
    tittel: String = "http://testlink",
    tittelI18n: Map<String, String>? = null,
    link: String = "http://testlink",
    metrics: Map<String, String>? = null
) = """
    {
     "@event_name": "created",
    "utkastId": "$utkastId",
    "ident": "$ident",
    "link": "$link",
    "tittel": "$tittel"
    ${if (tittelI18n != null) ",\"tittel_i18n\": ${tittelI18n.toJson()}" else ""}
    ${if (metrics != null) ",\"metrics\": ${metrics.toJson()}" else ""}
    }

""".trimIndent()

@Language("JSON")
internal fun updateUtkastTestPacket(
    utkastId: String,
    tittel: String? = null,
    link: String? = null,
    tittelI18n: Map<String, String>? = null,
    metrics: Map<String, String>? = null
) = """
    {
    "@event_name":"updated",
    "utkastId": "$utkastId"
    ${if (tittel != null) ",\"tittel\": \"$tittel\"" else ""}
    ${if (link != null) ",\"link\": \"$link\"" else ""}
    ${if (tittelI18n != null) ",\"tittel_i18n\": ${tittelI18n.toJson()}" else ""}
    ${if (metrics != null) ",\"metrics\": ${metrics.toJson()}" else ""}
    }
""".trimIndent()

@Language("JSON")
internal fun deleteUtkastTestPacket(utkastId: String) = """
    {
    "@event_name":"deleted",
    "utkastId": "$utkastId"
    }
""".trimIndent()

internal infix fun LocalDateTime?.shouldBeCaSameAs(expected: LocalDateTime?) {
    if (expected == null) {
        this shouldBe null
    } else {
        require(this != null)
        this shouldBeAfter expected.minusMinutes(2)
        this shouldNotBeAfter expected
    }
}

private fun Map<String, String>.toJson(): String = mapValues { (_, v) ->
    JsonPrimitive(v)
}.let { JsonObject(it) }
    .toString()

internal data class UtkastData(
    val utkastId: String,
    val tittel: String,
    val tittelI18n: Map<String, String>,
    val link: String,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime?,
    val slettet: LocalDateTime?,
    val metrics: Map<String,String>? = null
)
