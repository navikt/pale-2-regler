package no.nav.syfo.services

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.DelicateCoroutinesApi
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.client.LegeSuspensjonClient
import no.nav.syfo.client.NorskHelsenettClient
import no.nav.syfo.metrics.RULE_NODE_RULE_HIT_COUNTER
import no.nav.syfo.metrics.RULE_NODE_RULE_PATH_COUNTER
import no.nav.syfo.model.ReceivedLegeerklaering
import no.nav.syfo.model.RuleInfo
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.dsl.printRulePath
import no.nav.syfo.util.LoggingMeta
import no.nav.syfo.util.extractBornDate
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@DelicateCoroutinesApi
class RuleService(
    private val legeSuspensjonClient: LegeSuspensjonClient,
    private val norskHelsenettClient: NorskHelsenettClient,
    private val pdlPersonService: PdlPersonService,
    private val ruleExecutionService: RuleExecutionService,
) {

    private val log: Logger = LoggerFactory.getLogger("ruleservice")

    suspend fun executeRuleChains(
        receivedLegeerklaering: ReceivedLegeerklaering
    ): ValidationResult {
        val loggingMeta =
            LoggingMeta(
                mottakId = receivedLegeerklaering.navLogId,
                orgNr = receivedLegeerklaering.legekontorOrgNr,
                msgId = receivedLegeerklaering.msgId,
                legeerklaeringId = receivedLegeerklaering.legeerklaering.id,
            )

        log.info("Mottatt legeerklæring, validerer mot regler, {}", fields(loggingMeta))

        val legeerklaring = receivedLegeerklaering.legeerklaering

        val doctorSuspend =
            legeSuspensjonClient
                .checkTherapist(
                    receivedLegeerklaering.personNrLege,
                    receivedLegeerklaering.msgId,
                    DateTimeFormatter.ISO_DATE.format(
                        receivedLegeerklaering.legeerklaering.signaturDato
                    ),
                )
                .suspendert

        val avsenderBehandler =
            norskHelsenettClient.finnBehandler(
                behandlerFnr = receivedLegeerklaering.personNrLege,
                msgId = receivedLegeerklaering.msgId,
                loggingMeta = loggingMeta,
            )

        val pdlPerson = pdlPersonService.getPdlPerson(legeerklaring.pasient.fnr, loggingMeta)
        val fodsel = pdlPerson.foedsel?.firstOrNull()
        val borndate =
            if (fodsel?.foedselsdato?.isNotEmpty() == true) {
                log.info("Extracting borndate from PDL date")
                LocalDate.parse(fodsel.foedselsdato)
            } else {
                log.info("Extracting borndate from personNrPasient")
                extractBornDate(legeerklaring.pasient.fnr)
            }

        if (avsenderBehandler == null) {
            return ValidationResult(
                status = Status.INVALID,
                ruleHits =
                    listOf(
                        RuleInfo(
                            ruleName = "BEHANDLER_NOT_IN_HPR",
                            messageForSender =
                                "Den som har skrevet legeerklæringen ble ikke funnet i Helsepersonellregisteret (HPR)",
                            messageForUser =
                                "Avsenders fødselsnummer er ikke registert i Helsepersonellregisteret (HPR)",
                            ruleStatus = Status.INVALID,
                        ),
                    ),
            )
        }

        val ruleMetadata =
            RuleMetadata(
                receivedDate = receivedLegeerklaering.mottattDato,
                signatureDate = receivedLegeerklaering.legeerklaering.signaturDato,
                patientPersonNumber = receivedLegeerklaering.personNrPasient,
                legekontorOrgnr = receivedLegeerklaering.legekontorOrgNr,
                tssid = receivedLegeerklaering.tssid,
                avsenderfnr = receivedLegeerklaering.personNrLege,
                patientBorndate = borndate,
                behandler = avsenderBehandler,
                doctorSuspensjon = doctorSuspend,
            )

        val result = ruleExecutionService.runRules(legeerklaring, ruleMetadata)
        result.forEach {
            RULE_NODE_RULE_PATH_COUNTER.labels(
                    it.printRulePath(),
                )
                .inc()
        }

        val validationResult = validationResult(result.map { it })
        RULE_NODE_RULE_HIT_COUNTER.labels(
                validationResult.status.name,
                validationResult.ruleHits.firstOrNull()?.ruleName ?: validationResult.status.name,
            )
            .inc()

        return validationResult
    }

    private fun validationResult(
        results: List<TreeOutput<out Enum<*>, RuleResult>>
    ): ValidationResult =
        ValidationResult(
            status =
                results
                    .map { result -> result.treeResult.status }
                    .let { it.firstOrNull { status -> status == Status.INVALID } ?: Status.OK },
            ruleHits =
                results
                    .mapNotNull { it.treeResult.ruleHit }
                    .map { result ->
                        RuleInfo(
                            result.rule,
                            result.messageForSender,
                            result.messageForUser,
                            result.status,
                        )
                    },
        )
}
