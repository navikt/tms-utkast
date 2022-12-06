package no.nav.tms.utkast

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.utkast.builder.UtkastValidator.validateLink
import no.nav.tms.utkast.builder.UtkastValidator.validateTittel
import no.nav.tms.utkast.config.JsonMessageHelper.keepFields
import no.nav.tms.utkast.database.UtkastRepository

class UtkastUpdatedSink(
    rapidsConnection: RapidsConnection,
    private val utkastRepository: UtkastRepository,
    private val rapidMetricsProbe: RapidMetricsProbe
) :
    River.PacketListener {

    private val log = KotlinLogging.logger {}

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "updated") }
            validate { it.requireKey("utkastId") }
            validate {
                it.interestedIn("link") { jsonNode -> validateLink(jsonNode.textValue()) }
                it.interestedIn("tittel") { jsonNode -> validateTittel(jsonNode.textValue()) }
                it.interestedIn("tittel_i18n") { languages ->
                    languages.forEach { title -> validateTittel(title.textValue()) }
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val utkastId = packet["utkastId"].asText()

        if(!packet["tittel_i18n"].isEmpty) utkastRepository.updateUtkastI18n(utkastId, packet["tittel_i18n"].toString())

        packet.keepFields("tittel", "link")
            .toString()
            .let { utkastRepository.updateUtkast(utkastId, it) }

        rapidMetricsProbe.countUtkastChanged("updated")
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.info(problems.toString())
    }
}