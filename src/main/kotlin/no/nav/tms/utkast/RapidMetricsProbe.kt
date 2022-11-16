package no.nav.tms.utkast

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.personbruker.dittnav.common.metrics.MetricsReporter

private const val METRIC_NAMESPACE = "tms.utkast.v1"

class RapidMetricsProbe(private val metricsReporter: MetricsReporter) {

    private val IOScope = CoroutineScope(Dispatchers.IO)

    fun countUtkastReceived() = IOScope.launch {
        metricsReporter.registerDataPoint(
            measurementName = "$METRIC_NAMESPACE.utkast.received",
            fields = counterField(),
            tags = mapOf()
        )
    }

    fun countUtkastChanged(operationName: String) = IOScope.launch {
        metricsReporter.registerDataPoint(
            measurementName = "$METRIC_NAMESPACE.utkast.changed",
            fields = counterField(),
            tags = mapOf("operation" to operationName)
        )
    }

    private fun counterField(): Map<String, Int> = listOf("counter" to 1).toMap()
}
