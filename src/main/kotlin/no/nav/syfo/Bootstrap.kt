package no.nav.syfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.error
import kotlinx.coroutines.*
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val objectMapper: ObjectMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .registerKotlinModule()
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.pale-2-regler")

@KtorExperimentalAPI
fun main() {
    val environment = Environment()
    val applicationState = ApplicationState()

    val applicationEngine = createApplicationEngine(environment, applicationState)

    ApplicationServer(applicationEngine).start()

    applicationState.ready = true

//    launchListeners(applicationState, environment)

}

@KtorExperimentalAPI
fun launchListeners(
    applicationState: ApplicationState,
    env: Environment
) {
    createListener(applicationState) {

    }
}

fun createListener(applicationState: ApplicationState, action: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch {
        try {
            action()
        } catch (e: Exception) {
            log.error(e)
        } finally {
            applicationState.alive = false
        }
    }