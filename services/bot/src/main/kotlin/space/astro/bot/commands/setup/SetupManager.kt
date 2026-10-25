package space.astro.bot.commands.setup

import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import org.springframework.stereotype.Component
import space.astro.bot.components.InterfaceManager
import space.astro.bot.interactions.InteractionException
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.ui.Buttons
import space.astro.bot.ui.Containers
import space.astro.shared.core.clients.PremiumManager
import space.astro.shared.core.lang.L
import space.astro.shared.core.models.database.guildSettings.GeneratorSettings
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository

@Component
class SetupManager(
    private val premiumManager: PremiumManager,
    private val interfaceManager: InterfaceManager,
    private val guildSettingsRepository: GuildSettingsRepository,
) {
    companion object {
        private const val MAX_GUILD_CHANNELS = 500
    }

    suspend fun setup(ctx: InteractionContext) {
        val guild = ctx.guild

        val guildSettings =
            withContext(Dispatchers.IO) {
                guildSettingsRepository.findByGuildID(guild.id)
                    ?: guildSettingsRepository.createNewGuildSettings(guild.id, guild.locale.locale)
            }

        if (!premiumManager.canCreateGenerator(guildSettings)) {
            ctx.responder.reply(
                Containers.error(
                    ctx.t("setup.generator_limit", "max" to PremiumManager.FREE_MAX_GENERATORS),
                ),
                ActionRow.of(Buttons.ultimate(ctx.locale), Buttons.guildDashboard(ctx.locale, guild.id)),
            )
            return
        }

        if (!premiumManager.canCreateInterface(guildSettings)) {
            ctx.responder.reply(
                Containers.error(
                    ctx.t("setup.interface_limit", "max" to PremiumManager.FREE_MAX_INTERFACES),
                ),
                ActionRow.of(Buttons.ultimate(ctx.locale), Buttons.guildDashboard(ctx.locale, guild.id)),
            )
            return
        }

        // category + interface + generator
        if (guild.channels.size > MAX_GUILD_CHANNELS - 3) {
            throw InteractionException("setup.max_channels")
        }

        val reason = ctx.t("setup.reason")

        try {
            val category = guild.createCategory("Astro").reason(reason).await()

            val interfaceChannel =
                guild
                    .createTextChannel("interface")
                    .addMemberPermissionOverride(guild.selfMember.idLong, Permission.VIEW_CHANNEL.rawValue, 0)
                    .setParent(category)
                    .reason(reason)
                    .await()

            guildSettings.interfaces.add(interfaceManager.createInterface(guild, interfaceChannel.id, ctx.locale))

            val generatorChannel =
                guild
                    .createVoiceChannel("➕ Generator")
                    .setParent(category)
                    .reason(reason)
                    .await()

            guildSettings.generators.add(
                GeneratorSettings(
                    id = generatorChannel.id,
                    category = category.id,
                    chatCategory = category.id,
                    // the variables are kept as they are, they get replaced when the temporary VC is created
                    defaultName = L.t(guildSettings.javaLocale, "temporary.vc.name.default", "nickname" to "{nickname}"),
                    defaultChatName = L.t(guildSettings.javaLocale, "temporary.chat.name.default", "vc_name" to "{vc_name}"),
                    defaultWaitingName = L.t(guildSettings.javaLocale, "temporary.waiting.name.default", "vc_name" to "{vc_name}"),
                ),
            )

            withContext(Dispatchers.IO) { guildSettingsRepository.save(guildSettings) }

            ctx.responder.reply(
                Containers.default(
                    ctx.t(
                        "setup.success",
                        "category" to category.asMention,
                        "generator" to generatorChannel.asMention,
                        "interface" to interfaceChannel.asMention,
                    ),
                ),
                ActionRow.of(Buttons.guildDashboard(ctx.locale, guild.id), Buttons.guides(ctx.locale)),
            )
        } catch (e: ErrorResponseException) {
            if (e.errorResponse == ErrorResponse.MAX_CHANNELS) {
                throw InteractionException("setup.max_channels")
            }

            throw e
        }
    }
}
