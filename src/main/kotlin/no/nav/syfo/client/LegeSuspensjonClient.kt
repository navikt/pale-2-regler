package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.io.IOException
import no.nav.syfo.logger

class LegeSuspensjonClient(
    private val endpointUrl: String,
    private val accessTokenClientV2: AccessTokenClientV2,
    private val httpClient: HttpClient,
    private val scope: String,
    private val consumerAppName: String,
) {

    suspend fun checkTherapist(
        therapistId: String,
        ediloggid: String,
        oppslagsdato: String,
    ): Suspendert {
        val httpResponse: HttpResponse =
            httpClient.post("$endpointUrl/api/v1/suspensjon/soek") {
                accept(ContentType.Application.Json)
                val accessToken = accessTokenClientV2.getAccessTokenV2(scope)

                headers {
                    append("Nav-Call-Id", ediloggid)
                    append("Nav-Consumer-Id", consumerAppName)
                    append("Authorization", "Bearer $accessToken")
                }

                contentType(ContentType.Application.Json)
                setBody(mapOf("personident" to therapistId, "oppslagsdato" to oppslagsdato))
            }
        when (httpResponse.status) {
            HttpStatusCode.OK -> {
                logger.info("Hentet supensjonstatus for ediloggId {}", ediloggid)
                return httpResponse.call.response.body<Suspendert>()
            }
            else -> {
                logger.error(
                    "Btsys svarte med kode {} for ediloggId {}",
                    httpResponse.status,
                    ediloggid,
                )
                throw IOException(
                    "Btsys svarte med uventet kode ${httpResponse.status} for $ediloggid"
                )
            }
        }
    }
}

data class Suspendert(val suspendert: Boolean)
