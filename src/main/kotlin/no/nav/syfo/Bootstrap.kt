package no.nav.syfo

import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.clients.HttpClients
import no.nav.syfo.services.RuleService
import no.nav.syfo.util.getFileAsString
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.pale-2-regler")

@KtorExperimentalAPI
fun main() {
    val env = Environment()
    val applicationState = ApplicationState()

    val vaultSecrets = VaultSecrets(
        serviceuserPassword = getFileAsString("/secrets/serviceuser/password"),
        serviceuserUsername = getFileAsString("/secrets/serviceuser/username"),
        clientId = getFileAsString("/secrets/azuread/pale-2/client_id"),
        clientsecret = getFileAsString("/secrets/azuread/pale-2/client_secret")
    )

    val applicationEngine = createApplicationEngine(env, applicationState,
        RuleService(HttpClients(env, vaultSecrets)))

    ApplicationServer(applicationEngine).start()

    applicationState.ready = true
}
