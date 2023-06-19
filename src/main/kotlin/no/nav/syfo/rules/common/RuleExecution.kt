package no.nav.syfo.rules.common

import no.nav.syfo.model.Legeerklaering
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.rules.dsl.TreeOutput

interface RuleExecution<T> {
    fun runRules(
        legeerklaring: Legeerklaering,
        ruleMetadata: RuleMetadata
    ): TreeOutput<T, RuleResult>
}
