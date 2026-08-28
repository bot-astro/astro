package space.astro.shared.core.utils.redis

//enum class RedisKey(val key: String) {
//    /**
//     * Format with: BOT_ID
//     */
//    GLOBAL_RATELIMIT("GR:%s"),
//
//    /**
//     * Generic key for Redis rate limiting implementation
//     *
//     * Format with: NAMESPACE+KEY
//     */
//    RATELIMIT("RATELIMIT:%s:%s"),
//
//    GENERATOR_RATELIMIT_FOR_USER("GENR"),
//
//    COMMAND_RATELIMIT_FOR_USER("CMDR"),
//
//    /**
//     * Format with: USER_ID
//     */
//    DISCORD_USER_CREDENTIALS("DUC:%s"),
//
//    /**
//     * Format with: DISCORD_USER_ID:SESSION_TOKEN
//     */
//    WEB_SESSION_TOKEN("WST:%s:%s"),
//
//    /**
//     * Format with: DISCORD_USER_ID
//     */
//    WEB_SESSION_TOKENS("WST:%s:*"),
//
//    TEMPORARY_VCS("TEMP_VCS"),
//
//    GUILD_DATA("GD"),
//
//    USER_DATA("UD"),
//
//    /**
//     * Format with: GUILD_ID
//     */
//    DASHBOARD_GUILDS("DASH_GUILDS"),
//
//    VOTES("VOTES")
//}

object RedisKey {
    fun RATE_LIMIT(namespace: String, key: String) = "RL:$namespace:$key"
    fun AUTH_SESSION(userId: String, sessionId: String) = "AT:$userId:$sessionId"
    fun DISCORD_USER_TOKEN(userId: String) = "DUT:$userId"
    fun DISCORD_USER_GUILDS() = "DUG"
}
