package space.astro.bot.commands.setup

import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import org.springframework.stereotype.Component
import space.astro.bot.components.InterfaceManager
import space.astro.bot.components.UpdatesChannelManager
import space.astro.bot.interactions.InteractionException
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.ui.Buttons
import space.astro.bot.ui.Containers
import space.astro.shared.core.clients.PremiumManager
import space.astro.shared.core.lang.L
import space.astro.shared.core.models.database.guildSettings.GeneratorSettings
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.models.database.guildSettings.InterfaceData
import space.astro.shared.core.models.database.guildSettings.UpdatesChannelData
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import java.util.Locale

@Component
class SetupManager(
    private val premiumManager: PremiumManager,
    private val interfaceManager: InterfaceManager,
    private val updatesChannelManager: UpdatesChannelManager,
    private val guildSettingsRepository: GuildSettingsRepository,
) {
    companion object {
        private const val MAX_GUILD_CHANNELS = 500
    }

    /**
     * What [createSetup] created, none of it is saved in the guild settings yet
     *
     * @param updatesChannelId the updates channel of the guild, null if it has none
     * @param newUpdatesChannel the data of the updates channel when it has been created by the setup, null if it
     * already existed or couldn't be created
     */
    data class SetupResult(
        val category: Category,
        val interfaceChannel: TextChannel,
        val generatorChannel: VoiceChannel,
        val updatesChannelId: String?,
        val newUpdatesChannel: UpdatesChannelData?,
        val interfaceData: InterfaceData,
        val generatorSettings: GeneratorSettings,
    )

    /**
     * @return true if the guild has enough free channel slots for the category, the interface, the generator and the updates channel
     */
    fun hasRoomForSetup(guild: Guild) = guild.channels.size <= MAX_GUILD_CHANNELS - 4

    /**
     * Creates a category with an interface, a generator and, if the guild doesn't have one yet, an updates channel inside it.
     * The caller adds the returned data to the [guildSettings] and saves them
     *
     * @throws ErrorResponseException
     */
    suspend fun createSetup(
        guild: Guild,
        guildSettings: GuildSettingsEntity,
        locale: Locale,
        reason: String,
    ): SetupResult {
        val category = guild.createCategory("Astro").reason(reason).await()

        // the updates channel is skipped when it already exists or when Astro can't set up its webhooks
        val existingUpdatesChannelId =
            guildSettings.updatesChannel?.channelId?.takeIf { guild.getTextChannelById(it) != null }
        val newUpdatesChannel =
            if (existingUpdatesChannelId == null && guild.selfMember.hasPermission(Permission.MANAGE_WEBHOOKS)) {
                updatesChannelManager.createAndConfigure(guild, category, locale, reason, guildSettings.updatesChannel)
            } else {
                null
            }

        val interfaceChannel =
            guild
                .createTextChannel("interface")
                .addMemberPermissionOverride(guild.selfMember.idLong, Permission.VIEW_CHANNEL.rawValue, 0)
                .setParent(category)
                .reason(reason)
                .await()

        val interfaceData = interfaceManager.createInterface(guild, interfaceChannel.id, locale)

        val generatorChannel =
            guild
                .createVoiceChannel("➕ Generator")
                .setParent(category)
                .reason(reason)
                .await()

        val generatorSettings =
            GeneratorSettings(
                id = generatorChannel.id,
                category = category.id,
                chatCategory = category.id,
                waitingCategory = category.id,
                // the variables are kept as they are, they get replaced when the temporary VC is created
                defaultName = L.t(guildSettings.javaLocale, "temporary.vc.name.default", "nickname" to "{nickname}"),
                defaultChatName = L.t(guildSettings.javaLocale, "temporary.chat.name.default", "vc_name" to "{vc_name}"),
                defaultWaitingName = L.t(guildSettings.javaLocale, "temporary.waiting.name.default", "vc_name" to "{vc_name}"),
            )

        return SetupResult(
            category = category,
            interfaceChannel = interfaceChannel,
            generatorChannel = generatorChannel,
            updatesChannelId = existingUpdatesChannelId ?: newUpdatesChannel?.channelId,
            newUpdatesChannel = newUpdatesChannel,
            interfaceData = interfaceData,
            generatorSettings = generatorSettings,
        )
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

        if (!hasRoomForSetup(guild)) {
            throw InteractionException("setup.max_channels")
        }

        try {
            val setup = createSetup(guild, guildSettings, ctx.locale, ctx.t("setup.reason"))

            setup.newUpdatesChannel?.let { guildSettings.updatesChannel = it }
            guildSettings.interfaces.add(setup.interfaceData)
            guildSettings.generators.add(setup.generatorSettings)

            withContext(Dispatchers.IO) { guildSettingsRepository.save(guildSettings) }

            ctx.responder.reply(
                Containers.default(
                    if (setup.updatesChannelId != null) {
                        ctx.t(
                            "setup.success",
                            "category" to setup.category.asMention,
                            "generator" to setup.generatorChannel.asMention,
                            "interface" to setup.interfaceChannel.asMention,
                            "updates" to "<#${setup.updatesChannelId}>",
                        )
                    } else {
                        ctx.t(
                            "setup.success_no_updates",
                            "category" to setup.category.asMention,
                            "generator" to setup.generatorChannel.asMention,
                            "interface" to setup.interfaceChannel.asMention,
                        )
                    },
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

    suspend fun createGenerator(
        ctx: InteractionContext,
        category: Category?,
    ) {
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

        try {
            val generatorChannel =
                guild
                    .createVoiceChannel("➕ Generator")
                    .setParent(category)
                    .reason(ctx.t("setup.reason"))
                    .await()

            guildSettings.generators.add(
                GeneratorSettings(
                    id = generatorChannel.id,
                    category = category?.id,
                    chatCategory = category?.id,
                    waitingCategory = category?.id,
                    // the variables are kept as they are, they get replaced when the temporary VC is created
                    defaultName = L.t(guildSettings.javaLocale, "temporary.vc.name.default", "nickname" to "{nickname}"),
                    defaultChatName = L.t(guildSettings.javaLocale, "temporary.chat.name.default", "vc_name" to "{vc_name}"),
                    defaultWaitingName = L.t(guildSettings.javaLocale, "temporary.waiting.name.default", "vc_name" to "{vc_name}"),
                ),
            )

            withContext(Dispatchers.IO) { guildSettingsRepository.save(guildSettings) }

            ctx.responder.reply(
                Containers.default(ctx.t("setup.generator_success", "generator" to generatorChannel.asMention)),
                ActionRow.of(Buttons.guildDashboard(ctx.locale, guild.id), Buttons.guides(ctx.locale)),
            )
        } catch (e: ErrorResponseException) {
            if (e.errorResponse == ErrorResponse.MAX_CHANNELS) {
                throw InteractionException("setup.max_channels")
            }

            throw e
        }
    }

    suspend fun createInterface(ctx: InteractionContext) {
        val guild = ctx.guild

        val guildSettings =
            withContext(Dispatchers.IO) {
                guildSettingsRepository.findByGuildID(guild.id)
                    ?: guildSettingsRepository.createNewGuildSettings(guild.id, guild.locale.locale)
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

        try {
            val interfaceChannel =
                guild
                    .createTextChannel("interface")
                    .addMemberPermissionOverride(guild.selfMember.idLong, Permission.VIEW_CHANNEL.rawValue, 0)
                    .reason(ctx.t("setup.reason"))
                    .await()

            guildSettings.interfaces.add(interfaceManager.createInterface(guild, interfaceChannel.id, ctx.locale))

            withContext(Dispatchers.IO) { guildSettingsRepository.save(guildSettings) }

            ctx.responder.reply(
                Containers.default(ctx.t("setup.interface_success", "interface" to interfaceChannel.asMention)),
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
