package no.nav.syfo.rules.hpr

import java.time.LocalDateTime
import no.nav.syfo.client.Behandler
import no.nav.syfo.client.Godkjenning
import no.nav.syfo.client.Kode
import no.nav.syfo.model.Arbeidsgiver
import no.nav.syfo.model.Diagnose
import no.nav.syfo.model.ForslagTilTiltak
import no.nav.syfo.model.FunksjonsOgArbeidsevne
import no.nav.syfo.model.Henvisning
import no.nav.syfo.model.Kontakt
import no.nav.syfo.model.Legeerklaering
import no.nav.syfo.model.Pasient
import no.nav.syfo.model.Plan
import no.nav.syfo.model.Prognose
import no.nav.syfo.model.ReceivedLegeerklaering
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Signatur
import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykdomsopplysninger
import no.nav.syfo.util.extractBornDate
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class HPRTest {
    private val ruleTree = HPRRulesExecution()

    @Test
    internal fun `Should check all the hpr rules, Status OK`() {
        val behandler =
            Behandler(
                listOf(
                    Godkjenning(
                        autorisasjon =
                            Kode(
                                aktiv = true,
                                oid = 7704,
                                verdi = "1",
                            ),
                        helsepersonellkategori =
                            Kode(
                                aktiv = true,
                                oid = 0,
                                verdi = "LE",
                            ),
                    ),
                ),
            )

        val receivedLegeerklaering = getReceivedLegeerklaering(getLegeerklaering())
        val borndate = extractBornDate(receivedLegeerklaering.legeerklaering.pasient.fnr)

        val ruleMetadata =
            RuleMetadata(
                receivedDate = receivedLegeerklaering.mottattDato,
                signatureDate = receivedLegeerklaering.legeerklaering.signaturDato,
                patientPersonNumber = receivedLegeerklaering.personNrPasient,
                legekontorOrgnr = receivedLegeerklaering.legekontorOrgNr,
                tssid = receivedLegeerklaering.tssid,
                avsenderfnr = receivedLegeerklaering.personNrLege,
                patientBorndate = borndate,
                behandler = behandler,
                doctorSuspensjon = false,
            )

        val status = ruleTree.runRules(receivedLegeerklaering.legeerklaering, ruleMetadata)

        Assertions.assertEquals(Status.OK, status.treeResult.status)
        Assertions.assertEquals(
            listOf(
                HPRRules.BEHANDLER_IKKE_GYLDIG_I_HPR to false,
                HPRRules.BEHANDLER_MANGLER_AUTORISASJON_I_HPR to false,
                HPRRules.BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR to false,
            ),
            status.rulePath.map { it.rule to it.ruleResult },
        )

        Assertions.assertEquals(
            status.ruleInputs,
            mapOf(
                "behandlerGodkjenninger" to behandler.godkjenninger,
                "behandlerGodkjenninger" to behandler.godkjenninger,
                "behandlerGodkjenninger" to behandler.godkjenninger,
                "behandlerGodkjenninger" to behandler.godkjenninger,
            ),
        )

        Assertions.assertEquals(null, status.treeResult.ruleHit)
    }

    @Test
    internal fun `Should trigger rule BEHANDLER_IKKE_GYLDIG_I_HPR, Status INVALID`() {
        val behandler =
            Behandler(
                listOf(
                    Godkjenning(
                        autorisasjon =
                            Kode(
                                aktiv = false,
                                oid = 7704,
                                verdi = "1",
                            ),
                        helsepersonellkategori =
                            Kode(
                                aktiv = true,
                                oid = 0,
                                verdi = "LE",
                            ),
                    ),
                ),
            )

        val receivedLegeerklaering = getReceivedLegeerklaering(getLegeerklaering())
        val borndate = extractBornDate(receivedLegeerklaering.legeerklaering.pasient.fnr)

        val ruleMetadata =
            RuleMetadata(
                receivedDate = receivedLegeerklaering.mottattDato,
                signatureDate = receivedLegeerklaering.legeerklaering.signaturDato,
                patientPersonNumber = receivedLegeerklaering.personNrPasient,
                legekontorOrgnr = receivedLegeerklaering.legekontorOrgNr,
                tssid = receivedLegeerklaering.tssid,
                avsenderfnr = receivedLegeerklaering.personNrLege,
                patientBorndate = borndate,
                behandler = behandler,
                doctorSuspensjon = false,
            )

        val status = ruleTree.runRules(receivedLegeerklaering.legeerklaering, ruleMetadata)

        Assertions.assertEquals(Status.INVALID, status.treeResult.status)
        Assertions.assertEquals(
            listOf(
                HPRRules.BEHANDLER_IKKE_GYLDIG_I_HPR to true,
            ),
            status.rulePath.map { it.rule to it.ruleResult },
        )

        Assertions.assertEquals(
            status.ruleInputs,
            mapOf(
                "behandlerGodkjenninger" to behandler.godkjenninger,
            ),
        )

        Assertions.assertEquals(
            HPRRuleHit.BEHANDLER_IKKE_GYLDIG_I_HPR.ruleHit,
            status.treeResult.ruleHit
        )
    }

    @Test
    internal fun `Should trigger rule BEHANDLER_MANGLER_AUTORISASJON_I_HPR, Status INVALID`() {
        val behandler =
            Behandler(
                listOf(
                    Godkjenning(
                        autorisasjon =
                            Kode(
                                aktiv = true,
                                oid = 7702,
                                verdi = "19",
                            ),
                        helsepersonellkategori =
                            Kode(
                                aktiv = true,
                                oid = 0,
                                verdi = "LE",
                            ),
                    ),
                ),
            )

        val receivedLegeerklaering = getReceivedLegeerklaering(getLegeerklaering())
        val borndate = extractBornDate(receivedLegeerklaering.legeerklaering.pasient.fnr)

        val ruleMetadata =
            RuleMetadata(
                receivedDate = receivedLegeerklaering.mottattDato,
                signatureDate = receivedLegeerklaering.legeerklaering.signaturDato,
                patientPersonNumber = receivedLegeerklaering.personNrPasient,
                legekontorOrgnr = receivedLegeerklaering.legekontorOrgNr,
                tssid = receivedLegeerklaering.tssid,
                avsenderfnr = receivedLegeerklaering.personNrLege,
                patientBorndate = borndate,
                behandler = behandler,
                doctorSuspensjon = false,
            )

        val status = ruleTree.runRules(receivedLegeerklaering.legeerklaering, ruleMetadata)

        Assertions.assertEquals(Status.INVALID, status.treeResult.status)
        Assertions.assertEquals(
            listOf(
                HPRRules.BEHANDLER_IKKE_GYLDIG_I_HPR to false,
                HPRRules.BEHANDLER_MANGLER_AUTORISASJON_I_HPR to true,
            ),
            status.rulePath.map { it.rule to it.ruleResult },
        )

        Assertions.assertEquals(
            mapOf(
                "behandlerGodkjenninger" to behandler.godkjenninger,
                "behandlerGodkjenninger" to behandler.godkjenninger,
            ),
            status.ruleInputs,
        )

        Assertions.assertEquals(
            HPRRuleHit.BEHANDLER_MANGLER_AUTORISASJON_I_HPR.ruleHit,
            status.treeResult.ruleHit
        )
    }

    @Test
    internal fun `Should trigger rule BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR, Status INVALID`() {
        val behandler =
            Behandler(
                listOf(
                    Godkjenning(
                        autorisasjon =
                            Kode(
                                aktiv = true,
                                oid = 7704,
                                verdi = "18",
                            ),
                        helsepersonellkategori =
                            Kode(
                                aktiv = true,
                                oid = 0,
                                verdi = "PL",
                            ),
                    ),
                ),
            )

        val receivedLegeerklaering = getReceivedLegeerklaering(getLegeerklaering())
        val borndate = extractBornDate(receivedLegeerklaering.legeerklaering.pasient.fnr)

        val ruleMetadata =
            RuleMetadata(
                receivedDate = receivedLegeerklaering.mottattDato,
                signatureDate = receivedLegeerklaering.legeerklaering.signaturDato,
                patientPersonNumber = receivedLegeerklaering.personNrPasient,
                legekontorOrgnr = receivedLegeerklaering.legekontorOrgNr,
                tssid = receivedLegeerklaering.tssid,
                avsenderfnr = receivedLegeerklaering.personNrLege,
                patientBorndate = borndate,
                behandler = behandler,
                doctorSuspensjon = false,
            )

        val status = ruleTree.runRules(receivedLegeerklaering.legeerklaering, ruleMetadata)

        Assertions.assertEquals(Status.INVALID, status.treeResult.status)
        Assertions.assertEquals(
            listOf(
                HPRRules.BEHANDLER_IKKE_GYLDIG_I_HPR to false,
                HPRRules.BEHANDLER_MANGLER_AUTORISASJON_I_HPR to false,
                HPRRules.BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR to true,
            ),
            status.rulePath.map { it.rule to it.ruleResult },
        )

        Assertions.assertEquals(
            mapOf(
                "behandlerGodkjenninger" to behandler.godkjenninger,
                "behandlerGodkjenninger" to behandler.godkjenninger,
                "behandlerGodkjenninger" to behandler.godkjenninger,
            ),
            status.ruleInputs,
        )

        Assertions.assertEquals(
            HPRRuleHit.BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR.ruleHit,
            status.treeResult.ruleHit
        )
    }
}

