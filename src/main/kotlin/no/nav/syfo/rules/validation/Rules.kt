package no.nav.syfo.rules.validation

import no.nav.syfo.model.Legeerklaering
import no.nav.syfo.model.RuleMetadata

import no.nav.syfo.rules.dsl.RuleResult


typealias Rule<T> = (legeerklaring: Legeerklaering, ruleMetadata: RuleMetadata) -> RuleResult<T>
typealias ValidationRule = Rule<ValidationRules>

val pasientUnder13Aar: ValidationRule = { legeerklaring, ruleMetadata ->

    val signatureDate = ruleMetadata.signatureDate.toLocalDate()
    val pasientFodselsdato = ruleMetadata.patientBorndate

    val pasientUnder13Aar = signatureDate < pasientFodselsdato.plusYears(13)

    RuleResult(
        ruleInputs = mapOf("pasientUnder13Aar" to pasientUnder13Aar),
        rule = ValidationRules.PASIENT_YNGRE_ENN_13,
        ruleResult = pasientUnder13Aar
    )
}



val ugyldingOrgNummerLengde: ValidationRule = { _, ruleMetadata ->
    val legekontorOrgnr = ruleMetadata.legekontorOrgnr

    val ugyldingOrgNummerLengde = legekontorOrgnr != null && legekontorOrgnr.length != 9

    RuleResult(
        ruleInputs = mapOf("ugyldingOrgNummerLengde" to ugyldingOrgNummerLengde),
        rule = ValidationRules.UGYLDIG_ORGNR_LENGDE,
        ruleResult = ugyldingOrgNummerLengde
    )
}

val avsenderSammeSomPasient: ValidationRule = { _, ruleMetadata ->
    val avsenderFnr = ruleMetadata.avsenderfnr

    val patientPersonNumber = ruleMetadata.patientPersonNumber

    val avsenderSammeSomPasient = avsenderFnr == patientPersonNumber

    RuleResult(
        ruleInputs = mapOf("avsenderSammeSomPasient" to avsenderSammeSomPasient),
        rule = ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR,
        ruleResult = avsenderSammeSomPasient
    )
}

