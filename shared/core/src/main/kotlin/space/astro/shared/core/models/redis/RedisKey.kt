package space.astro.shared.core.models.redis

enum class RedisKey(val key: String) {
    /**
     * Format with: BOT_ID
     */
    GLOBAL_RATELIMIT("GR:%s"),

    /**
     * Format with: USER_ID
     */
    GENERATOR_RATELIMIT_FOR_USER("GENERATOR_RATELIMIT:%s"),

    /**
     * Format with: USER_ID
     */
    DISCORD_USER_CREDENTIALS("DUC:%s"),

    /**
     * Format with: DISCORD_USER_ID:SESSION_TOKEN
     */
    WEB_SESSION_TOKEN("WST:%s:%s"),

    /**
     * Format with: DISCORD_USER_ID
     */
    WEB_SESSION_TOKENS("WST:%s:*"),

    TEMPORARY_VCS("TEMP_VCS"),

    GUILD_DATA("GD")
}
