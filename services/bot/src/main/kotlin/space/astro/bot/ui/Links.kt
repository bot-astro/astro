package space.astro.bot.ui

/**
 * Astro most used links
 */
object Links {
    const val SUPPORT_SERVER = "https://discord.gg/yeXwVhg"
    const val DASHBOARD = "https://astro-bot.space/guilds"
    const val GUIDES = "https://astro-bot.space/guides"
    const val VOTE = "https://top.gg/bot/715621848489918495/vote"
    const val APP_DIRECTORY_ULTIMATE = "https://discord.com/discovery/applications/715621848489918495/store/1096107722115661934"
    const val GITHUB = "https://github.com/bot-astro/astro"
    const val BLOG_ARTICLE = "https://giuliopime.dev/blog/so-i-built-a-discord-bot"

    fun guildDashboard(guildId: String) = "https://astro-bot.space/guilds/$guildId"
}
