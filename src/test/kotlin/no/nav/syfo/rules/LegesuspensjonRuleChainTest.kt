package no.nav.syfo.rules

import io.mockk.mockk
import no.nav.syfo.model.Legeerklaering
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

internal class LegesuspensjonRuleChainTest {

    private val legeerklaring = mockk<Legeerklaering>()

    private fun ruleData(
        legeerklaring: Legeerklaering,
        suspended: Boolean
    ): RuleData<Boolean> = RuleData(legeerklaring, suspended)

    @Test
    internal fun `Should check rule BEHANDLER_SUSPENDERT, should trigger rule`() {
        val suspended = true

        LegesuspensjonRuleChain.BEHANDLER_SUSPENDERT(ruleData(legeerklaring, suspended)) shouldBeEqualTo true
    }

    @Test
    internal fun `Should check rule BEHANDLER_SUSPENDERT, should NOT trigger rule`() {
        val suspended = false

        LegesuspensjonRuleChain.BEHANDLER_SUSPENDERT(ruleData(legeerklaring, suspended)) shouldBeEqualTo false
    }
}
