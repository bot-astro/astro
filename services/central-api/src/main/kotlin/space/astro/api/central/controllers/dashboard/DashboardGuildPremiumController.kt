package space.astro.api.central.controllers.dashboard

import com.chargebee.models.Subscription
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import space.astro.api.central.configs.DiscordApplicationConfig
import space.astro.api.central.util.getUserID
import space.astro.shared.core.components.web.CentralApiRoutes
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.daos.UserDao
import space.astro.shared.core.models.database.GuildEntitlement
import space.astro.shared.core.models.database.GuildUpgradeData
import space.astro.shared.core.services.chargebee.ChargebeeClientService
import space.astro.shared.core.services.discord.DiscordEntitlementsFetchService

@RestController
@Tag(name = "dashboard-premium")
class DashboardGuildPremiumController(
    private val guildDao: GuildDao,
    private val userDao: UserDao,
    private val chargebeeClientService: ChargebeeClientService,
    private val discordApplicationConfig: DiscordApplicationConfig,
    private val discordEntitlementsFetchService: DiscordEntitlementsFetchService
) {
    @GetMapping(CentralApiRoutes.Dashboard.GUILD_ENTITLEMENTS_REFRESH)
    suspend fun refreshEntitlements(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val guildData = guildDao.get(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        val entitlements = discordEntitlementsFetchService.fetchEntitlements(
            applicationId = discordApplicationConfig.id,
            authToken = discordApplicationConfig.token,
            guildId = guildID,
            userId = null
        )

        entitlements.forEach { entitlement ->
            val entitlementIndex = guildData.entitlements.indexOfFirst { it.id == entitlement.id }
            if (entitlementIndex >= 0) {
                guildData.entitlements[entitlementIndex] = GuildEntitlement(
                    entitlement.id,
                    entitlement.skuId,
                    entitlement.endsAt?.toInstant()?.toEpochMilli()
                )

                guildDao.save(guildData)
            } else {
                guildData.entitlements.add(
                    GuildEntitlement(
                        entitlement.id,
                        entitlement.skuId,
                        entitlement.endsAt?.toInstant()?.toEpochMilli()
                    )
                )

                guildDao.save(guildData)
            }
        }

        return ResponseEntity.ok(guildData)
    }

    @GetMapping(CentralApiRoutes.Dashboard.GUILD_UPGRADE)
    suspend fun upgradeGuild(
        @PathVariable guildID: String,
        @PathVariable subscriptionID: String,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val userID = exchange.getUserID()
        val userData = userDao.getOrCreate(userID)

        val guildData = guildDao.get(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        if (guildData.upgradedByUserID != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Server is already upgraded to ultimate")
        }

        val chargebeeSubscription = chargebeeClientService.getActiveServerSubscriptionsOfUser(userID)
            .firstOrNull { it.subscription().id() == subscriptionID }
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Subscription with the provided ID not found")

        val quantityUsed = userData.guildActiveUpgrades.count { it.subscriptionID == subscriptionID }

        if (quantityUsed >= chargebeeSubscription.subscription().subscriptionItems().first().quantity()) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("You already used all your available upgrades for this subscription")
        }

        userData.guildActiveUpgrades.add(
            GuildUpgradeData(
                guildID,
                subscriptionID,
                chargebeeSubscription.subscription().billingPeriodUnit() == Subscription.BillingPeriodUnit.YEAR
            )
        )

        userDao.save(userData)
        guildData.upgradedByUserID = userID
        guildDao.save(guildData)

        return ResponseEntity.ok().build<Any>()
    }

    @GetMapping(CentralApiRoutes.Dashboard.GUILD_DOWNGRADE)
    suspend fun downgradeGuild(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val userID = exchange.getUserID()
        val userData = userDao.get(userID)
            ?: return ResponseEntity.notFound().build<Any>()

        val guildData = guildDao.get(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        if (guildData.upgradedByUserID != userID) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not the one who upgraded this server")
        }

        guildData.upgradedByUserID = null
        guildDao.save(guildData)
        userData.guildActiveUpgrades.removeIf { it.guildID == guildID }
        userDao.save(userData)

        return ResponseEntity.ok().build<Any>()
    }
}