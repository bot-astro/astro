package space.astro.api.central.controllers.v1

import com.chargebee.v4.models.hostedPage.params.HostedPageCheckoutNewForItemsParams
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import space.astro.api.central.models.chargebee.*
import space.astro.api.central.services.discord.DiscordApiClientService
import space.astro.api.central.util.getAccessToken
import space.astro.api.central.util.getUserID
import space.astro.shared.core.clients.ChargebeeClientHelper
import space.astro.shared.core.properties.PremiumProperties
import tools.jackson.databind.ObjectMapper
import kotlin.collections.iterator


private val log = KotlinLogging.logger { }

@RestController
class ChargebeeController(
    private val chargebeeClientHelper: ChargebeeClientHelper,
    private val discordApiClientService: DiscordApiClientService,
    private val premiumProperties: PremiumProperties,
    private val objectMapper: ObjectMapper,
) {
    @GetMapping(Routes.Chargebee.PORTAL_SESSION)
    suspend fun createPortalSession(
        exchange: ServerWebExchange
    ): ResponseEntity<*> {
        val userID = exchange.getUserID()
        log.info { "creating Chargebee portal session for user $userID" }

        val accessUrl = chargebeeClientHelper.createPortalSession(userID)

        return if (accessUrl != null)
            ResponseEntity.ok(accessUrl)
        else {
            log.error { "failed creating portal session access url for user $userID" }
            ResponseEntity.badRequest().build<Any>()
        }
    }

    @PostMapping(Routes.Chargebee.CHECKOUT)
    suspend fun createCheckout(
        @RequestBody checkoutBody: CheckoutBody,
        exchange: ServerWebExchange
    ): ResponseEntity<*> {
        val userID = exchange.getUserID()
        val userEmail = discordApiClientService.getSelfUser(exchange.getAccessToken()).email


        val chargebeeRes = chargebeeClientHelper.client.hostedPages().checkoutNewForItems(
            HostedPageCheckoutNewForItemsParams.builder()
                .customer(HostedPageCheckoutNewForItemsParams.CustomerParams.builder()
                    .id(userID)
                    .email(userEmail)
                    .build()
                )
                .subscriptionItems(listOf(
                    HostedPageCheckoutNewForItemsParams.SubscriptionItemsParams.builder()
                        .itemPriceId(if (checkoutBody.monthly) premiumProperties.monthlyPlanId else premiumProperties.yearlyPlanId)
                        .quantity(checkoutBody.quantity)
                        .build()
                    )
                )
                .build()
        )

        val hostedPageJson = objectMapper
            .readTree(chargebeeRes.responsePayload())
            .get("hosted_page")
            .toString()

        return ResponseEntity.ok(hostedPageJson)
    }

    @GetMapping(Routes.Chargebee.USER_ACTIVE_SUBSCRIPTIONS)
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

    @GetMapping(Routes.Chargebee.LOGGED_USER_ACTIVE_SUBSCRIPTIONS)
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

    @PostMapping(Routes.Chargebee.EVENT_SUB_CREATE)
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

    @PostMapping(Routes.Chargebee.EVENT_SUB_CANCEL)
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