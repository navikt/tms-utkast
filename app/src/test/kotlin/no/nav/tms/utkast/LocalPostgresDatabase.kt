package no.nav.tms.utkast

import com.zaxxer.hikari.HikariDataSource
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.date.shouldNotBeAfter
import io.kotest.matchers.shouldBe
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
            packet->>'eventId' as eventId,
            packet->>'tittel' as tittel,
            packet->>'link' as link,
            sistendret, opprettet, slettet 
        from utkast"""
    )
        .map { row ->
            UtkastData(
                eventId = row.string("eventId"),
                tittel = row.string("tittel"),
                link = row.string("link"),
                opprettet = row.localDateTime("opprettet"),
                sistEndret = row.localDateTimeOrNull("sistendret"),
                slettet = row.localDateTimeOrNull("slettet")
            )

        }.asList

@Language("JSON")
internal fun createUtkastTestPacket(
    eventId: String,
    ident: String,
    link: String = "http://testlink",
    tittel: String = "Utkasttittel"
) = """
    {
     "@event_name": "created",
    "eventId": "$eventId",
    "ident": "$ident",
    "link": "$link",
    "tittel": "$tittel"
    }
""".trimIndent()

@Language("JSON")
internal fun updateUtkastTestPacket(eventId: String) = """
    {
    "@event_name":"updated",
    "eventId": "$eventId"
    }
""".trimIndent()

@Language("JSON")
internal fun deleteUtkastTestPacket(eventId: String) = """
    {
    "@event_name":"deleted",
    "eventId": "$eventId"
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

internal data class UtkastData(
    val eventId: String,
    val tittel: String,
    val link: String,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime?,
    val slettet: LocalDateTime?
)
