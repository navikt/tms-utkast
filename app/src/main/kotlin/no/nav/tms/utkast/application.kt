package no.nav.tms.utkast

import io.ktor.client.*
import kotlinx.coroutines.runBlocking
import no.nav.tms.common.postgres.Postgres
import no.nav.tms.kafka.application.Domain
import no.nav.tms.kafka.application.KafkaApplication
import no.nav.tms.token.support.tokendings.exchange.TokendingsServiceBuilder
import no.nav.tms.utkast.api.UtkastApiRepository
import no.nav.tms.utkast.api.UtkastFetcher
import no.nav.tms.utkast.api.utkastApi
import no.nav.tms.utkast.expiry.PeriodicUtkastDeleter
import no.nav.tms.utkast.setup.Environment
import no.nav.tms.utkast.setup.configureClient
import no.nav.tms.utkast.sink.UtkastRepository
import no.nav.tms.utkast.sink.UtkastCreatedSubscriber
import no.nav.tms.utkast.sink.UtkastDeletedSubscriber
import no.nav.tms.utkast.sink.UtkastUpdatedSubscriber
import org.flywaydb.core.Flyway

fun main() {
    val environment = Environment()

    val httpClient = HttpClient {
        configureClient()
    }

    val database = Postgres.connectToJdbcUrl(environment.jdbcUrl)

    val utkastDeleter = PeriodicUtkastDeleter(database)

    val readUtkastRepository = UtkastApiRepository(database)
    val writeUtkastRepository = UtkastRepository(database)

    val utkastFetcher = UtkastFetcher(
        digiSosBaseUrl = environment.digisosBaseUrl,
        aapBaseUrl = "http://innsending.aap",
        httpClient = httpClient,
        digisosClientId = environment.digisosClientId,
        tokendingsService = TokendingsServiceBuilder.buildTokendingsService(),
        aapClientId = environment.aapClientId,
    )

    KafkaApplication.build {
        kafkaConfig {
            groupId = environment.groupId
            readTopic(environment.utkastTopic)
        }

        ktorModule {
            utkastApi(readUtkastRepository, utkastFetcher)
        }
        minSideMdc {
            this.idFieldName = "utkastId"
            this.domain = Domain.utkast
            this.producedByFieldName = "producer"
            this.allowMissingProducerField=true
        }

        subscribers(
            UtkastCreatedSubscriber(writeUtkastRepository),
            UtkastUpdatedSubscriber(writeUtkastRepository),
            UtkastDeletedSubscriber(writeUtkastRepository)
        )

        onStartup {
            Flyway.configure()
                .dataSource(database.dataSource)
                .load()
                .migrate()
        }

        onReady {
            utkastDeleter.start()
        }

        onShutdown {
            runBlocking {
                utkastDeleter.stop()
            }
        }
    }.start()
}
