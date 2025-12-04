package no.nav.syfo

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.prometheus.client.hotspot.DefaultExports
import java.net.SocketTimeoutException
import java.net.URI
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.DelicateCoroutinesApi
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.client.AccessTokenClientV2
import no.nav.syfo.client.LegeSuspensjonClient
import no.nav.syfo.client.NorskHelsenettClient
import no.nav.syfo.metrics.monitorHttpRequests
import no.nav.syfo.nais.isalive.naisIsAliveRoute
import no.nav.syfo.nais.isready.naisIsReadyRoute
import no.nav.syfo.nais.prometheus.naisPrometheusRoute
import no.nav.syfo.pdl.client.PdlClient
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.rules.api.registerRuleApi
import no.nav.syfo.services.RuleExecutionService
import no.nav.syfo.services.RuleService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("no.nav.syfo.pale-2-regler")
val applicationState = ApplicationState()

fun main() {
    val applicationState = ApplicationState()
    val embeddedServer =
        embeddedServer(
            Netty,
            port = EnvironmentVariables().applicationPort,
            module = Application::module,
        )
    Runtime.getRuntime()
        .addShutdownHook(
            Thread {
                embeddedServer.stop(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(10))
            },
        )
    embeddedServer.monitor.subscribe(ApplicationStopped) {
        applicationState.ready = false
        applicationState.alive = false
    }
    embeddedServer.start(true)
}

@OptIn(DelicateCoroutinesApi::class)
fun Application.configureRouting(
    applicationState: ApplicationState,
    environmentVariables: EnvironmentVariables,
    jwkProviderAadV2: JwkProvider,
    ruleService: RuleService
) {
    setupAuth(
        environmentVariables = environmentVariables,
        jwkProviderAadV2 = jwkProviderAadV2,
    )
    routing {
        naisIsAliveRoute(applicationState)
        naisIsReadyRoute(applicationState)
        naisPrometheusRoute()
        authenticate("servicebrukerAAD") { registerRuleApi(ruleService) }
    }
    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")

            logger.error("Caught exception", cause)
            throw cause
        }
    }
    intercept(ApplicationCallPipeline.Monitoring, monitorHttpRequests())
}

@OptIn(DelicateCoroutinesApi::class)
fun Application.module() {
    val environmentVariables = EnvironmentVariables()

    val jwkProviderAad =
        JwkProviderBuilder(URI.create(environmentVariables.jwkKeysUrl).toURL())
            .cached(10, Duration.ofHours(24))
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
                    is SocketTimeoutException ->
                        throw ServiceUnavailableException(exception.message)
                }
            }
        }
        expectSuccess = false

        install(HttpTimeout) {
            socketTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
            requestTimeoutMillis = 30_000
        }
        install(HttpRequestRetry) {
            exponentialDelay(maxDelayMs = 30_000)
            retryOnExceptionIf(5) { request, throwable ->
                logger.warn("Caught exception ${throwable.message}, for url ${request.url}")
                true
            }
            retryIf(5) { request, response ->
                if (response.status.value.let { it in 500..599 }) {
                    logger.warn(
                        "Retrying for statuscode ${response.status.value}, for url ${request.url}"
                    )
                    true
                } else {
                    false
                }
            }
        }
    }

    val httpClient = HttpClient(Apache, config)

    val accessTokenClientV2 =
        AccessTokenClientV2(
            aadAccessTokenUrl = environmentVariables.aadAccessTokenV2Url,
            clientId = environmentVariables.clientIdV2,
            clientSecret = environmentVariables.clientSecretV2,
            httpClient = httpClient,
        )

    val legeSuspensjonClient =
        LegeSuspensjonClient(
            environmentVariables.legeSuspensjonEndpointURL,
            accessTokenClientV2,
            httpClient,
            environmentVariables.legeSuspensjonProxyScope,
            environmentVariables.applicationName,
        )

    val norskHelsenettClient =
        NorskHelsenettClient(
            environmentVariables.norskHelsenettEndpointURL,
            accessTokenClientV2,
            environmentVariables.helsenettproxyScope,
            httpClient
        )

    val pdlClient =
        PdlClient(
            httpClient,
            environmentVariables.pdlGraphqlPath,
            PdlClient::class
                .java
                .getResource("/graphql/getPerson.graphql")!!
                .readText()
                .replace(Regex("[\n\t]"), ""),
        )
    val pdlService = PdlPersonService(pdlClient, accessTokenClientV2, environmentVariables.pdlScope)

    val ruleService =
        RuleService(legeSuspensjonClient, norskHelsenettClient, pdlService, RuleExecutionService())

    configureRouting(
        applicationState = applicationState,
        environmentVariables = environmentVariables,
        jwkProviderAadV2 = jwkProviderAad,
        ruleService = ruleService
    )

    DefaultExports.initialize()
}

fun Application.setupAuth(
    environmentVariables: EnvironmentVariables,
    jwkProviderAadV2: JwkProvider,
) {
    install(Authentication) {
        jwt(name = "servicebrukerAAD") {
            verifier(jwkProviderAadV2, environmentVariables.jwtIssuer)
            validate { credentials ->
                when {
                    harTilgang(credentials, environmentVariables.clientIdV2) ->
                        JWTPrincipal(credentials.payload)
                    else -> unauthorized(credentials)
                }
            }
        }
    }
}

fun harTilgang(credentials: JWTCredential, clientId: String): Boolean {
    val appid: String = credentials.payload.getClaim("azp").asString()
    logger.debug("authorization attempt for $appid")
    return credentials.payload.audience.contains(clientId)
}

fun unauthorized(credentials: JWTCredential): Unit? {
    logger.warn(
        "Auth: Unexpected audience for jwt {}, {}",
        StructuredArguments.keyValue("issuer", credentials.payload.issuer),
        StructuredArguments.keyValue("audience", credentials.payload.audience),
    )
    return null
}

class ServiceUnavailableException(message: String?) : Exception(message)

data class ApplicationState(
    var alive: Boolean = true,
    var ready: Boolean = true,
)
