package space.astro.api.central.controllers

import com.fasterxml.jackson.databind.JsonMappingException
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import space.astro.api.central.configs.Mappings
import space.astro.api.central.configs.getUserID
import space.astro.api.central.models.chargebee.SubscriptionWebhookData
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.daos.UserDao
import space.astro.shared.core.models.database.GuildUpgradeData
import space.astro.shared.core.services.chargebee.ChargebeeClientService

private val log = KotlinLogging.logger { }

@RestController
class ChargebeeController(
    val chargebeeClientService: ChargebeeClientService,
    val guildDao: GuildDao,
    val userDao: UserDao
) {
    @GetMapping(Mappings.Chargebee.PORTAL_SESSION)
    suspend fun createPortalSession(
        exchange: ServerWebExchange
    ): ResponseEntity<*> {
        val userID = exchange.getUserID()
        val accessUrl = chargebeeClientService.createPortalSession(userID)

        return if (accessUrl != null)
            ResponseEntity.ok(accessUrl)
        else
            ResponseEntity.badRequest().body(null)
    }

    @PostMapping(Mappings.Chargebee.EVENT_SUB_CREATE)
    suspend fun receiveSubscriptionCreation(@RequestBody subscriptionWebhookData: SubscriptionWebhookData): ResponseEntity<*> {
        // TODO: Handle user and guild subscription creation
        // TODO: Emit event to support-bot
        return ResponseEntity.ok(null)
    }

    @PostMapping(Mappings.Chargebee.EVENT_SUB_CANCEL)
    suspend fun receiveSubscriptionCancellation(@RequestBody subscriptionWebhookData: SubscriptionWebhookData): ResponseEntity<*> {
        // TODO: Handle user subscription creation
        // TODO: Emit event to support-bot
        try {
            val subID = subscriptionWebhookData.content.subscription.id
            val userID = subscriptionWebhookData.content.customer.id

            val user = userDao.get(userID)
            if (user != null) {
                for (guildUpgraded: GuildUpgradeData in user.guildActiveUpgrades) {
                    if (guildUpgraded.subscriptionID == subID) {
                        val guildData = guildDao.get(guildUpgraded.guildID)

                        if (guildData != null) {
                            guildData.upgradedByUserID = null
                            guildDao.save(guildData)
                        }
                    }
                }

                if (user.guildActiveUpgrades.removeIf { it.subscriptionID == subID }) {
                    userDao.save(user)
                }
            }

            return ResponseEntity.ok(null)
        } catch (e: JsonMappingException) {
            log.error("Could not parse chargebee canceled subscription webhook", e)

            return ResponseEntity.badRequest().body(null)
        }
    }
}