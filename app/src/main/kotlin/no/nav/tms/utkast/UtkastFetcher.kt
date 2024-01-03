package no.nav.tms.utkast

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.utkast.database.Utkast
import java.time.LocalDateTime


class UtkastFetcher(
    val digiSosBaseUrl: String,
    val httpClient: HttpClient,
    val digisosClientId: String,
    val aapClientId: String,
    val tokendingsService: TokendingsService,
) {
    suspend fun allExternal(accessToken: String) = digisos(accessToken) + aap(accessToken)

    suspend fun digisos(accessToken: String) =
        httpClient.fetchUtkast<List<DigisosBeskjed>>(
            url = "$digiSosBaseUrl/dittnav/pabegynte/aktive",
            tokenxToken = tokendingsService.exchangeToken(accessToken, digisosClientId),
            service = "Digisos",
            transform = { this.map { it.toUtkast() } }
        )

    private suspend fun aap(accessToken: String) = httpClient.fetchUtkast<ExternalUtkast>(
        url = "http://innsending.aap/mellomlagring/søknad/finnes",
        tokenxToken = tokendingsService.exchangeToken(accessToken, aapClientId),
        service = "AAP",
        transform = { listOf(toUtkast("AAP")) }
    )

    private suspend inline fun <reified T> HttpClient.fetchUtkast(
        url: String,
        tokenxToken: String,
        service: String,
        transform: T.() -> List<Utkast>
    ) =
        try {
            withContext(Dispatchers.IO) {
                request {
                    url(url)
                    method = HttpMethod.Get
                    header(HttpHeaders.Authorization, "Bearer $tokenxToken")
                }
            }.let {
                if (it.status == HttpStatusCode.NotFound)
                    emptyList<T>()
                else it.body<T>().transform()
            }
        } catch (e: Exception) {
            throw ExternalServiceException(
                operation = "henting av utkast ($url)",
                originalException = e,
                service = service
            )
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
        metrics = mapOf()
    )
}

class ExternalServiceException(val operation: String, val service: String, val originalException: Exception) :
    Exception("Feil i $operation fra $service: ${originalException.message}")


