package no.nav.tms.utkast

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.utkast.builder.UtkastValidator
import no.nav.tms.utkast.config.JsonMessageHelper.keepFields
import no.nav.tms.utkast.config.withErrorLogging
import no.nav.tms.utkast.database.UtkastRepository
import observability.Contenttype
import observability.traceUtkast
import observability.withTraceLogging

class UtkastCreatedSink(
    rapidsConnection: RapidsConnection,
    private val utkastRepository: UtkastRepository
) :
    River.PacketListener {

    private val log = KotlinLogging.logger {}

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "created") }
            validate {
                it.require("utkastId") { jsonNode -> UtkastValidator.validateUtkastId(jsonNode.textValue()) }
                it.require("ident") { jsonNode -> UtkastValidator.validateIdent(jsonNode.textValue()) }
                it.require("link") { jsonNode -> UtkastValidator.validateLink(jsonNode.textValue()) }
                it.requireKey("tittel")
                it.interestedIn("tittel_i18n")
                it.interestedIn("metrics")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        traceUtkast(id = packet["utkastId"].asText()) {
            log.info { "utkast created" }
            packet.keepFields("utkastId", "ident", "link", "tittel", "tittel_i18n", "metrics")
                .toString()
                .let {
                    withErrorLogging {
                        jsonMessage = packet
                        utkastRepository.createUtkast(it)
                    }
                }
            UtkastMetricsReporter.countUtkastOpprettet()
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.info { problems.toString() }
    }
}
