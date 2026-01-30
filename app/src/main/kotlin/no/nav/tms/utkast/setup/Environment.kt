package no.nav.tms.utkast.setup

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import no.nav.tms.common.util.config.StringEnvVar.getEnvVar

data class Environment(
    val groupId: String = getEnvVar("GROUP_ID"),
    val jdbcUrl: String = jdbcUrl(),
    val kafkaBrokers: String = getEnvVar("KAFKA_BROKERS"),
    val kafkaSchemaRegistry: String = getEnvVar("KAFKA_SCHEMA_REGISTRY"),
    val utkastTopic: String = getEnvVar("KAFKA_TOPIC"),
    val digisosClientId: String= getEnvVar("DIGISOS_CLIENT_ID"),
    val digisosBaseUrl: String = getEnvVar("DIGISOS_BASE_URL"),
    val aapClientId : String = getEnvVar("AAP_CLIENT_ID")
)

private fun jdbcUrl(): String {
    val host: String = getEnvVar("DB_HOST")
    val name: String = getEnvVar("DB_DATABASE")
    val user: String = getEnvVar("DB_USERNAME")
    val password: String = getEnvVar("DB_PASSWORD")

    return "jdbc:postgresql://${host}/$name?user=$user&password=$password"
}


fun HttpClientConfig<*>.configureClient() {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
    install(HttpTimeout)
}



