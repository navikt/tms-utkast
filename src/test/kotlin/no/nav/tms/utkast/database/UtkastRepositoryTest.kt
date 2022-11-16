package no.nav.tms.utkast.database

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotliquery.queryOf
import no.nav.tms.utkast.LocalPostgresDatabase
import no.nav.tms.utkast.alleUtkast
import no.nav.tms.utkast.assert
import no.nav.tms.utkast.config.LocalDateTimeHelper
import no.nav.tms.utkast.createUtkastTestPacket
import no.nav.tms.utkast.shouldBeCaSameAs
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

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
        utkastRepository.updateUtkast("123", update(oppdatertTittel))

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

    @Test
    fun `utkast for ident`() {
        val eventId = "ajfslkf"
        val excpectedTittel = "Utkast: Søknad om dagpenger"
        val expectedLink = "https://utkast.test/$eventId"
        val slettEventId = "77fii"
        val oppdaterEventId = "77fhs"
        utkastRepository.createUtkast(createUtkastTestPacket(eventId = slettEventId, testFnr))
        utkastRepository.createUtkast(createUtkastTestPacket(eventId = oppdaterEventId, testFnr))
        utkastRepository.createUtkast(
            createUtkastTestPacket(
                eventId = eventId,
                testFnr,
                tittel = excpectedTittel,
                link = expectedLink
            )
        )
        utkastRepository.createUtkast(createUtkastTestPacket(eventId = "qqeedd8", ident = "99887766"))
        utkastRepository.updateUtkast(oppdaterEventId, update("shinyyyy"))
        utkastRepository.deleteUtkast(slettEventId)

        utkastRepository.getUtkast(testFnr).assert {
            size shouldBe 2
            find { utkast -> utkast.eventId == eventId }.assert {
                require(this != null)
                this.tittel shouldBe excpectedTittel
                this.link shouldBe expectedLink
            }
            find { utkast -> utkast.eventId == oppdaterEventId }.assert {
                require(this!=null)
                this.tittel shouldBe "shinyyyy"
                this.sistEndret shouldNotBe null
            }
        }
    }
}

fun update(oppdatertTittel: String): ObjectNode = ObjectMapper().createObjectNode().apply {
    replace("tittel", TextNode.valueOf(oppdatertTittel))
    replace("link", TextNode.valueOf("https://nei.takk"))
}