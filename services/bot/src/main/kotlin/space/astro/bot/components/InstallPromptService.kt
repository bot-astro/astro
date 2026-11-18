package space.astro.bot.components

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import space.astro.bot.interactions.InteractionCooldownService
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.ui.Buttons
import space.astro.bot.ui.Containers
import space.astro.shared.core.clients.PremiumManager
import kotlin.time.Duration.Companion.days

private val log = KotlinLogging.logger { }

/**
 * Occasionally shows an "Add Astro to your server" link, only to the user that just used a voice channel setting
 * and only on free guilds, premium guilds don't get any promotion
 */
@Service
class InstallPromptService(
    private val premiumManager: PremiumManager,
    private val interactionCooldownService: InteractionCooldownService,
) {
    private companion object {
        val COOLDOWN = 14.days
        const val COOLDOWN_KEY = "install_prompt"
    }

    /**
     * Sends the prompt as an ephemeral follow up of an acknowledged interaction, unless the guild is premium
     * or the user already saw it recently. Never throws, the interaction it follows has already succeeded.
     */
    suspend fun promptIfEligible(ctx: InteractionContext) {
        try {
            if (ctx.guildSettings?.let { premiumManager.isGuildPremium(it) } == true) return
            if (interactionCooldownService.acquire(ctx.memberId, COOLDOWN_KEY, COOLDOWN).isPositive()) return

            ctx.responder.followUp(
                Containers.container(
                    Containers.purple,
                    Containers.section(Buttons.addToServer(ctx.locale, "interface"), ctx.t("install_prompt.text")),
                ),
            )
        } catch (e: Exception) {
            log.warn(e) { "Failed to send the install prompt in guild ${ctx.guildId}" }
        }
    }
}
