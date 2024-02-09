package space.astro.api.central.controllers

import com.fasterxml.jackson.databind.JsonMappingException
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import space.astro.api.central.controllers.body.SubscriptionWebhookData
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.daos.UserDao
import space.astro.shared.core.models.database.GuildUpgradeData
import space.astro.shared.core.services.chargebee.ChargebeeClientService

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/chargebee")
class ChargebeeController(
    val chargebeeClientService: ChargebeeClientService,
    val guildDao: GuildDao,
    val userDao: UserDao
) {
    @GetMapping("/portalSession/{id}")
    suspend fun createPortalSession(@PathVariable id: String): ResponseEntity<*> {
        val accessUrl = chargebeeClientService.createPortalSession(id)
        return if (accessUrl != null)
            ResponseEntity.ok(accessUrl)
        else
            ResponseEntity.badRequest().body(null)
    }

    @PostMapping("/cancel")
    suspend fun receiveSubscriptionCancellation(@RequestBody subscriptionWebhookData: SubscriptionWebhookData): ResponseEntity<*> {
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