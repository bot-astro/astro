package space.astro.bot.interactions.handlers.command.impl.predashboard

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.await
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.sharding.ShardManager
import space.astro.bot.components.managers.PremiumRequirementDetector
import space.astro.bot.core.ui.Buttons
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.SettingsInteractionContext
import space.astro.bot.interactions.context.TemplateSettingsInteractionContext
import space.astro.bot.interactions.handlers.command.*
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.models.database.TemplateData
import space.astro.shared.core.util.extention.asChannelMention

@Command(
    name = "template",
    description = "Create predefined channel templates and use them anywhere",
    requiredPermissions = [Permission.MANAGE_CHANNEL],
    category = CommandCategory.SETTINGS,
    action = InteractionAction.SETTINGS
)
class TemplateCommand(
    private val premiumRequirementDetector: PremiumRequirementDetector,
    private val guildDao: GuildDao,
    private val shardManager: ShardManager
) : AbstractCommand() {

    //////////////
    /// BASICS ///
    //////////////
    @SubCommand(
        name = "create",
        description = "Create a template that can be easely assigned to voice channels"
    )
    suspend fun create(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext,
        @CommandOption(
            description = "The name of the new template",
            type = OptionType.STRING
        )
        name: String
    ) {
        if (!premiumRequirementDetector.canCreateTemplate(ctx.guildData)) {
            ctx.replyHandler.replyEmbedAndComponent(
                Embeds.error(
                    "There are already 2 Templates setup in this server.\nPremium is required to have more than 2 Templates." +
                    "\nPossible solutions:" +
                            "\n• Get ${Emojis.premium.formatted} Premium" +
                            "\n• Delete an existing template with `/template delete`"
                ), Buttons.premium
            )
            return
        }

        val template = TemplateData(name = name)
        ctx.guildData.templates.add(template)
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(Embeds.success(
            "The template has been created with the name: `$name`" +
                    "\n" +
                    "\n**Edit this template settings with the `/template edit` commands.**"
        ))
    }

    @SubCommand(
        name = "delete",
        description = "Deletes a template"
    )
    suspend fun delete(
        event: SlashCommandInteractionEvent,
        ctx: TemplateSettingsInteractionContext
    ) {
        ctx.guildData.templates.removeAt(ctx.templateIndex)

        guildDao.save(ctx.guildData)
        ctx.replyHandler.replyEmbed(
            Embeds.success(
                "The template has been deleted, to create a new one use `/template create`."
            )
        )
    }

    @SubCommand(
        name = "enabled-generators",
        description = "Set which generators can use a certain template"
    )
    suspend fun enabledGenerators(
        event: SlashCommandInteractionEvent,
        ctx: TemplateSettingsInteractionContext
    ) {
        val generators = ctx.guildData.generators.take(25)
        val selectMenu = StringSelectMenu.create(InteractionIds.getRandom())
            .setPlaceholder("Select the generators")
            .setRequiredRange(0, generators.size)
            .addOptions(
                generators.mapIndexed { index, generatorDto ->
                    val genChannel = ctx.guild.getVoiceChannelById(generatorDto.id)

                    SelectOption.of(
                        "${index + 1} - #${genChannel?.name ?: "deleted-channel"}",
                        index.toString()
                    )
                        .withDescription("From category: ${genChannel?.parentCategory?.name ?: "not in a category"}")
                        .withEmoji(Emojis.generator)
                }
            )
            .build()

        val allGenButton = Button.of(ButtonStyle.SUCCESS, InteractionIds.getRandom(), "All generators")

        ctx.replyHandler.reply(
            embed = Embeds.selector("Select the generators for which this template is enabled." +
                    "\nYou can select specific generators with the select menu below, or all generators with the green button below"),
            components = listOf(
                ActionRow.of(selectMenu),
                ActionRow.of(allGenButton)
            )
        )

        withTimeoutOrNull(60000) {
            return@withTimeoutOrNull shardManager.await<GenericComponentInteractionCreateEvent> {
                (it is StringSelectInteractionEvent && it.componentId == selectMenu.id)
                        || (it is ButtonInteractionEvent && it.componentId == allGenButton.id!!)
            }
        }?.let { newEvent ->
            ctx.replyHandler.setCallbacks(newEvent, newEvent, newEvent)

            if (newEvent is StringSelectInteractionEvent) {
                val selectedIndexes = newEvent.values.map { it.toInt() }
                val generatorIdsSelected = generators.filterIndexed { index, _ -> index in selectedIndexes }.map { it.id }

                ctx.guildData.templates[ctx.templateIndex].enabledGeneratorIds = generatorIdsSelected.toMutableList()
                guildDao.save(ctx.guildData)

                ctx.replyHandler.replyEmbed(Embeds.success(
                    if (selectedIndexes.isNotEmpty())
                        "This template has been restricted to the following generators:" +
                                generatorIdsSelected.joinToString { "\n• ${it.asChannelMention()}" }
                    else
                        "This template will not be usable by any of the generators."
                ))
            } else {
                ctx.guildData.templates[ctx.templateIndex].enabledGeneratorIds = null
                guildDao.save(ctx.guildData)

                ctx.replyHandler.replyEmbed(Embeds.success(
                    "This template will be usable by all the generators!"
                ))
            }
        } ?: ctx.replyHandler.replyEmbed(Embeds.timeExpired)
    }


    ////////////
    /// EDIT ///
    ////////////
    @SubCommand(
        name = "bitrate",
        description = "Set the bitrate for a vc template",
        group = "edit",
        groupDescription = "0"
    )
    suspend fun editBitrate(
        event: SlashCommandInteractionEvent,
        ctx: TemplateSettingsInteractionContext,
        @CommandOption(
            description = "The bitrate for the template (in kbps)",
            type = OptionType.INTEGER,
            minValue = 8
        )
        bitrate: Int?
    ) {
        val computedBitrate = bitrate?.times(1000)?.coerceAtMost(ctx.guild.maxBitrate)
        ctx.guildData.templates[ctx.templateIndex].vcBitrate = computedBitrate
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(Embeds.success(
            if (computedBitrate == null)
                "The template doesn't have a specific bitrate anymore"
            else
                "The bitrate has been set to ${computedBitrate / 1000} kbps."
        ))
    }

    @SubCommand(
        name = "limit",
        description = "Set the user limit for a vc template",
        group = "edit",
        groupDescription = "0"
    )
    suspend fun editLimit(
        event: SlashCommandInteractionEvent,
        ctx: TemplateSettingsInteractionContext,
        @CommandOption(
            description = "The user limit for the template",
            type = OptionType.INTEGER,
            minValue = 0,
            maxValue = 99
        )
        limit: Int?
    ) {
        ctx.guildData.templates[ctx.templateIndex].vcLimit = limit
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                if (limit == null)
                    "The template doesn't have a specific user limit anymore"
                else
                    "The user limit has been set to `$limit`"
            ))
    }

    @SubCommand(
        name = "name",
        description = "Set the name for a vc template",
        group = "edit",
        groupDescription = "0"
    )
    suspend fun editName(
        event: SlashCommandInteractionEvent,
        ctx: TemplateSettingsInteractionContext,
        @CommandOption(
            description = "The name for the template (can include variables)",
            type = OptionType.STRING,
            minLength = 2,
            maxLength = 100
        )
        name: String?
    ) {
        ctx.guildData.templates[ctx.templateIndex].vcName = name
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                if (name == null)
                    "The template doesn't have a specific name anymore"
                else
                    "The name has been set to `$name`."
            ))
    }

    @SubCommand(
        name = "region",
        description = "Set the region for a vc template",
        group = "edit",
        groupDescription = "0"
    )
    suspend fun editRegion(
        event: SlashCommandInteractionEvent,
        ctx: TemplateSettingsInteractionContext
    ) {
        ctx.replyHandler.deferReply()

        val regionsAvailable = ctx.guild.retrieveRegions(false).await().toList()

        val selectMenu = StringSelectMenu.create(InteractionIds.getRandom())
            .addOptions(regionsAvailable.mapIndexed { index, region ->
                SelectOption.of(
                    region.getName(),
                    index.toString(),
                ).withEmoji(
                    if (region.emoji == null)
                        Emojis.region
                    else
                        Emoji.fromUnicode(region.emoji!!))
            })
            .setPlaceholder("Choose a region")
            .setRequiredRange(0, 1)
            .build()

        ctx.replyHandler.replyWithSelectMenu(
            Embeds.selector("Choose the region for the template"),
            selectMenu,
            true
        ) {
            if (it.isEmpty()) {
                ctx.guildData.templates[ctx.templateIndex].vcRegion = null
                guildDao.save(ctx.guildData)

                ctx.replyHandler.replyEmbed(
                    Embeds.success(
                        "The template doesn't have a specific region anymore"
                    )
                )
            } else {
                val selectedRegion = regionsAvailable[it.first().toInt()]

                ctx.guildData.templates[ctx.templateIndex].vcRegion = selectedRegion.key
                guildDao.save(ctx.guildData)

                ctx.replyHandler.replyEmbed(
                    Embeds.success(
                        "Default region for the template set to: ${selectedRegion.emoji ?: Emojis.region.formatted} ${selectedRegion.getName()}"
                    )
                )
            }
        }
    }

    @SubCommand(
        name = "template-name",
        description = "Modify the name of the actual template",
        group = "edit",
        groupDescription = "0"
    )
    suspend fun editTemplateName(
        event: SlashCommandInteractionEvent,
        ctx: TemplateSettingsInteractionContext,
        @CommandOption(
            description = "The new name for the template",
            type = OptionType.STRING,
            maxLength = SelectOption.LABEL_MAX_LENGTH
        )
        name: String
    ) {
        ctx.guildData.templates[ctx.templateIndex].name = name
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(Embeds.success(
            "This template is now called `$name`"
        ))
    }
}