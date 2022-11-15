package no.nav.tms.utkast.database

import LocalPostgresDatabase
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

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
        sessionOf(ds).run(
            queryOf("select * from utkast")
                .map { row ->
                    row.string("packet")
                }.asList
        ).size shouldBe 3
    }
}

private fun testUtkast(
    eventId: String,
    fnr: String,
    eventName: String = "create",
    link: String = "https://test.link",
    tittel: String = "Utkasttittel"
) = """
    {
     "@event_name": "$eventName",
    "event_id": "$eventId",
    "ident": "$fnr",
    "link": "$link",
    "tittel": "$tittel"
    }
""".trimIndent()