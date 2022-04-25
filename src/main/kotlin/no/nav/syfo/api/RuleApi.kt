package no.nav.syfo.api

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import kotlinx.coroutines.DelicateCoroutinesApi
import no.nav.syfo.metrics.RULE_HIT_STATUS_COUNTER
import no.nav.syfo.model.ReceivedLegeerklaering
import no.nav.syfo.services.RuleService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smregler")

@DelicateCoroutinesApi
fun Routing.registerRuleApi(ruleService: RuleService) {
    post("/v1/rules/validate") {
        log.info("Got an request to validate rules")

        val receivedLegeerklaering: ReceivedLegeerklaering = call.receive()

        val validationResult = ruleService.executeRuleChains(receivedLegeerklaering)
        RULE_HIT_STATUS_COUNTER.labels(validationResult.status.name).inc()
        call.respond(validationResult)
    }
}
