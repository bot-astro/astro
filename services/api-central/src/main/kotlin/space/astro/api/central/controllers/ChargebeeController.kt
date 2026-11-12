package space.astro.api.central.controllers

import com.chargebee.v4.models.hostedPage.params.HostedPageCheckoutNewForItemsParams
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.api.central.models.requests.ChargebeeCheckoutBody
import space.astro.api.central.models.requests.ChargebeeWebhookBody
import space.astro.api.central.models.responses.UserSubscriptionsResponse
import space.astro.api.central.services.UserSubscriptionsService
import space.astro.shared.core.clients.ChargebeeClientHelper
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.clients.SupportBotApiClient
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.AUnknownException
import space.astro.shared.core.properties.ChargebeeClientProperties
import space.astro.shared.core.properties.PremiumProperties
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.repositories.mongo.UserRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint
import tools.jackson.databind.json.JsonMapper

private val log = KotlinLogging.logger { }

@RestController
@Tag(name = "Chargebee")
class ChargebeeController(
    private val chargebeeClientHelper: ChargebeeClientHelper,
    private val chargebeeClientProperties: ChargebeeClientProperties,
    private val premiumProperties: PremiumProperties,
    private val discordApiClient: DiscordApiClient,
    private val supportBotApiClient: SupportBotApiClient,
    private val userSubscriptionsService: UserSubscriptionsService,
    private val userRepository: UserRepository,
    private val guildSettingsRepository: GuildSettingsRepository,
    private val jsonMapper: JsonMapper,
) {
    @Operation(
        summary = "Create portal session",
        description = "Returns the url of the Chargebee portal where the user can manage the subscriptions",
    )
    @GetMapping(CentralApiEndpoint.CHARGEBEE_PORTAL_SESSION)
    fun createPortalSession(
        @AuthenticationPrincipal authPrincipal: AuthPrincipal,
    ): ResponseEntity<String> {
        val accessUrl =
            chargebeeClientHelper.createPortalSession(authPrincipal.userId)
                ?: throw AUnknownException("Failed to create the portal session of user ${authPrincipal.userId}")

        return ResponseEntity.ok(accessUrl)
    }

    @Operation(
        summary = "Create checkout",
        description = "Returns the Chargebee hosted page (as JSON) that the user has to complete to buy the subscription",
    )
    @PostMapping(CentralApiEndpoint.CHARGEBEE_CHECKOUT)
    fun createCheckout(
        @AuthenticationPrincipal authPrincipal: AuthPrincipal,
        @RequestBody checkoutBody: ChargebeeCheckoutBody,
    ): ResponseEntity<String> {
        if (checkoutBody.quantity < 1) {
            throw ABadRequestException("The quantity must be at least 1")
        }

        val userEmail = discordApiClient.getSelfUser(authPrincipal.userDiscordToken).email

        val chargebeeRes =
            chargebeeClientHelper.client.hostedPages().checkoutNewForItems(
                HostedPageCheckoutNewForItemsParams
                    .builder()
                    .customer(
                        HostedPageCheckoutNewForItemsParams.CustomerParams
                            .builder()
                            .id(authPrincipal.userId)
                            .email(userEmail)
                            .build(),
                    ).subscriptionItems(
                        listOf(
                            HostedPageCheckoutNewForItemsParams.SubscriptionItemsParams
                                .builder()
                                .itemPriceId(if (checkoutBody.monthly) premiumProperties.monthlyPlanId else premiumProperties.yearlyPlanId)
                                .quantity(checkoutBody.quantity)
                                .build(),
                        ),
                    ).build(),
            )

        val hostedPageJson =
            jsonMapper
                .readTree(chargebeeRes.responsePayload())
                .get("hosted_page")
                .toString()

        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(hostedPageJson)
    }

    @Operation(
        summary = "Get subscriptions",
        description = "Active subscriptions of the user and the guilds that they upgraded with them",
    )
    @GetMapping(CentralApiEndpoint.CHARGEBEE_SUBSCRIPTIONS)
    fun getSubscriptions(
        @AuthenticationPrincipal authPrincipal: AuthPrincipal,
    ): ResponseEntity<UserSubscriptionsResponse> {
        val user = userSubscriptionsService.getOrCreateUser(authPrincipal.userId)

        return ResponseEntity.ok(userSubscriptionsService.getSubscriptionsInfo(user))
    }

    @Operation(
        summary = "Subscription created webhook",
        description = "Called by Chargebee, authenticated with basic auth by the security config",
    )
    @PostMapping(CentralApiEndpoint.CHARGEBEE_EVENT_SUBSCRIPTION_CREATE)
    fun receiveSubscriptionCreation(
        @RequestBody body: ChargebeeWebhookBody,
    ): ResponseEntity<Void> {
        val userId = body.content.customer.id
        if (body.content.subscription.subscriptionItems.none {
                it.itemPriceId.startsWith(
                    chargebeeClientProperties.serverUltimatePlanId,
                )
            }
        ) {
            return ResponseEntity.noContent().build()
        }

        log.info { "Received Chargebee subscription creation event for user $userId" }

        // failures are only logged, Chargebee would retry the webhook for nothing
        try {
            supportBotApiClient.addPremiumRole(userId)
        } catch (_: ANotFoundException) {
            // not in the support server, the support bot calculates whether they should get the role when they join
        } catch (e: Exception) {
            log.error(e) { "Failed to add the premium role to user $userId" }
        }

        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Subscription cancelled webhook",
        description = "Called by Chargebee, authenticated with basic auth by the security config",
    )
    @PostMapping(CentralApiEndpoint.CHARGEBEE_EVENT_SUBSCRIPTION_CANCEL)
    fun receiveSubscriptionCancellation(
        @RequestBody body: ChargebeeWebhookBody,
    ): ResponseEntity<Void> {
        val userId = body.content.customer.id
        val subscriptionId = body.content.subscription.id
        if (body.content.subscription.subscriptionItems.none {
                it.itemPriceId.startsWith(
                    chargebeeClientProperties.serverUltimatePlanId,
                )
            }
        ) {
            return ResponseEntity.noContent().build()
        }

        log.info { "Received Chargebee subscription cancellation event for user $userId" }

        removeGuildUpgrades(userId, subscriptionId)

        return ResponseEntity.noContent().build()
    }

    /**
     * Removes the premium from all the guilds that the user upgraded with the cancelled subscription
     */
    private fun removeGuildUpgrades(
        userId: String,
        subscriptionId: String,
    ) {
        val user = userRepository.findByUserID(userId) ?: return

        user.guildActiveUpgrades
            .filter { it.subscriptionID == subscriptionId }
            .forEach { upgrade ->
                val guildSettings = guildSettingsRepository.findByGuildID(upgrade.guildID) ?: return@forEach

                if (guildSettings.upgradedByUserID == userId) {
                    guildSettings.upgradedByUserID = null
                    guildSettingsRepository.save(guildSettings)
                    log.debug { "Removed premium from guild ${upgrade.guildID}" }
                }
            }

        if (user.guildActiveUpgrades.removeIf { it.subscriptionID == subscriptionId }) {
            userRepository.save(user)
            log.debug { "Updated guild active upgrades of user $userId" }
        }
    }
}
