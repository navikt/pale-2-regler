package no.nav.syfo.clients

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.features.HttpResponseValidator
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.network.sockets.SocketTimeoutException
import no.nav.syfo.Environment
import no.nav.syfo.VaultSecrets
import no.nav.syfo.application.exception.ServiceUnavailableException
import no.nav.syfo.client.AccessTokenClientV2
import no.nav.syfo.client.LegeSuspensjonClient
import no.nav.syfo.client.NorskHelsenettClient
import no.nav.syfo.client.StsOidcClient
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import java.net.ProxySelector

class HttpClients(env: Environment, vaultSecrets: VaultSecrets) {

    private val config: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        HttpResponseValidator {
            handleResponseException { exception ->
                when (exception) {
                    is SocketTimeoutException -> throw ServiceUnavailableException(exception.message)
                }
            }
        }
        expectSuccess = false
    }

    private val proxyConfig: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
        config()
        engine {
            customizeClient {
                setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
            }
        }
    }

    private val httpClientWithProxy = HttpClient(Apache, proxyConfig)
    private val httpClient = HttpClient(Apache, config)

    private val oidcClient = StsOidcClient(vaultSecrets.serviceuserUsername, vaultSecrets.serviceuserPassword, env.securityTokenServiceURL)
    private val accessTokenClientV2 = AccessTokenClientV2(
        aadAccessTokenUrl = env.aadAccessTokenV2Url,
        clientId = env.clientIdV2,
        clientSecret = env.clientSecretV2,
        httpClient = httpClientWithProxy
    )

    val legeSuspensjonClient = LegeSuspensjonClient(
        env.legeSuspensjonEndpointURL,
        vaultSecrets,
        oidcClient,
        httpClient
    )

    val norskHelsenettClient = NorskHelsenettClient(env.norskHelsenettEndpointURL, accessTokenClientV2, env.helsenettproxyScope, httpClient)
}
