package no.nav.syfo.rules.hpr

import no.nav.syfo.client.Behandler
import no.nav.syfo.client.Godkjenning
import no.nav.syfo.model.HelsepersonellKategori
import no.nav.syfo.model.Legeerklaering
import no.nav.syfo.rules.dsl.RuleResult

typealias Rule<T> = (legeerklaring: Legeerklaering, behandler: Behandler) -> RuleResult<T>
typealias HPRRule = Rule<HPRRules>

val behanderIkkeGyldigHPR: HPRRule = { _, behandler ->
    val behandlerGodkjenninger = behandler.godkjenninger

    val aktivAutorisasjon = behandlerGodkjenninger.any {
        (it.autorisasjon?.aktiv != null && it.autorisasjon.aktiv)
    }

    RuleResult(
        ruleInputs = mapOf("behandlerGodkjenninger" to behandlerGodkjenninger),
        rule = HPRRules.BEHANDLER_IKKE_GYLDIG_I_HPR,
        ruleResult = !aktivAutorisasjon,
    )
}

val behandlerManglerAutorisasjon: HPRRule = { _, behandler ->
    val behandlerGodkjenninger = behandler.godkjenninger

    val gyldigeGodkjenninger = behandlerGodkjenninger.any {
        (
            it.autorisasjon?.aktiv != null &&
                it.autorisasjon.aktiv &&
                it.autorisasjon.oid == 7704 &&
                it.autorisasjon.verdi != null &&
                it.autorisasjon.verdi in arrayOf("1", "17", "4", "2", "14", "18")
            )
    }

    RuleResult(
        ruleInputs = mapOf("behandlerGodkjenninger" to behandlerGodkjenninger),
        rule = HPRRules.BEHANDLER_MANGLER_AUTORISASJON_I_HPR,
        ruleResult = !gyldigeGodkjenninger,
    )
}

val behandlerIkkeLEKIMTTLFTPS: HPRRule = { _, behandler ->
    val behandlerGodkjenninger = behandler.godkjenninger

    val behandlerLEKIMTTLFT = behandlerGodkjenninger.any {
        (
            it.helsepersonellkategori?.aktiv != null &&
                it.autorisasjon?.aktiv == true && it.helsepersonellkategori.verdi != null &&
                harAktivHelsepersonellAutorisasjonsSom(
                    behandlerGodkjenninger,
                    listOf(
                        HelsepersonellKategori.LEGE.verdi,
                        HelsepersonellKategori.KIROPRAKTOR.verdi,
                        HelsepersonellKategori.MANUELLTERAPEUT.verdi,
                        HelsepersonellKategori.TANNLEGE.verdi,
                        HelsepersonellKategori.FYSIOTERAPAEUT.verdi,
                        HelsepersonellKategori.PSYKOLOG.verdi,
                    ),
                )
            )
    }

    RuleResult(
        ruleInputs = mapOf("behandlerGodkjenninger" to behandlerGodkjenninger),
        rule = HPRRules.BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR,
        ruleResult = !behandlerLEKIMTTLFT,
    )
}

private fun harAktivHelsepersonellAutorisasjonsSom(
    behandlerGodkjenninger: List<Godkjenning>,
    helsepersonerVerdi: List<String>,
): Boolean =
    behandlerGodkjenninger.any { godkjenning ->
        godkjenning.helsepersonellkategori?.aktiv != null &&
            godkjenning.autorisasjon?.aktiv == true && godkjenning.helsepersonellkategori.verdi != null &&
            godkjenning.helsepersonellkategori.let {
                it.aktiv && it.verdi in helsepersonerVerdi
            }
    }
