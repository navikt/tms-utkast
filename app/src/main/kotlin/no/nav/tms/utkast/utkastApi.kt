package no.nav.tms.utkast

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import no.nav.tms.token.support.authentication.installer.installAuthenticators
import no.nav.tms.token.support.tokenx.validation.user.TokenXUserFactory
import no.nav.tms.utkast.config.logExceptionAsWarning
import no.nav.tms.utkast.database.DatabaseException
import no.nav.tms.utkast.database.UtkastRepository
import java.text.DateFormat
import java.util.*

internal fun Application.utkastApi(
    utkastRepository: UtkastRepository,
    digisosHttpClient: DigisosHttpClient,
    installAuthenticatorsFunction: Application.() -> Unit = installAuth(),
) {
    installAuthenticatorsFunction()

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is DatabaseException -> {
                    logExceptionAsWarning(
                        unsafeLogInfo = "Henting fra database feilet for kall til ${call.request.uri}",
                        secureLogInfo = cause.details,
                        cause = cause
                    )
                    call.respond(HttpStatusCode.InternalServerError)
                }

                is DigisosException -> {
                    val ident = TokenXUserFactory.createTokenXUser(call)
                    logExceptionAsWarning(
                        unsafeLogInfo = cause.message!!,
                        secureLogInfo = "${cause.message} for $ident",
                        cause = cause.originalException
                    )
                    call.respond(HttpStatusCode.ServiceUnavailable)
                }

                else -> {
                    logExceptionAsWarning(
                        unsafeLogInfo = "Ukjent feil for kall til ${call.request.uri}",
                        cause = cause
                    )
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }

        }
    }
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
                    call.respond(utkastRepository.getUtkastForIdent(userIdent, localeParam))
                }
                get("antall") {
                    val antall = utkastRepository.getUtkastForIdent(userIdent).size
                    call.respond(jacksonObjectMapper().createObjectNode().put("antall", antall))
                }
                get("digisos") {
                    call.respond(digisosHttpClient.getUtkast(accessToken))

                }
                get("digisos/antall") {
                    val antall = digisosHttpClient.getAntall(accessToken)
                    call.respond(jacksonObjectMapper().createObjectNode().put("antall", antall))
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

private val PipelineContext<Unit, ApplicationCall>.userIdent get() = TokenXUserFactory.createTokenXUser(call).ident
private val PipelineContext<Unit, ApplicationCall>.accessToken get() = TokenXUserFactory.createTokenXUser(call).tokenString


private val PipelineContext<Unit, ApplicationCall>.localeParam
    get() = call.request.queryParameters["la"]?.let {
        Locale(
            it
        )
    }