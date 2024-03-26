package no.nav.tms.utkast.sink

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.MessageException
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription
import no.nav.tms.utkast.builder.FieldValidationException
import no.nav.tms.utkast.builder.UtkastValidator
import no.nav.tms.utkast.sink.JsonMessageHelper.keepFields
import no.nav.tms.utkast.setup.UtkastMetricsReporter
import no.nav.tms.utkast.setup.withErrorLogging
import observability.traceUtkast

class UtkastUpdatedSubscriber(
    private val utkastRepository: UtkastRepository
) : Subscriber() {

    private val log = KotlinLogging.logger {}

    override fun subscribe() = Subscription.forEvent("updated")
        .withFields("utkastId")
        .withOptionalFields("link", "tittel", "tittel_i18n")

    override suspend fun receive(jsonMessage: JsonMessage) {
        val utkastId = jsonMessage["utkastId"].asText()
        traceUtkast(id = utkastId) {
            validateLink(jsonMessage)

            jsonMessage.getOrNull("tittel_i18n")
                ?.takeIf { !it.isEmpty }
                ?.toString()
                ?.let {
                    withErrorLogging {
                        utkastRepository.updateUtkastI18n(utkastId, it)
                    }
                }

            jsonMessage.keepFields("tittel", "link")
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

    private fun validateLink(jsonMessage: JsonMessage) = try {
        jsonMessage.getOrNull("link")
            ?.textValue()
            ?.let { UtkastValidator.validateLink(it) }
    } catch (e: FieldValidationException) {
        throw MessageException(e.message ?: "ukjent valideringsfeil")
    }
}
