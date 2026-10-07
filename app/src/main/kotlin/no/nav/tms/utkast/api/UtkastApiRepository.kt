package no.nav.tms.utkast.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import no.nav.tms.common.postgres.JsonbHelper.toJsonb
import no.nav.tms.common.postgres.PostgresDatabase
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance
import no.nav.tms.utkast.sink.Utkast
import org.postgresql.util.PGobject
import java.util.*

class UtkastApiRepository(private val database: PostgresDatabase) {

    private val objectMapper = jacksonObjectMapper()

    internal fun getUtkastForIdent(
        ident: String,
        levelOfAssurance: LevelOfAssurance,
        locale: Locale? = null
    ): List<Utkast> =
        database.list {
            queryOf(
                """
                    SELECT 
                        packet->>'utkastId' AS utkastId,
                        coalesce(packet->'tittel_i18n'->>:locale, packet->>'tittel') AS tittel,
                        packet->>'link' AS link,
                        packet->>'metrics' AS metrics,
                        sistendret, opprettet, slettesEtter
                    FROM utkast
                    WHERE packet @> :ident
                        AND (
                            packet->>'levelOfAssurance' is NULL
                            OR packet->>'levelOfAssurance' = 'Substantial'
                            OR (
                                packet->>'levelOfAssurance' = 'High'
                                AND :levelOfAssurance = 'High'
                            )
                        )
                    """,
                mapOf(
                    "ident" to identParam(ident),
                    "locale" to locale?.language,
                    "levelOfAssurance" to levelOfAssurance.name
                )
            )
                .map { row ->
                    Utkast(
                        utkastId = row.string("utkastId"),
                        tittel = row.string("tittel"),
                        link = row.string("link"),
                        opprettet = row.localDateTime("opprettet"),
                        sistEndret = row.localDateTimeOrNull("sistendret"),
                        slettesEtter = row.zonedDateTimeOrNull("slettesEtter"),
                        metrics = row.stringOrNull("metrics")
                            ?.let {
                                objectMapper.readValue<Map<String,String>>(it)
                            }

                    )
                }
        }

    private fun identParam(ident: String): PGobject {
        return mapOf("ident" to ident).toJsonb()!!
    }
}
