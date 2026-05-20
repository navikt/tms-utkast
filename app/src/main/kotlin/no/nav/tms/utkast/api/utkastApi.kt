package no.nav.tms.utkast.api

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.withLoggingContext

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.tms.common.metrics.installTmsApiMetrics
import no.nav.tms.utkast.api.FetchResult.Companion.responseStatus
import no.nav.tms.utkast.api.FetchResult.Companion.utkast
import no.nav.tms.utkast.setup.logExceptionAsWarning
import no.nav.tms.common.observability.ApiMdc
import no.nav.tms.common.observability.Domain
import no.nav.tms.token.support.user.token.exchange.UserTokenExchangeException
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance
import no.nav.tms.token.support.user.token.verification.UserPrincipal
import no.nav.tms.token.support.user.token.verification.userToken
import java.text.DateFormat
import java.util.*

internal fun Application.utkastApi(
    utkastRepository: UtkastApiRepository,
    utkastFetcher: UtkastFetcher,
    installAuthenticatorsFunction: Application.() -> Unit = installAuth(),
) {
    installAuthenticatorsFunction()

    installTmsApiMetrics {
        setupMetricsRoute = false
    }
    install(ApiMdc) {
        applicationDomain = Domain.utkast
    }
    install(StatusPages) {
        exception<Exception> { call, cause ->
            when (cause) {
                is UserTokenExchangeException -> {
                    logExceptionAsWarning(
                        logInfo = cause.message ?: "Ukjent feil mot tokendings",
                        teamLogInfo = cause.message ?: "Ukjent feil mot tokendings",
                        cause = cause
                    )
                    call.respond(HttpStatusCode.ServiceUnavailable)
                }

                else -> {
                    logExceptionAsWarning(
                        logInfo = "Ukjent feil",
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
            route("v2/utkast") {
                get {
                    withLoggingContext("lang" to localeCode) {
                        val externalresult = utkastFetcher.allExternal(userToken)
                        val internal =
                            utkastRepository.getUtkastForIdent(
                                userIdent,
                                localeParam
                            )

                        call.respond(
                            status = externalresult.responseStatus(),
                            internal + externalresult.utkast()
                        )
                    }
                }
                get("antall") {
                    val externalresult = utkastFetcher.allExternal(userToken)
                    val internal = utkastRepository.getUtkastForIdent(userIdent)

                    call.respond(
                        status = externalresult.responseStatus(),
                        jacksonObjectMapper().createObjectNode()
                            .put("antall", (externalresult.utkast() + internal).size)
                    )
                }
            }
        }
    }
}

private fun installAuth(): Application.() -> Unit = {
    authentication {
        userToken {
            levelOfAssurance = LevelOfAssurance.Substantial
        }
    }
}

private val RoutingContext.userIdent get() = call.principal<UserPrincipal>()?.ident
    ?: throw IllegalStateException("Fant ikke UserPrincipal i context")

private val RoutingContext.userToken get() = call.principal<UserPrincipal>()?.accessToken
    ?: throw IllegalStateException("Fant ikke UserPrincipal i context")


private val RoutingContext.localeParam
    get() = call.request.queryParameters["la"]?.let {
        Locale.of(it)
    }

private val RoutingContext.localeCode
    get() = call.request.queryParameters["la"] ?: "nb"
