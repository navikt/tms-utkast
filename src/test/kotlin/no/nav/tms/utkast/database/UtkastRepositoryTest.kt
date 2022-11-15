package no.nav.tms.utkast.database

import LocalPostgresDatabase
import alleUtkast
import io.kotest.matchers.shouldBe
import kotliquery.sessionOf
import org.junit.jupiter.api.Test
import testUtkast

internal class UtkastRepositoryTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val ds = database.dataSource
    private val utkastRepository = UtkastRepository(ds)
    private val testFnr = "12345678910"

    @Test
    fun createUtkast() {
        utkastRepository.createUtkast(testUtkast(eventId = "qqeedd1", testFnr))
        utkastRepository.createUtkast(testUtkast(eventId = "qqeedd2", testFnr))
        utkastRepository.createUtkast(testUtkast(eventId = "qqeedd3", testFnr))
        sessionOf(ds).alleUtkast().size shouldBe 3
    }
}