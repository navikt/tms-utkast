package no.nav.tms.utkast.database

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.json.Json
import kotliquery.queryOf
import no.nav.tms.common.postgres.JsonbHelper.json
import no.nav.tms.common.postgres.JsonbHelper.toJsonb
import no.nav.tms.common.postgres.Postgres
import no.nav.tms.common.postgres.PostgresDatabase
import no.nav.tms.utkast.UtkastData
import no.nav.tms.utkast.sink.Utkast
import no.nav.tms.utkast.sink.utkastIdParam
import org.flywaydb.core.Flyway
import org.testcontainers.postgresql.PostgreSQLContainer

object LocalPostgresDatabase {

    private val container = PostgreSQLContainer("postgres:14.5")
        .also { it.start() }

    private val instance by lazy {
        Postgres.connectToContainer(container).also {
            migrate(it.dataSource)
        }
    }

    fun getCleanInstance(): PostgresDatabase {
        resetInstance()
        return instance
    }

    fun resetInstance() {
        instance.update { queryOf("delete from utkast") }
    }

    private fun migrate(dataSource: HikariDataSource) {
        Flyway.configure()
            .connectRetries(3)
            .dataSource(dataSource)
            .load()
            .migrate()
    }

    fun alleUtkast(): List<UtkastData> = instance.list {
        queryOf(
            """
            select 
                packet,
                packet->>'utkastId' as utkastId,
                packet->>'tittel' as tittel,
                packet->>'tittel_i18n' as tittel_i18n,
                packet->>'link' as link,
                packet->>'metrics' as metrics,
                sistendret, opprettet, slettesEtter, slettet
            from utkast"""
        )
        .map { row ->
            val hello = row.string("packet").let { jacksonObjectMapper().readTree(it) }

            println(hello)

            UtkastData(
                utkastId = row.string("utkastId"),
                tittel = row.string("tittel"),
                tittelI18n = row.stringOrNull("tittel_i18n")?.let { Json.decodeFromString(it) } ?: emptyMap(),
                link = row.string("link"),
                opprettet = row.localDateTime("opprettet"),
                sistEndret = row.localDateTimeOrNull("sistendret"),
                slettesEtter = row.zonedDateTimeOrNull("slettesEtter"),
                slettet = row.localDateTimeOrNull("slettet"),
                metrics = row.stringOrNull("metrics")
                    ?.let {
                        val jsonValues = jacksonObjectMapper().readTree(it)
                        mapOf(
                            "skjemakode" to jsonValues["skjemakode"].asText(),
                            "skjemanavn" to jsonValues["skjemanavn"].asText()
                        )
                    }
            )
        }
    }

    fun getUtkast(utkastId: String): UtkastData? {
        return instance.singleOrNull {
            queryOf(
                """
                select 
                    packet->>'utkastId' as utkastId,
                    packet->>'tittel' as tittel,
                    packet->>'tittel_i18n' as tittel_i18n,
                    packet->>'link' as link,
                    packet ->>'metrics' as metrics,
                    sistendret, opprettet, slettesEtter, slettet
                from utkast 
                where packet @> :utkastId""",
                mapOf("utkastId" to utkastIdParam(utkastId))
            )
            .map { row ->
                UtkastData(
                    utkastId = row.string("utkastId"),
                    tittel = row.string("tittel"),
                    tittelI18n = row.stringOrNull("tittel_i18n")?.let { Json.decodeFromString(it) } ?: emptyMap(),
                    link = row.string("link"),
                    opprettet = row.localDateTime("opprettet"),
                    sistEndret = row.localDateTimeOrNull("sistendret"),
                    slettesEtter = row.zonedDateTimeOrNull("slettesEtter"),
                    slettet = row.localDateTimeOrNull("slettet"),
                    metrics = row.stringOrNull("metrics")
                        ?.let {
                            val jsonValues = jacksonObjectMapper().readTree(it)
                            mapOf(
                                "skjemakode" to jsonValues["skjemakode"].asText(),
                                "skjemanavn" to jsonValues["skjemanavn"].asText()
                            )
                        }
                )
            }
        }
    }
}

