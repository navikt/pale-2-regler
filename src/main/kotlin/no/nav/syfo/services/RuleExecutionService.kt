package no.nav.syfo.services

import no.nav.syfo.model.Legeerklaering
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.rules.common.RuleExecution
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.hpr.HPRRulesExecution
import no.nav.syfo.rules.hpr.hprRuleTree
import no.nav.syfo.rules.legesuspensjon.LegeSuspensjonRulesExecution
import no.nav.syfo.rules.legesuspensjon.legeSuspensjonRuleTree
import no.nav.syfo.rules.validation.ValidationRulesExecution
import no.nav.syfo.rules.validation.validationRuleTree

class RuleExecutionService() {

    private val ruleExecution = sequenceOf(
        LegeSuspensjonRulesExecution(legeSuspensjonRuleTree),
        HPRRulesExecution(hprRuleTree),
        ValidationRulesExecution(validationRuleTree),
    )

    fun runRules(
        legeerklaring: Legeerklaering,
        ruleMetadata: RuleMetadata,
        sequence: Sequence<RuleExecution<out Enum<*>>> = ruleExecution,
    ): List<TreeOutput<out Enum<*>, RuleResult>> {
        var lastStatus = Status.OK
        val results = sequence
            .map { it.runRules(legeerklaring, ruleMetadata) }
            .takeWhile {
                if (lastStatus == Status.OK) {
                    lastStatus = it.treeResult.status
                    true
                } else {
                    false
                }
            }
        return results.toList()
    }
}
