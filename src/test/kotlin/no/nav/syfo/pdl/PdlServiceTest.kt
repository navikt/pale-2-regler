package no.nav.syfo.pdl

import io.mockk.coEvery
import io.mockk.mockkClass
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.AccessTokenClientV2
import no.nav.syfo.pdl.client.PdlClient
import no.nav.syfo.pdl.client.model.Foedsel
import no.nav.syfo.pdl.client.model.GraphQLResponse
import no.nav.syfo.pdl.client.model.HentPerson
import no.nav.syfo.pdl.client.model.IdentInformasjon
import no.nav.syfo.pdl.client.model.Identliste
import no.nav.syfo.pdl.client.model.PdlResponse
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.util.LoggingMeta
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PdlServiceTest {
    val pdlClient = mockkClass(PdlClient::class)
    val accessTokenClientV2Mock = mockkClass(AccessTokenClientV2::class)
    val pdlService = PdlPersonService(pdlClient, accessTokenClientV2Mock, "scope")

    val loggingMeta = LoggingMeta("mottakId", "orgNr", "msgId", "legerkærigId")

    @Test
    internal fun `hente person fra pdl`() {
        coEvery { accessTokenClientV2Mock.getAccessTokenV2(any()) } returns "accessToken"
        coEvery { pdlClient.getPerson(any(), any()) } returns
            GraphQLResponse(
                PdlResponse(
                    hentPerson = HentPerson(listOf(Foedsel("1900-01-01"))),
                    hentIdenter =
                        Identliste(
                            listOf(
                                IdentInformasjon(
                                    ident = "01245678901",
                                    gruppe = "FOLKEREGISTERIDENT",
                                    historisk = false,
                                ),
                            ),
                        ),
                ),
                errors = null,
            )

        val person = runBlocking { pdlService.getPdlPerson("01245678901", loggingMeta) }
        assertEquals("01245678901", person.fnr)
        assertEquals("1900-01-01", person.foedselsdato?.firstOrNull()?.foedselsdato)
    }

    @Test
    internal fun `Skal feile naar person ikke finnes`() {
        coEvery { accessTokenClientV2Mock.getAccessTokenV2(any()) } returns "accessToken"
        coEvery { pdlClient.getPerson(any(), any()) } returns
            GraphQLResponse(
                PdlResponse(null, null),
                errors = null,
            )

        val personNotFoundInPdlException: Throwable = assertThrows {
            runBlocking { pdlService.getPdlPerson("123", loggingMeta) }
        }
        assertEquals("Klarte ikke hente ut person fra PDL", personNotFoundInPdlException.message)
    }

    @Test
    internal fun `Skal feile naar ident er tom liste`() {
        coEvery { accessTokenClientV2Mock.getAccessTokenV2(any()) } returns "accessToken"
        coEvery { pdlClient.getPerson(any(), any()) } returns
            GraphQLResponse(
                PdlResponse(
                    hentPerson =
                        HentPerson(
                            foedselsdato = emptyList(),
                        ),
                    hentIdenter = Identliste(emptyList()),
                ),
                errors = null,
            )

        val personNotFoundInPdlException: Throwable = assertThrows {
            runBlocking { pdlService.getPdlPerson("123", loggingMeta) }
        }

        assertEquals("Fant ikke ident i PDL", personNotFoundInPdlException.message)
    }
}
