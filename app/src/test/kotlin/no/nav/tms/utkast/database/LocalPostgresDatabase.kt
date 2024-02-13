package no.nav.tms.utkast.database

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.json.Json
import kotliquery.queryOf
import no.nav.tms.utkast.UtkastData
import no.nav.tms.utkast.setup.Database
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

class LocalPostgresDatabase private constructor() : Database {

    private val memDataSource: HikariDataSource
    private val container = PostgreSQLContainer("postgres:14.5")

    companion object {
        private val instance by lazy {
            LocalPostgresDatabase().also {
                it.migrate()
            }
        }

        fun cleanDb(): LocalPostgresDatabase {
            instance.update { queryOf("delete from utkast") }
            return instance
        }
    }

    init {
        container.start()
        memDataSource = createDataSource()
    }

    override val dataSource: HikariDataSource
        get() = memDataSource

    private fun createDataSource(): HikariDataSource {
        return HikariDataSource().apply {
            jdbcUrl = container.jdbcUrl
            username = container.username
            password = container.password
            isAutoCommit = true
            validate()
        }
    }

    private fun migrate() {
        Flyway.configure()
            .connectRetries(3)
            .dataSource(dataSource)
            .load()
            .migrate()
    }
}

internal val alleUtkast =
    queryOf(
        """
        select 
            packet->>'utkastId' as utkastId,
            packet->>'tittel' as tittel,
            packet->>'tittel_i18n' as tittel_i18n,
            packet->>'link' as link,
            packet ->> 'metrics' as metrics,
            sistendret, opprettet, slettet 
        from utkast"""
    )
        .map { row ->
            UtkastData(
                utkastId = row.string("utkastId"),
                tittel = row.string("tittel"),
                tittelI18n = row.stringOrNull("tittel_i18n")?.let { Json.decodeFromString(it) } ?: emptyMap(),
                link = row.string("link"),
                opprettet = row.localDateTime("opprettet"),
                sistEndret = row.localDateTimeOrNull("sistendret"),
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

        }.asList

