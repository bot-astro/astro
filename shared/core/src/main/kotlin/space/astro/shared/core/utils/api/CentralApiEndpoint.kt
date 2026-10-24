package space.astro.shared.core.utils.api

object CentralApiEndpoint {
    const val DISCORD_LOGIN = "/v2/auth/discord"
    const val DISCORD_OAUTH_CALLBACK = "/v2/auth/discord/callback"
    const val ME = "/v2/auth/me"
    const val LOGOUT = "/v2/auth/logout"

    const val ACTUATOR_READINESS = "/actuator/custom/readiness"
    const val ACTUATOR_SHUTDOWN = "/actuator/custom/shutdown"

    const val GUILD_MEDIA = "/v2/guilds/{guildId}/media"
    const val GUILD_ERRORS = "/v2/guilds/{guildId}/errors"
    const val GUILD_SETTINGS = "/v2/guilds/{guildId}/settings"
    const val GUILD_SETTINGS_VC_CACHE = "/v2/guilds/{guildId}/settings/vcs/cache"
    const val GUILD_SETTINGS_PROFILE = "/v2/guilds/{guildId}/settings/profile"
    const val GUILD_SETTINGS_GENERATORS = "/v2/guilds/{guildId}/settings/generators"
    const val GUILD_SETTINGS_GENERATOR = "/v2/guilds/{guildId}/settings/generators/{generatorId}"
    const val GUILD_SETTINGS_INTERFACES = "/v2/guilds/{guildId}/settings/interfaces"
    const val GUILD_SETTINGS_INTERFACE = "/v2/guilds/{guildId}/settings/interfaces/{interfaceId}"
    const val GUILD_SETTINGS_CONNECTIONS = "/v2/guilds/{guildId}/settings/connections"
    const val GUILD_SETTINGS_CONNECTION = "/v2/guilds/{guildId}/settings/connections/{connectionId}"

    const val GUILD_PREMIUM_UPGRADE = "/v2/guilds/{guildId}/premium/upgrade"

    const val CHARGEBEE_PORTAL_SESSION = "/v2/chargebee/portal-session"
    const val CHARGEBEE_CHECKOUT = "/v2/chargebee/checkout"
    const val CHARGEBEE_SUBSCRIPTIONS = "/v2/chargebee/subscriptions"
    const val CHARGEBEE_EVENT_SUBSCRIPTION_CREATE = "/v2/chargebee/events/subscription/create"
    const val CHARGEBEE_EVENT_SUBSCRIPTION_CANCEL = "/v2/chargebee/events/subscription/cancel"

    const val DISCORD_CHANNEL_RELATED_PERMISSIONS = "/v2/discord/constants/permissions"

    const val DISCORD_SELF_USER = "/v2/discord/users/@me"
    const val DISCORD_USER_GUILDS = "/v2/discord/guilds"
    const val DISCORD_GUILD_CHANNELS = "/v2/discord/guilds/{guildId}/channels"
    const val DISCORD_GUILD_ROLES = "/v2/discord/guilds/{guildId}/roles"
}
