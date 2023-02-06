package no.nav.syfo.services

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.Behandler
import no.nav.syfo.client.Godkjenning
import no.nav.syfo.client.Kode
import no.nav.syfo.client.LegeSuspensjonClient
import no.nav.syfo.client.NorskHelsenettClient
import no.nav.syfo.client.Suspendert
import no.nav.syfo.model.Arbeidsgiver
import no.nav.syfo.model.Diagnose
import no.nav.syfo.model.ForslagTilTiltak
import no.nav.syfo.model.FunksjonsOgArbeidsevne
import no.nav.syfo.model.HelsepersonellKategori
import no.nav.syfo.model.Henvisning
import no.nav.syfo.model.Kontakt
import no.nav.syfo.model.Legeerklaering
import no.nav.syfo.model.Pasient
import no.nav.syfo.model.Plan
import no.nav.syfo.model.Prognose
import no.nav.syfo.model.ReceivedLegeerklaering
import no.nav.syfo.model.Signatur
import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykdomsopplysninger
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.pdl.client.model.Foedsel
import no.nav.syfo.pdl.model.PdlPerson
import no.nav.syfo.pdl.service.PdlPersonService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RuleServiceTest {
    private val legeSuspensjonClient = mockk<LegeSuspensjonClient>()
    private val norskHelsenettClient = mockk<NorskHelsenettClient>()
    private val pdlPersonService = mockk<PdlPersonService>()

    // Sette opp en godkjent behandler
    private val legeKode = Kode(aktiv = true, oid = 9060, HelsepersonellKategori.LEGE.verdi)
    private val helsepersonell = Kode(aktiv = true, oid = 7704, HelsepersonellKategori.MANUELLTERAPEUT.verdi)
    private val autorisasjonHPR = Kode(aktiv = true, oid = 7704, verdi = "17")
    private val godkjenningLege = Godkjenning(helsepersonellkategori = helsepersonell, autorisasjon = legeKode)
    private val godkjenningHPR = Godkjenning(helsepersonellkategori = null, autorisasjon = autorisasjonHPR)
    private val behandler = Behandler(listOf(godkjenningLege, godkjenningHPR), hprNummer = null)

    @DelicateCoroutinesApi
    @Test
    fun `Test med utfyllt foedselsdato og pasient under 13 aar`() {
        val foedselsdato = LocalDate.now().minusYears(13).plusDays(1)

        coEvery { legeSuspensjonClient.checkTherapist(any(), any(), any()) } answers { Suspendert(suspendert = false) }
        coEvery { norskHelsenettClient.finnBehandler(any(), any(), any()) } returns behandler
        coEvery { pdlPersonService.getPdlPerson(any(), any()) } returns PdlPerson(
            listOf(),
            listOf(Foedsel(foedselsdato.format(DateTimeFormatter.ISO_DATE)))
        )
        val ruleService = RuleService(legeSuspensjonClient, norskHelsenettClient, pdlPersonService)
        val receivedLegeerklaering = getReceivedLegeerklaering(getLegeerklaering())
        val validationResult: ValidationResult
        runBlocking {
            validationResult = ruleService.executeRuleChains(receivedLegeerklaering)
        }
        assertEquals("PASIENT_YNGRE_ENN_13", validationResult.ruleHits[0].ruleName)
    }

    @DelicateCoroutinesApi
    @Test
    fun `Test med utfyllt foedselsdato`() {
        coEvery { legeSuspensjonClient.checkTherapist(any(), any(), any()) } answers { Suspendert(suspendert = false) }
        coEvery { norskHelsenettClient.finnBehandler(any(), any(), any()) } returns behandler
        coEvery { pdlPersonService.getPdlPerson(any(), any()) } returns PdlPerson(
            listOf(),
            listOf(Foedsel("1985-05-17"))
        )
        val ruleService = RuleService(legeSuspensjonClient, norskHelsenettClient, pdlPersonService)
        val receivedLegeerklaering = getReceivedLegeerklaering(getLegeerklaering())
        val validationResult: ValidationResult
        runBlocking {
            validationResult = ruleService.executeRuleChains(receivedLegeerklaering)
        }
        assertEquals(Status.OK, validationResult.status)
    }

    @DelicateCoroutinesApi
    @Test
    fun `Test uten utfyllt foedselsdato`() {
        coEvery { legeSuspensjonClient.checkTherapist(any(), any(), any()) } answers { Suspendert(suspendert = false) }
        coEvery { norskHelsenettClient.finnBehandler(any(), any(), any()) } returns behandler
        coEvery { pdlPersonService.getPdlPerson(any(), any()) } returns PdlPerson(listOf(), listOf())
        val ruleService = RuleService(legeSuspensjonClient, norskHelsenettClient, pdlPersonService)
        val validationResult: ValidationResult
        val receivedLegeerklaering = getReceivedLegeerklaering(getLegeerklaering())
        runBlocking {
            validationResult = ruleService.executeRuleChains(receivedLegeerklaering)
        }
        assertEquals(Status.OK, validationResult.status)
    }

    @DelicateCoroutinesApi
    @Test
    fun `Test uten utfyllt foedselsdato og pasient under 13 aar`() {
        coEvery { legeSuspensjonClient.checkTherapist(any(), any(), any()) } answers { Suspendert(suspendert = false) }
        coEvery { norskHelsenettClient.finnBehandler(any(), any(), any()) } returns behandler
        coEvery { pdlPersonService.getPdlPerson(any(), any()) } returns PdlPerson(listOf(), listOf())
        val ruleService = RuleService(legeSuspensjonClient, norskHelsenettClient, pdlPersonService)
        val foedselsdato = LocalDate.now().minusYears(13).plusDays(1)
        val foedselsnr = foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + "87654"
        val legeerklaering = getReceivedLegeerklaering(getLegeerklaering(foedselsnr))
        val validationResult: ValidationResult
        runBlocking {
            validationResult = ruleService.executeRuleChains(legeerklaering)
        }
        assertEquals("PASIENT_YNGRE_ENN_13", validationResult.ruleHits[0].ruleName)
    }

    @DelicateCoroutinesApi
    @Test
    fun `Finner ikke behandler`() {
        coEvery { legeSuspensjonClient.checkTherapist(any(), any(), any()) } answers { Suspendert(suspendert = false) }
        coEvery { norskHelsenettClient.finnBehandler(any(), any(), any()) } returns null
        coEvery { pdlPersonService.getPdlPerson(any(), any()) } returns PdlPerson(listOf(), listOf())
        val ruleService = RuleService(legeSuspensjonClient, norskHelsenettClient, pdlPersonService)
        val receivedLegeerklaering = getReceivedLegeerklaering(getLegeerklaering())
        val validationResult: ValidationResult
        runBlocking {
            runBlocking {
                validationResult = ruleService.executeRuleChains(receivedLegeerklaering)
            }
        }
        assertEquals("BEHANDLER_NOT_IN_HPR", validationResult.ruleHits[0].ruleName)
    }

    @Test
    @DelicateCoroutinesApi
    fun `Behandler ikke gyldig`() {
        coEvery { legeSuspensjonClient.checkTherapist(any(), any(), any()) } answers { Suspendert(suspendert = true) }
        coEvery { norskHelsenettClient.finnBehandler(any(), any(), any()) } returns Behandler(
            listOf(
                Godkjenning(
                    Kode(aktiv = false, oid = 1234, verdi = "Kvakksalver")
                )
            )
        )
        coEvery { pdlPersonService.getPdlPerson(any(), any()) } returns PdlPerson(
            identer = listOf(),
            foedsel = listOf()
        )
        val ruleService = RuleService(legeSuspensjonClient, norskHelsenettClient, pdlPersonService)
        val receivedLegeerklaering = getReceivedLegeerklaering(getLegeerklaering())
        val validationResult: ValidationResult
        runBlocking {
            runBlocking {
                validationResult = ruleService.executeRuleChains(receivedLegeerklaering)
            }
        }
        assertEquals("BEHANDLER_IKKE_GYLDIG_I_HPR", validationResult.ruleHits[0].ruleName)
    }
}

