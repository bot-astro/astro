package space.astro.bot.ui

/**
 * Astro most used links
 */
object Links {
    const val SUPPORT_SERVER = "https://discord.gg/yeXwVhg"
    const val DASHBOARD = "https://astro-bot.space/guilds"
    const val GUIDES = "https://astro-bot.space/guides"
    const val GITHUB = "https://github.com/bot-astro/astro"
    const val BLOG_ARTICLE = "https://giuliopime.dev/blog/so-i-built-a-discord-bot"

    fun guildDashboard(guildId: String) = "https://astro-bot.space/guilds/$guildId"
}
