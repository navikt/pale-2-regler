package no.nav.syfo.model

import no.nav.syfo.client.Behandler
import java.time.LocalDate
import java.time.LocalDateTime

data class RuleMetadata(
    val signatureDate: LocalDateTime,
    val receivedDate: LocalDateTime,
    val patientPersonNumber: String,
    val legekontorOrgnr: String?,
    val tssid: String?,
    val avsenderfnr: String,
    val patientBorndate: LocalDate,
    val behandler: Behandler,
    val doctorSuspensjon: Boolean,
)
