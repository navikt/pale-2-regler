package no.nav.syfo

import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.DelicateCoroutinesApi
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.clients.HttpClients
import no.nav.syfo.services.RuleService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.pale-2-regler")

@DelicateCoroutinesApi
fun main() {
    val env = Environment()
    val applicationState = ApplicationState()

    val applicationEngine = createApplicationEngine(
        env,
        applicationState,
        RuleService(HttpClients(env))
    )

    DefaultExports.initialize()

    ApplicationServer(applicationEngine, applicationState).start()
}
