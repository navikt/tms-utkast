package no.nav.tms.utkast.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.tms.utkast.builder.FieldValidationException
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
                .takeUnless { it.isMissingOrNull()}
                ?.let { objectNode.replace(field, it) }
        }

        return objectNode
    }
}

object JsonNodeHelper {
    fun JsonNode.checkForProblems(fieldName: String, validator: (String) -> Unit): String? {
        return try {
            val field = get(fieldName)
            when {
                field == null -> {}
                field.isTextual -> field.checkForProblems(validator)
                field.isObject -> field.elements().forEach { it.checkForProblems(validator) }
                field.isArray -> field.elements().forEach { it.checkForProblems(validator) }
                else -> throw IllegalArgumentException()
            }
            null
        } catch (ie: IllegalArgumentException) {
            "Felt $fieldName må være en String eller et objekt med kun String-verdier"
        } catch (fe: FieldValidationException) {
            fe.message
        }
    }

    private fun JsonNode.checkForProblems(validator: (String) -> Unit) {
        if (isTextual) {
            validator(textValue())
        } else {
            throw IllegalArgumentException()
        }
    }
}
