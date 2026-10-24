package space.astro.api.central.controllers.guild_settings

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import space.astro.api.central.components.middlewares.CanManageGuildMiddleware
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.api.central.models.requests.GuildUpgradeBody
import space.astro.api.central.properties.DiscordApplicationProperties
import space.astro.api.central.services.UserSubscriptionsService
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.AUnauthorizedException
import space.astro.shared.core.models.database.guildSettings.GuildEntitlementData
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.models.database.userSettings.UserGuildUpgradeData
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.repositories.mongo.UserRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint

@RestController
@Tag(name = "Premium settings")
class GuildPremiumController(
    private val guildSettingsRepository: GuildSettingsRepository,
    private val userRepository: UserRepository,
    private val userSubscriptionsService: UserSubscriptionsService,
    private val discordApiClient: DiscordApiClient,
    private val discordApplicationProperties: DiscordApplicationProperties,
) {
    @Operation(
        summary = "Upgrade guild",
        description = "Upgrades the guild to premium using one of the available Chargebee subscriptions of the user",
    )
    @PostMapping(CentralApiEndpoint.GUILD_PREMIUM_UPGRADE)
    @CanManageGuildMiddleware
    fun upgradeGuild(
        @PathVariable guildId: String,
        @AuthenticationPrincipal authPrincipal: AuthPrincipal,
        @RequestBody body: GuildUpgradeBody,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        if (guildSettings.upgradedByUserID != null) {
            throw ABadRequestException("This server is already upgraded to premium")
        }

        val user = userSubscriptionsService.getOrCreateUser(authPrincipal.userId)
        val subscription =
            userSubscriptionsService
                .getActiveSubscriptions(user)
                .firstOrNull { it.subscriptionId == body.subscriptionId }
                ?: throw ANotFoundException("Active subscription with ID ${body.subscriptionId} not found")

        if (subscription.available <= 0) {
            throw ABadRequestException("All the upgrades of this subscription are already used")
        }

        user.guildActiveUpgrades.add(
            UserGuildUpgradeData(
                guildID = guildId,
                subscriptionID = subscription.subscriptionId,
                yearly = subscription.annual,
            ),
        )
        userRepository.save(user)

        guildSettings.upgradedByUserID = authPrincipal.userId
        guildSettingsRepository.save(guildSettings)

        return ResponseEntity.ok(guildSettings)
    }

    @Operation(
        summary = "Downgrade guild",
        description = "Removes the premium given to the guild via a Chargebee subscription, frees the upgrade for another guild",
    )
    @DeleteMapping(CentralApiEndpoint.GUILD_PREMIUM_UPGRADE)
    @CanManageGuildMiddleware
    fun downgradeGuild(
        @PathVariable guildId: String,
        @AuthenticationPrincipal authPrincipal: AuthPrincipal,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        if (guildSettings.upgradedByUserID != authPrincipal.userId) {
            throw AUnauthorizedException("Only the user that upgraded the server can downgrade it")
        }

        guildSettings.upgradedByUserID = null
        guildSettingsRepository.save(guildSettings)

        val user = userSubscriptionsService.getOrCreateUser(authPrincipal.userId)
        if (user.guildActiveUpgrades.removeIf { it.guildID == guildId }) {
            userRepository.save(user)
        }

        return ResponseEntity.ok(guildSettings)
    }

    @Operation(
        summary = "Refresh guild entitlements",
        description = "Fetches the entitlements of the guild from Discord and stores the new or changed ones",
    )
    @PostMapping(CentralApiEndpoint.GUILD_PREMIUM_ENTITLEMENTS_REFRESH)
    @CanManageGuildMiddleware
    fun refreshEntitlements(
        @PathVariable guildId: String,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        val discordEntitlements =
            discordApiClient.getEntitlements(
                applicationId = discordApplicationProperties.id,
                botToken = discordApplicationProperties.token,
                guildId = guildId,
            )

        discordEntitlements.forEach { discordEntitlement ->
            val entitlement =
                GuildEntitlementData(
                    id = discordEntitlement.id,
                    skuId = discordEntitlement.skuId,
                    endsAt = discordEntitlement.endsAt?.toInstant()?.toEpochMilli(),
                )

            val index = guildSettings.entitlements.indexOfFirst { it.id == entitlement.id }
            if (index >= 0) {
                guildSettings.entitlements[index] = entitlement
            } else {
                guildSettings.entitlements.add(entitlement)
            }
        }

        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }
}
