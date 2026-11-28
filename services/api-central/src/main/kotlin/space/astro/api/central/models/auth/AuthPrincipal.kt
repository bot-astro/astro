package space.astro.api.central.models.auth

/**
 * @param mcpGrantId the grant of the token when the request is made by an MCP client, null for the dashboard session
 */
data class AuthPrincipal(
    val sessionId: String,
    val userId: String,
    val userDiscordToken: String,
    val mcpGrantId: String? = null,
)
