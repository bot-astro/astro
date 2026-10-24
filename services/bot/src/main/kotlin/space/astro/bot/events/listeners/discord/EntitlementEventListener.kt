package space.astro.bot.events.listeners.discord

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Entitlement
import net.dv8tion.jda.api.events.entitlement.EntitlementCreateEvent
import net.dv8tion.jda.api.events.entitlement.EntitlementDeleteEvent
import net.dv8tion.jda.api.events.entitlement.EntitlementUpdateEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.shared.core.clients.SupportBotApiClient
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.models.database.guildSettings.GuildEntitlementData
import space.astro.shared.core.properties.PremiumProperties
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository

private val log = KotlinLogging.logger { }

@Component
class EntitlementEventListener(
    private val guildSettingsRepository: GuildSettingsRepository,
    private val premiumProperties: PremiumProperties,
    private val supportBotApiClient: SupportBotApiClient,
) {
    // IO because the repositories are blocking
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }

    @EventListener
    fun onEntitlementCreate(event: EntitlementCreateEvent) = upsertEntitlement(event.jda, event.entitlement)

    @EventListener
    fun onEntitlementUpdate(event: EntitlementUpdateEvent) = upsertEntitlement(event.jda, event.entitlement)

    @EventListener
    fun onEntitlementDelete(event: EntitlementDeleteEvent) {
        val entitlement = event.entitlement
        val guildId = entitlement.serverEntitlementGuildId() ?: return

        scope.launch {
            try {
                val guildSettings = guildSettingsRepository.findByGuildID(guildId) ?: return@launch

                if (guildSettings.entitlements.removeIf { it.id == entitlement.id }) {
                    guildSettingsRepository.save(guildSettings)
                }
            } catch (e: Exception) {
                log.error(e) { "Unhandled exception while handling an entitlement event in guild $guildId" }
            }

            // failures are only logged, when the user is not in the support server the support bot calculates the role on join
            try {
                supportBotApiClient.removePremiumRole(entitlement.userId)
            } catch (_: ANotFoundException) {
                // not in the support server
            } catch (e: Exception) {
                log.error(e) { "Failed to remove the premium role of user ${entitlement.userId}" }
            }
        }
    }

    private fun upsertEntitlement(
        jda: JDA,
        entitlement: Entitlement,
    ) {
        val guildId = entitlement.serverEntitlementGuildId() ?: return

        scope.launch {
            try {
                val guildSettings =
                    guildSettingsRepository.findByGuildID(guildId)
                        ?: guildSettingsRepository.createNewGuildSettings(
                            guildID = guildId,
                            locale = jda.getGuildById(guildId)?.locale?.locale ?: "en-US",
                        )

                val entitlementData =
                    GuildEntitlementData(
                        id = entitlement.id,
                        skuId = entitlement.skuId,
                        endsAt = entitlement.timeEnding?.toInstant()?.toEpochMilli(),
                    )

                val index = guildSettings.entitlements.indexOfFirst { it.id == entitlement.id }
                if (index >= 0) {
                    guildSettings.entitlements[index] = entitlementData
                } else {
                    guildSettings.entitlements.add(entitlementData)
                }

                guildSettingsRepository.save(guildSettings)
            } catch (e: Exception) {
                log.error(e) { "Unhandled exception while handling an entitlement event in guild $guildId" }
            }

            // failures are only logged, when the user is not in the support server the support bot calculates the role on join
            try {
                supportBotApiClient.addPremiumRole(entitlement.userId)
            } catch (_: ANotFoundException) {
                // not in the support server
            } catch (e: Exception) {
                log.error(e) { "Failed to add the premium role of user ${entitlement.userId}" }
            }
        }
    }

    /**
     * @return the id of the guild that owns the entitlement, or null if it is not a server premium entitlement
     */
    private fun Entitlement.serverEntitlementGuildId(): String? {
        if (skuId != premiumProperties.serverSkuId) {
            log.warn { "Received entitlement with unknown sku id\nData: $this" }
            return null
        }

        return guildId
    }

    private fun withExceptionHandling(
        guildId: String,
        block: () -> Unit,
    ) {
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                log.error(e) { "Unhandled exception while handling an entitlement event in guild $guildId" }
            }
        }
    }
}
