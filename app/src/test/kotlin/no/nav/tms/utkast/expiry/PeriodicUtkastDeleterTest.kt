package no.nav.tms.utkast.expiry

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.throwables.shouldNotThrow
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
import no.nav.tms.utkast.sink.LocalDateTimeHelper.nowAtUtc
import no.nav.tms.utkast.sink.Utkast
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject
import java.time.Duration.ofMinutes
import java.time.LocalDateTime

internal class PeriodicUtkastDeleterTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val leaderElection: PodLeaderElection = mockk()

    private val gammeltUtkast1 = utkast("u1", opprettet = nowAtUtc().minusMonths(4))
    private val gammeltUtkast2 = utkast("u2", opprettet = nowAtUtc().minusMonths(5))

    private val opprettet = nowAtUtc().minusMonths(2)
    private val nyereUtkast = utkast("u3", opprettet = opprettet)

    private val objectMapper = jacksonMapperBuilder()
        .addModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()
        .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)

    @BeforeEach
    fun setup() {
        insertUtkast(gammeltUtkast1, gammeltUtkast2, nyereUtkast)
    }

    @AfterEach
    fun cleanUp() {
        clearMocks(leaderElection)
        database.update { queryOf("delete from utkast") }
    }

    @Test
    fun `sletter gamle utkast`() = runTest {

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

        shouldNotThrow<Exception> { getUtkast("u3") }
    }

    @Test
    fun `does nothing when not leader`() = runTest {
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
        opprettet: LocalDateTime
    ) = Utkast(
        utkastId = utkastId,
        tittel = "Tittel for utkast",
        link = "https://lenke-for-utkast",
        opprettet = opprettet,
        sistEndret = null,
        metrics = null,
    )

    private fun insertUtkast(vararg utkast: Utkast) {
        utkast.forEach { utkast ->
            database.update {
                queryOf(
                    "INSERT INTO utkast (packet, opprettet) values (:packet, :opprettet) ON CONFLICT DO NOTHING",
                    mapOf("packet" to utkast.toJsonb(), "opprettet" to utkast.opprettet)
                )
            }
        }
    }

    fun getUtkast(utkastId: String) = database.single {
        queryOf(
            "select packet, opprettet from utkast where packet->>'utkastId' = :utkastId",
            mapOf("utkastId" to utkastId)
        ).map {
            objectMapper.readValue<Utkast>(it.string("packet")).copy(opprettet = it.localDateTime("opprettet"))
        }
    }

    fun Utkast.toJsonB() = objectMapper.writeValueAsString(this).let { utkast ->
        PGobject().apply {
            type = "jsonb"
            value = utkast
        }
    }

    fun runTest(testBlock: suspend () -> Unit) = runBlocking { testBlock() }
}


