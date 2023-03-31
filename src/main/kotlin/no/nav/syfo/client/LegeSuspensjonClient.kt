package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import no.nav.syfo.log
import java.io.IOException

class LegeSuspensjonClient(
    private val endpointUrl: String,
    private val accessTokenClientV2: AccessTokenClientV2,
    private val httpClient: HttpClient,
    private val scope: String,
    private val consumerAppName: String,
) {

    suspend fun checkTherapist(therapistId: String, ediloggid: String, oppslagsdato: String): Suspendert {
        val httpResponse: HttpResponse = httpClient.get("$endpointUrl/btsys/api/v1/suspensjon/status") {
            accept(ContentType.Application.Json)
            val accessToken = accessTokenClientV2.getAccessTokenV2(scope)

            headers {
                append("Nav-Call-Id", ediloggid)
                append("Nav-Consumer-Id", consumerAppName)
                append("Nav-Personident", therapistId)

                append("Authorization", "Bearer $accessToken")
            }
            parameter("oppslagsdato", oppslagsdato)
        }
        when (httpResponse.status) {
            HttpStatusCode.OK -> {
                log.info("Hentet supensjonstatus for ediloggId {}", ediloggid)
                return httpResponse.call.response.body<Suspendert>()
            }
            else -> {
                log.error("Btsys (smgcp-proxy) svarte med kode {} for ediloggId {}", httpResponse.status, ediloggid)
                throw IOException("Btsys svarte med uventet kode ${httpResponse.status} for $ediloggid")
            }
        }
    }
}

data class Suspendert(val suspendert: Boolean)
