package space.astro.support.bot.controllers

import net.dv8tion.jda.api.entities.entitlement.Entitlement
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import space.astro.support.bot.config.DiscordApplicationConfig

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

        val guild = shardManager.getGuildById(discordApplicationConfig.guildIdForPremiumRole)
            ?: throw RuntimeException("Could not find guild for premium role with id ${discordApplicationConfig.guildIdForPremiumRole}!")

        val role = guild.getRoleById(discordApplicationConfig.premiumRoleId)
            ?: throw RuntimeException("Could not find role for premium users with id ${discordApplicationConfig.premiumRoleId}!")

        val user = guild.getMemberById(entitlement.userId.toString())
            ?: return ResponseEntity.notFound().build<Any>()

        guild.addRoleToMember(user, role).queue()

        return ResponseEntity.noContent().build<Any>()
    }

    @PostMapping("/updated")
    suspend fun entitlementUpdated(@RequestBody entitlement: Entitlement): ResponseEntity<*> {
        if (entitlement.skuId != discordApplicationConfig.premiumSkuId.toString()) {
            return ResponseEntity.badRequest().build<Any>()
        }

        val guild = shardManager.getGuildById(discordApplicationConfig.guildIdForPremiumRole)
            ?: throw RuntimeException("Could not find guild for premium role with id ${discordApplicationConfig.guildIdForPremiumRole}!")

        val role = guild.getRoleById(discordApplicationConfig.premiumRoleId)
            ?: throw RuntimeException("Could not find role for premium users with id ${discordApplicationConfig.premiumRoleId}!")

        val user = guild.getMemberById(entitlement.userId.toString())
            ?: return ResponseEntity.notFound().build<Any>()

        guild.addRoleToMember(user, role).queue()

        return ResponseEntity.noContent().build<Any>()
    }

    @PostMapping("/deleted")
    suspend fun entitlementDeleted(@RequestBody entitlement: Entitlement): ResponseEntity<*> {
        if (entitlement.skuId != discordApplicationConfig.premiumSkuId.toString()) {
            return ResponseEntity.badRequest().build<Any>()
        }

        val guild = shardManager.getGuildById(discordApplicationConfig.guildIdForPremiumRole)
            ?: throw RuntimeException("Could not find guild for premium role with id ${discordApplicationConfig.guildIdForPremiumRole}!")

        val role = guild.getRoleById(discordApplicationConfig.premiumRoleId)
            ?: throw RuntimeException("Could not find role for premium users with id ${discordApplicationConfig.premiumRoleId}!")

        val user = guild.getMemberById(entitlement.userId.toString())
            ?: return ResponseEntity.notFound().build<Any>()

        guild.removeRoleFromMember(user, role).queue()

        return ResponseEntity.noContent().build<Any>()
    }
}