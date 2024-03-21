package no.nav.tms.utkast.sink

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.reader.JsonMessage
import no.nav.tms.kafka.reader.Subscriber
import no.nav.tms.kafka.reader.Subscription
import no.nav.tms.utkast.builder.FieldValidationException
import no.nav.tms.utkast.builder.UtkastValidator
import no.nav.tms.utkast.setup.UtkastMetricsReporter
import no.nav.tms.utkast.setup.withErrorLogging
import observability.traceUtkast

class UtkastCreatedListener(
    private val utkastRepository: UtkastSinkRepository
) : Subscriber() {

    private val log = KotlinLogging.logger {}

    override fun subscribe() = Subscription.forEvent("created")
        .withFields("utkastId", "ident", "link", "tittel")
        .withOptionalFields("tittel_i18n", "metrics")

    override suspend fun receive(jsonMessage: JsonMessage) {
        traceUtkast(id = jsonMessage["utkastId"].asText()) {
            if (!validate(jsonMessage)) {
                return@traceUtkast
            }

            withErrorLogging {
                eventMessage = jsonMessage
                utkastRepository.createUtkast(jsonMessage.json.toString())
            }

            log.info { "utkast created" }
            UtkastMetricsReporter.countUtkastOpprettet()
        }
    }

    private fun validate(jsonMessage: JsonMessage): Boolean {
        return try {
            UtkastValidator.validateUtkastId(jsonMessage["utkastId"].textValue())
            UtkastValidator.validateIdent(jsonMessage["ident"].textValue())
            UtkastValidator.validateLink(jsonMessage["link"].textValue())
            true
        } catch (e: FieldValidationException) {
            log.info { "Skipping invalid event" }
            false
        }
    }
}
