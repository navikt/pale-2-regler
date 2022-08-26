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
import no.nav.syfo.pdl.error.PersonNotFoundInPdl
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.util.LoggingMeta
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class PdlServiceTest {
    val pdlClient = mockkClass(PdlClient::class)
    val accessTokenClientV2Mock = mockkClass(AccessTokenClientV2::class)
    val pdlService = PdlPersonService(pdlClient, accessTokenClientV2Mock, "scope")

    val loggingMeta = LoggingMeta("mottakId", "orgNr", "msgId", "legerkærigId")

    @Test
    internal suspend fun `hente person fra pdl`() {
        coEvery { accessTokenClientV2Mock.getAccessTokenV2(any()) } returns "accessToken"
        coEvery { pdlClient.getPerson(any(), any()) } returns GraphQLResponse(
            PdlResponse(
                hentPerson = HentPerson(listOf(Foedsel("1900-01-01"))),
                hentIdenter = Identliste(
                    listOf(
                        IdentInformasjon(
                            ident = "01245678901",
                            gruppe = "FOLKEREGISTERIDENT",
                            historisk = false
                        )
                    )
                )
            ),
            errors = null
        )

        val person = pdlService.getPdlPerson("01245678901", loggingMeta)
        person.fnr shouldBeEqualTo "01245678901"
        person.foedsel?.firstOrNull()?.foedselsdato shouldBeEqualTo "1900-01-01"
    }

    @Test
    internal fun `Skal feile når person ikke finnes`() {
        coEvery { accessTokenClientV2Mock.getAccessTokenV2(any()) } returns "accessToken"
        coEvery { pdlClient.getPerson(any(), any()) } returns GraphQLResponse(
            PdlResponse(null, null),
            errors = null
        )

        val exception = assertFailsWith<PersonNotFoundInPdl> {
            runBlocking {
                pdlService.getPdlPerson("123", loggingMeta)
            }
        }
        exception.message shouldBeEqualTo "Klarte ikke hente ut person fra PDL"
    }

    @Test
    internal fun `Skal feile når ident er tom liste`() {
        coEvery { accessTokenClientV2Mock.getAccessTokenV2(any()) } returns "accessToken"
        coEvery { pdlClient.getPerson(any(), any()) } returns GraphQLResponse(
            PdlResponse(
                hentPerson = HentPerson(
                    foedsel = emptyList()
                ),
                hentIdenter = Identliste(emptyList())
            ),
            errors = null
        )
        val exception = assertFailsWith<PersonNotFoundInPdl> {
            runBlocking {
                pdlService.getPdlPerson("123", loggingMeta)
            }
        }
        exception.message shouldBeEqualTo "Fant ikke ident i PDL"
    }
}
