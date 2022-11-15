package no.nav.tms.utkast.database

import LocalPostgresDatabase
import alleUtkast
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.date.shouldNotBeAfter
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tms.utkast.assert
import no.nav.tms.utkast.config.LocalDateTimeHelper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import createUtkastTestPacket
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
                null
                utkast.opprettet shouldBeCaSameAs LocalDateTimeHelper.nowAtUtc()
                utkast.sistEndret shouldBe null
                utkast.slettet shouldBe null
            }
        }
    }

    @Test
    fun updateUtkast() {
        val testEventId = "77fhs"
        utkastRepository.createUtkast(createUtkastTestPacket(eventId = testEventId, testFnr))
        utkastRepository.createUtkast(createUtkastTestPacket(eventId = "qqeedd2", testFnr))
        utkastRepository.updateUtkast(testEventId)
        database.list { alleUtkast }.assert {
            size shouldBe 2
            find { it.eventId == testEventId }.assert {
                require(this != null)
                sistEndret shouldBeCaSameAs LocalDateTimeHelper.nowAtUtc()
                slettet shouldBe null
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
    this shouldBeAfter LocalDateTimeHelper.nowAtUtc().minusMinutes(2)
    this shouldNotBeAfter LocalDateTimeHelper.nowAtUtc()
}