fun getReceivedLegeerklaering(legeerklaering: Legeerklaering): ReceivedLegeerklaering {
    return ReceivedLegeerklaering(
        legeerklaering = legeerklaering,
        personNrPasient = "54321",
        pasientAktoerId = "pasientAktoerId",
        personNrLege = "17037447234",
        legeAktoerId = "legeAktoerId",
        navLogId = "navLogId",
        msgId = "msgId",
        legekontorOrgNr = "913459105",
        legekontorHerId = "721636",
        legekontorReshId = "3123",
        legekontorOrgName = "Ensjøbyen Medisinske Senter AS",
        mottattDato = LocalDateTime.now(),
        fellesformat = "fellesformat",
        tssid = "tssid"
    )
}

fun getLegeerklaering(foedselsnr: String = "23057245631"): Legeerklaering {
    return Legeerklaering(
        id = "12314",
        arbeidsvurderingVedSykefravaer = true,
        arbeidsavklaringspenger = true,
        yrkesrettetAttforing = false,
        uforepensjon = true,
        pasient = Pasient(
            fornavn = "Test",
            mellomnavn = "Testerino",
            etternavn = "Testsen",
            fnr = foedselsnr,
            navKontor = "NAV Stockholm",
            adresse = "Oppdiktet veg 99",
            postnummer = 9999,
            poststed = "Stockholm",
            yrke = "Taco spesialist",
            arbeidsgiver = Arbeidsgiver(
                navn = "NAV IKT",
                adresse = "Sannergata 2",
                postnummer = 557,
                poststed = "Oslo"
            )
        ),
        sykdomsopplysninger = Sykdomsopplysninger(
            hoveddiagnose = Diagnose(
                tekst = "Fysikalsk behandling/rehabilitering",
                kode = "-57"
            ),
            bidiagnose = listOf(
                Diagnose(
                    tekst = "Engstelig for hjertesykdom",
                    kode = "K24"
                )
            ),
            arbeidsuforFra = LocalDateTime.now().minusDays(3),
            sykdomshistorie = "Tekst",
            statusPresens = "Tekst",
            borNavKontoretVurdereOmDetErEnYrkesskade = true,
            yrkesSkadeDato = LocalDateTime.now().minusDays(4)
        ),
        plan = Plan(
            utredning = null,
            behandling = Henvisning(
                tekst = "2 timer i uken med svømming",
                dato = LocalDateTime.now(),
                antattVentetIUker = 1
            ),
            utredningsplan = "Tekst",
            behandlingsplan = "Tekst",
            vurderingAvTidligerePlan = "Tekst",
            narSporreOmNyeLegeopplysninger = "Tekst",
            videreBehandlingIkkeAktueltGrunn = "Tekst"
        ),
        forslagTilTiltak = ForslagTilTiltak(
            behov = true,
            kjopAvHelsetjenester = true,
            reisetilskudd = false,
            aktivSykmelding = false,
            hjelpemidlerArbeidsplassen = true,
            arbeidsavklaringspenger = true,
            friskmeldingTilArbeidsformidling = false,
            andreTiltak = "Trenger taco i lunsjen",
            naermereOpplysninger = "Tacoen må bestå av ordentlige råvarer",
            tekst = "Pasienten har store problemer med fordøying av annen mat enn Taco"

        ),
        funksjonsOgArbeidsevne = FunksjonsOgArbeidsevne(
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
            kanIkkeTaAnnetArbeid = "Spise annen mat enn Taco"
        ),
        prognose = Prognose(
            vilForbedreArbeidsevne = true,
            anslattVarighetSykdom = "1 uke",
            anslattVarighetFunksjonsnedsetting = "2 uker",
            anslattVarighetNedsattArbeidsevne = "4 uker"
        ),
        arsakssammenheng = "Funksjonsnedsettelsen har stor betydning for at arbeidsevnen er nedsatt",
        andreOpplysninger = "Tekst",
        kontakt = Kontakt(
            skalKontakteBehandlendeLege = true,
            skalKontakteArbeidsgiver = true,
            skalKontakteBasisgruppe = false,
            kontakteAnnenInstans = null,
            onskesKopiAvVedtak = true
        ),
        tilbakeholdInnhold = false,
        pasientenBurdeIkkeVite = null,
        signatur = Signatur(
            dato = LocalDateTime.now().minusDays(1),
            navn = "Lege Legesen",
            adresse = "Legeveien 33",
            postnummer = "9999",
            poststed = "Stockholm",
            signatur = "Lege Legesen",
            tlfNummer = "98765432"
        ),
        signaturDato = LocalDateTime.now()
    )
}
