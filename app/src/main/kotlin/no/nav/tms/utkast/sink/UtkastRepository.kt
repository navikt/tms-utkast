package no.nav.tms.utkast.sink

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.queryOf
import no.nav.tms.common.postgres.JsonbHelper.toJsonb
import no.nav.tms.common.postgres.PostgresDatabase
import org.postgresql.util.PGobject
import java.time.LocalDateTime
import java.time.ZonedDateTime

class UtkastRepository(private val database: PostgresDatabase) {
    fun createUtkast(created: String, slettesEtter: ZonedDateTime? = null) =
        database.update {
            queryOf(
                """
                    INSERT INTO utkast (packet, opprettet, slettesEtter) 
                    values (:packet, :opprettet, :slettesEtter)
                    ON CONFLICT DO NOTHING
                """,
                mapOf(
                    "packet" to created.asJsonb(),
                    "opprettet" to LocalDateTimeHelper.nowAtUtc(),
                    "slettesEtter" to slettesEtter
                )
            )
        }

    fun updateUtkast(utkastId: String, update: String, slettesEtter: ZonedDateTime? = null) {
        database.update {
            queryOf(
                """
                    UPDATE utkast 
                    SET sistEndret = :now, packet = packet || :update, slettesEtter = coalesce(:slettesEtter, slettesEtter)
                    WHERE packet @> :utkastId
                """,
                mapOf(
                    "update" to update.asJsonb(),
                    "utkastId" to utkastIdParam(utkastId),
                    "slettesEtter" to slettesEtter,
                    "now" to LocalDateTimeHelper.nowAtUtc()
                )
            )
        }
    }

    fun updateUtkastI18n(utkastId: String, tittelI18nUpdate: String) {
        database.update {
            queryOf(
                """
                UPDATE utkast SET sistEndret=:now, 
                    packet = ( CASE 
                        WHEN packet->'tittel_i18n' is not null
                        THEN jsonb_set(packet, '{tittel_i18n}', packet->'tittel_i18n' || :update)                        
                        WHEN packet->'tittel_i18n' is null
                        THEN jsonb_insert(packet, '{tittel_i18n}', :update)
                    END )
                WHERE packet @> :utkastId
                """,
                mapOf(
                    "update" to tittelI18nUpdate.asJsonb(),
                    "utkastId" to utkastIdParam(utkastId),
                    "now" to LocalDateTimeHelper.nowAtUtc()
                )
            )
        }
    }

    fun markUtkastAsDeleted(utkastId: String) {
        database.update {
            queryOf(
                "UPDATE utkast SET slettet = :now WHERE packet @> :utkastId",
                mapOf(
                    "now" to LocalDateTimeHelper.nowAtUtc(), "utkastId" to utkastIdParam(utkastId))
            )
        }
    }

    private fun String.asJsonb() = PGobject().apply {
        type = "jsonb"
        value = this@asJsonb
    }
}

fun utkastIdParam(utkastId: String): PGobject {
    return mapOf("utkastId" to utkastId).toJsonb()!!
}

data class Utkast(
    val utkastId: String,
    val tittel: String,
    val link: String,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime?,
    val slettesEtter: ZonedDateTime?,
    val metrics: Map<String,String>?
)

class DatabaseException: Throwable(){
    val details= ""
}

