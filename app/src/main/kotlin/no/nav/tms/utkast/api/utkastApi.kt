package no.nav.tms.utkast.api

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
import nav.no.tms.common.metrics.installTmsApiMetrics
import no.nav.tms.token.support.tokenx.validation.tokenX
import no.nav.tms.token.support.tokenx.validation.user.TokenXUserFactory
import no.nav.tms.utkast.api.FetchResult.Companion.responseStatus
import no.nav.tms.utkast.api.FetchResult.Companion.utkast
import no.nav.tms.utkast.setup.logExceptionAsWarning
import no.nav.tms.utkast.sink.DatabaseException
import observability.ApiMdc
import observability.Contenttype
import observability.withApiTracing
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
    install(ApiMdc)
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

                else -> {
                    logExceptionAsWarning(
                        unsafeLogInfo = "Ukjent feil",
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
                    withApiMDC("utkast/utkast", extra = mapOf("lang" to localeCode)) {
                        call.respond(utkastRepository.getUtkastForIdent(userIdent, localeParam))
                    }
                }
                get("antall") {
                    withApiMDC("utkast/antall") {
                        val antall = utkastRepository.getUtkastForIdent(userIdent).size
                        call.respond(jacksonObjectMapper().createObjectNode().put("antall", antall))
                    }
                }
                get("digisos") {
                    withApiMDC("utkast/digisos") {
                        call.respond(utkastFetcher.digisos(accessToken).utkast)
                    }

                }
                get("digisos/antall") {
                    withApiMDC("utkast/digisos/antall") {
                        val antall = utkastFetcher.digisos(accessToken).utkast.size
                        call.respond(jacksonObjectMapper().createObjectNode().put("antall", antall))
                    }
                }
            }
            route("v2/utkast") {
                get {
                    withApiMDC("v2/utkast", extra = mapOf("lang" to localeCode)) {
                        val externalresult = utkastFetcher.allExternal(accessToken)
                        val internal =
                            utkastRepository.getUtkastForIdent(
                                userIdent
                            )

                        call.respond(
                            status = externalresult.responseStatus(),
                            internal + externalresult.utkast()
                        )
                    }
                }
                get("antall") {
                    withApiMDC("v2/utkast/antall") {
                        val externalresult = utkastFetcher.allExternal(accessToken)
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
}

private fun installAuth(): Application.() -> Unit = {
    authentication {
        tokenX {
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

private val PipelineContext<Unit, ApplicationCall>.localeCode
    get() = call.request.queryParameters["la"] ?: "nb"

private suspend fun withApiMDC(
    route: String,
    extra: Map<String, String> = emptyMap(),
    method: String = "GET",
    function: suspend () -> Unit
) {
    withApiTracing(route = route, contenttype = Contenttype.utkast, extra = extra, method = method) {
        function()
    }
}
