package space.astro.shared.core.components.web

object BotApiRoutes {
    object Kube {
        const val READY = "/actuator/health/readiness"
        const val LIVENESS = "/actuator/health/liveness"
        const val SHUTDOWN = "/shutdown"
    }

    object Dashboard {
        object Prefixes {
            const val DASHBOARD = "/api"
        }
        
        const val GUILD = "${Prefixes.DASHBOARD}/{guildID}"
        const val GUILD_CHANNELS = "$GUILD/channels"
        const val GUILD_ROLES = "$GUILD/roles"
        const val CREATE_GENERATOR = "$GUILD/generator/create"
        const val CREATE_INTERFACE = "$GUILD/interface/create/{channelID}"
        const val UPDATE_INTERFACE = "$GUILD/interface/update"
        
        const val IS_BOT_IN_GUILD = "$GUILD/is-bot-in-guild"
    }
}