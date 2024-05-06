package no.nav.tms.utkast

import io.ktor.http.*
import io.ktor.server.routing.*
import no.nav.tms.common.testutils.RouteProvider


abstract class DigisosBaseRoute(statusCode: HttpStatusCode) :
    RouteProvider(path = "/dittnav/pabegynte/aktive", statusCode = statusCode, routeMethodFunction = Routing::get) {
    override fun content(): String = ""

}
internal class DigisosErrorRoute: DigisosBaseRoute(HttpStatusCode.InternalServerError)
internal class DigisosTestRoute(private val expextedUtkastData: List<UtkastData> = emptyList()) :
    DigisosBaseRoute(statusCode = HttpStatusCode.OK) {
    override fun content(): String = expextedUtkastData.joinToString(
        prefix = "[",
        postfix = "]",
        separator = ","
    ) { it.toDigisosResponse() }

}

internal class AapTestRoute(private val expextedUtkastData: UtkastData? = null) :
    RouteProvider(
        path = "/mellomlagring/søknad/finnes",
        routeMethodFunction = Routing::get,
        statusCode = statusCode(expextedUtkastData)
    ) {
    override fun content(): String = expextedUtkastData?.toAapResponse() ?: ""

    companion object {

        private fun statusCode(expextedUtkastData: UtkastData?) =
            if (expextedUtkastData == null) HttpStatusCode.NotFound else HttpStatusCode.OK

    }
}
