package no.nav.tms.utkast.sink

import kotliquery.queryOf
import no.nav.tms.common.postgres.PostgresDatabase
import org.postgresql.util.PGobject
import java.time.LocalDateTime

class UtkastRepository(private val database: PostgresDatabase) {
    fun createUtkast(created: String) =
        database.update {
            queryOf(
                "INSERT INTO utkast (packet, opprettet) values (:packet,:opprettet) ON CONFLICT DO NOTHING",
                mapOf("packet" to created.jsonB(), "opprettet" to LocalDateTimeHelper.nowAtUtc())
            )
        }

    fun updateUtkast(utkastId: String, update: String) {
        database.update {
            queryOf(
                "UPDATE utkast SET sistEndret=:now, packet=packet || :update WHERE packet-> 'utkastId' ?? :utkastId",
                mapOf(
                    "update" to update.jsonB(),
                    "utkastId" to utkastId,
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
                WHERE packet-> 'utkastId' ?? :utkastId
                """,
                mapOf(
                    "update" to tittelI18nUpdate.jsonB(),
                    "utkastId" to utkastId,
                    "now" to LocalDateTimeHelper.nowAtUtc()
                )
            )
        }
    }

    fun deleteUtkast(utkastId: String) {
        database.update {
            queryOf(
                "UPDATE utkast SET slettet=:now WHERE packet-> 'utkastId' ?? :utkastId",
                mapOf("now" to LocalDateTimeHelper.nowAtUtc(), "utkastId" to utkastId)
            )
        }
    }


}

private fun String.jsonB() = PGobject().apply {
    type = "jsonb"
    value = this@jsonB
}

data class Utkast(
    val utkastId: String,
    val tittel: String,
    val link: String,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime?,
    val metrics: Map<String,String>?
)

class DatabaseException: Throwable(){
    val details= ""
}

