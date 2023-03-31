package no.nav.syfo.rules.legesuspensjon

import no.nav.syfo.client.Behandler
import no.nav.syfo.client.Godkjenning
import no.nav.syfo.client.Kode
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.rules.hpr.getLegeerklaering
import no.nav.syfo.rules.hpr.getReceivedLegeerklaering
import no.nav.syfo.util.extractBornDate
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class LegeSuspensjonTest {
    private val ruleTree = LegeSuspensjonRulesExecution()

    @Test
    internal fun `Should check all the legesuspensjon rules, Status OK`() {
        val receivedLegeerklaering = getReceivedLegeerklaering(getLegeerklaering())
        val borndate = extractBornDate(receivedLegeerklaering.legeerklaering.pasient.fnr)

        val behandler = Behandler(
            listOf(
                Godkjenning(
                    autorisasjon = Kode(
                        aktiv = true,
                        oid = 7704,
                        verdi = "1",
                    ),
                    helsepersonellkategori = Kode(
                        aktiv = true,
                        oid = 0,
                        verdi = "LE",
                    ),
                ),
            ),
        )

        val ruleMetadata = RuleMetadata(
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
                LegeSuspensjonRules.BEHANDLER_SUSPENDERT to false,
            ),
            status.rulePath.map { it.rule to it.ruleResult },
        )

        Assertions.assertEquals(
            mapOf(
                "suspendert" to false,
            ),
            status.ruleInputs,
        )

        Assertions.assertEquals(null, status.treeResult.ruleHit)
    }

    @Test
    internal fun `Should check all the legesuspensjon rules, Status INVALID`() {
        val receivedLegeerklaering = getReceivedLegeerklaering(getLegeerklaering())
        val borndate = extractBornDate(receivedLegeerklaering.legeerklaering.pasient.fnr)

        val behandler = Behandler(
            listOf(
                Godkjenning(
                    autorisasjon = Kode(
                        aktiv = true,
                        oid = 7704,
                        verdi = "1",
                    ),
                    helsepersonellkategori = Kode(
                        aktiv = true,
                        oid = 0,
                        verdi = "LE",
                    ),
                ),
            ),
        )

        val ruleMetadata = RuleMetadata(
            receivedDate = receivedLegeerklaering.mottattDato,
            signatureDate = receivedLegeerklaering.legeerklaering.signaturDato,
            patientPersonNumber = receivedLegeerklaering.personNrPasient,
            legekontorOrgnr = receivedLegeerklaering.legekontorOrgNr,
            tssid = receivedLegeerklaering.tssid,
            avsenderfnr = receivedLegeerklaering.personNrLege,
            patientBorndate = borndate,
            behandler = behandler,
            doctorSuspensjon = true,
        )

        val status = ruleTree.runRules(receivedLegeerklaering.legeerklaering, ruleMetadata)

        Assertions.assertEquals(Status.INVALID, status.treeResult.status)
        Assertions.assertEquals(
            listOf(
                LegeSuspensjonRules.BEHANDLER_SUSPENDERT to true,
            ),
            status.rulePath.map { it.rule to it.ruleResult },
        )

        Assertions.assertEquals(
            mapOf(
                "suspendert" to true,
            ),
            status.ruleInputs,
        )

        Assertions.assertEquals(LegeSuspensjonRuleHit.BEHANDLER_SUSPENDERT.ruleHit, status.treeResult.ruleHit)
    }
}
