package no.nav.tms.utkast.sink

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.reader.JsonMessage
import no.nav.tms.kafka.reader.Subscriber
import no.nav.tms.kafka.reader.Subscription
import no.nav.tms.utkast.setup.UtkastMetricsReporter
import no.nav.tms.utkast.setup.withErrorLogging
import observability.traceUtkast

class UtkastDeletedListener(
    private val utkastRepository: UtkastSinkRepository
) : Subscriber() {

    private val log = KotlinLogging.logger { }


    override fun subscribe() = Subscription.forEvent("deleted")
        .withFields("utkastId")

    override suspend fun receive(jsonMessage: JsonMessage) {
        traceUtkast(id = jsonMessage["utkastId"].asText()) {
            withErrorLogging {
                message = "Feil ved sletting av utkast med id ${jsonMessage["utkastId"].asText()}"
                utkastRepository.deleteUtkast(jsonMessage["utkastId"].asText())
            }
            log.info { "Utkast deleted" }

            UtkastMetricsReporter.countUtkastSlettet()
        }
    }
}
