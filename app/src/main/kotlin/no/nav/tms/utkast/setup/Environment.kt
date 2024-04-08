package no.nav.tms.utkast.setup

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import no.nav.tms.common.util.config.StringEnvVar.getEnvVar

data class Environment(
    val groupId: String = getEnvVar("GROUP_ID"),
    val dbHost: String = getEnvVar("DB_HOST"),
    val dbPort: String = getEnvVar("DB_PORT"),
    val dbName: String = getEnvVar("DB_DATABASE"),
    val dbUser: String = getEnvVar("DB_USERNAME"),
    val dbPassword: String = getEnvVar("DB_PASSWORD"),
    val dbUrl: String = getDbUrl(dbHost, dbPort, dbName),
    val kafkaBrokers: String = getEnvVar("KAFKA_BROKERS"),
    val kafkaSchemaRegistry: String = getEnvVar("KAFKA_SCHEMA_REGISTRY"),
    val utkastTopic: String = getEnvVar("RAPID_TOPIC"),
    val digisosClientId: String= getEnvVar("DIGISOS_CLIENT_ID"),
    val digisosBaseUrl: String = getEnvVar("DIGISOS_BASE_URL"),
    val aapClientId : String = getEnvVar("AAP_CLIENT_ID")
)

fun getDbUrl(host: String, port: String, name: String): String {
    return if (host.endsWith(":$port")) {
        "jdbc:postgresql://${host}/$name"
    } else {
        "jdbc:postgresql://${host}:${port}/${name}"
    }
}

fun HttpClientConfig<*>.configureJackson() {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
}



