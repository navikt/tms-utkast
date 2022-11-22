package no.nav.tms.utkast.builder

import java.net.MalformedURLException
import java.net.URL

object UtkastValidator {
    private const val BASE_16 = "[0-9a-fA-F]"
    private val UUID_PATTERN = "^$BASE_16{8}-$BASE_16{4}-$BASE_16{4}-$BASE_16{4}-$BASE_16{12}$".toRegex()

    private const val BASE_32_ULID = "[0-9ABCDEFGHJKMNPQRSTVWXYZabcdefghjkmnpqrstvwxyz]"
    private val ULID_PATTERN = "^[0-7]$BASE_32_ULID{25}$".toRegex()

    const val tittelMaxLength = 120
    const val identMaxLength = 11

    fun validateEventId(eventId: String): String {
        if (UUID_PATTERN.containsMatchIn(eventId) || ULID_PATTERN.containsMatchIn(eventId)) {
            return eventId
        } else {
            throw FieldValidationException("Feltet `eventId` må enten være UUID eller ULID.")
        }
    }

    fun validateIdent(ident: String) = validateMaxLength(ident, "ident", identMaxLength)

    fun validateTittel(tittel: String) = validateMaxLength(tittel, "tittel", tittelMaxLength)

    fun validateLink(link: String): String {
        try {
            URL(link)
            return link
        } catch (e: MalformedURLException) {
            throw FieldValidationException("Feltet `link` må være en gyldig URL.")
        }
    }

    private fun validateMaxLength(field: String, fieldName: String, maxLength: Int): String {
        if (field.length <= maxLength) {
            return field
        } else {
            throw FieldValidationException("Feltet `$fieldName` kan ikke overskride $maxLength tegn.")
        }
    }
}

class FieldValidationException(message: String): RuntimeException(message)
