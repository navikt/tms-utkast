package no.nav.tms.utkast.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.utkast.setup.logExceptionAsError
import no.nav.tms.utkast.sink.Utkast
import java.time.LocalDateTime


class UtkastFetcher(
    val digiSosBaseUrl: String,
    val aapBaseUrl: String,
    val httpClient: HttpClient,
    val digisosClientId: String,
    val aapClientId: String,
    val tokendingsService: TokendingsService,
) {
    suspend fun allExternal(accessToken: String): List<FetchResult> =
        listOf(digisos(accessToken), aap(accessToken))


    private suspend fun digisos(accessToken: String) = try {
        httpClient.fetchUtkast<List<DigisosBeskjed>>(
            url = "$digiSosBaseUrl/dittnav/pabegynte/aktive",
            tokenxToken = tokendingsService.exchangeToken(accessToken, digisosClientId),
            service = "Digisos",
            transform = { this.map { it.toUtkast() } }
        )
    } catch (e: Exception) {
        logExceptionAsError(logInfo = "Feil ved henting av utkast fra digisos", cause = e)
        FetchResult(wasSuccess = false, emptyList())
    }

    private suspend fun aap(accessToken: String) = try {
        httpClient.fetchUtkast<ExternalUtkast>(
            url = "$aapBaseUrl/mellomlagring/søknad/finnes",
            tokenxToken = tokendingsService.exchangeToken(accessToken, aapClientId),
            service = "AAP",
            transform = { listOf(toUtkast("AAP")) }
        )
    } catch (e: Exception) {
        logExceptionAsError(logInfo = "Feil ved henting av utkast fra aap", cause = e)
        FetchResult(wasSuccess = false, emptyList())
    }

    private suspend inline fun <reified T> HttpClient.fetchUtkast(
        url: String,
        tokenxToken: String,
        service: String,
        transform: T.() -> List<Utkast>
    ): FetchResult =
        withContext(Dispatchers.IO) {
            request {
                url(url)
                method = HttpMethod.Get
                header(HttpHeaders.Authorization, "Bearer $tokenxToken")
                timeout {
                    this.requestTimeoutMillis = 8000
                    this.connectTimeoutMillis = 8000
                }
            }
        }.let {
            val result = if (it.status == HttpStatusCode.NotFound || it.status == HttpStatusCode.NoContent)
                emptyList()
            else it.body<T>().transform()
            FetchResult(wasSuccess = true, result)
        }
}


class DigisosBeskjed(
    private val eventId: String,
    private val tekst: String,
    private val link: String,
    private val eventTidspunkt: LocalDateTime,
    private val sistOppdatert: LocalDateTime? = null
) {
    fun toUtkast() =
        Utkast(
            utkastId = eventId,
            tittel = tekst,
            link = link,
            opprettet = eventTidspunkt,
            sistEndret = sistOppdatert,
            slettesEtter = null,
            metrics = mapOf()
        )
}

class ExternalUtkast(
    private val tittel: String,
    private val sistEndret: LocalDateTime,
    private val link: String,
) {
    fun toUtkast(service: String) = Utkast(
        utkastId = service,
        tittel = tittel,
        link = link,
        opprettet = sistEndret,
        sistEndret = sistEndret,
        slettesEtter = null,
        metrics = mapOf()
    )
}

class FetchResult(val wasSuccess: Boolean, val utkast: List<Utkast>) {
    companion object {
        fun List<FetchResult>.responseStatus() =
            if (any { !it.wasSuccess }) HttpStatusCode.MultiStatus else HttpStatusCode.OK

        fun List<FetchResult>.utkast(): List<Utkast> = flatMap { it.utkast }
    }
}
