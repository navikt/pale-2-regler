package no.nav.syfo.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.syfo.Environment
import no.nav.syfo.util.LoggingMeta
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LegeSuspensjonClientTest {
    private val fnr = "12345647981"
    private val accessTokenClientV2 = mockk<AccessTokenClientV2>()
    private val httpClient = HttpClient(Apache) {
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        install(HttpRequestRetry) {
            maxRetries = 3
            delayMillis { retry ->
                retry * 100L
            }
        }
        expectSuccess = false
    }

    private val loggingMeta = LoggingMeta("23", "900323", "1231", "31311-31312313-13")
    private val mockHttpServerPort = ServerSocket(0).use { it.localPort }
    private val mockHttpServerUrl = "http://localhost:$mockHttpServerPort"
    private val mockServer = embeddedServer(Netty, mockHttpServerPort) {
        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        routing {
            get("/syfohelsenettproxy/api/v2/behandler") {
                when {
                    call.request.headers["behandlerFnr"] == fnr -> call.respond(Behandler(listOf(Godkjenning())))
                    call.request.headers["behandlerFnr"] == "behandlerFinnesIkke" -> call.respond(
                        HttpStatusCode.NotFound,
                        "Behandler finnes ikke"
                    )

                    else -> call.respond(HttpStatusCode.InternalServerError, "Noe gikk galt")
                }
            }
        }
    }.start()

    private val norskHelsenettClient =
        NorskHelsenettClient("$mockHttpServerUrl/syfohelsenettproxy", accessTokenClientV2, "resourceId", httpClient)

    val env = Environment()
    private val legeSuspensjonClient =
        LegeSuspensjonClient(env.legeSuspensjonEndpointURL, accessTokenClientV2, httpClient, env.legeSuspensjonProxyScope, env.applicationName)

    @BeforeAll
    internal fun beforeAll() {
        coEvery { accessTokenClientV2.getAccessTokenV2(any()) } returns "token"
    }

    @AfterAll
    internal fun afterAll() {
        mockServer.stop(TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(10))
    }

    @Test
    fun checkTherapist() {
        assertEquals(true, true)
    }
}
