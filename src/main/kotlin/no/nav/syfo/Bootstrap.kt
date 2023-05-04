package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.DelicateCoroutinesApi
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.application.exception.ServiceUnavailableException
import no.nav.syfo.client.AccessTokenClientV2
import no.nav.syfo.client.LegeSuspensjonClient
import no.nav.syfo.client.NorskHelsenettClient
import no.nav.syfo.pdl.client.PdlClient
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.services.RuleExecutionService
import no.nav.syfo.services.RuleService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.TimeUnit

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.pale-2-regler")

@DelicateCoroutinesApi
fun main() {
    val env = Environment()
    val applicationState = ApplicationState()

    val jwkProviderAad = JwkProviderBuilder(URL(env.jwkKeysUrl))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    val config: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, _ ->
                when (exception) {
                    is SocketTimeoutException -> throw ServiceUnavailableException(exception.message)
                }
            }
        }
        expectSuccess = false

        install(HttpTimeout) {
            socketTimeoutMillis = 120_000
            connectTimeoutMillis = 40_000
            requestTimeoutMillis = 40_000
        }
        install(HttpRequestRetry) {
            constantDelay(100, 0, false)
            retryOnExceptionIf(3) { request, throwable ->
                log.warn("Caught exception ${throwable.message}, for url ${request.url}")
                true
            }
            retryIf(maxRetries) { request, response ->
                if (response.status.value.let { it in 500..599 }) {
                    log.warn("Retrying for statuscode ${response.status.value}, for url ${request.url}")
                    true
                } else {
                    false
                }
            }
        }
    }

    val httpClient = HttpClient(Apache, config)

    val accessTokenClientV2 = AccessTokenClientV2(
        aadAccessTokenUrl = env.aadAccessTokenV2Url,
        clientId = env.clientIdV2,
        clientSecret = env.clientSecretV2,
        httpClient = httpClient,
    )

    val legeSuspensjonClient = LegeSuspensjonClient(
        env.legeSuspensjonEndpointURL,
        accessTokenClientV2,
        httpClient,
        env.legeSuspensjonProxyScope,
        env.applicationName,
    )

    val norskHelsenettClient =
        NorskHelsenettClient(env.norskHelsenettEndpointURL, accessTokenClientV2, env.helsenettproxyScope, httpClient)

    val pdlClient = PdlClient(
        httpClient,
        env.pdlGraphqlPath,
        PdlClient::class.java.getResource("/graphql/getPerson.graphql")!!.readText().replace(Regex("[\n\t]"), ""),
    )
    val pdlService = PdlPersonService(pdlClient, accessTokenClientV2, env.pdlScope)

    val applicationEngine = createApplicationEngine(
        env,
        applicationState,
        RuleService(legeSuspensjonClient, norskHelsenettClient, pdlService, RuleExecutionService()),
        jwkProviderAad,
    )

    DefaultExports.initialize()

    ApplicationServer(applicationEngine, applicationState).start()
}
