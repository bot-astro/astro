package space.astro.support.bot.components

import dev.minn.jda.ktx.coroutines.await
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Component
import space.astro.shared.core.clients.ChargebeeClientHelper
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.AUnknownException
import space.astro.support.bot.properties.SupportServerProperties
import java.time.OffsetDateTime
import java.time.ZoneOffset

private val log = KotlinLogging.logger { }

/**
 * Handles the premium role of the support server
 */
@Component
class PremiumRoleService(
    private val shardManager: ShardManager,
    private val chargebeeClientHelper: ChargebeeClientHelper,
    private val discordApiClient: DiscordApiClient,
    private val supportServerProperties: SupportServerProperties,
) {
    /**
     * Checks if the user has an active premium subscription on Chargebee
     * or an active entitlement for the premium SKU on Discord
     *
     * This function is blocking.
     */
    fun hasPremium(userId: String): Boolean {
        if (chargebeeClientHelper.getActiveServerSubscriptionsOfUser(userId).isNotEmpty()) {
            return true
        }

        val now = OffsetDateTime.now(ZoneOffset.UTC)

        return discordApiClient
            .getEntitlements(
                applicationId = supportServerProperties.entitlementsApplicationId,
                botToken = supportServerProperties.entitlementsApplicationToken,
                userId = userId,
            ).any { it.skuId == supportServerProperties.premiumSkuId && it.endsAt?.isAfter(now) != false }
    }

    /**
     * Gives the premium role to the user
     *
     * @throws ANotFoundException if the user is not in the support server
     */
    suspend fun addPremiumRole(userId: String) {
        val guild = getGuild()
        val role = getPremiumRole(guild)
        val member = retrieveMember(guild, userId)

        guild.addRoleToMember(member, role).reason("Premium role added").await()

        log.info { "Added premium role to member with id $userId" }
    }

    /**
     * Removes the premium role from the user
     *
     * @throws ANotFoundException if the user is not in the support server
     */
    suspend fun removePremiumRole(userId: String) {
        val guild = getGuild()
        val role = getPremiumRole(guild)
        val member = retrieveMember(guild, userId)

        guild.removeRoleFromMember(member, role).reason("Premium role removed").await()

        log.info { "Removed premium role from member with id $userId" }
    }

    private fun getGuild(): Guild =
        shardManager.getGuildById(supportServerProperties.guildId)
            ?: throw AUnknownException("Support server with id ${supportServerProperties.guildId} not found")

    private fun getPremiumRole(guild: Guild): Role =
        guild.getRoleById(supportServerProperties.premiumRoleId)
            ?: throw AUnknownException("Premium role with id ${supportServerProperties.premiumRoleId} not found")

    private suspend fun retrieveMember(
        guild: Guild,
        userId: String,
    ): Member {
        if (userId.toLongOrNull() == null) {
            throw ABadRequestException("Invalid user id $userId")
        }

        try {
            return guild.retrieveMemberById(userId).await()
        } catch (e: ErrorResponseException) {
            when (e.errorResponse) {
                ErrorResponse.UNKNOWN_MEMBER, ErrorResponse.UNKNOWN_USER ->
                    throw ANotFoundException("User with id $userId is not in the support server", e)
                else -> throw AUnknownException(e.message ?: "Error from Discord API", e)
            }
        }
    }
}
