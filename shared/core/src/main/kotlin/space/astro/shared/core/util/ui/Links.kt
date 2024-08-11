package space.astro.shared.core.util.ui

/**
 * Utility class for Astro most used links
 */
object Links {
    const val WEBSITE = "https://astro-bot.space"
    const val INVITE = "https://astro-bot.space/invite"
    const val SUPPORT_SERVER = "https://discord.gg/yeXwVhg"
    const val WEBSITE_PREMIUM = "https://astro-bot.space/ultimate"
    const val APP_DIRECTORY_PREMIUM = "https://discord.com/application-directory/715621848489918495/premium"
    const val DASHBOARD = "https://astro-bot.space/guilds"
    fun GUILD_DASHBOARD(guildId: Long) = "https://astro-bot.space/guilds/$guildId"

    const val LOGO = "https://cdn.discordapp.com/avatars/715621848489918495/dc0affdf8de07a3d88c4d192efad649f.png?size=2048"
    const val INTERFACE_BUTTONS_IMAGE = "$WEBSITE/new-interface-image.png"
    const val EXAMPLE_CUSTOM_INTERFACE = "$WEBSITE/custom-interface-example.png"

    const val EXAMPLE_CONNECTION = "$WEBSITE/role_link_demo.gif"

    object Guides {
        const val ALL = "https://astro-bot.space/guides"
        const val BASIC = "https://astro-bot.space/guides/basic"
        const val GENERATOR = "https://astro-bot.space/guides/generator"
        const val TEMPLATE = "https://astro-bot.space/guides/template"
        const val INTERFACE = "https://astro-bot.space/guides/interface"
        const val VOICE_ROLE = "https://astro-bot.space/guides/voice-role"
    }

    object ExternalGuides {
        const val GET_ID = "https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-"
        const val ROLE_HIERARCHY = "https://support.discord.com/hc/en-us/articles/214836687-Role-Management-101"
        const val COMMAND_PERMISSIONS = "https://discord.com/blog/slash-commands-permissions-discord-apps-bots"
        const val PREMIUM_FAQ = "https://support.discord.com/hc/en-us/articles/9359445233303-Premium-App-Subscriptions-FAQ"
    }
}
