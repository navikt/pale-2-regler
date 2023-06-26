package no.nav.syfo.rules.api

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.DelicateCoroutinesApi
import no.nav.syfo.logger
import no.nav.syfo.model.ReceivedLegeerklaering
import no.nav.syfo.services.RuleService

@DelicateCoroutinesApi
fun Route.registerRuleApi(ruleService: RuleService) {
    post("/v1/rules/validate") {
        logger.info("Got an request to validate rules")

        val receivedLegeerklaering: ReceivedLegeerklaering = call.receive()

        val validationResult = ruleService.executeRuleChains(receivedLegeerklaering)
        call.respond(validationResult)
    }
}
