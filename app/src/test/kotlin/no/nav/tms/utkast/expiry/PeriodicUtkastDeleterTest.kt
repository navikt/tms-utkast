package no.nav.tms.utkast.expiry

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotliquery.queryOf
import no.nav.tms.common.kubernetes.PodLeaderElection
import no.nav.tms.common.postgres.JsonbHelper.toJsonb
import no.nav.tms.utkast.database.LocalPostgresDatabase
import no.nav.tms.utkast.sink.LocalDateTimeHelper
import no.nav.tms.utkast.sink.LocalDateTimeHelper.nowAtUtc
import no.nav.tms.utkast.sink.Utkast
import no.nav.tms.utkast.sink.ZonedDateTimeHelper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject
import java.time.Duration.ofMinutes
import java.time.LocalDateTime
import java.time.ZonedDateTime

internal class PeriodicUtkastDeleterTest {

    private val database = LocalPostgresDatabase.getCleanInstance()
    private val leaderElection: PodLeaderElection = mockk()

    private val gammeltUtkast1 = utkast("u1", opprettet = nowAtUtc().minusMonths(4))
    private val gammeltUtkast2 = utkast("u2", opprettet = nowAtUtc().minusMonths(5))

    private val opprettet = nowAtUtc().minusMonths(2)
    private val nyereUtkast = utkast("u3", opprettet = opprettet)

    private val utkastSlettesEtterFortid = utkast("u4", slettesEtter = ZonedDateTimeHelper.nowAtUtc().minusDays(1))
    private val utkastSlettesEtterFremtid = utkast("u5", slettesEtter = ZonedDateTimeHelper.nowAtUtc().plusDays(1))
    private val utkastIngenAutomatiskSletting = utkast("u6", slettesEtter = null)

    private val objectMapper = jacksonMapperBuilder()
        .addModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()
        .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)

    @AfterEach
    fun cleanUp() {
        clearMocks(leaderElection)
        LocalPostgresDatabase.resetInstance()
    }

    @Test
    fun `fjerner gamle utkast`() = runTest {
        insertUtkast(gammeltUtkast1, gammeltUtkast2, nyereUtkast)

        coEvery { leaderElection.isLeader() } returns true

        val deleter = PeriodicUtkastDeleter(
            database = database,
            interval = ofMinutes(10),
            leaderElection = leaderElection
        )

        deleter.start()
        delayUntilNRemains(1)
        deleter.stop()

        utkastInDbCount() shouldBe 1

        LocalPostgresDatabase.getUtkast("u3").shouldNotBeNull()
    }

    @Test
    fun `sletter utkast basert på felt slettesEtter`() = runTest {
        insertUtkast(utkastSlettesEtterFortid, utkastSlettesEtterFremtid, utkastIngenAutomatiskSletting)

        coEvery { leaderElection.isLeader() } returns true

        val deleter = PeriodicUtkastDeleter(
            database = database,
            interval = ofMinutes(10),
            leaderElection = leaderElection
        )

        deleter.start()
        delay(1000)
        deleter.stop()

        LocalPostgresDatabase.getUtkast(utkastSlettesEtterFortid.utkastId).let {
            it.shouldBeNull()
        }

        LocalPostgresDatabase.getUtkast(utkastSlettesEtterFremtid.utkastId).let {
            it.shouldNotBeNull()
        }

        LocalPostgresDatabase.getUtkast(utkastIngenAutomatiskSletting.utkastId).let {
            it.shouldNotBeNull()
        }
    }

    @Test
    fun `does nothing when not leader`() = runTest {
        insertUtkast(gammeltUtkast1, gammeltUtkast2, nyereUtkast)

        coEvery { leaderElection.isLeader() } returns false

        val deleter = PeriodicUtkastDeleter(
            database = database,
            interval = ofMinutes(10),
            leaderElection = leaderElection
        )

        deleter.start()
        delay(2000)
        deleter.stop()

        utkastInDbCount() shouldBe 3
    }

    private suspend fun delayUntilNRemains(remainingVarsler: Int = 0) {
        withTimeout(5000) {
            while (utkastInDbCount() > remainingVarsler) {
                delay(100)
            }
        }
    }

    private fun utkastInDbCount(): Int {
        return database.single {
            queryOf("select count(*) as antall from utkast")
                .map { it.int("antall") }
        }
    }

    private fun utkast(
        utkastId: String,
        opprettet: LocalDateTime = nowAtUtc(),
        slettesEtter: ZonedDateTime? = null
    ) = Utkast(
        utkastId = utkastId,
        tittel = "Tittel for utkast",
        link = "https://lenke-for-utkast",
        opprettet = opprettet,
        sistEndret = null,
        slettesEtter = slettesEtter,
        metrics = null,
    )

    private fun insertUtkast(vararg utkast: Utkast) {
        utkast.forEach { utkast ->
            database.update {
                queryOf(
                    "INSERT INTO utkast (packet, opprettet, slettesEtter) values (:packet, :opprettet, :slettesEtter) ON CONFLICT DO NOTHING",
                    mapOf("packet" to utkast.toJsonb(), "opprettet" to utkast.opprettet, "slettesEtter" to utkast.slettesEtter)
                )
            }
        }
    }

    fun runTest(testBlock: suspend () -> Unit) = runBlocking { testBlock() }
}


