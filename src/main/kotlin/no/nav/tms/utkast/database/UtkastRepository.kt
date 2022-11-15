package no.nav.tms.utkast.database

import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tms.utkast.config.LocalDateTimeHelper
import org.postgresql.util.PGobject

class UtkastRepository(private val dataSource: HikariDataSource) {
    fun createUtkast(packet: String) =
        sessionOf(dataSource).run(
            queryOf(
                "INSERT INTO utkast (packet, opprettet) values (:packet,:opprettet)",
                mapOf("packet" to packet.jsonB(), "opprettet" to LocalDateTimeHelper.nowAtUtc())
            ).asUpdate
        )
}

private fun String.jsonB() = PGobject().apply {
    type = "jsonb"
    value = this@jsonB
}
