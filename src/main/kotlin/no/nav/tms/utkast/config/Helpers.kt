package no.nav.tms.utkast.config

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

object LocalDateTimeHelper {
    val EPOCH_START: LocalDateTime = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC)
    fun nowAtUtc(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
}