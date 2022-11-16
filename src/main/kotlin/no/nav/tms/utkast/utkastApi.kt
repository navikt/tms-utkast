package no.nav.tms.utkast

import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.tms.utkast.database.UtkastRepository

internal fun Application.utkastApi(utkastRepository: UtkastRepository) {

    install(ContentNegotiation) {
        jackson { }
    }

    routing {
        route("utkast") {
            get {
                call.respond(utkastRepository.getUtkast("ghjgk"))
            }
        }
    }
}