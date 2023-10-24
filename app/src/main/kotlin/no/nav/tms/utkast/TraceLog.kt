package no.nav.tms.utkast

import io.github.oshai.kotlinlogging.withLoggingContext

enum class Events {
    disable, enable, updated, deleted, created
}

enum class Contenttype {
    microfrontend, varsel, utkast
}

fun traceUtkast(
    id: String,
    event: Events,
    extra: Map<String, String> = emptyMap(),
    function: () -> Unit
) = withTraceLogging(id, event, Contenttype.utkast, extra) { function() }

fun withTraceLogging(
    id: String,
    event: Events,
    contenttype: Contenttype,
    extra: Map<String, String> = emptyMap(),
    function: () -> Unit
) {
    withLoggingContext(
        mapOf(
            "id" to id,
            "event" to event.name,
            "contenttype" to contenttype.name
        ) + extra
    ) { function() }
}
