package no.nav.tms.utkast

import com.zaxxer.hikari.HikariDataSource
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.date.shouldNotBeAfter
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotliquery.queryOf
import no.nav.tms.utkast.config.Database
import org.flywaydb.core.Flyway
import org.intellij.lang.annotations.Language
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDateTime

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
                slettet = row.localDateTimeOrNull("slettet")
            )

        }.asList

@Language("JSON")
internal fun createUtkastTestPacket(
    utkastId: String,
    ident: String,
    tittel: String = "http://testlink",
    tittelI18n: Map<String, String>? = null,
    link: String = "http://testlink"
) = """
    {
     "@event_name": "created",
    "utkastId": "$utkastId",
    "ident": "$ident",
    "link": "$link",
    "tittel": "$tittel"
    ${if (tittelI18n != null) ",\"tittel_i18n\": ${tittelI18n.toJson()}" else ""}
    }
""".trimIndent()

@Language("JSON")
internal fun updateUtkastTestPacket(utkastId: String, tittel: String? = null, link: String? = null, tittelI18n: Map<String, String>? = null) = """
    {
    "@event_name":"updated",
    "utkastId": "$utkastId"
    ${if (tittel != null) ",\"tittel\": \"$tittel\"" else ""}
    ${if (link != null) ",\"link\": \"$link\"" else ""}
    ${if (tittelI18n != null) ",\"tittel_i18n\": ${tittelI18n.toJson()}" else ""}
    }
""".trimIndent()

@Language("JSON")
internal fun deleteUtkastTestPacket(utkastId: String) = """
    {
    "@event_name":"deleted",
    "utkastId": "$utkastId"
    }
""".trimIndent()

internal infix fun LocalDateTime?.shouldBeCaSameAs(expected: LocalDateTime?) {
    if (expected == null) {
        this shouldBe null
    } else {
        require(this!=null)
        this shouldBeAfter expected.minusMinutes(2)
        this shouldNotBeAfter expected
    }
}

private fun Map<String, String>.toJson(): String = mapValues { (_, v) ->
        JsonPrimitive(v)
    }.let { JsonObject(it) }
    .toString()

internal data class UtkastData(
    val utkastId: String,
    val tittel: String,
    val tittelI18n: Map<String, String>,
    val link: String,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime?,
    val slettet: LocalDateTime?
)
