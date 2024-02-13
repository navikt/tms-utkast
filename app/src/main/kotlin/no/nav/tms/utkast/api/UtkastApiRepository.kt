package no.nav.tms.utkast.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import no.nav.tms.utkast.setup.Database
import no.nav.tms.utkast.sink.Utkast
import java.util.*

class UtkastApiRepository(private val database: Database) {

    internal fun getUtkastForIdent(ident: String, locale: Locale? = null): List<Utkast> =
        database.list {
            queryOf(
                """
                    SELECT 
                        packet->>'utkastId' AS utkastId,
                        coalesce(packet->'tittel_i18n'->>:locale, packet->>'tittel') AS tittel,
                        packet->>'link' AS link,
                        packet->>'metrics' AS metrics,
                        sistendret, opprettet
                    FROM utkast
                    WHERE packet->'ident' ?? :ident AND slettet IS NULL""",
                mapOf("ident" to ident, "locale" to locale?.language)
            )
                .map { row ->
                    Utkast(
                        utkastId = row.string("utkastId"),
                        tittel = row.string("tittel"),
                        link = row.string("link"),
                        opprettet = row.localDateTime("opprettet"),
                        sistEndret = row.localDateTimeOrNull("sistendret"),
                        metrics = row.stringOrNull("metrics")
                            ?.let {
                                val jsonValues = jacksonObjectMapper().readTree(it)
                                mapOf(
                                    "skjemakode" to jsonValues["skjemakode"].asText(),
                                    "skjemanavn" to jsonValues["skjemanavn"].asText()
                                )
                                jacksonObjectMapper().readValue<Map<String,String>>(it)
                            }
                    )
                }.asList
        }
}
