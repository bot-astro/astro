package space.astro.support.bot.controllers

import dev.minn.jda.ktx.coroutines.await
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.entitlement.Entitlement
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import space.astro.support.bot.config.DiscordApplicationConfig

private val log = KotlinLogging.logger {  }

@RestController
@RequestMapping("/entitlements")
class EntitlementsController(
    private val discordApplicationConfig: DiscordApplicationConfig,
    private val shardManager: ShardManager
) {

    @PostMapping("/create")
    suspend fun entitlementCreated(@RequestBody entitlement: Entitlement): ResponseEntity<*> {
        if (entitlement.skuId != discordApplicationConfig.premiumSkuId.toString()) {
            return ResponseEntity.badRequest().build<Any>()
        }

        log.info { "Received entitlement create event" }

        val guild = shardManager.getGuildById(discordApplicationConfig.guildIdForPremiumRole)
            ?: throw RuntimeException("Could not find guild for premium role with id ${discordApplicationConfig.guildIdForPremiumRole}!")

        val role = guild.getRoleById(discordApplicationConfig.premiumRoleId)
            ?: throw RuntimeException("Could not find role for premium users with id ${discordApplicationConfig.premiumRoleId}!")

        return try {
            val user = guild.retrieveMemberById(entitlement.userId.toString()).await()
                ?: return ResponseEntity.notFound().build<Any>()

            guild.addRoleToMember(user, role).queue()

            log.info { "Added premium role to member with id ${entitlement.userId}" }

            ResponseEntity.noContent().build<Any>()
        } catch (e: ErrorResponseException) {
            log.error { "Could not find user for premium role: $e" }

            ResponseEntity.notFound().build<Any>()
        }
    }

    @PostMapping("/updated")
    suspend fun entitlementUpdated(@RequestBody entitlement: Entitlement): ResponseEntity<*> {
        if (entitlement.skuId != discordApplicationConfig.premiumSkuId.toString()) {
            return ResponseEntity.badRequest().build<Any>()
        }

        log.info { "Received entitlement update event" }

        val guild = shardManager.getGuildById(discordApplicationConfig.guildIdForPremiumRole)
            ?: throw RuntimeException("Could not find guild for premium role with id ${discordApplicationConfig.guildIdForPremiumRole}!")

        val role = guild.getRoleById(discordApplicationConfig.premiumRoleId)
            ?: throw RuntimeException("Could not find role for premium users with id ${discordApplicationConfig.premiumRoleId}!")

        return try {
            val user = guild.retrieveMemberById(entitlement.userId.toString()).await()
                ?: return ResponseEntity.notFound().build<Any>()

            guild.addRoleToMember(user, role).queue()

            log.info { "Added premium role to member with id ${entitlement.userId}" }

            ResponseEntity.noContent().build<Any>()
        } catch (e: ErrorResponseException) {
            log.error { "Could not find user for premium role: $e" }

            ResponseEntity.notFound().build<Any>()
        }
    }

    @PostMapping("/deleted")
    suspend fun entitlementDeleted(@RequestBody entitlement: Entitlement): ResponseEntity<*> {
        if (entitlement.skuId != discordApplicationConfig.premiumSkuId.toString()) {
            return ResponseEntity.badRequest().build<Any>()
        }

        log.info { "Received entitlement delete event" }

        val guild = shardManager.getGuildById(discordApplicationConfig.guildIdForPremiumRole)
            ?: throw RuntimeException("Could not find guild for premium role with id ${discordApplicationConfig.guildIdForPremiumRole}!")

        val role = guild.getRoleById(discordApplicationConfig.premiumRoleId)
            ?: throw RuntimeException("Could not find role for premium users with id ${discordApplicationConfig.premiumRoleId}!")

        return try {
            val user = guild.retrieveMemberById(entitlement.userId.toString()).await()
                ?: return ResponseEntity.notFound().build<Any>()

            guild.removeRoleFromMember(user, role).queue()

            log.info { "Removed premium role from member with id ${entitlement.userId}" }

            ResponseEntity.noContent().build<Any>()
        } catch (e: ErrorResponseException) {
            log.error { "Could not find user for premium role: $e" }

            ResponseEntity.notFound().build<Any>()
        }
    }
}