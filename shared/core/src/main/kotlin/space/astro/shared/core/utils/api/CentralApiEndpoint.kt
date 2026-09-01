package space.astro.shared.core.utils.api

object CentralApiEndpoint {
    const val DISCORD_LOGIN = "/v2/auth/discord"
    const val DISCORD_OAUTH_CALLBACK = "/v2/auth/discord/callback"
    const val ME = "/v2/auth/me"
    const val LOGOUT = "/v2/auth/logout"

    const val DISCORD_CHANNEL_RELATED_PERMISSIONS = "/v2/discord/constants/permissions"

    const val DISCORD_SELF_USER = "/v2/discord/users/@me"
    const val DISCORD_USER_GUILDS = "/v2/discord/guilds"
    const val DISCORD_GUILD_CHANNELS = "/v2/discord/guilds/{guildId}/channels"
}