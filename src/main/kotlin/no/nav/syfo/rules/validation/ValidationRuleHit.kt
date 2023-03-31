package no.nav.syfo.rules.validation

import no.nav.syfo.model.Status
import no.nav.syfo.rules.common.RuleHit

enum class ValidationRuleHit(
    val ruleHit: RuleHit,
) {
    PASIENT_YNGRE_ENN_13(
        ruleHit = RuleHit(
            rule = "PASIENT_YNGRE_ENN_13",
            status = Status.INVALID,
            messageForSender = "Pasienten er under 13 år. Sykmelding kan ikke benyttes.",
            messageForUser = "Pasienten er under 13 år. Sykmelding kan ikke benyttes.",
        ),
    ),
    UGYLDIG_ORGNR_LENGDE(
        ruleHit = RuleHit(
            rule = "UGYLDIG_ORGNR_LENGDE",
            status = Status.INVALID,
            messageForSender = "Legeerklæringen kan ikke rettes, det må skrives en ny. " +
                "Feil format på organisasjonsnummer. Dette skal være 9 sifre.",
            messageForUser = "Den må ha riktig organisasjonsnummer.",
        ),
    ),
    AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(
        ruleHit = RuleHit(
            rule = "AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR",
            status = Status.INVALID,
            messageForSender = "Avsender fnr er det samme som pasient fnr",
            messageForUser = "Den som signert legeerklæringen er også pasient.",
        ),
    ),
}
