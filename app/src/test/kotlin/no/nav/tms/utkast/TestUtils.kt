package no.nav.tms.utkast

import io.mockk.mockk
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tms.utkast.database.UtkastRepository

internal fun setupSinks(
    rapidsConnection: RapidsConnection,
    utkastRepository: UtkastRepository,
    metricsProbe: RapidMetricsProbe = mockk(relaxed = true)
) {
    UtkastUpdatedSink(
        rapidsConnection = rapidsConnection,
        utkastRepository = utkastRepository,
        rapidMetricsProbe = metricsProbe
    )
    UtkastDeletedSink(
        rapidsConnection = rapidsConnection,
        utkastRepository = utkastRepository,
        rapidMetricsProbe = metricsProbe
    )
    UtkastCreatedSink(
        rapidsConnection = rapidsConnection,
        utkastRepository = utkastRepository,
        rapidMetricsProbe = metricsProbe
    )
}