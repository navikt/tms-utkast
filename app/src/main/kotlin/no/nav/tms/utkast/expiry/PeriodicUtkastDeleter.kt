package no.nav.tms.utkast.expiry

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.queryOf
import no.nav.tms.common.kubernetes.PodLeaderElection
import no.nav.tms.common.util.scheduling.PeriodicJob
import no.nav.tms.utkast.setup.Database
import no.nav.tms.utkast.sink.LocalDateTimeHelper.nowAtUtc
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZonedDateTime

class PeriodicUtkastDeleter(
    private val database: Database,
    private val leaderElection: PodLeaderElection = PodLeaderElection(),
    interval: Duration = Duration.ofMinutes(5),
) : PeriodicJob(interval) {

    private val log = KotlinLogging.logger { }

    override val job = initializeJob {
        if (leaderElection.isLeader()) {
           updateExpiredVarsel()
        }
    }

    private fun updateExpiredVarsel() {
        try {
            val threshold = nowAtUtc().minusMonths(3)

            log.info { "Sletter utkast opprettet før [$threshold].." }

            val deleted = deleteUtkastOlderThan(threshold)

            if (deleted > 0) {
                log.info { "Slettet $deleted utkast." }
            } else {
                log.info { "Fant ingen utgåtte utkast." }
            }
        } catch (e: Exception) {
            log.error(e) { "Uventet feil ved prosessering av utgåtte utkast." }
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
            }.asSingle
        }
    }
}
