package no.nav.tms.utkast

import io.ktor.client.*
import kotlinx.coroutines.runBlocking
import no.nav.tms.kafka.application.KafkaApplication
import no.nav.tms.token.support.tokendings.exchange.TokendingsServiceBuilder
import no.nav.tms.utkast.api.UtkastApiRepository
import no.nav.tms.utkast.api.UtkastFetcher
import no.nav.tms.utkast.api.utkastApi
import no.nav.tms.utkast.expiry.PeriodicUtkastDeleter
import no.nav.tms.utkast.setup.Environment
import no.nav.tms.utkast.setup.Flyway
import no.nav.tms.utkast.setup.configureClient
import no.nav.tms.utkast.setup.PostgresDatabase
import no.nav.tms.utkast.sink.UtkastRepository
import no.nav.tms.utkast.sink.UtkastCreatedSubscriber
import no.nav.tms.utkast.sink.UtkastDeletedSubscriber
import no.nav.tms.utkast.sink.UtkastUpdatedSubscriber

fun main() {
    val environment = Environment()

    val httpClient = HttpClient {
        configureClient()
    }

    val database = PostgresDatabase(environment)

    val utkastDeleter = PeriodicUtkastDeleter(database)

    startApplicaiton(
        environment = environment,
        readUtkastRepository = UtkastApiRepository(database),
        writeUtkastRepository = UtkastRepository(database),
        utkastFetcher = UtkastFetcher(
            digiSosBaseUrl = environment.digisosBaseUrl,
            aapBaseUrl = "http://innsending.aap",
            httpClient = httpClient,
            digisosClientId = environment.digisosClientId,
            tokendingsService = TokendingsServiceBuilder.buildTokendingsService(),
            aapClientId = environment.aapClientId,
        ),
        utkastDeleter
    )
}

private fun startApplicaiton(
    environment: Environment,
    readUtkastRepository: UtkastApiRepository,
    writeUtkastRepository: UtkastRepository,
    utkastFetcher: UtkastFetcher,
    utkastDeleter: PeriodicUtkastDeleter
) = KafkaApplication.build {
    kafkaConfig {
        groupId = environment.groupId
        readTopic(environment.utkastTopic)
    }

    ktorModule {
        utkastApi(readUtkastRepository, utkastFetcher)
    }

    subscribers(
        UtkastCreatedSubscriber(writeUtkastRepository),
        UtkastUpdatedSubscriber(writeUtkastRepository),
        UtkastDeletedSubscriber(writeUtkastRepository)
    )

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
