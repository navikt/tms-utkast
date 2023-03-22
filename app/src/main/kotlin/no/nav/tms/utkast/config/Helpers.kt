package no.nav.tms.utkast.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.isMissingOrNull
import java.time.LocalDateTime
import java.time.ZoneId

object LocalDateTimeHelper {
    fun nowAtUtc(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
}

object JsonMessageHelper {
    private val objectMapper = ObjectMapper()

    fun JsonMessage.keepFields(vararg fields: String): JsonNode {
        val objectNode = objectMapper.createObjectNode()

        fields.forEach { field ->
            get(field)
                .takeUnless { it.isMissingOrNull() }
                ?.let { objectNode.replace(field, it) }
        }

        return objectNode
    }
}

private val secureLog = KotlinLogging.logger("secureLog")
private val log = KotlinLogging.logger {}

fun withErrorLogging(function: ErrorContext.() -> Any) {
    val errorContext = ErrorContext()
    try {
        errorContext.function()
    } catch (exception: Exception) {
        log.error { errorContext.message }
        secureLog.error {
            """
            ${errorContext.message}\n
            ident: ${errorContext.ident}\n
            "origin": $exception
        """.trimIndent()
        }
    }
}

class ErrorContext() {
    var ident: String? = null
        get() = field ?: "Ukjent"
        set(value) {
            if (field == null)
                field = value
        }

    var message: String? = null
        get() = field ?: ""
        set(value) {
            if (field == null)
                field = value
        }

    var jsonMessage: JsonMessage? = null
        set(value) {
            field = value
            ident = value.ident()
            message = "Feil i utkast med id ${value.utkastId()}"
        }
}

private fun JsonMessage?.utkastId(): String =
    this?.let { packet ->
        packet["utkastId"].asText("Ukjent")
    } ?: "ukjent"


private fun JsonMessage?.ident(): String? =
    this?.let { packet ->
        packet["ident"].asText("Ukjent")
    } ?: "ukjent"

