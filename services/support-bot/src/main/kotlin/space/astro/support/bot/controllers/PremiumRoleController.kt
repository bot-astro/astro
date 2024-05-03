package space.astro.support.bot.controllers

import dev.minn.jda.ktx.coroutines.await
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Entitlement
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import space.astro.support.bot.config.DiscordApplicationConfig

private val log = KotlinLogging.logger {  }

@RestController
@RequestMapping("/premium/role")
class PremiumRoleController(
    private val discordApplicationConfig: DiscordApplicationConfig,
    private val shardManager: ShardManager
) {

    @GetMapping("/add/{userID}")
    suspend fun entitlementCreated(
        @PathVariable userID: String,
    ): ResponseEntity<*> {
        log.info { "Received premium role add event" }

        val guild = shardManager.getGuildById(discordApplicationConfig.guildIdForPremiumRole)
            ?: throw RuntimeException("Could not find guild for premium role with id ${discordApplicationConfig.guildIdForPremiumRole}!")

        val role = guild.getRoleById(discordApplicationConfig.premiumRoleId)
            ?: throw RuntimeException("Could not find role for premium users with id ${discordApplicationConfig.premiumRoleId}!")

        return try {
            val user = guild.retrieveMemberById(userID).await()
                ?: return ResponseEntity.notFound().build<Any>()

            guild.addRoleToMember(user, role).queue()

            log.info { "Added premium role to member with id $userID" }

            ResponseEntity.ok().build<Any>()
        } catch (e: ErrorResponseException) {
            log.error { "Could not find user for premium role: $e" }

            ResponseEntity.notFound().build<Any>()
        }
    }

    @GetMapping("/remove/{userID}")
    suspend fun entitlementUpdated(
        @PathVariable userID: String,
    ): ResponseEntity<*> {
        log.info { "Received premium role remove event" }

        val guild = shardManager.getGuildById(discordApplicationConfig.guildIdForPremiumRole)
            ?: throw RuntimeException("Could not find guild for premium role with id ${discordApplicationConfig.guildIdForPremiumRole}!")

        val role = guild.getRoleById(discordApplicationConfig.premiumRoleId)
            ?: throw RuntimeException("Could not find role for premium users with id ${discordApplicationConfig.premiumRoleId}!")

        return try {
            val user = guild.retrieveMemberById(userID).await()
                ?: return ResponseEntity.notFound().build<Any>()

            guild.addRoleToMember(user, role).queue()

            log.info { "Remove premium role from member with id $userID" }

            ResponseEntity.ok().build<Any>()
        } catch (e: ErrorResponseException) {
            log.error { "Could not find user for premium role: $e" }

            ResponseEntity.notFound().build<Any>()
        }
    }
}