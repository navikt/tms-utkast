package no.nav.tms.utkast

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.date.shouldNotBeAfter
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import no.nav.tms.kafka.application.MessageBroadcaster
import no.nav.tms.kafka.application.isMissingOrNull
import no.nav.tms.utkast.sink.UtkastRepository
import no.nav.tms.utkast.sink.UtkastCreatedSubscriber
import no.nav.tms.utkast.sink.UtkastDeletedSubscriber
import no.nav.tms.utkast.sink.UtkastUpdatedSubscriber
import org.intellij.lang.annotations.Language
import org.slf4j.MDC
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

internal fun setupBroadcaster(
    utkastRepository: UtkastRepository,
) = MessageBroadcaster(
    listOf(
        UtkastUpdatedSubscriber(utkastRepository),
        UtkastDeletedSubscriber(utkastRepository),
        UtkastCreatedSubscriber(utkastRepository)
    )
)

fun JsonNode.asLocalDateTime() = if (this.isMissingOrNull()) {
    null
} else {
    LocalDateTime.parse(this.asText())
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
    val metrics: Map<String, String>? = null
) {
    @Language("JSON")
    fun toDigisosResponse() =
        """
        {
        "eventTidspunkt" : "$opprettet",
        "eventId":"$utkastId",
        "grupperingsId":"tadda",
        "sikkerhetsniva": "3",
        "link": "$link",
        "tekst": "$tittel",
        "sistOppdatert": null,
        "isAktiv": true
        }
    """.trimIndent()

    @Language("JSON")
    fun toAapResponse() =
        """
        {
        "tittel": "$tittel",
        "link": "$link",
        "sistEndret": "$opprettet"
        }
    """.trimIndent()
}

internal fun testUtkastData(tittelI18n: Map<String, String> = emptyMap(), startTestTime: LocalDateTime) = UtkastData(
    utkastId = UUID.randomUUID().toString(),
    tittel = "testTittel ${Random.nextInt(0, 10)}",
    tittelI18n = tittelI18n,
    link = "https://test.link",
    opprettet = startTestTime,
    sistEndret = null,
    slettet = null
)
