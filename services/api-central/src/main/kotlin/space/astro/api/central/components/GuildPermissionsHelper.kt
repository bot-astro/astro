package space.astro.api.central.components

import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.stereotype.Component
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.api.central.services.DiscordUserGuildsPersistenceService
import space.astro.shared.core.exceptions.AUnauthorizedException
import space.astro.shared.core.exceptions.ANotFoundException

@Component("guildPermissionsHelper")
class GuildPermissionsHelper(
    private val discordUserGuildsPersistenceService: DiscordUserGuildsPersistenceService
) {
    companion object {
        const val MANAGEABLE_DISCORD_GUILD_REQUEST_ATTRIBUTE = "manageableDiscordGuild"
    }

    fun authedUserCanManageGuild(authPrincipal: AuthPrincipal, guildId: String): Boolean {
        val discordGuild = discordUserGuildsPersistenceService.getUserGuild(authPrincipal.userId, guildId)
            ?: discordUserGuildsPersistenceService.fetchFromDiscord(
                authPrincipal.userId,
                authPrincipal.userDiscordToken
            ).firstOrNull { it.id == guildId }
            ?: throw ANotFoundException("Guild with ID $guildId not found in the user's Discord guilds")

        if (!discordGuild.canManage)
            throw AUnauthorizedException("User is not allowed to manage guild with ID $guildId")

        val currentRequestAttributes = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
        currentRequestAttributes.request.setAttribute(MANAGEABLE_DISCORD_GUILD_REQUEST_ATTRIBUTE, discordGuild)

        return true
    }
}
