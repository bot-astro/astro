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
import space.astro.shared.core.models.database.guildSettings.GuildEntitlementData
import space.astro.shared.core.properties.PremiumProperties
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository

private val log = KotlinLogging.logger { }

// TODO: premium, the legacy listeners also added/removed the premium role of the user in the support server (SupportBotApiService)
@Component
class EntitlementEventListener(
    private val guildSettingsRepository: GuildSettingsRepository,
    private val premiumProperties: PremiumProperties,
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
