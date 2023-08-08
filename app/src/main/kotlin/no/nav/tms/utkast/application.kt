package no.nav.tms.utkast

import io.ktor.client.*
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidApplication.RapidApplicationConfig.Companion.fromEnv
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tms.token.support.tokendings.exchange.TokendingsServiceBuilder
import no.nav.tms.utkast.config.Environment
import no.nav.tms.utkast.config.Flyway
import no.nav.tms.utkast.config.configureJackson
import no.nav.tms.utkast.database.PostgresDatabase
import no.nav.tms.utkast.database.UtkastRepository
import java.util.concurrent.TimeUnit

fun main() {
    val environment = Environment()
    val httpClient = HttpClient {
        configureJackson()
    }
    startRapid(
        environment = environment,
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
    digisosHttpClient: DigisosHttpClient
) {

    RapidApplication.Builder(fromEnv(environment.rapidConfig())).withKtorModule {
        utkastApi(utkastRepository, digisosHttpClient)
    }.build().apply {
        UtkastCreatedSink(
            rapidsConnection = this,
            utkastRepository = utkastRepository
        )
        UtkastUpdatedSink(
            rapidsConnection = this,
            utkastRepository = utkastRepository
        )
        UtkastDeletedSink(
            rapidsConnection = this,
            utkastRepository = utkastRepository
        )
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                Flyway.runFlywayMigrations(environment)
            }
        })
    }.start()
}
