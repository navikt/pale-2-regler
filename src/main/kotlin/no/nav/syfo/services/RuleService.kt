package no.nav.syfo.services

import kotlinx.coroutines.DelicateCoroutinesApi
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.client.LegeSuspensjonClient
import no.nav.syfo.client.NorskHelsenettClient
import no.nav.syfo.metrics.RULE_HIT_COUNTER
import no.nav.syfo.model.ReceivedLegeerklaering
import no.nav.syfo.model.RuleInfo
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.rules.HPRRuleChain
import no.nav.syfo.rules.LegesuspensjonRuleChain
import no.nav.syfo.rules.Rule
import no.nav.syfo.rules.ValidationRuleChain
import no.nav.syfo.rules.executeFlow
import no.nav.syfo.util.LoggingMeta
import no.nav.syfo.util.extractBornDate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@DelicateCoroutinesApi
class RuleService(
    private val legeSuspensjonClient: LegeSuspensjonClient,
    private val norskHelsenettClient: NorskHelsenettClient,
    private val pdlPersonService: PdlPersonService
) {

    private val log: Logger = LoggerFactory.getLogger("ruleservice")
    suspend fun executeRuleChains(receivedLegeerklaering: ReceivedLegeerklaering): ValidationResult {
        val loggingMeta = LoggingMeta(
            mottakId = receivedLegeerklaering.navLogId,
            orgNr = receivedLegeerklaering.legekontorOrgNr,
            msgId = receivedLegeerklaering.msgId,
            legeerklaeringId = receivedLegeerklaering.legeerklaering.id
        )

        log.info("Mottatt legeerklæring, validerer mot regler, {}", fields(loggingMeta))

        val legeerklaring = receivedLegeerklaering.legeerklaering

        val doctorSuspend = legeSuspensjonClient.checkTherapist(
            receivedLegeerklaering.personNrLege,
            receivedLegeerklaering.msgId,
            DateTimeFormatter.ISO_DATE.format(receivedLegeerklaering.legeerklaering.signaturDato)
        ).suspendert

        val avsenderBehandler = norskHelsenettClient.finnBehandler(
            behandlerFnr = receivedLegeerklaering.personNrLege,
            msgId = receivedLegeerklaering.msgId,
            loggingMeta = loggingMeta
        )

        val pdlPerson = pdlPersonService.getPdlPerson(legeerklaring.pasient.fnr, loggingMeta)
        val fodsel = pdlPerson.foedsel?.firstOrNull()
        val borndate = if (fodsel?.foedselsdato?.isNotEmpty() == true) {
            log.info("Extracting borndate from PDL date")
            LocalDate.parse(fodsel.foedselsdato)
        } else {
            log.info("Extracting borndate from personNrPasient")
            extractBornDate(legeerklaring.pasient.fnr)
        }

        if (avsenderBehandler == null) {
            return ValidationResult(
                status = Status.INVALID,
                ruleHits = listOf(
                    RuleInfo(
                        ruleName = "BEHANDLER_NOT_IN_HPR",
                        messageForSender = "Den som har skrevet legeerklæringen ble ikke funnet i Helsepersonellregisteret (HPR)",
                        messageForUser = "Avsenders fødselsnummer er ikke registert i Helsepersonellregisteret (HPR)",
                        ruleStatus = Status.INVALID
                    )
                )
            )
        }

        val results = listOf(
            ValidationRuleChain.values().executeFlow(
                legeerklaring,
                RuleMetadata(
                    receivedDate = receivedLegeerklaering.mottattDato,
                    signatureDate = receivedLegeerklaering.legeerklaering.signaturDato,
                    patientPersonNumber = receivedLegeerklaering.personNrPasient,
                    legekontorOrgnr = receivedLegeerklaering.legekontorOrgNr,
                    tssid = receivedLegeerklaering.tssid,
                    avsenderfnr = receivedLegeerklaering.personNrLege,
                    patientBorndate = borndate
                )
            ),
            HPRRuleChain.values().executeFlow(legeerklaring, avsenderBehandler),
            LegesuspensjonRuleChain.values().executeFlow(legeerklaring, doctorSuspend)
        ).flatten()

        logRuleResultMetrics(results)

        log.info("Rules hit {}, {}", results.map { it.name }, fields(loggingMeta))

        return validationResult(results)
    }

    private fun validationResult(results: List<Rule<Any>>): ValidationResult = ValidationResult(
        status = results
            .map { status -> status.status }.let {
                it.firstOrNull { status -> status == Status.INVALID }
                    ?: Status.OK
            },
        ruleHits = results.map { rule ->
            RuleInfo(
                rule.name,
                rule.messageForSender!!,
                rule.messageForUser!!,
                rule.status
            )
        }
    )
    private fun logRuleResultMetrics(result: List<Rule<Any>>) {
        result
            .filter { it.name.isEmpty() }
            .forEach {
                RULE_HIT_COUNTER.labels(it.name).inc()
            }
    }
}
