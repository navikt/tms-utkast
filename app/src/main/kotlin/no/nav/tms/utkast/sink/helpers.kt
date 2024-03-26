package no.nav.tms.utkast.sink

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.tms.kafka.application.JsonMessage
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
            getOrNull(field)
                ?.let { objectNode.replace(field, it) }
        }

        return objectNode
    }
}
