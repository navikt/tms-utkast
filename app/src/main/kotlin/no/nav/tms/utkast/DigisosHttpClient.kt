package no.nav.tms.utkast

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tms.utkast.database.Utkast
import java.time.LocalDateTime


class DigisosHttpClient(val baseUrl: String, val httpClient: HttpClient) {

    suspend fun getUtkast(accessToken: String) = httpClient.getUtkastFromDigisos(accessToken).map { it.toUtkast() }
    suspend fun getAntall(accessToken: String) = httpClient.getUtkastFromDigisos(accessToken).size

    private suspend fun HttpClient.getUtkastFromDigisos(accessToken: String) = try {
        withContext(Dispatchers.IO) {
            request {
                url("$baseUrl/dittnav/pabegynte/aktive")
                method = HttpMethod.Get
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }.body<List<DigisosBeskjed>>()
    } catch (e: Exception) {
        throw DigisosException(
            operation = "henting av utkast ($baseUrl/dittnav/pabegynte/aktive)",
            originalException = e
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

class DigisosException(val operation: String, val originalException: Exception) :
    Exception("Feil i $operation fra DigiSos: ${originalException.message}")


