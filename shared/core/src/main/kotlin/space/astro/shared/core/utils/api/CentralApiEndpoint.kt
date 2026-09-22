package space.astro.shared.core.utils.api

object CentralApiEndpoint {
    const val DISCORD_LOGIN = "/v2/auth/discord"
    const val DISCORD_OAUTH_CALLBACK = "/v2/auth/discord/callback"
    const val ME = "/v2/auth/me"
    const val LOGOUT = "/v2/auth/logout"

    const val GUILD_ERRORS = "/v2/guilds/{guildId}/errors"
    const val GUILD_SETTINGS = "/v2/guilds/{guildId}/settings"
    const val GUILD_GENERATORS = "/v2/guilds/{guildId}/settings/generators"
    const val GUILD_GENERATOR = "/v2/guilds/{guildId}/settings/generators/{generatorId}"

    const val DISCORD_CHANNEL_RELATED_PERMISSIONS = "/v2/discord/constants/permissions"

    const val DISCORD_SELF_USER = "/v2/discord/users/@me"
    const val DISCORD_USER_GUILDS = "/v2/discord/guilds"
    const val DISCORD_GUILD_CHANNELS = "/v2/discord/guilds/{guildId}/channels"
    const val DISCORD_GUILD_ROLES = "/v2/discord/guilds/{guildId}/roles"
}