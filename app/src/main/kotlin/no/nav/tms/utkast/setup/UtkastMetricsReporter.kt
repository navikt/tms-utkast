package no.nav.tms.utkast.setup

import io.prometheus.client.Counter

object UtkastMetricsReporter {

    private const val NAMESPACE = "tms_utkast_v1"

    private const val UTKAST_OPPRETTET_NAME = "utkast_opprettet"
    private const val UTKAST_ENDRET_NAME = "utkast_endret"
    private const val UTKAST_SLETTET_NAME = "utkast_slettet"


    private val UTKAST_OPPRETTET: Counter = Counter.build()
        .name(UTKAST_OPPRETTET_NAME)
        .namespace(NAMESPACE)
        .help("Antall utkast opprettet")
        .register()


    private val UTKAST_ENDRET: Counter = Counter.build()
        .name(UTKAST_ENDRET_NAME)
        .namespace(NAMESPACE)
        .help("Antall utkast endret")
        .register()


    private val UTKAST_SLETTET: Counter = Counter.build()
        .name(UTKAST_SLETTET_NAME)
        .namespace(NAMESPACE)
        .help("Antall utkast slettet")
        .register()

    fun countUtkastOpprettet() = UTKAST_OPPRETTET.inc()
    fun countUtkastEndret() = UTKAST_ENDRET.inc()
    fun countUtkastSlettet() = UTKAST_SLETTET.inc()
}
