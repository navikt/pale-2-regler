package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson3.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.mockk.coEvery
import io.mockk.mockk
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LegeSuspensjonClientTest {
    private val accessTokenClientV2 = mockk<AccessTokenClientV2>()
    private val httpClient =
        HttpClient(Apache5) {
            install(ContentNegotiation) { jackson {} }
            install(HttpRequestRetry) {
                maxRetries = 3
                delayMillis { retry -> retry * 100L }
            }
            expectSuccess = false
        }

    private val mockHttpServerPort = ServerSocket(0).use { it.localPort }
    private val mockHttpServerUrl = "http://localhost:$mockHttpServerPort"
    private val mockServer =
        embeddedServer(Netty, mockHttpServerPort, module = Application::myApplicationModule).start()

    private val legeSuspensjonClient =
        LegeSuspensjonClient(
            endpointUrl = mockHttpServerUrl,
            accessTokenClientV2 = accessTokenClientV2,
            consumerAppName = "consumerAppName",
            httpClient = httpClient,
            scope = "scope",
        )

    @BeforeAll
    internal fun beforeAll() {
        coEvery { accessTokenClientV2.getAccessTokenV2(any()) } returns "token"
    }

    @AfterAll
    internal fun afterAll() {
        mockServer.stop(TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(10))
    }

    @Test
    fun `CheckTherapist should return Suspendert true`() = runTest {
        val suspendert =
            legeSuspensjonClient.checkTherapist(
                therapistId = "1",
                ediloggid = "55-4321",
                oppslagsdato = "2023-01-26",
            )
        assertEquals(true, suspendert.suspendert)
    }

    @Test
    fun `CheckTherapist should return Suspendert false`() = runTest {
        val suspendert =
            legeSuspensjonClient.checkTherapist(
                therapistId = "2",
                ediloggid = "55-4321",
                oppslagsdato = "2023-01-26",
            )
        assertEquals(false, suspendert.suspendert)
    }

    @Test
    fun `CheckTherapist should return IOException`() = runTest {
        val btsysException: Throwable = assertThrows {
            legeSuspensjonClient.checkTherapist(
                therapistId = "3",
                ediloggid = "55-4321",
                oppslagsdato = "2023-01-26",
            )
        }
        assertEquals(
            "Btsys svarte med uventet kode 500 Internal Server Error for 55-4321",
            btsysException.message,
        )
    }
}

fun Application.myApplicationModule() {
    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) { jackson {} }
    routing {
        post("/api/v1/suspensjon/soek") {
            val payload = call.receive<Map<String, Any>>()
            val ident = payload["personident"]

            when {
                ident == "1" -> call.respond(Suspendert(true))
                ident == "2" -> call.respond(Suspendert(false))
                else -> call.respond(HttpStatusCode.InternalServerError, "Noe gikk galt")
            }
        }
    }
}
