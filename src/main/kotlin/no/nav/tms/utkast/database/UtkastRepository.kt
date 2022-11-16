package no.nav.tms.utkast.database

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.queryOf
import no.nav.tms.utkast.config.Database
import no.nav.tms.utkast.config.LocalDateTimeHelper
import org.postgresql.util.PGobject
import java.time.LocalDateTime

class UtkastRepository(private val database: Database) {
    fun createUtkast(packet: String) =
        database.update {
            queryOf(
                "INSERT INTO utkast (packet, opprettet) values (:packet,:opprettet) ON CONFLICT DO NOTHING",
                mapOf("packet" to packet.jsonB(), "opprettet" to LocalDateTimeHelper.nowAtUtc())
            )
        }

    fun updateUtkast(eventId: String, update: JsonNode) {
        database.update {
            queryOf(
                "UPDATE utkast SET sistEndret=:now, packet=packet || :update WHERE packet->>'eventId'=:eventId",
                mapOf(
                    "update" to update.toString().jsonB(),
                    "eventId" to eventId,
                    "now" to LocalDateTimeHelper.nowAtUtc()
                )
            )
        }
    }

    fun deleteUtkast(eventId: String) {
        database.update {
            queryOf(
                "UPDATE utkast SET slettet=:now WHERE packet->>'eventId'=:eventId",
                mapOf("now" to LocalDateTimeHelper.nowAtUtc(), "eventId" to eventId)
            )
        }
    }
}

private fun String.jsonB() = PGobject().apply {
    type = "jsonb"
    value = this@jsonB
}

internal data class Utkast(
    val eventId: String,
    val tittel: String,
    val link: String,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime?,
    val slettet: LocalDateTime?
)
