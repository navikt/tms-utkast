package no.nav.tms.utkast.sink

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.reader.JsonMessage
import no.nav.tms.kafka.reader.Subscriber
import no.nav.tms.kafka.reader.Subscription
import no.nav.tms.utkast.builder.FieldValidationException
import no.nav.tms.utkast.builder.UtkastValidator
import no.nav.tms.utkast.sink.JsonMessageHelper.keepFields
import no.nav.tms.utkast.setup.UtkastMetricsReporter
import no.nav.tms.utkast.setup.withErrorLogging
import observability.traceUtkast

class UtkastUpdatedListener(
    private val utkastRepository: UtkastSinkRepository
) : Subscriber() {

    private val log = KotlinLogging.logger {}

    override fun subscribe() = Subscription.forEvent("updated")
        .withFields("utkastId")
        .withOptionalFields("link", "tittel", "tittel_i18n")

    override suspend fun receive(jsonMessage: JsonMessage) {
        val utkastId = jsonMessage["utkastId"].asText()

        traceUtkast(id = utkastId) {

            if (!validate(jsonMessage)) {
                return@traceUtkast
            }

            jsonMessage.getOrNull("tittel_i18n")
                ?.takeIf { !it.isEmpty }
                ?.toString()
                ?.let {
                    withErrorLogging {
                        utkastRepository.updateUtkastI18n(utkastId, it)
                    }
                }

            jsonMessage.json.keepFields("tittel", "link")
                .toString()
                .let {
                    withErrorLogging {
                        utkastRepository.updateUtkast(utkastId, it)
                    }
                }

            log.info { "utkast updated" }

            UtkastMetricsReporter.countUtkastEndret()
        }
    }

    private fun validate(jsonMessage: JsonMessage): Boolean {
        return try {
            UtkastValidator.validateLink(jsonMessage["link"].textValue())
            true
        } catch (e: FieldValidationException) {
            log.info { "Skipping invalid event" }
            false
        }
    }
}
