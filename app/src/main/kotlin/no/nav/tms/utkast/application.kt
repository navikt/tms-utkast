package no.nav.tms.utkast

import io.ktor.client.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidApplication.RapidApplicationConfig.Companion.fromEnv
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tms.kafka.reader.KafkaApplication
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
import no.nav.tms.utkast.sink.UtkastCreatedListener
import no.nav.tms.utkast.sink.UtkastDeletedListener
import no.nav.tms.utkast.sink.UtkastUpdatedListener

suspend fun main() {
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

private suspend fun startRapid(
    environment: Environment,
    readUtkastRepository: UtkastApiRepository,
    writeUtkastRepository: UtkastSinkRepository,
    utkastFetcher: UtkastFetcher,
    utkastDeleter: PeriodicUtkastDeleter
) {

    KafkaApplication.build {
        kafkaConfig {
            readTopic(environment.kafkaTopic)
            groupId = environment.groupId
        }

        ktorModule {
            utkastApi(readUtkastRepository, utkastFetcher)
        }

        subscriber {
            UtkastCreatedListener(utkastRepository = writeUtkastRepository)
        }

        subscriber {
            UtkastUpdatedListener(utkastRepository = writeUtkastRepository)
        }

        subscriber {
            UtkastDeletedListener(utkastRepository = writeUtkastRepository)
        }

        onStartup {
            Flyway.runFlywayMigrations(environment)
            utkastDeleter.start()
        }

        onShutdown {
            runBlocking {
                utkastDeleter.stop()
            }
        }
    }.start()
}
