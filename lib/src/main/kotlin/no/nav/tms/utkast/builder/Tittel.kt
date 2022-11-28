package no.nav.tms.utkast.builder

import java.util.*

internal class Tittel(
    val tittel: String,
    val language: String
) {
    constructor(tittel: String, locale: Locale): this(tittel, locale.language)
}
