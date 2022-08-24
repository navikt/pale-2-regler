package no.nav.syfo.rules

import com.devskiller.jfairy.Fairy
import com.devskiller.jfairy.producer.person.PersonProperties
import com.devskiller.jfairy.producer.person.PersonProvider
import io.mockk.mockk
import no.nav.syfo.model.Legeerklaering
import no.nav.syfo.model.RuleMetadata
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

val fairy: Fairy = Fairy.create() // (Locale("no", "NO"))

class ValidationRuleChainTest {

    private val legeerklaring = mockk<Legeerklaering>()

    fun ruleData(
        legeerklaring: Legeerklaering = mockk(),
        receivedDate: LocalDateTime = LocalDateTime.now(),
        signatureDate: LocalDateTime = LocalDateTime.now(),
        patientPersonNumber: String = "1234567891",
        legekontorOrgNr: String = "123456789",
        tssid: String? = "1314445",
        avsenderfnr: String = "131515",
        patientBorndate: LocalDate = LocalDate.now()
    ): RuleData<RuleMetadata> = RuleData(
        legeerklaring,
        RuleMetadata(signatureDate, receivedDate, patientPersonNumber, legekontorOrgNr, tssid, avsenderfnr, patientBorndate)
    )

    @Test
    internal fun `Should check rule UGYLDIG_FNR_LENGDE, should trigger rule`() {
        ValidationRuleChain.UGYLDIG_FNR_LENGDE_PASIENT(
            ruleData(legeerklaring, patientPersonNumber = "3006310441")
        ) shouldBeEqualTo true
    }

    @Test
    internal fun `Should check rule UGYLDIG_FNR_LENGDE, should NOT trigger rule`() {
        ValidationRuleChain.UGYLDIG_FNR_LENGDE_PASIENT(
            ruleData(legeerklaring, patientPersonNumber = "04030350265")
        ) shouldBeEqualTo false
    }

    @Test
    internal fun `Should check rule UGYLDIG_FNR_PASIENT, should trigger rule`() {
        ValidationRuleChain.UGYLDIG_FNR_PASIENT(
            ruleData(legeerklaring, patientPersonNumber = "30063104424")
        ) shouldBeEqualTo true
    }

    @Test
    internal fun `Should check rule UGYLDIG_FNR, should NOT trigger rule`() {
        ValidationRuleChain.UGYLDIG_FNR_PASIENT(
            ruleData(legeerklaring, patientPersonNumber = "04030350265")
        ) shouldBeEqualTo false
    }

    @Test
    internal fun `Should check rule PASIENT_YNGRE_ENN_13,should trigger rule`() {
        val person = fairy.person(PersonProperties.ageBetween(PersonProvider.MIN_AGE, 12))

        ValidationRuleChain.PASIENT_YNGRE_ENN_13(
            ruleData(
                legeerklaring,
                patientBorndate = person.dateOfBirth
            )
        ) shouldBeEqualTo true
    }

    @Test
    internal fun `Should check rule PASIENT_YNGRE_ENN_13,should NOT trigger rule`() {
        val person = fairy.person(
            PersonProperties.ageBetween(13, 70)
        )

        ValidationRuleChain.PASIENT_YNGRE_ENN_13(
            ruleData(
                legeerklaring,
                patientBorndate = person.dateOfBirth
            )
        ) shouldBeEqualTo false
    }

    @Test
    internal fun `UGYLDIG_ORGNR_LENGDE should trigger on when orgnr lengt is not 9`() {
        ValidationRuleChain.UGYLDIG_ORGNR_LENGDE(
            ruleData(legeerklaring, legekontorOrgNr = "1234567890")
        ) shouldBeEqualTo true
    }

    @Test
    internal fun `UGYLDIG_ORGNR_LENGDE should not trigger on when orgnr is 9`() {
        ValidationRuleChain.UGYLDIG_ORGNR_LENGDE(
            ruleData(legeerklaring, legekontorOrgNr = "123456789")
        ) shouldBeEqualTo false
    }

    @Test
    internal fun `UGYLDIG_FNR_AVSENDER should trigger on rule`() {
        ValidationRuleChain.UGYLDIG_FNR_AVSENDER(
            ruleData(legeerklaring, avsenderfnr = "30063104424")
        ) shouldBeEqualTo true
    }

    @Test
    internal fun `UGYLDIG_FNR_AVSENDER should not trigger on rule`() {
        ValidationRuleChain.UGYLDIG_FNR_AVSENDER(
            ruleData(legeerklaring, avsenderfnr = "04030350265")
        ) shouldBeEqualTo false
    }

    @Test
    internal fun `AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR should trigger on rule`() {
        ValidationRuleChain.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(
            ruleData(legeerklaring, avsenderfnr = "30063104424", patientPersonNumber = "30063104424")
        ) shouldBeEqualTo true
    }

    @Test
    internal fun `AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR should not trigger on rule`() {
        ValidationRuleChain.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(
            ruleData(legeerklaring, avsenderfnr = "04030350265", patientPersonNumber = "04030350261")
        ) shouldBeEqualTo false
    }
}
