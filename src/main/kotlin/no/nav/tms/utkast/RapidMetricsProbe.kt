package no.nav.tms.utkast

import no.nav.personbruker.dittnav.common.metrics.MetricsReporter

private const val METRIC_NAMESPACE = "tms.utkast.v1"

class RapidMetricsProbe(private val metricsReporter: MetricsReporter) {

    suspend fun countUtkastReceived() {
        metricsReporter.registerDataPoint(
            measurementName = "$METRIC_NAMESPACE.utkast.received",
            fields = counterField(),
            tags = mapOf()
        )
    }
    private fun counterField(): Map<String, Int> = listOf("counter" to 1).toMap()

}
