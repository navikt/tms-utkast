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
        utkastRepository.createUtkast(createUtkastTestPacket(utkastId = "qqeedd1", ident = testFnr))
        utkastRepository.createUtkast(createUtkastTestPacket(utkastId = "qqeedd1", testFnr))
        utkastRepository.createUtkast(createUtkastTestPacket(utkastId = "qqeedd2", testFnr))
        utkastRepository.createUtkast(createUtkastTestPacket(utkastId = "qqeedd3", testFnr))
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
        utkastRepository.createUtkast(
            createUtkastTestPacket(
                "123",
                testFnr,
                tittel = originalTittel,
                metrics = metrics("11-08", "Skjemnavn med Å")
            )
        )
        utkastRepository.createUtkast(createUtkastTestPacket("456", testFnr, tittel = originalTittel))

        val oppdatertTittel = "Oppdatert tittel."
        utkastRepository.updateUtkast("123", updateJson(oppdatertTittel).toString())


        database.list { alleUtkast }.assert {
            size shouldBe 2
            find { it.utkastId == "123" }.assert {
                require(this != null)
                sistEndret shouldBeCaSameAs LocalDateTimeHelper.nowAtUtc()
                slettet shouldBe null
                tittel shouldBe oppdatertTittel
                metrics!!["skjemakode"] shouldBe "11-08"
                metrics!!["skjemanavn"] shouldBe "Skjemnavn med Å"
            }

            find { it.utkastId == "456" }.assert {
                require(this != null)
                sistEndret shouldBe null
                slettet shouldBe null
                tittel shouldBe originalTittel
                metrics shouldBe null
            }
        }
    }

    @Test
    fun updateTittelI18n() {
        val originalTittelNo = "Original tittel."
        utkastRepository.createUtkast(
            createUtkastTestPacket(
                "123",
                testFnr,
                tittelI18n = mapOf("no" to originalTittelNo)
            )
        )

        val nyTittelEn = "Original title."
        utkastRepository.updateUtkastI18n("123", """{"en": "$nyTittelEn"}""")


        database.list { alleUtkast }.assert {
            find { it.utkastId == "123" }.assert {
                require(this != null)
                sistEndret shouldBeCaSameAs LocalDateTimeHelper.nowAtUtc()
                slettet shouldBe null
                tittelI18n["no"] shouldBe originalTittelNo
                tittelI18n["en"] shouldBe nyTittelEn
            }
        }
    }

    @Test
    fun deleteUtkast() {
        val testUtkastId = "77fhs"
        utkastRepository.createUtkast(createUtkastTestPacket(utkastId = testUtkastId, testFnr))
        utkastRepository.createUtkast(createUtkastTestPacket(utkastId = "qqeedd2", testFnr))
        utkastRepository.deleteUtkast(testUtkastId)
        database.list { alleUtkast }.assert {
            size shouldBe 2
            find { it.utkastId == testUtkastId }.assert {
                require(this != null)
                slettet shouldBeCaSameAs LocalDateTimeHelper.nowAtUtc()
            }

        }
    }

    @Test
    fun `utkast for ident`() {
        val utkastId = "ajfslkf"
        val excpectedTittel = "Utkast: Søknad om dagpenger"
        val excpectedTittelEn = mapOf("en" to "Draft: Application")
        val expectedLink = "https://utkast.test/$utkastId"
        val slettUtkastId = "77fii"
        val oppdaterUtkastId = "77fhs"
        utkastRepository.createUtkast(createUtkastTestPacket(utkastId = slettUtkastId, testFnr))
        utkastRepository.createUtkast(createUtkastTestPacket(utkastId = oppdaterUtkastId, testFnr))
        utkastRepository.createUtkast(
            createUtkastTestPacket(
                utkastId = utkastId,
                ident = testFnr,
                tittel = excpectedTittel,
                tittelI18n = excpectedTittelEn,
                link = expectedLink
            )
        )
        utkastRepository.createUtkast(createUtkastTestPacket(utkastId = "qqeedd8", ident = "99887766"))
        utkastRepository.updateUtkast(oppdaterUtkastId, updateJson("shinyyyy").toString())
        utkastRepository.deleteUtkast(slettUtkastId)

        utkastRepository.getUtkast(testFnr).assert {
            size shouldBe 2
            find { utkast -> utkast.utkastId == utkastId }.assert {
                require(this != null)
                tittel shouldBe excpectedTittel
                link shouldBe expectedLink
            }
            find { utkast -> utkast.utkastId == oppdaterUtkastId }.assert {
                require(this != null)
                this.tittel shouldBe "shinyyyy"
                this.sistEndret shouldNotBe null
            }
        }
    }
}

fun updateJson(oppdatertTittel: String): ObjectNode = ObjectMapper().createObjectNode().apply {
    replace("tittel", TextNode.valueOf(oppdatertTittel))
    replace("link", TextNode.valueOf("https://nei.takk"))
}


fun metrics(skjemakode: String, skjemanavn: String) = mapOf(
    "skjemakode" to skjemakode,
    "skjemanavn" to skjemanavn
)
