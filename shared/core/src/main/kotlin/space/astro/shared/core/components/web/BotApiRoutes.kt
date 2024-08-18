package space.astro.shared.core.components.web

object BotApiRoutes {
    object Kube {
        const val READY = "/ready"
        const val LIVENESS = "/actuator/health/liveness"
        const val SHUTDOWN = "/shutdown"
    }


    object DASHBOARD {
        const val IS_BOT_IN_GUILD = "/api/{guildID}/is-bot-in-guild"
        const val GUILD_CHANNELS = "/api/{guildID}/channels"
        const val GUILD_ROLES = "/api/{guildID}/roles"
        const val CREATE_GENERATOR = "/api/{guildID}/generator/create"
        const val CREATE_INTERFACE = "/api/{guildID}/interface/create/{channelID}"
        const val UPDATE_INTERFACE = "/api/{guildID}/interface/update"
    }
}