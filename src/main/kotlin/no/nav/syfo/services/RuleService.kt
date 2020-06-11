package no.nav.syfo.services

import io.ktor.util.KtorExperimentalAPI
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.GlobalScope
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.clients.HttpClients
import no.nav.syfo.model.ReceivedLegeerklaering
import no.nav.syfo.model.RuleInfo
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.rules.HPRRuleChain
import no.nav.syfo.rules.LegesuspensjonRuleChain
import no.nav.syfo.rules.Rule
import no.nav.syfo.rules.ValidationRuleChain
import no.nav.syfo.rules.executeFlow
import no.nav.syfo.util.LoggingMeta
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@KtorExperimentalAPI
class RuleService(
    httpClients: HttpClients
) {
    private val legeSuspensjonClient = httpClients.legeSuspensjonClient
    private val norskHelsenettClient = httpClients.norskHelsenettClient

    private val log: Logger = LoggerFactory.getLogger("ruleservice")
    suspend fun executeRuleChains(receivedLegeerklaering: ReceivedLegeerklaering): ValidationResult =
        with(GlobalScope) {

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
                    legeerklaring, RuleMetadata(
                        receivedDate = receivedLegeerklaering.mottattDato,
                        signatureDate = receivedLegeerklaering.legeerklaering.signaturDato,
                        patientPersonNumber = receivedLegeerklaering.personNrPasient,
                        legekontorOrgnr = receivedLegeerklaering.legekontorOrgNr,
                        tssid = receivedLegeerklaering.tssid,
                        avsenderfnr = receivedLegeerklaering.personNrLege
                    )
                ),
                HPRRuleChain.values().executeFlow(legeerklaring, avsenderBehandler),
                LegesuspensjonRuleChain.values().executeFlow(legeerklaring, doctorSuspend)
            ).flatten()

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
}
