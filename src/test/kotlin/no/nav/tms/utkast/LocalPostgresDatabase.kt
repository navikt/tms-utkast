import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tms.utkast.config.Database
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

class LocalPostgresDatabase private constructor() : Database {

    private val memDataSource: HikariDataSource
    private val container = PostgreSQLContainer("postgres:12.6")

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
    queryOf("select * from utkast")
        .map { row ->
            row.string("packet")
        }.asList

internal fun testUtkast(
    eventId: String,
    fnr: String,
    eventName: String = "created",
    link: String = "testlink",
    tittel: String = "Utkasttittel"
) = """
    {
     "@event_name": "$eventName",
    "eventId": "$eventId",
    "ident": "$fnr",
    "link": "$link",
    "tittel": "$tittel"
    }
""".trimIndent()