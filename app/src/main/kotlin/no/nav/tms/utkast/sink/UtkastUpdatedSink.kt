package no.nav.tms.utkast.sink

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helse.rapids_rivers.*
import no.nav.tms.utkast.builder.UtkastValidator
import no.nav.tms.utkast.sink.JsonMessageHelper.keepFields
import no.nav.tms.utkast.setup.UtkastMetricsReporter
import no.nav.tms.utkast.setup.withErrorLogging
import observability.traceUtkast

class UtkastUpdatedSink(
    rapidsConnection: RapidsConnection,
    private val utkastRepository: UtkastSinkRepository
) :
    River.PacketListener {

    private val log = KotlinLogging.logger {}

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "updated") }
            validate { it.requireKey("utkastId") }
            validate {
                it.interestedIn("link") { jsonNode -> UtkastValidator.validateLink(jsonNode.textValue()) }
                it.interestedIn("tittel")
                it.interestedIn("tittel_i18n")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val utkastId = packet["utkastId"].asText()
        traceUtkast(id = utkastId) {
            log.info { "utkast updated" }
            packet["tittel_i18n"].takeIf { !it.isEmpty }
                ?.toString()
                ?.let {
                    withErrorLogging {
                        utkastRepository.updateUtkastI18n(utkastId, it)
                    }
                }

            packet.keepFields("tittel", "link")
                .toString()
                .let {
                    withErrorLogging {
                        utkastRepository.updateUtkast(utkastId, it)
                    }
                }

            UtkastMetricsReporter.countUtkastEndret()
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.info { problems.toString() }
    }
}
