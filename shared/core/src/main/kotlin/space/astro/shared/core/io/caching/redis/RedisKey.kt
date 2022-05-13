package space.astro.shared.core.io.caching.redis

enum class RedisKey(val key: String) {

    DISCORD_USER_CREDENTIALS("DUC:%s"), // USER_ID,
    WEB_SESSION_TOKEN("WST:%s:%s"), // DISCORD_USER_ID:SESSION_TOKEN
    WEB_SESSION_TOKENS("WST:%s:*") // DISCORD_USER_ID
}
