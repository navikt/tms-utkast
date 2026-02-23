package no.nav.tms.utkast.sink

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.MessageException
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription
import no.nav.tms.utkast.builder.FieldValidationException
import no.nav.tms.utkast.builder.UtkastValidator
import no.nav.tms.utkast.setup.UtkastMetricsReporter
import no.nav.tms.utkast.setup.withErrorLogging
import no.nav.tms.common.observability.traceUtkast
import java.time.ZonedDateTime

class UtkastCreatedSubscriber(
    private val utkastRepository: UtkastRepository
) : Subscriber() {

    private val log = KotlinLogging.logger {}

    override fun subscribe() = Subscription.forEvent("created")
        .withFields("utkastId", "ident", "link", "tittel")
        .withOptionalFields("tittel_i18n", "metrics", "slettesEtter")

    override suspend fun receive(jsonMessage: JsonMessage) {
        traceUtkast(id = jsonMessage["utkastId"].asText()) {
            validateUtkast(jsonMessage)

            jsonMessage.json
                .toString()
                .let {
                    withErrorLogging {
                        originalMessage = jsonMessage
                        utkastRepository.createUtkast(it, slettesEtter(jsonMessage))
                    }
                }

            log.info { "utkast created" }
            UtkastMetricsReporter.countUtkastOpprettet()
        }
    }

    private fun slettesEtter(jsonMessage: JsonMessage) = try {
        jsonMessage.getOrNull("slettesEtter")
            ?.asText()
            ?.let(ZonedDateTime::parse)
    } catch (e: Exception) {
        log.error { "Fikk feilaktig tidspunkt i 'slettesEtter'" }
        null
    }

    private fun validateUtkast(jsonMessage: JsonMessage) = try {
        UtkastValidator.validateUtkastId(jsonMessage["utkastId"].textValue())
        UtkastValidator.validateIdent(jsonMessage["ident"].textValue())
        UtkastValidator.validateLink(jsonMessage["link"].textValue())
    } catch (e: FieldValidationException) {
        throw MessageException(e.message ?: "Ukjent valideringsfeil")
    }
}
