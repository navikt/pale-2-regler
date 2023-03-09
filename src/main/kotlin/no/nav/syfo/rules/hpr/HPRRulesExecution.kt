package no.nav.syfo.rules.hpr

import no.nav.syfo.client.Behandler
import no.nav.syfo.log
import no.nav.syfo.model.Legeerklaering
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.rules.common.RuleExecution
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.*

typealias HPRTreeOutput = TreeOutput<HPRRules, RuleResult>
typealias HPRTreeNode = TreeNode<HPRRules, RuleResult>

class HPRRulesExecution(private val rootNode: HPRTreeNode = hprRuleTree) : RuleExecution<HPRRules> {
    override fun runRules(legeerklaring: Legeerklaering, ruleMetadata: RuleMetadata) =
        rootNode
            .evaluate(legeerklaring, ruleMetadata.behandler)
            .also { hprRulePath ->
                log.info("Rules ${legeerklaring.id}, ${hprRulePath.printRulePath()}")
            }
}

private fun TreeNode<HPRRules, RuleResult>.evaluate(
    legeerklaring: Legeerklaering,
    behandler: Behandler,
): HPRTreeOutput =
    when (this) {
        is ResultNode -> HPRTreeOutput(treeResult = result)
        is RuleNode -> {
            val rule = getRule(rule)
            val result = rule(legeerklaring, behandler)
            val childNode = if (result.ruleResult) yes else no
            result join childNode.evaluate(legeerklaring, behandler)
        }
    }