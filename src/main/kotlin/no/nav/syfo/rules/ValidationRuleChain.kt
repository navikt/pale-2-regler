package no.nav.syfo.rules

import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status

enum class ValidationRuleChain(
    override val ruleId: Int?,
    override val status: Status,
    override val messageForUser: String,
    override val messageForSender: String,
    override val predicate: (RuleData<RuleMetadata>) -> Boolean
) : Rule<RuleData<RuleMetadata>> {

    PASIENT_YNGRE_ENN_13(
        1101,
        Status.INVALID,
        "Pasienten er under 13 år. Legeerklæring kan ikke benyttes.",
        "Pasienten er under 13 år. Legeerklæring kan ikke benyttes.",
        { (_, metadata) ->
            metadata.signatureDate.toLocalDate() < metadata.patientBorndate.plusYears(13)
        }
    ),

    UGYLDIG_ORGNR_LENGDE(
        9999,
        Status.INVALID,
        "Den må ha riktig organisasjonsnummer.",
        "Feil format på organisasjonsnummer. Dette skal være 9 sifre.",
        { (_, metadata) ->
            metadata.legekontorOrgnr != null && metadata.legekontorOrgnr.length != 9
        }
    ),

    AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(
        9999,
        Status.INVALID,
        "Den som signert legeerklæringen er også pasient.",
        "Avsender fnr er det samme som pasient fnr",
        { (_, metadata) ->
            metadata.avsenderfnr.equals(metadata.patientPersonNumber)
        }
    )
}
