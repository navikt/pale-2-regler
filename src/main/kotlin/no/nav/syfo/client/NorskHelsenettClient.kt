package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.log
import no.nav.syfo.util.LoggingMeta
import java.io.IOException

class NorskHelsenettClient(
    private val endpointUrl: String,
    private val accessTokenClientV2: AccessTokenClientV2,
    private val scope: String,
    private val httpClient: HttpClient
) {

    suspend fun finnBehandler(behandlerFnr: String, msgId: String, loggingMeta: LoggingMeta): Behandler? {
        log.info("Henter behandler fra syfohelsenettproxy for msgId {}", msgId)
        val httpResponse: HttpResponse = httpClient.get("$endpointUrl/api/v2/behandler") {
            accept(ContentType.Application.Json)
            val accessToken = accessTokenClientV2.getAccessTokenV2(scope)
            headers {
                append("Authorization", "Bearer $accessToken")
                append("Nav-CallId", msgId)
                append("behandlerFnr", behandlerFnr)
            }
        }
        when (httpResponse.status) {
            InternalServerError -> {
                log.error("Syfohelsenettproxy svarte med feilmelding for msgId {}, {}", msgId, fields(loggingMeta))
                throw IOException("Syfohelsenettproxy svarte med feilmelding for $msgId")
            }

            BadRequest -> {
                log.error("BehandlerFnr mangler i request for msgId {}, {}", msgId, fields(loggingMeta))
                return null
            }

            NotFound -> {
                log.warn("BehandlerFnr ikke funnet {}, {}", msgId, fields(loggingMeta))
                return null
            }
            else -> {
                log.info("Hentet behandler for msgId {}, {}", msgId, fields(loggingMeta))
                return httpResponse.call.response.body<Behandler>()
            }
        }
    }
}

data class Behandler(
    val godkjenninger: List<Godkjenning>,
    val hprNummer: Int? = null
)

data class Godkjenning(
    val helsepersonellkategori: Kode? = null,
    val autorisasjon: Kode? = null
)

data class Kode(
    val aktiv: Boolean,
    val oid: Int,
    val verdi: String?
)
