package no.nav.tms.utkast.database

import kotliquery.queryOf
import no.nav.tms.utkast.config.Database
import no.nav.tms.utkast.config.LocalDateTimeHelper
import org.postgresql.util.PGobject
import java.time.LocalDateTime

class UtkastRepository(private val database: Database) {
    fun createUtkast(created: String) =
        database.update {
            queryOf(
                "INSERT INTO utkast (packet, opprettet) values (:packet,:opprettet) ON CONFLICT DO NOTHING",
                mapOf("packet" to created.jsonB(), "opprettet" to LocalDateTimeHelper.nowAtUtc())
            )
        }

    fun updateUtkast(eventId: String, update: String) {
        database.update {
            queryOf(
                "UPDATE utkast SET sistEndret=:now, packet=packet || :update WHERE packet->>'eventId'=:eventId",
                mapOf(
                    "update" to update.jsonB(),
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

    internal fun getUtkast(ident: String): List<Utkast> =
        database.list {
            queryOf(
                """
                    SELECT 
                        packet->>'eventId' AS eventId,
                        packet->>'tittel' AS tittel,
                        packet->>'link' AS link,
                        sistendret, opprettet
                    FROM utkast
                    WHERE packet->>'ident'=:ident AND slettet IS NULL""",
                mapOf("ident" to ident)
            )
                .map { row ->
                    Utkast(
                        eventId = row.string("eventId"),
                        tittel = row.string("tittel"),
                        link = row.string("link"),
                        opprettet = row.localDateTime("opprettet"),
                        sistEndret = row.localDateTimeOrNull("sistendret"),
                    )
                }.asList
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
)