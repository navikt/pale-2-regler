package no.nav.syfo.services

import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.model.Legeerklaering
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.rules.common.RuleExecution
import no.nav.syfo.rules.common.RuleHit
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.TreeOutput
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

enum class TestRules {
    RULE1
}

class RuleExecutionServiceTest {

    val sykmeldnig = mockk<Legeerklaering>(relaxed = true)
    val ruleMetadata = mockk<RuleMetadata>(relaxed = true)
    val rulesExecution = mockk<RuleExecution<TestRules>>(relaxed = true)
    val ruleExecutionService = RuleExecutionService()

    @Test
    fun `Run ruleTrees`() {
        every {
            rulesExecution.runRules(
                any(),
                any()
            )
        } returns (
            TreeOutput<TestRules, RuleResult>(
                treeResult = RuleResult(
                    status = Status.OK,
                    ruleHit = null
                )
            )
            )

        val rule = ruleExecutionService.runRules(sykmeldnig, ruleMetadata, sequenceOf(rulesExecution)).first()
        Assertions.assertEquals(Status.OK, rule.treeResult.status)
    }

    @Test
    fun `should not run all rules if first no OK`() {
        val okRule = mockk<RuleExecution<TestRules>>().also {
            every { it.runRules(any(), any()) } returns (
                TreeOutput<TestRules, RuleResult>(
                    treeResult = RuleResult(
                        status = Status.OK,
                        ruleHit = null
                    )
                )
                )
        }
        val invalidRuleExecution = mockk<RuleExecution<TestRules>>().also {
            every { it.runRules(any(), any()) } returns (
                TreeOutput<TestRules, RuleResult>(
                    treeResult = RuleResult(
                        status = Status.INVALID,
                        ruleHit = RuleHit(Status.INVALID, TestRules.RULE1.name, "message", "message")
                    )
                )
                )
        }
        val results = ruleExecutionService.runRules(sykmeldnig, ruleMetadata, sequenceOf(invalidRuleExecution, okRule))
        Assertions.assertEquals(1, results.size)
        Assertions.assertEquals(Status.INVALID, results.first().treeResult.status)
    }
}
