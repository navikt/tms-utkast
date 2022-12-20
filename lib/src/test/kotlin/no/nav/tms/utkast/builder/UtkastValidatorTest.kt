package no.nav.tms.utkast.builder

import de.huxhorn.sulky.ulid.ULID
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test
import java.util.*

internal class UtkastValidatorTest {

    @Test
    fun `godtar kun UUID og ULID som utkastId`() {
        val uuid = UUID.randomUUID().toString()
        val ulid = ULID().nextULID()
        val neither = "Hubba Bubba"

        shouldNotThrowAny {
            UtkastValidator.validateUtkastId(uuid)
            UtkastValidator.validateUtkastId(ulid)
        }

        shouldThrow<FieldValidationException> {
            UtkastValidator.validateUtkastId(neither)
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
