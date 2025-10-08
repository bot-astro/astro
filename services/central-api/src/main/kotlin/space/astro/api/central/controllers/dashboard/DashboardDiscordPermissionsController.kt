package space.astro.api.central.controllers.dashboard

import io.swagger.v3.oas.annotations.tags.Tag
import net.dv8tion.jda.api.Permission
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import space.astro.shared.core.components.web.CentralApiRoutes

@RestController
@Tag(name = "dashboard-discord-permissions")
class DashboardDiscordPermissionsController {

    companion object DiscordPermissionsInfoSet {
        data class DiscordPermissionInfo(
            val id: String,
            val name: String,
            val channelTypes: List<String>,
            val category: String
        )

        val set = Permission.values().mapNotNull {
            when(it) {
                Permission.MANAGE_CHANNEL -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "general")
                Permission.MANAGE_SERVER -> null
                Permission.VIEW_AUDIT_LOGS -> null
                Permission.VIEW_CHANNEL -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "general")
                Permission.VIEW_GUILD_INSIGHTS -> null
                Permission.MANAGE_ROLES -> null
                Permission.MANAGE_PERMISSIONS -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "general")
                Permission.MANAGE_WEBHOOKS -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "general")
                Permission.MANAGE_GUILD_EXPRESSIONS -> null
                Permission.MANAGE_EVENTS -> null
                Permission.USE_EMBEDDED_ACTIVITIES -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "apps")
                Permission.VIEW_CREATOR_MONETIZATION_ANALYTICS -> null
                Permission.CREATE_GUILD_EXPRESSIONS -> null
                Permission.CREATE_SCHEDULED_EVENTS -> null
                Permission.CREATE_INSTANT_INVITE -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "membership")
                Permission.KICK_MEMBERS -> null
                Permission.BAN_MEMBERS -> null
                Permission.NICKNAME_CHANGE -> null
                Permission.NICKNAME_MANAGE -> null
                Permission.MODERATE_MEMBERS -> null
                Permission.MESSAGE_ADD_REACTION -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "text")
                Permission.MESSAGE_SEND -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "text")
                Permission.MESSAGE_TTS -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "text")
                Permission.MESSAGE_MANAGE -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "text")
                Permission.MESSAGE_EMBED_LINKS -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "text")
                Permission.MESSAGE_ATTACH_FILES -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "text")
                Permission.MESSAGE_HISTORY -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "text")
                Permission.MESSAGE_MENTION_EVERYONE -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "text")
                Permission.MESSAGE_EXT_EMOJI -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "text")
                Permission.USE_APPLICATION_COMMANDS -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "apps")
                Permission.MESSAGE_EXT_STICKER -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "text")
                Permission.MESSAGE_ATTACH_VOICE_MESSAGE -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "text")
                Permission.MESSAGE_SEND_POLLS -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "text")
                Permission.USE_EXTERNAL_APPLICATIONS -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "apps")
                Permission.MANAGE_THREADS -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "text")
                Permission.CREATE_PUBLIC_THREADS -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "text")
                Permission.CREATE_PRIVATE_THREADS -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "text")
                Permission.MESSAGE_SEND_IN_THREADS -> DiscordPermissionInfo(it.name, it.getName(), listOf("text", "voice", "stage"), "text")
                Permission.PRIORITY_SPEAKER -> DiscordPermissionInfo(it.name, it.getName(), listOf("voice", "stage"), "voice")
                Permission.VOICE_STREAM -> DiscordPermissionInfo(it.name, it.getName(), listOf("voice"), "voice")
                Permission.VOICE_CONNECT -> DiscordPermissionInfo(it.name, it.getName(), listOf("voice", "stage"), "voice")
                Permission.VOICE_SPEAK -> DiscordPermissionInfo(it.name, it.getName(), listOf("voice"), "voice")
                Permission.VOICE_MUTE_OTHERS -> DiscordPermissionInfo(it.name, it.getName(), listOf("voice", "stage"), "voice")
                Permission.VOICE_DEAF_OTHERS -> DiscordPermissionInfo(it.name, it.getName(), listOf("voice"), "voice")
                Permission.VOICE_MOVE_OTHERS -> DiscordPermissionInfo(it.name, it.getName(), listOf("voice", "stage"), "voice")
                Permission.VOICE_USE_VAD -> DiscordPermissionInfo(it.name, it.getName(), listOf("voice"), "voice")
                Permission.VOICE_USE_SOUNDBOARD -> DiscordPermissionInfo(it.name, it.getName(), listOf("voice"), "voice")
                Permission.VOICE_USE_EXTERNAL_SOUNDS -> DiscordPermissionInfo(it.name, it.getName(), listOf("voice"), "voice")
                Permission.VOICE_SET_STATUS -> DiscordPermissionInfo(it.name, it.getName(), listOf("voice"), "voice")
                Permission.REQUEST_TO_SPEAK -> DiscordPermissionInfo(it.name, it.getName(), listOf("voice"), "voice")
                Permission.ADMINISTRATOR -> null
                Permission.UNKNOWN -> null

            }
        }
    }

    @GetMapping(CentralApiRoutes.Dashboard.DISCORD_PERMISSIONS)
    suspend fun getAvailableDiscordPermissions(
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        return ResponseEntity.ok(set)
    }
}