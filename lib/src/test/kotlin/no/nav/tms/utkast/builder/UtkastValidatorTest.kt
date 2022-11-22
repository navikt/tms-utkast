package no.nav.tms.utkast.builder

import de.huxhorn.sulky.ulid.ULID
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test
import java.util.*

internal class UtkastValidatorTest {

    @Test
    fun `godtar kun UUID og ULID som eventId`() {
        val uuid = UUID.randomUUID().toString()
        val ulid = ULID().nextULID()
        val neither = "Hubba Bubba"

        shouldNotThrowAny {
            UtkastValidator.validateEventId(uuid)
            UtkastValidator.validateEventId(ulid)
        }

        shouldThrow<FieldValidationException> {
            UtkastValidator.validateEventId(neither)
        }
    }

    @Test
    fun `godtar kun tittel innen en viss lenge`() {
        val short = "abc"
        val long = "a".repeat(UtkastValidator.tittelMaxLength + 1)

        shouldNotThrowAny {
            UtkastValidator.validateTittel(short)
        }

        shouldThrow<FieldValidationException> {
            UtkastValidator.validateTittel(long)
        }
    }

    @Test
    fun `godtar ikke ugyldig lenke`() {
        val validLink = "http://some.domain"
        val invalidLink = "htp::/some.domain"

        shouldNotThrowAny {
            UtkastValidator.validateLink(validLink)
        }

        shouldThrow<FieldValidationException> {
            UtkastValidator.validateLink(invalidLink)
        }
    }

    @Test
    fun `godtar ikke for lang ident`() {
        val identish = "01010100000"
        val other = "12345678912345789"

        shouldNotThrowAny {
            UtkastValidator.validateIdent(identish)
        }

        shouldThrow<FieldValidationException> {
            UtkastValidator.validateIdent(other)
        }
    }
}
