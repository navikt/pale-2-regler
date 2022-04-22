package no.nav.syfo.rules

import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import no.nav.syfo.model.Legeerklaering
import org.amshove.kluent.shouldBeEqualTo

class LegesuspensjonRuleChainSpek : FunSpec({

    val legeerklaring = mockk<Legeerklaering>()

    fun ruleData(
        legeerklaring: Legeerklaering,
        suspended: Boolean
    ): RuleData<Boolean> = RuleData(legeerklaring, suspended)

    context("Testing validation rules and checking the rule outcomes") {
        test("Should check rule BEHANDLER_SUSPENDERT, should trigger rule") {
            val suspended = true

            LegesuspensjonRuleChain.BEHANDLER_SUSPENDERT(ruleData(legeerklaring, suspended)) shouldBeEqualTo true
        }

        test("Should check rule BEHANDLER_SUSPENDERT, should NOT trigger rule") {
            val suspended = false

            LegesuspensjonRuleChain.BEHANDLER_SUSPENDERT(ruleData(legeerklaring, suspended)) shouldBeEqualTo false
        }
    }
})
