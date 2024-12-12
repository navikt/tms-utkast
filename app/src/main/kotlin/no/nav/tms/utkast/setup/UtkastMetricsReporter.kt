package no.nav.tms.utkast.setup

import io.prometheus.metrics.core.metrics.Counter

object UtkastMetricsReporter {

    private const val NAMESPACE = "tms_utkast_v1"

    private const val UTKAST_OPPRETTET_NAME = "${NAMESPACE}_utkast_opprettet"
    private const val UTKAST_ENDRET_NAME = "${NAMESPACE}_utkast_endret"
    private const val UTKAST_SLETTET_NAME = "${NAMESPACE}_utkast_slettet"


    private val UTKAST_OPPRETTET: Counter = Counter.builder()
        .name(UTKAST_OPPRETTET_NAME)
        .help("Antall utkast opprettet")
        .register()


    private val UTKAST_ENDRET: Counter = Counter.builder()
        .name(UTKAST_ENDRET_NAME)
        .help("Antall utkast endret")
        .register()


    private val UTKAST_SLETTET: Counter = Counter.builder()
        .name(UTKAST_SLETTET_NAME)
        .help("Antall utkast slettet")
        .register()

    fun countUtkastOpprettet() = UTKAST_OPPRETTET.inc()
    fun countUtkastEndret() = UTKAST_ENDRET.inc()
    fun countUtkastSlettet() = UTKAST_SLETTET.inc()
}
