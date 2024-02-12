package no.nav.tms.utkast.setup

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage

private val secureLog = KotlinLogging.logger("secureLog")
private val log = KotlinLogging.logger {}

fun withErrorLogging(function: ErrorContext.() -> Any) {
    val errorContext = ErrorContext()
    try {
        errorContext.function()
    } catch (exception: Throwable) {
        log.error { errorContext.message }
        secureLog.error {
            """
            ${errorContext.message}\n
            ident: ${errorContext.ident}\n
            "origin": $exception
        """
        }
    }
}

fun logExceptionAsWarning(unsafeLogInfo: String, cause: Throwable, secureLogInfo: String? = null) {
    log.warn { unsafeLogInfo }
    secureLog.error {
        """
            ${secureLogInfo?.let { secureLogInfo }}
            "origin": ${cause.stackTrace.contentToString()}
        """
    }
}

class ErrorContext {
    var ident: String? = null
        get() = field ?: "Ukjent"
        set(value) {
            if (field == null)
                field = value
        }

    var message: String? = null
        get() = field ?: ""
        set(value) {
            if (field == null)
                field = value
        }

    var jsonMessage: JsonMessage? = null
        set(value) {
            field = value
            ident = value.ident()
            message = "Feil i utkast med id ${value.utkastId()}"
        }
}


private fun JsonMessage?.utkastId(): String =
    this?.let { packet ->
        packet["utkastId"].asText("Ukjent")
    } ?: "ukjent"


private fun JsonMessage?.ident(): String? =
    this?.let { packet ->
        packet["ident"].asText("Ukjent")
    } ?: "ukjent"
