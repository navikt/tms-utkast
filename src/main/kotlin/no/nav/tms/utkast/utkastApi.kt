package no.nav.tms.utkast

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.tms.token.support.authentication.installer.installAuthenticators
import no.nav.tms.token.support.tokenx.validation.user.TokenXUserFactory
import no.nav.tms.utkast.database.UtkastRepository
import java.text.DateFormat

internal fun Application.utkastApi(
    utkastRepository: UtkastRepository,
    installAuthenticatorsFunction: Application.() -> Unit = installAuth(),
) {
    installAuthenticatorsFunction()

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            dateFormat = DateFormat.getDateTimeInstance()
        }
    }

    routing {
        authenticate {
            route("utkast") {
                get {
                    val ident = TokenXUserFactory.createTokenXUser(call).ident
                    call.respond(utkastRepository.getUtkast(ident))
                }
            }
        }
    }
}

private fun installAuth(): Application.() -> Unit = {
    installAuthenticators {
        installTokenXAuth {
            setAsDefault = true
        }
    }
}