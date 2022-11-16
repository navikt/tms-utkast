package no.nav.tms.utkast.database

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.TextNode
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.date.shouldNotBeAfter
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tms.utkast.LocalPostgresDatabase
import no.nav.tms.utkast.alleUtkast
import no.nav.tms.utkast.assert
import no.nav.tms.utkast.config.LocalDateTimeHelper
import no.nav.tms.utkast.createUtkastTestPacket
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class UtkastRepositoryTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val utkastRepository = UtkastRepository(database)
    private val testFnr = "12345678910"

    @AfterEach
    fun cleanup() {
        database.update {
            queryOf("delete from utkast")
        }
    }

    @Test
    fun createUtkast() {
        utkastRepository.createUtkast(createUtkastTestPacket(eventId = "qqeedd1", testFnr))
        utkastRepository.createUtkast(createUtkastTestPacket(eventId = "qqeedd2", testFnr))
        utkastRepository.createUtkast(createUtkastTestPacket(eventId = "qqeedd3", testFnr))
        database.list { alleUtkast }.assert {
            size shouldBe 3
            forEach { utkast ->
                utkast.opprettet shouldBeCaSameAs LocalDateTimeHelper.nowAtUtc()
                utkast.sistEndret shouldBe null
                utkast.slettet shouldBe null
            }
        }
    }

    @Test
    fun updateUtkast() {

        val originalTittel = "Original tittel."
        utkastRepository.createUtkast(createUtkastTestPacket("123", testFnr, tittel = originalTittel))
        utkastRepository.createUtkast(createUtkastTestPacket("456", testFnr, tittel = originalTittel))

        val oppdatertTittel = "Oppdatert tittel."

        val update = ObjectMapper().createObjectNode().apply {
            replace("tittel", TextNode.valueOf(oppdatertTittel))
            replace("link", TextNode.valueOf("https://nei.takk"))
        }

        utkastRepository.updateUtkast("123", update)

        database.list { alleUtkast }.assert {
            size shouldBe 2
            find { it.eventId == "123" }.assert {
                require(this != null)
                sistEndret shouldBeCaSameAs LocalDateTimeHelper.nowAtUtc()
                slettet shouldBe null
                //          tittel shouldBe oppdatertTittel
            }

            find { it.eventId == "456" }.assert {
                require(this != null)
                sistEndret shouldBe null
                slettet shouldBe null
                tittel shouldBe originalTittel
            }
        }
    }

    @Test
    fun deleteUtkast() {
        val testEventId = "77fhs"
        utkastRepository.createUtkast(createUtkastTestPacket(eventId = testEventId, testFnr))
        utkastRepository.createUtkast(createUtkastTestPacket(eventId = "qqeedd2", testFnr))
        utkastRepository.deleteUtkast(testEventId)
        database.list { alleUtkast }.assert {
            size shouldBe 2
            find { it.eventId == testEventId }.assert {
                require(this != null)
                slettet shouldBeCaSameAs LocalDateTimeHelper.nowAtUtc()
            }

        }
    }
}

private infix fun LocalDateTime?.shouldBeCaSameAs(expected: LocalDateTime) {
    require(this != null)
    this shouldBeAfter expected.minusMinutes(2)
    this shouldNotBeAfter expected
}
