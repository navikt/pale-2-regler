package no.nav.syfo.rules.legesuspensjon

import no.nav.syfo.log
import no.nav.syfo.model.Legeerklaering
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.rules.common.RuleExecution
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.ResultNode
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.TreeNode
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.dsl.join
import no.nav.syfo.rules.dsl.printRulePath

typealias LegeSuspensjonTreeOutput = TreeOutput<LegeSuspensjonRules, RuleResult>

typealias LegeSuspensjonTreeNode = TreeNode<LegeSuspensjonRules, RuleResult>

class LegeSuspensjonRulesExecution(
    val rootNode: TreeNode<LegeSuspensjonRules, RuleResult> = legeSuspensjonRuleTree
) : RuleExecution<LegeSuspensjonRules> {
    override fun runRules(
        legeerklaring: Legeerklaering,
        ruleMetadata: RuleMetadata
    ): LegeSuspensjonTreeOutput =
        rootNode.evaluate(legeerklaring.id, ruleMetadata.doctorSuspensjon).also {
            legeSuspensjonRulePath ->
            log.info("Rules ${legeerklaring.id}, ${legeSuspensjonRulePath.printRulePath()}")
        }
}

private fun TreeNode<LegeSuspensjonRules, RuleResult>.evaluate(
    sykmeldingId: String,
    behandlerSuspendert: Boolean,
): LegeSuspensjonTreeOutput =
    when (this) {
        is ResultNode -> LegeSuspensjonTreeOutput(treeResult = result)
        is RuleNode -> {
            val rule = getRule(rule)
            val result = rule(behandlerSuspendert)
            val childNode = if (result.ruleResult) yes else no
            result join childNode.evaluate(sykmeldingId, behandlerSuspendert)
        }
    }
