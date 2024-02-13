package no.nav.tms.utkast

import io.ktor.client.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidApplication.RapidApplicationConfig.Companion.fromEnv
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tms.common.kubernetes.PodLeaderElection
import no.nav.tms.token.support.tokendings.exchange.TokendingsServiceBuilder
import no.nav.tms.utkast.api.UtkastApiRepository
import no.nav.tms.utkast.api.UtkastFetcher
import no.nav.tms.utkast.api.utkastApi
import no.nav.tms.utkast.expiry.PeriodicUtkastDeleter
import no.nav.tms.utkast.setup.Environment
import no.nav.tms.utkast.setup.Flyway
import no.nav.tms.utkast.setup.configureJackson
import no.nav.tms.utkast.setup.PostgresDatabase
import no.nav.tms.utkast.sink.UtkastSinkRepository
import no.nav.tms.utkast.sink.UtkastCreatedSink
import no.nav.tms.utkast.sink.UtkastDeletedSink
import no.nav.tms.utkast.sink.UtkastUpdatedSink

fun main() {
    val environment = Environment()

    val httpClient = HttpClient {
        configureJackson()
    }

    val database = PostgresDatabase(environment)

    val utkastDeleter = PeriodicUtkastDeleter(database)

    startRapid(
        environment = environment,
        readUtkastRepository = UtkastApiRepository(database),
        writeUtkastRepository = UtkastSinkRepository(database),
        utkastFetcher = UtkastFetcher(
            digiSosBaseUrl = environment.digisosBaseUrl,
            httpClient = httpClient,
            digisosClientId = environment.digisosClientId,
            tokendingsService = TokendingsServiceBuilder.buildTokendingsService(),
            aapClientId = environment.aapClientId,
        ),
        utkastDeleter
    )
}

private fun startRapid(
    environment: Environment,
    readUtkastRepository: UtkastApiRepository,
    writeUtkastRepository: UtkastSinkRepository,
    utkastFetcher: UtkastFetcher,
    utkastDeleter: PeriodicUtkastDeleter
) {

    RapidApplication.Builder(fromEnv(environment.rapidConfig())).withKtorModule {
        utkastApi(readUtkastRepository, utkastFetcher)
    }.build().apply {
        UtkastCreatedSink(
            rapidsConnection = this,
            utkastRepository = writeUtkastRepository
        )
        UtkastUpdatedSink(
            rapidsConnection = this,
            utkastRepository = writeUtkastRepository
        )
        UtkastDeletedSink(
            rapidsConnection = this,
            utkastRepository = writeUtkastRepository
        )
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                Flyway.runFlywayMigrations(environment)
                utkastDeleter.start()
            }

            override fun onShutdown(rapidsConnection: RapidsConnection) {
                runBlocking {
                    utkastDeleter.stop()
                }
            }
        })
    }.start()
}
