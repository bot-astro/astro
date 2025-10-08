package space.astro.api.central.controllers

import com.chargebee.Result
import com.chargebee.models.HostedPage
import com.chargebee.models.Subscription
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import space.astro.api.central.models.chargebee.*
import space.astro.api.central.services.discord.DiscordUserService
import space.astro.api.central.util.getAccessToken
import space.astro.api.central.util.getUserID
import space.astro.shared.core.components.web.CentralApiRoutes
import space.astro.shared.core.configs.PremiumFeaturesConfig
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.daos.UserDao
import space.astro.shared.core.models.database.GuildUpgradeData
import space.astro.shared.core.services.chargebee.ChargebeeClientService
import space.astro.shared.core.services.support.SupportBotApiService
import space.astro.shared.core.util.exceptions.NotFoundException


private val log = KotlinLogging.logger { }

@RestController
@Tag(
    name = "chargebee",
    description = "handling of all chargebee webhooks"
)
class ChargebeeController(
    val chargebeeClientService: ChargebeeClientService,
    val guildDao: GuildDao,
    val userDao: UserDao,
    val supportBotApiService: SupportBotApiService,
    val coroutineScope: CoroutineScope,
    private val discordUserService: DiscordUserService,
    private val premiumFeaturesConfig: PremiumFeaturesConfig
) {
    @GetMapping(CentralApiRoutes.Chargebee.PORTAL_SESSION)
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "portal session created, access url is contained in the response body"
            ),
        ApiResponse(
            responseCode = "400",
            description = "portal session couldn't be created with the provided user ID"
        )
        ]
    )
    suspend fun createPortalSession(
        exchange: ServerWebExchange
    ): ResponseEntity<*> {
        val userID = exchange.getUserID()
        log.info { "creating Chargebee portal session for user $userID" }

        val accessUrl = chargebeeClientService.createPortalSession(userID)

        return if (accessUrl != null)
            ResponseEntity.ok(accessUrl)
        else {
            log.error { "failed creating portal session access url for user $userID" }
            ResponseEntity.badRequest().build<Any>()
        }
    }

    @PostMapping(CentralApiRoutes.Chargebee.CHECKOUT)
    suspend fun createCheckout(
        @RequestBody checkoutBody: CheckoutBody,
        exchange: ServerWebExchange
    ): ResponseEntity<*> {
        val userID = exchange.getUserID()
        val userEmail = discordUserService.fetchSelfUser(exchange.getAccessToken()).email

        val result: Result = HostedPage.checkoutNewForItems()
            .customerId(userID)
            .customerEmail(userEmail)
            .subscriptionItemItemPriceId(0, if (checkoutBody.monthly) premiumFeaturesConfig.monthlyPlanId else premiumFeaturesConfig.yearlyPlanId)
            .subscriptionItemQuantity(0, checkoutBody.quantity)
            .request()

        val hostedPage: HostedPage = result.hostedPage()

        return ResponseEntity.ok(hostedPage.jsonObj.toString())
    }

    @GetMapping(CentralApiRoutes.Chargebee.USER_ACTIVE_SUBSCRIPTIONS)
    suspend fun getUserActiveSubscriptions(
        @PathVariable userID: String,
        exchange: ServerWebExchange
    ): ResponseEntity<*> {
        val userData = userDao.get(userID)
            ?: return ResponseEntity.notFound().build<Any>()

        val activeSubscriptions = chargebeeClientService.getActiveServerSubscriptionsOfUser(userID)

        val subscriptionsInfo = UserSubscriptionsInfo(
            upgradedGuilds = userData.guildActiveUpgrades.map {
                UpgradedGuildInfo(
                    subscriptionId = it.subscriptionID,
                    guildId = it.guildID,
                )
            },
            subscriptions = activeSubscriptions.map {
                val quantities = it.subscription().subscriptionItems().firstOrNull()?.quantity() ?: 0
                val used = userData.guildActiveUpgrades.count { upgrade -> upgrade.subscriptionID == it.subscription().id() }
                val available = quantities - used

                UserSubscription(
                    subscriptionId = it.subscription().id(),
                    annual = it.subscription().billingPeriodUnit() == Subscription.BillingPeriodUnit.YEAR,
                    quantities = quantities,
                    used = used,
                    available = available
                )
            },
        )

        return ResponseEntity.ok(subscriptionsInfo)
    }

    @GetMapping(CentralApiRoutes.Chargebee.LOGGED_USER_ACTIVE_SUBSCRIPTIONS)
    suspend fun getLoggedUserActiveSubscriptions(
        exchange: ServerWebExchange
    ): ResponseEntity<*> {
        val userID = exchange.getUserID()
        val userData = userDao.getOrCreate(userID)

        val activeSubscriptions = chargebeeClientService.getActiveServerSubscriptionsOfUser(userID)

        val subscriptionsInfo = UserSubscriptionsInfo(
            upgradedGuilds = userData.guildActiveUpgrades.map {
                UpgradedGuildInfo(
                    subscriptionId = it.subscriptionID,
                    guildId = it.guildID,
                )
            },
            subscriptions = activeSubscriptions.map {
                val quantities = it.subscription().subscriptionItems().firstOrNull()?.quantity() ?: 0
                val used = userData.guildActiveUpgrades.count { upgrade -> upgrade.subscriptionID == it.subscription().id() }
                val available = quantities - used

                UserSubscription(
                    subscriptionId = it.subscription().id(),
                    annual = it.subscription().billingPeriodUnit() == Subscription.BillingPeriodUnit.YEAR,
                    quantities = quantities,
                    used = used,
                    available = available
                )
            },
        )

        return ResponseEntity.ok(subscriptionsInfo)
    }

    @PostMapping(CentralApiRoutes.Chargebee.EVENT_SUB_CREATE)
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "webhook handled correctly"
            )
        ]
    )
    suspend fun receiveSubscriptionCreation(@RequestBody subscriptionWebhookData: SubscriptionWebhookData): ResponseEntity<*> {
        log.info { "received chargebee subscription creation event" }

        coroutineScope.launch {
            try {
                supportBotApiService.addPremiumRoleToUser(subscriptionWebhookData.content.customer.id)
            } catch (_: NotFoundException) {
                /*
                Don't need to do anything,
                 when the user joins the support server the support-bot will detect it
                 and calculate whether the user should get the premium role
                 */
            }
        }

        return ResponseEntity.noContent().build<Any>()
    }

    @PostMapping(CentralApiRoutes.Chargebee.EVENT_SUB_CANCEL)
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "webhook handled correctly"
            )
        ]
    )
    suspend fun receiveSubscriptionCancellation(@RequestBody subscriptionWebhookData: SubscriptionWebhookData): ResponseEntity<*> {
        log.info { "received chargebee subscription cancellation event" }

        coroutineScope.launch {
            try {
                supportBotApiService.removePremiumRoleFromUser(subscriptionWebhookData.content.customer.id)
            } catch (_: NotFoundException) {
                /*
                Don't need to do anything,
                 when the user joins the support server the support-bot will detect it
                 and calculate whether the user should get the premium role
                 */
            }
        }

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
                        log.debug { "removed premium from guild with ID ${guildData.guildID}" }
                    }
                }
            }

            if (user.guildActiveUpgrades.removeIf { it.subscriptionID == subID }) {
                userDao.save(user)
                log.debug { "updated guild active upgrades for user $userID" }
            }
        }

        return ResponseEntity.noContent().build<Any>()
    }
}