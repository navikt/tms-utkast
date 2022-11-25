package no.nav.tms.utkast

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.*
import no.nav.tms.utkast.builder.UtkastValidator
import no.nav.tms.utkast.config.JsonMessageHelper.keepFields
import no.nav.tms.utkast.config.JsonNodeHelper.checkForProblems
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
            validate { it.interestedIn("tittel", "link", "tittel_i18n") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {

        val utkastId = packet["utkastId"].asText()

        packet.keepFields("tittel_i18n")
            .takeIf { !it.isEmpty }
            ?.validate()
            ?.get("tittel_i18n")
            ?.toString()
            ?.let { utkastRepository.updateUtkastI18n(utkastId, it) }

        packet.keepFields("tittel", "link")
            .validate()
            .toString()
            .let { utkastRepository.updateUtkast(utkastId, it) }

        rapidMetricsProbe.countUtkastChanged("updated")
    }

    private fun JsonNode.validate(): JsonNode {

        val potentialProblems = listOf(
            checkForProblems("tittel", UtkastValidator::validateTittel),
            checkForProblems("tittel_i18n", UtkastValidator::validateTittel),
            checkForProblems("link", UtkastValidator::validateLink)
        )

        handleProblems(potentialProblems)

        return this
    }

    private fun handleProblems(potentialProblems: List<String?>) {
        val problems = potentialProblems.filterNotNull()
            .takeIf { it.isNotEmpty() }

        if (problems != null) {
            val messageProblems = MessageProblems("Feil ved validering av utkast opprettet.")

            problems.forEach {
                messageProblems.severe(it)
            }

            throw MessageProblems.MessageException(messageProblems)
        }
    }

    override fun onSevere(error: MessageProblems.MessageException, context: MessageContext) {
        log.info("Valideringsfeil ved oppdatering av utkast", error)
    }
}


