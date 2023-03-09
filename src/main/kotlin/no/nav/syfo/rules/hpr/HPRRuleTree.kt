package no.nav.syfo.rules.hpr

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.OK
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree

enum class HPRRules {
    BEHANDLER_IKKE_GYLDIG_I_HPR,
    BEHANDLER_MANGLER_AUTORISASJON_I_HPR,
    BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR
}

val hprRuleTree = tree<HPRRules, RuleResult>(HPRRules.BEHANDLER_IKKE_GYLDIG_I_HPR) {
    yes(Status.INVALID, HPRRuleHit.BEHANDLER_IKKE_GYLDIG_I_HPR)
    no(HPRRules.BEHANDLER_MANGLER_AUTORISASJON_I_HPR) {
        yes(Status.INVALID, HPRRuleHit.BEHANDLER_MANGLER_AUTORISASJON_I_HPR)
        no(HPRRules.BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR) {
            yes(Status.INVALID, HPRRuleHit.BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR)
            no(OK)
        }
    }
}

internal fun RuleNode<HPRRules, RuleResult>.yes(status: Status, ruleHit: HPRRuleHit? = null) {
    yes(RuleResult(status, ruleHit?.ruleHit))
}

internal fun RuleNode<HPRRules, RuleResult>.no(status: Status, ruleHit: HPRRuleHit? = null) {
    no(RuleResult(status, ruleHit?.ruleHit))
}

fun getRule(rules: HPRRules): Rule<HPRRules> {
    return when (rules) {
        HPRRules.BEHANDLER_IKKE_GYLDIG_I_HPR -> behanderIkkeGyldigHPR
        HPRRules.BEHANDLER_MANGLER_AUTORISASJON_I_HPR -> behandlerManglerAutorisasjon
        HPRRules.BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR -> behandlerIkkeLEKIMTTLFTPS
    }
}
