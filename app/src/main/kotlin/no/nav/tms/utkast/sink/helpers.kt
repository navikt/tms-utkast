package no.nav.tms.utkast.sink

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.tms.kafka.application.JsonMessage
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object LocalDateTimeHelper {
    fun nowAtUtc(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
}

object ZonedDateTimeHelper {
    fun nowAtUtc(): ZonedDateTime = ZonedDateTime.now(ZoneId.of("Z")).truncatedTo(ChronoUnit.MILLIS)
}

object JsonMessageHelper {
    private val objectMapper = ObjectMapper()

    fun JsonMessage.keepFields(vararg fields: String): JsonNode {
        val objectNode = objectMapper.createObjectNode()

        fields.forEach { field ->
            getOrNull(field)
                ?.let { objectNode.replace(field, it) }
        }

        return objectNode
    }

    fun JsonMessage.withoutFields(vararg fields: String): JsonNode {
        val objectNode = objectMapper.createObjectNode()

        json.fieldNames().forEach { field ->
            if (!fields.contains(field)) {
                objectNode.replace(field, get(field))
            }
        }

        return objectNode
    }
}
