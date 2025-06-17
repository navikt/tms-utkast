package no.nav.tms.utkast.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*

fun ApplicationTestBuilder.initExternalServices(
    host: String,
    vararg handlers: HttpRouteConfig
) {
    externalServices {
        hosts(host) {
            routing {
                handlers.forEach(::initService)
            }
        }
    }
}

fun Routing.initService(routeConfig: HttpRouteConfig) {
    route(routeConfig.path) {
        method(routeConfig.method) {
            handle {
                routeConfig.assertionsBlock.invoke(call)
                call.respondText(routeConfig.responseContent, status = routeConfig.statusCode, contentType = routeConfig.contentType)
            }
        }
    }
}

class HttpRouteConfig(
    val path: String,
    val statusCode: HttpStatusCode = HttpStatusCode.OK,
    val method: HttpMethod = HttpMethod.Get,
    val responseContent: String = """{"ok": "ok"}""",
    val contentType: ContentType = ContentType.Application.Json,
    val assertionsBlock: suspend (ApplicationCall) -> Unit = {},
)
