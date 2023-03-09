package no.nav.syfo.rules.hpr

import no.nav.syfo.model.Status
import no.nav.syfo.rules.common.RuleHit

enum class HPRRuleHit(
    val ruleHit: RuleHit
) {
    BEHANDLER_IKKE_GYLDIG_I_HPR(
        ruleHit = RuleHit(
            rule = "BEHANDLER_IKKE_GYLDIG_I_HPR",
            status = Status.INVALID,
            messageForSender = "Behandler er ikke gyldig i HPR på konsultasjonstidspunkt",
            messageForUser = "Den som skrev legeerklæringen manglet autorisasjon",
        )
    ),
    BEHANDLER_MANGLER_AUTORISASJON_I_HPR(
        ruleHit = RuleHit(
            rule = "BEHANDLER_MANGLER_AUTORISASJON_I_HPR",
            status = Status.INVALID,
            messageForSender = "Behandler har ikke gyldig autorisasjon i HPR.",
            messageForUser = "Den som skrev legeerklæringen manglet autorisasjon.",
        )
    ),
    BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR(
        ruleHit = RuleHit(
            rule = "BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR",
            status = Status.INVALID,
            messageForSender = "Behandler finnes i HPR men er ikke lege, kiropraktor, fysioterapeut, manuellterapeut," +
                    " pysykolog eller tannlege",
            messageForUser = "Den som skrev legeerklæringen manglet autorisasjon.",
        )
    )
}
