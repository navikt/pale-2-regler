package no.nav.syfo.rules.validation

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.INVALID
import no.nav.syfo.model.Status.OK
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree

enum class ValidationRules {
    PASIENT_YNGRE_ENN_13,
    UGYLDIG_ORGNR_LENGDE,
    AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR,
}

val validationRuleTree =
    tree<ValidationRules, RuleResult>(ValidationRules.PASIENT_YNGRE_ENN_13) {
        yes(INVALID, ValidationRuleHit.PASIENT_YNGRE_ENN_13)
        no(ValidationRules.UGYLDIG_ORGNR_LENGDE) {
            yes(INVALID, ValidationRuleHit.UGYLDIG_ORGNR_LENGDE)
            no(ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR) {
                yes(INVALID, ValidationRuleHit.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR)
                no(OK)
            }
        }
    }

internal fun RuleNode<ValidationRules, RuleResult>.yes(
    status: Status,
    ruleHit: ValidationRuleHit? = null
) {
    yes(RuleResult(status, ruleHit?.ruleHit))
}

internal fun RuleNode<ValidationRules, RuleResult>.no(
    status: Status,
    ruleHit: ValidationRuleHit? = null
) {
    no(RuleResult(status, ruleHit?.ruleHit))
}

fun getRule(rules: ValidationRules): Rule<ValidationRules> {
    return when (rules) {
        ValidationRules.PASIENT_YNGRE_ENN_13 -> pasientUnder13Aar
        ValidationRules.UGYLDIG_ORGNR_LENGDE -> ugyldingOrgNummerLengde
        ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR -> avsenderSammeSomPasient
    }
}
