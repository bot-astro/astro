package space.astro.shared.core.properties.api_central

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("central.api")
data class CentralApiProperties(
    val auth: String,
    val sessionCookieName: String,
    val sessionCookieAllowOrigin: String,
    val sessionCookieDomain: String?,
    val sessionCookieSecure: Boolean,
    val sessionCookieSameSite: String,
    val corsAllowedOrigins: List<String>,
    /** Public base URL of api-central, it is the issuer of the OAuth authorization server used by MCP clients */
    val publicUrl: String,
    /** URL of the MCP server, the only `resource` (RFC 8707) that tokens can be issued for */
    val mcpResourceUrl: String,
    /** Password of the basic auth that Chargebee uses when calling the webhooks */
    val chargebeeWebhookToken: String,
    /** Signing secret of the Vote Tracker forwarding, used to verify the signature of the vote webhooks */
    val voteTrackerWebhookSecret: String,
)
