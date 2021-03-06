package no.nav.syfo.api

import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.metrics.RULE_HIT_STATUS_COUNTER
import no.nav.syfo.model.ReceivedLegeerklaering
import no.nav.syfo.services.RuleService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smregler")

@KtorExperimentalAPI
fun Routing.registerRuleApi(ruleService: RuleService) {
    post("/v1/rules/validate") {
        log.info("Got an request to validate rules")

        val receivedLegeerklaering: ReceivedLegeerklaering = call.receive()

        val validationResult = ruleService.executeRuleChains(receivedLegeerklaering)
        RULE_HIT_STATUS_COUNTER.labels(validationResult.status.name).inc()
        call.respond(validationResult)
    }
}