fun getReceivedLegeerklaering(
    legeerklaering: Legeerklaering,
    orgnr: String = "913459105",
    personnrPasient: String = "54321"
): ReceivedLegeerklaering {
    return ReceivedLegeerklaering(
        legeerklaering = legeerklaering,
        personNrPasient = personnrPasient,
        pasientAktoerId = "pasientAktoerId",
        personNrLege = "17037447234",
        legeAktoerId = "legeAktoerId",
        navLogId = "navLogId",
        msgId = "msgId",
        legekontorOrgNr = orgnr,
        legekontorHerId = "721636",
        legekontorReshId = "3123",
        legekontorOrgName = "Ensjøbyen Medisinske Senter AS",
        mottattDato = LocalDateTime.now(),
        fellesformat = "fellesformat",
        tssid = "tssid",
    )
}

fun getLegeerklaering(foedselsnr: String = "23057245631"): Legeerklaering {
    return Legeerklaering(
        id = "12314",
        arbeidsvurderingVedSykefravaer = true,
        arbeidsavklaringspenger = true,
        yrkesrettetAttforing = false,
        uforepensjon = true,
        pasient =
            Pasient(
                fornavn = "Test",
                mellomnavn = "Testerino",
                etternavn = "Testsen",
                fnr = foedselsnr,
                navKontor = "NAV Stockholm",
                adresse = "Oppdiktet veg 99",
                postnummer = 9999,
                poststed = "Stockholm",
                yrke = "Taco spesialist",
                arbeidsgiver =
                    Arbeidsgiver(
                        navn = "NAV IKT",
                        adresse = "Sannergata 2",
                        postnummer = 557,
                        poststed = "Oslo",
                    ),
            ),
        sykdomsopplysninger =
            Sykdomsopplysninger(
                hoveddiagnose =
                    Diagnose(
                        tekst = "Fysikalsk behandling/rehabilitering",
                        kode = "-57",
                    ),
                bidiagnose =
                    listOf(
                        Diagnose(
                            tekst = "Engstelig for hjertesykdom",
                            kode = "K24",
                        ),
                    ),
                arbeidsuforFra = LocalDateTime.now().minusDays(3),
                sykdomshistorie = "Tekst",
                statusPresens = "Tekst",
                borNavKontoretVurdereOmDetErEnYrkesskade = true,
                yrkesSkadeDato = LocalDateTime.now().minusDays(4),
            ),
        plan =
            Plan(
                utredning = null,
                behandling =
                    Henvisning(
                        tekst = "2 timer i uken med svømming",
                        dato = LocalDateTime.now(),
                        antattVentetIUker = 1,
                    ),
                utredningsplan = "Tekst",
                behandlingsplan = "Tekst",
                vurderingAvTidligerePlan = "Tekst",
                narSporreOmNyeLegeopplysninger = "Tekst",
                videreBehandlingIkkeAktueltGrunn = "Tekst",
            ),
        forslagTilTiltak =
            ForslagTilTiltak(
                behov = true,
                kjopAvHelsetjenester = true,
                reisetilskudd = false,
                aktivSykmelding = false,
                hjelpemidlerArbeidsplassen = true,
                arbeidsavklaringspenger = true,
                friskmeldingTilArbeidsformidling = false,
                andreTiltak = "Trenger taco i lunsjen",
                naermereOpplysninger = "Tacoen må bestå av ordentlige råvarer",
                tekst = "Pasienten har store problemer med fordøying av annen mat enn Taco",
            ),
        funksjonsOgArbeidsevne =
            FunksjonsOgArbeidsevne(
                vurderingFunksjonsevne = "Kan ikke spise annet enn Taco",
                inntektsgivendeArbeid = false,
                hjemmearbeidende = false,
                student = false,
                annetArbeid = "Reisende taco tester",
                kravTilArbeid = "Kun taco i kantina",
                kanGjenopptaTidligereArbeid = true,
                kanGjenopptaTidligereArbeidNa = true,
                kanGjenopptaTidligereArbeidEtterBehandling = true,
                kanTaAnnetArbeid = true,
                kanTaAnnetArbeidNa = true,
                kanTaAnnetArbeidEtterBehandling = true,
                kanIkkeGjenopptaNaverendeArbeid = "Spise annen mat enn Taco",
                kanIkkeTaAnnetArbeid = "Spise annen mat enn Taco",
            ),
        prognose =
            Prognose(
                vilForbedreArbeidsevne = true,
                anslattVarighetSykdom = "1 uke",
                anslattVarighetFunksjonsnedsetting = "2 uker",
                anslattVarighetNedsattArbeidsevne = "4 uker",
            ),
        arsakssammenheng =
            "Funksjonsnedsettelsen har stor betydning for at arbeidsevnen er nedsatt",
        andreOpplysninger = "Tekst",
        kontakt =
            Kontakt(
                skalKontakteBehandlendeLege = true,
                skalKontakteArbeidsgiver = true,
                skalKontakteBasisgruppe = false,
                kontakteAnnenInstans = null,
                onskesKopiAvVedtak = true,
            ),
        tilbakeholdInnhold = false,
        pasientenBurdeIkkeVite = null,
        signatur =
            Signatur(
                dato = LocalDateTime.now().minusDays(1),
                navn = "Lege Legesen",
                adresse = "Legeveien 33",
                postnummer = "9999",
                poststed = "Stockholm",
                signatur = "Lege Legesen",
                tlfNummer = "98765432",
            ),
        signaturDato = LocalDateTime.now(),
    )
}
