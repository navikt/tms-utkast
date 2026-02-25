package no.nav.tms.utkast.expiry

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.queryOf
import no.nav.tms.common.kubernetes.PodLeaderElection
import no.nav.tms.common.postgres.PostgresDatabase
import no.nav.tms.common.util.scheduling.PeriodicJob
import no.nav.tms.utkast.sink.LocalDateTimeHelper.nowAtUtc
import no.nav.tms.utkast.sink.ZonedDateTimeHelper
import java.time.Duration
import java.time.LocalDateTime

class PeriodicUtkastDeleter(
    private val database: PostgresDatabase,
    private val leaderElection: PodLeaderElection = PodLeaderElection(),
    interval: Duration = Duration.ofMinutes(5),
) : PeriodicJob(interval) {

    private val log = KotlinLogging.logger { }

    override val job = initializeJob {
        if (leaderElection.isLeader()) {
            markUtkastPastExpiryAsDeleted()
            removeOldUtkast()
        }
    }

    private fun markUtkastPastExpiryAsDeleted() {
        try {

            log.debug { "Markerer utkast forbi slettesEtter som slettet" }

            val expired = updateUtkastPastExpiry()

            if (expired > 0) {
                log.info { "Markerete $expired utkast som slettet basert på angitt slettesEtter-tidspunkt" }
            } else {
                log.debug { "Fant ingen utkast forbi sitt slettesEtter-tidspunkt" }
            }
        } catch (e: Exception) {
            log.error(e) { "Uventet feil ved fjerning av gamle utkast" }
        }
    }

    private fun removeOldUtkast() {
        try {
            val threshold = nowAtUtc().minusMonths(3)

            log.debug { "Sletter utkast opprettet før [$threshold].." }

            val deleted = deleteUtkastOlderThan(threshold)

            if (deleted > 0) {
                log.info { "Slettet $deleted utkast opprettet før [$threshold]." }
            } else {
                log.debug { "Fant ingen utkast opprettet før [$threshold]." }
            }
        } catch (e: Exception) {
            log.error(e) { "Uventet feil ved fjerning av gamle utkast" }
        }
    }

    private fun updateUtkastPastExpiry(): Int {
        return database.update {
            queryOf(
                "update utkast set slettet = :nowLDT where slettesEtter < :nowZDT and slettet is null",
                mapOf(
                    "nowLDT" to nowAtUtc(),
                    "nowZDT" to ZonedDateTimeHelper.nowAtUtc()
                )
            )
        }
    }

    private fun deleteUtkastOlderThan(threshold: LocalDateTime): Int {
        return database.single {
            queryOf(
                """
                    with deleted as (
                        delete from utkast where opprettet < :threshold returning *
                    ) select count(*) as antall_slettet from deleted
                """.trimMargin(),
                mapOf("threshold" to threshold)
            ).map {
                it.int("antall_slettet")
            }
        }
    }
}
