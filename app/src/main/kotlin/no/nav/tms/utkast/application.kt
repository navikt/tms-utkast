package no.nav.tms.utkast

import io.ktor.client.*
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidApplication.RapidApplicationConfig.Companion.fromEnv
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import no.nav.personbruker.dittnav.common.metrics.influxdb.InfluxConfig
import no.nav.personbruker.dittnav.common.metrics.influxdb.InfluxMetricsReporter
import no.nav.tms.token.support.tokendings.exchange.TokendingsServiceBuilder
import no.nav.tms.utkast.config.Environment
import no.nav.tms.utkast.config.Flyway
import no.nav.tms.utkast.config.configureJackson
import no.nav.tms.utkast.database.PostgresDatabase
import no.nav.tms.utkast.database.UtkastRepository
import java.util.concurrent.TimeUnit

fun main() {
    val environment = Environment()
    val httpClient = HttpClient() {
        configureJackson()
    }
    val rapidMetricsProbe = RapidMetricsProbe(resolveMetricsReporter(environment))
    startRapid(
        environment = environment,
        rapidMetricsProbe = rapidMetricsProbe,
        utkastRepository = UtkastRepository(PostgresDatabase(environment)),
        digisosHttpClient = DigisosHttpClient(
            baseUrl = environment.digisosBaseUrl,
            httpClient = httpClient,
            digisosClientId = environment.digisosClientId,
            tokendingsService = TokendingsServiceBuilder.buildTokendingsService()
        )
    )
}

private fun startRapid(
    environment: Environment,
    utkastRepository: UtkastRepository,
    rapidMetricsProbe: RapidMetricsProbe,
    digisosHttpClient: DigisosHttpClient
) {

    RapidApplication.Builder(fromEnv(environment.rapidConfig())).withKtorModule {
        utkastApi(utkastRepository, digisosHttpClient)
    }.build().apply {
        UtkastCreatedSink(
            rapidsConnection = this,
            utkastRepository = utkastRepository,
            rapidMetricsProbe = rapidMetricsProbe
        )
        UtkastUpdatedSink(
            rapidsConnection = this,
            utkastRepository = utkastRepository,
            rapidMetricsProbe = rapidMetricsProbe
        )
        UtkastDeletedSink(
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
