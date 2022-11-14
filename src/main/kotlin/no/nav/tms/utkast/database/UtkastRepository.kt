package no.nav.tms.utkast.database

import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.rapids_rivers.JsonMessage
import org.postgresql.util.PGobject
import java.time.LocalDateTime

class UtkastRepository(private val dataSource: HikariDataSource) {
    fun createUtkast(packet: String) {
        sessionOf(dataSource).run(
            queryOf(
                "INSERT INTO utkast (packet, opprettet) values (:packet,:opprettet)",
                mapOf("packet" to packet.jsonB(), "opprettet" to LocalDateTime.now())
            ).asUpdate
        )
    }

}

private fun String.jsonB() = PGobject().apply {
    type = "jsonb"
    value = this@jsonB
}
