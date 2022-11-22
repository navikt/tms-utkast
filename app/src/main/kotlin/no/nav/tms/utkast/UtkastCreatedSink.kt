package no.nav.tms.utkast

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.*
import no.nav.tms.utkast.builder.UtkastValidator
import no.nav.tms.utkast.config.JsonMessageHelper.keepFields
import no.nav.tms.utkast.config.JsonNodeHelper.checkForProblems
import no.nav.tms.utkast.database.UtkastRepository


class UtkastCreatedSink(
    rapidsConnection: RapidsConnection,
    private val utkastRepository: UtkastRepository,
    private val rapidMetricsProbe: RapidMetricsProbe,
) :
    River.PacketListener {

    val log = KotlinLogging.logger {}

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "created") }
            validate { it.requireKey("link", "utkastId", "tittel", "ident")}
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {

        packet.keepFields("utkastId", "ident", "link", "tittel")
            .validate()
            .toString()
            .let { utkastRepository.createUtkast(it) }

        rapidMetricsProbe.countUtkastReceived()
    }

    private fun JsonNode.validate(): JsonNode {

        val potentialProblems = listOf (
            checkForProblems("utkastId", UtkastValidator::validateUtkastId),
            checkForProblems("ident", UtkastValidator::validateIdent),
            checkForProblems("tittel", UtkastValidator::validateTittel),
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
        log.info("Valideringsfeil ved oppretting av utkast", error)
    }
}

