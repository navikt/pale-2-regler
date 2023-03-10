package no.nav.syfo.rules.validation

import no.nav.syfo.client.Behandler
import no.nav.syfo.client.Godkjenning
import no.nav.syfo.client.Kode
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.rules.hpr.getLegeerklaering
import no.nav.syfo.rules.hpr.getReceivedLegeerklaering
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ValidationTest {

    private val ruleTree = ValidationRulesExecution()

    @Test
    internal fun `Should check all the validation rules, Status OK`() {
        val person14Years = LocalDate.now().minusYears(14)

        val receivedLegeerklaering = getReceivedLegeerklaering(getLegeerklaering())

        val behandler = Behandler(
            listOf(
                Godkjenning(
                    autorisasjon = Kode(
                        aktiv = true,
                        oid = 7704,
                        verdi = "1"
                    ),
                    helsepersonellkategori = Kode(
                        aktiv = true,
                        oid = 0,
                        verdi = "LE"
                    )
                )
            )
        )

        val ruleMetadata = RuleMetadata(
            receivedDate = receivedLegeerklaering.mottattDato,
            signatureDate = receivedLegeerklaering.legeerklaering.signaturDato,
            patientPersonNumber = receivedLegeerklaering.personNrPasient,
            legekontorOrgnr = receivedLegeerklaering.legekontorOrgNr,
            tssid = receivedLegeerklaering.tssid,
            avsenderfnr = receivedLegeerklaering.personNrLege,
            patientBorndate = person14Years,
            behandler = behandler,
            doctorSuspensjon = false
        )

        val status = ruleTree.runRules(receivedLegeerklaering.legeerklaering, ruleMetadata)

        Assertions.assertEquals(Status.OK, status.treeResult.status)
        Assertions.assertEquals(
            listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.UGYLDIG_ORGNR_LENGDE to false,
                ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR to false
            ),
            status.rulePath.map { it.rule to it.ruleResult }
        )

        Assertions.assertEquals(
            mapOf(
                "pasientUnder13Aar" to false,
                "ugyldingOrgNummerLengde" to false,
                "avsenderSammeSomPasient" to false

            ),
            status.ruleInputs
        )

        Assertions.assertEquals(null, status.treeResult.ruleHit)
    }

    @Test
    internal fun `Should trigger rule PASIENT_YNGRE_ENN_13, Status INVALID`() {
        val person12Years = LocalDate.now().minusYears(12)

        val receivedLegeerklaering = getReceivedLegeerklaering(getLegeerklaering())

        val behandler = Behandler(
            listOf(
                Godkjenning(
                    autorisasjon = Kode(
                        aktiv = true,
                        oid = 7704,
                        verdi = "1"
                    ),
                    helsepersonellkategori = Kode(
                        aktiv = true,
                        oid = 0,
                        verdi = "LE"
                    )
                )
            )
        )

        val ruleMetadata = RuleMetadata(
            receivedDate = receivedLegeerklaering.mottattDato,
            signatureDate = receivedLegeerklaering.legeerklaering.signaturDato,
            patientPersonNumber = receivedLegeerklaering.personNrPasient,
            legekontorOrgnr = receivedLegeerklaering.legekontorOrgNr,
            tssid = receivedLegeerklaering.tssid,
            avsenderfnr = receivedLegeerklaering.personNrLege,
            patientBorndate = person12Years,
            behandler = behandler,
            doctorSuspensjon = false
        )
        val status = ruleTree.runRules(receivedLegeerklaering.legeerklaering, ruleMetadata)

        Assertions.assertEquals(Status.INVALID, status.treeResult.status)
        Assertions.assertEquals(
            listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to true
            ),
            status.rulePath.map { it.rule to it.ruleResult }
        )

        Assertions.assertEquals(
            mapOf(
                "pasientUnder13Aar" to true

            ),
            status.ruleInputs
        )

        Assertions.assertEquals(ValidationRuleHit.PASIENT_YNGRE_ENN_13.ruleHit, status.treeResult.ruleHit)
    }

    @Test
    internal fun `Should trigger rule UGYLDIG_ORGNR_LENGDE, Status INVALID`() {
        val person31Years = LocalDate.now().minusYears(31)

        val receivedLegeerklaering = getReceivedLegeerklaering(
            legeerklaering = getLegeerklaering(),
            orgnr = "1345666666"
        )

        val behandler = Behandler(
            listOf(
                Godkjenning(
                    autorisasjon = Kode(
                        aktiv = true,
                        oid = 7704,
                        verdi = "1"
                    ),
                    helsepersonellkategori = Kode(
                        aktiv = true,
                        oid = 0,
                        verdi = "LE"
                    )
                )
            )
        )

        val ruleMetadata = RuleMetadata(
            receivedDate = receivedLegeerklaering.mottattDato,
            signatureDate = receivedLegeerklaering.legeerklaering.signaturDato,
            patientPersonNumber = receivedLegeerklaering.personNrPasient,
            legekontorOrgnr = receivedLegeerklaering.legekontorOrgNr,
            tssid = receivedLegeerklaering.tssid,
            avsenderfnr = receivedLegeerklaering.personNrLege,
            patientBorndate = person31Years,
            behandler = behandler,
            doctorSuspensjon = false
        )
        val status = ruleTree.runRules(receivedLegeerklaering.legeerklaering, ruleMetadata)

        Assertions.assertEquals(Status.INVALID, status.treeResult.status)
        Assertions.assertEquals(
            listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.UGYLDIG_ORGNR_LENGDE to true
            ),
            status.rulePath.map { it.rule to it.ruleResult }
        )

        Assertions.assertEquals(
            mapOf(
                "pasientUnder13Aar" to false,
                "ugyldingOrgNummerLengde" to true
            ),
            status.ruleInputs
        )

        Assertions.assertEquals(ValidationRuleHit.UGYLDIG_ORGNR_LENGDE.ruleHit, status.treeResult.ruleHit)
    }

    @Test
    internal fun `Should trigger rule AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR, Status INVALID`() {
        val person31Years = LocalDate.now().minusYears(31)

        val receivedLegeerklaering = getReceivedLegeerklaering(getLegeerklaering())

        val behandler = Behandler(
            listOf(
                Godkjenning(
                    autorisasjon = Kode(
                        aktiv = true,
                        oid = 7704,
                        verdi = "1"
                    ),
                    helsepersonellkategori = Kode(
                        aktiv = true,
                        oid = 0,
                        verdi = "LE"
                    )
                )
            )
        )

        val ruleMetadata = RuleMetadata(
            receivedDate = receivedLegeerklaering.mottattDato,
            signatureDate = receivedLegeerklaering.legeerklaering.signaturDato,
            patientPersonNumber = receivedLegeerklaering.personNrLege,
            legekontorOrgnr = receivedLegeerklaering.legekontorOrgNr,
            tssid = receivedLegeerklaering.tssid,
            avsenderfnr = receivedLegeerklaering.personNrLege,
            patientBorndate = person31Years,
            behandler = behandler,
            doctorSuspensjon = false
        )
        val status = ruleTree.runRules(receivedLegeerklaering.legeerklaering, ruleMetadata)

        Assertions.assertEquals(Status.INVALID, status.treeResult.status)
        Assertions.assertEquals(
            listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.UGYLDIG_ORGNR_LENGDE to false,
                ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR to true
            ),
            status.rulePath.map { it.rule to it.ruleResult }
        )

        Assertions.assertEquals(
            mapOf(
                "pasientUnder13Aar" to false,
                "ugyldingOrgNummerLengde" to false,
                "avsenderSammeSomPasient" to true
            ),
            status.ruleInputs
        )

        Assertions.assertEquals(
            ValidationRuleHit.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR.ruleHit,
            status.treeResult.ruleHit
        )
    }
}
