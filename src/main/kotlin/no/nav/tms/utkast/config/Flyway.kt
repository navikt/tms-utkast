package no.nav.tms.utkast.config

import com.zaxxer.hikari.HikariDataSource
import no.nav.tms.utkast.database.PostgresDatabase
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration

object Flyway {

    fun runFlywayMigrations(env: Environment) {
        val flyway = configure(env).load()
        flyway.migrate()
    }

    private fun configure(env: Environment): FluentConfiguration {
        val configBuilder = Flyway.configure().connectRetries(5)
        val dataSource = createDataSourceForLocalDbWithUser(env)
        configBuilder.dataSource(dataSource)

        return configBuilder
    }

    private fun createDataSourceForLocalDbWithUser(env: Environment): HikariDataSource {
        return PostgresDatabase.hikariFromLocalDb(env)
    }

}
