package no.nav.tms.utkast

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.utkast.builder.UtkastValidator
import no.nav.tms.utkast.config.JsonMessageHelper.keepFields
import no.nav.tms.utkast.database.UtkastRepository

class UtkastCreatedSink(
    rapidsConnection: RapidsConnection,
    private val utkastRepository: UtkastRepository,
    private val rapidMetricsProbe: RapidMetricsProbe,
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
        packet.keepFields("utkastId", "ident", "link", "tittel", "tittel_i18n","metrics")
            .toString()
            .let { utkastRepository.createUtkast(it) }

        rapidMetricsProbe.countUtkastReceived()
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.info(problems.toString())
    }
}

