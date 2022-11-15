package no.nav.tms.utkast.database

import LocalPostgresDatabase
import alleUtkast
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import testUtkast

internal class UtkastRepositoryTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val utkastRepository = UtkastRepository(database)
    private val testFnr = "12345678910"

    @AfterAll
    fun cleanup() {
        database.update {
            queryOf("delete from utkast")
        }
    }

    @Test
    fun createUtkast() {
        utkastRepository.createUtkast(testUtkast(eventId = "qqeedd1", testFnr))
        utkastRepository.createUtkast(testUtkast(eventId = "qqeedd2", testFnr))
        utkastRepository.createUtkast(testUtkast(eventId = "qqeedd3", testFnr))
        database.list { alleUtkast }.size shouldBe 3
    }
}