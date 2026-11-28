package space.astro.api.central.models.mcp

/**
 * Authorization request of an MCP client, kept while the user logs in with Discord and gives consent
 *
 * @param clientId the URL of the client metadata document
 * @param userId set once the user is logged in with Discord
 */
data class McpAuthRequest(
    val clientId: String,
    val clientName: String,
    val redirectUri: String,
    val codeChallenge: String,
    val state: String?,
    val scopes: List<String>,
    val resource: String,
    val userId: String? = null,
)

data class McpAuthCode(
    val clientId: String,
    val clientName: String,
    val redirectUri: String,
    val codeChallenge: String,
    val scopes: List<String>,
    val resource: String,
    val userId: String,
)

/**
 * An authorization of a user to an MCP client, shared by all the tokens obtained by refreshing
 *
 * @param createdAt epoch millis
 */
data class McpGrant(
    val grantId: String,
    val userId: String,
    val clientId: String,
    val clientName: String,
    val scopes: List<String>,
    val createdAt: Long,
)

/**
 * Data behind an access or refresh token, the token itself is stored only as a hash
 *
 * @param grantId the grant the token belongs to, it is the same for all the tokens that come from refreshing
 * @param resource the audience of the token
 */
data class McpToken(
    val tokenId: String,
    val grantId: String,
    val userId: String,
    val clientId: String,
    val scopes: List<String>,
    val resource: String,
)

data class McpClientMetadata(
    val clientId: String,
    val clientName: String,
    val redirectUris: List<String>,
)
