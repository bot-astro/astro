package space.astro.api.central.configs

object Mappings {
    object Kube {
        const val READY = "/ready"
        const val SHUTDOWN = "/shutdown"
    }

    object Docs {
        const val SWAGGER = "/swagger"
        const val API_DOCS = "/v3/api-docs"
        const val WEBJARS = "/webjars"
    }

    object Status {
        const val STATUS = "/status"
    }

    object Chargebee {
        object Prefixes {
            const val EVENT = "/chargebee/event"
        }

        const val PORTAL_SESSION = "/chargebee/portal-session"
        const val EVENT_SUB_CREATE = "${Prefixes.EVENT}/sub/create"
        const val EVENT_SUB_CANCEL = "${Prefixes.EVENT}/sub/cancel"
    }

    object Dashboard {
        object Prefixes {
            const val DASHBOARD = "/dashboard"
            const val LOGIN = "${DASHBOARD}/auth/login"
        }
        const val LOGIN = "${Prefixes.LOGIN}/{code}"
        const val LOGOUT = "${Prefixes.DASHBOARD}/auth/logout"

        const val USERS_ME = "${Prefixes.DASHBOARD}/users/@me"

        const val GUILDS = "${Prefixes.DASHBOARD}/guilds"
        const val GUILD = "${GUILDS}/{guildID}"
        const val GUILD_CHANNELS = "${GUILD}/channels"
        const val GUILD_ROLES = "${GUILD}/roles"

        const val GUILD_DATA = "${GUILD}/data"
        const val GUILD_UPDATE_SETTINGS = "${GUILD}/data/settings"
        const val GUILD_CREATE_GENERATOR = "${GUILD}/data/generator"
        const val GUILD_UPDATE_GENERATOR = "${GUILD}/data/generator/{generatorID}"
        const val GUILD_CREATE_INTERFACE = "${GUILD}/data/interface"
        const val GUILD_UPDATE_INTERFACE = "${GUILD}/data/interface/{interfaceID}"
        const val GUILD_CREATE_VOICE_ROLE = "${GUILD}/data/voice-role"
        const val GUILD_UPDATE_VOICE_ROLE = "${GUILD}/data/voice-role/{channelID}"
        const val GUILD_CREATE_TEMPLATE = "${GUILD}/data/template"
        const val GUILD_UPDATE_TEMPLATE = "${GUILD}/data/template/{templateID}"

        const val GUILD_UPGRADE = "${GUILD}/upgrade/{subscriptionID}"
        const val GUILD_DOWNGRADE = "${GUILD}/downgrade"

        const val GUILD_ERRORS = "${GUILD}/errors"

        const val GUILD_TEMPORARY_VOICE_CHANNELS_CACHE = "${GUILD}/vc/cache"
    }
}