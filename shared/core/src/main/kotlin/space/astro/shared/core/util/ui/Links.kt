package space.astro.shared.core.util.ui

/**
 * Utility class for Astro most used links
 */
object Links {
    const val WEBSITE = "https://astro-bot.space"
    const val DASHBOARD = "$WEBSITE/dashboard"
    const val INVITE = "https://discord.com/oauth2/authorize?client_id=715621848489918495&scope=bot+applications.commands&permissions=8"
    const val SUPPORT_SERVER = "https://discord.gg/yeXwVhg"
    const val APP_DIRECTORY_PREMIUM = "https://discord.com/application-directory/715621848489918495/premium"
    const val DEVELOPERS = "$WEBSITE/about"

    const val LOGO = "https://cdn.discordapp.com/avatars/715621848489918495/dc0affdf8de07a3d88c4d192efad649f.png?size=2048"
    const val INTERFACE_BUTTONS_IMAGE = "$WEBSITE/interface-image.png"

    // @Deprecated("topgg isn't used anymore")
    // const val topgg = "https://top.gg/bot/715621848489918495"
    // @Deprecated("topgg isn't used anymore")
    // const val topggLogo = "https://cdn.top.gg/icons/7b707bf868adef7730776f171a2dc709.jpeg"
    // @Deprecated("topgg isn't used anymore")
    // const val vote = "$topgg/vote"
    // const val github = "https://github.com/bot-astro"

    // @Deprecated("Premium not on the website anymore")
    // const val WEBSITE_PREMIUM = "$BASE/premium"
    // const val DOCUMENTATION = "https://docs.astro-bot.space"
    // const val deprecatedPremium = "$DOCUMENTATION/premium/#old-premium-members"

    object Documentation {
        const val DOCUMENTATION = "https://docs.astro-bot.space"
        const val GENERATOR = "https://docs.astro-bot.space/temporary-voice-channels/generators/"
    }

    object ExternalGuides {
        const val GET_ID = "https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-"
        const val ROLE_HIERARCHY = "https://support.discord.com/hc/en-us/articles/214836687-Role-Management-101"
    }
}
