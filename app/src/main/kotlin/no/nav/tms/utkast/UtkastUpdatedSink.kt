package no.nav.tms.utkast

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.utkast.builder.UtkastValidator.validateLink
import no.nav.tms.utkast.config.JsonMessageHelper.keepFields
import no.nav.tms.utkast.config.withErrorLogging
import no.nav.tms.utkast.database.UtkastRepository

class UtkastUpdatedSink(
    rapidsConnection: RapidsConnection,
    private val utkastRepository: UtkastRepository
) :
    River.PacketListener {

    private val log = KotlinLogging.logger {}

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "updated") }
            validate { it.requireKey("utkastId") }
            validate {
                it.interestedIn("link") { jsonNode -> validateLink(jsonNode.textValue()) }
                it.interestedIn("tittel")
                it.interestedIn("tittel_i18n")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val utkastId = packet["utkastId"].asText()


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

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.info { problems.toString() }
    }
}
