package no.nav.tms.utkast

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import no.nav.personbruker.dittnav.common.metrics.influxdb.InfluxConfig
import no.nav.personbruker.dittnav.common.metrics.influxdb.InfluxMetricsReporter
import no.nav.tms.utkast.database.Database
import no.nav.tms.utkast.config.Environment
import no.nav.tms.utkast.config.Flyway
import no.nav.tms.utkast.config.PostgresDatabase
import no.nav.tms.utkast.database.UtkastRepository
import no.nav.tms.utkast.varsel.UtkastCreatedSink
import java.util.concurrent.TimeUnit

fun main() {
    val environment = Environment()

    val database: Database = PostgresDatabase(environment)
    val rapidMetricsProbe = RapidMetricsProbe(resolveMetricsReporter(environment))

    startRapid(
        environment = environment,
        rapidMetricsProbe = rapidMetricsProbe,
        utkastRepository = UtkastRepository(database)
    )
}

private fun startRapid(
    environment: Environment,
    utkastRepository: UtkastRepository,
    rapidMetricsProbe: RapidMetricsProbe
) {
    RapidApplication.create(environment.rapidConfig()).apply {
        UtkastCreatedSink(
            rapidsConnection = this,
            utkastRepository = utkastRepository,
            rapidMetricsProbe = rapidMetricsProbe
        )
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                Flyway.runFlywayMigrations(environment)
            }
        })
    }.start()
}

fun resolveMetricsReporter(environment: Environment): MetricsReporter {
    return if (environment.influxdbHost == "" || environment.influxdbHost == "stub") {
        StubMetricsReporter()
    } else {
        val influxConfig = InfluxConfig(
            applicationName = "tms-utkast",
            hostName = environment.influxdbHost,
            hostPort = environment.influxdbPort,
            databaseName = environment.influxdbName,
            retentionPolicyName = environment.influxdbRetentionPolicy,
            clusterName = environment.clusterName,
            namespace = environment.namespace,
            userName = environment.influxdbUser,
            password = environment.influxdbPassword,
            timePrecision = TimeUnit.NANOSECONDS
        )
        InfluxMetricsReporter(influxConfig)
    }
}