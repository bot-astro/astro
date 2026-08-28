package space.astro.shared.core.utils.api

object CentralApiEndpoint {
    const val LOGIN_VIA_DISCORD = "/v2/auth/login/discord"
    const val LOGOUT = "/v2/auth/logout"

    const val DISCORD_CHANNEL_RELATED_PERMISSIONS = "/v2/discord/constants/permissions"

    const val DISCORD_USER_GUILDS = "/v2/discord/guilds"
    const val DISCORD_GUILD_CHANNELS = "/v2/discord/guilds/{guildId}/channels"
}