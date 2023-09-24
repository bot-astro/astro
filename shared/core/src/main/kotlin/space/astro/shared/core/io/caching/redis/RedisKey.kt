package space.astro.shared.core.io.caching.redis

enum class RedisKey(val key: String) {
    GLOBAL_RATELIMIT("GR"),

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

    TEMPORARY_VCS("vcs")
}
