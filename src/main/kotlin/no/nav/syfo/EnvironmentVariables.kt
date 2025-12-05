package no.nav.syfo

data class EnvironmentVariables(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "pale-2-regler"),
    val legeSuspensjonEndpointURL: String =
        getEnvVar("LEGE_SUSPENSJON_PROXY_ENDPOINT_URL", "http://btsys-api.team-rocket"),
    val legeSuspensjonProxyScope: String = getEnvVar("LEGE_SUSPENSJON_PROXY_SCOPE"),
    val norskHelsenettEndpointURL: String = "http://syfohelsenettproxy",
    val aadAccessTokenV2Url: String = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
    val clientIdV2: String = getEnvVar("AZURE_APP_CLIENT_ID"),
    val clientSecretV2: String = getEnvVar("AZURE_APP_CLIENT_SECRET"),
    val helsenettproxyScope: String = getEnvVar("HELSENETT_SCOPE"),
    val jwtIssuer: String = getEnvVar("AZURE_OPENID_CONFIG_ISSUER"),
    val jwkKeysUrl: String = getEnvVar("AZURE_OPENID_CONFIG_JWKS_URI"),
    val pdlScope: String = getEnvVar("PDL_SCOPE"),
    val pdlGraphqlPath: String = getEnvVar("PDL_GRAPHQL_PATH"),
    val texasUrl: String = getEnvVar("NAIS_TOKEN_ENDPOINT"),
    val cluster: String = getEnvVar("NAIS_CLUSTER_NAME"),
    val tsmPdlUrl: String = "http://tsm-pdl-cache.tsm",
    val tsmPdlScope: String = "api://$cluster.tsm.tsm-pdl-cache/.default",
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName)
        ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
