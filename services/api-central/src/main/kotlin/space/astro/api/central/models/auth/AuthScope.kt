package space.astro.api.central.models.auth

/**
 * OAuth scopes of the tokens of MCP clients. `GET` and `HEAD` requests need [GUILDS_READ], every other method needs
 * [GUILDS_WRITE]. Write includes read.
 */
object AuthScope {
    const val GUILDS_READ = "guilds:read"
    const val GUILDS_WRITE = "guilds:write"

    val ALL = listOf(GUILDS_READ, GUILDS_WRITE)

    /**
     * @param requested the space separated `scope` parameter of the authorization request
     * @return the scopes to grant (read only when none is requested), or null if a scope is unknown
     */
    fun parse(requested: String?): List<String>? {
        val scopes = requested?.split(' ')?.filter { it.isNotEmpty() }.orEmpty()
        if (scopes.any { it !in ALL }) return null
        if (scopes.isEmpty()) return listOf(GUILDS_READ)
        return ALL.filter { it in scopes || (it == GUILDS_READ && GUILDS_WRITE in scopes) }
    }
}
