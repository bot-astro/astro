package space.astro.bot.interactions.handlers.command.impl.predashboard

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import space.astro.bot.components.managers.PremiumRequirementDetector
import space.astro.bot.core.ui.Buttons
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.GeneratorSettingsInteractionContext
import space.astro.bot.interactions.context.SettingsInteractionContext
import space.astro.bot.interactions.handlers.command.*
import space.astro.bot.services.ConfigurationErrorService
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.models.database.*
import space.astro.shared.core.util.extention.asAbleOrUnable
import space.astro.shared.core.util.extention.asChannelMention
import space.astro.shared.core.util.extention.asEnabledOrDisabled
import space.astro.shared.core.util.extention.asOnOrOff

@Command(
    name = "generator",
    description = "0",
    requiredPermissions = [Permission.MANAGE_CHANNEL],
    category = CommandCategory.SETTINGS,
    action = InteractionAction.SETTINGS
)
class GeneratorCommand(
    private val premiumRequirementDetector: PremiumRequirementDetector,
    private val configurationErrorService: ConfigurationErrorService,
    private val guildDao: GuildDao
) : AbstractCommand() {

    /////////////
    /// BASIC ///
    /////////////
    @SubCommand(
        name = "create",
        description = "Create a temporary vc generator"
    )
    suspend fun create(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext
    ) {
        if (!premiumRequirementDetector.canCreateGenerator(ctx.guildData)) {
            ctx.replyHandler.replyEmbedAndComponent(
                embed = Embeds.error(
                    "There are already 3 Generators setup in this server.\nPremium is required to have more than 3 Generators." +
                        "\nPossible solutions:" +
                        "\n• Get ${Emojis.premium.formatted} Premium" +
                        "\n• Delete an existing generator with `/generator delete`"
                ),
                component = Buttons.premium
            )
            return
        }

        ctx.replyHandler.deferReply()

        val category =
            if (event.channelType == ChannelType.TEXT) (event.channel as TextChannel).parentCategory else null

        val generatorAction = ctx.guild.createVoiceChannel("➕ Generator")
        if (category != null)
            generatorAction.setParent(category)

        val generator = generatorAction.await()
        ctx.guildData.generators.add(
            GeneratorData(
                id = generator.id,
                category = category?.id,
                chatCategory = category?.id,
                chatPermissionsInherited = PermissionsInherited.GENERATOR
            )
        )

        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbedAndComponent(
            embed = Embeds.default("Now users can join ${generator.asMention} to create temporary voice channels."),
            component = Buttons.Help.generators
        )
    }

    @SubCommand(
        name = "delete",
        description = "Delete a generator"
    )
    suspend fun delete(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {
        ctx.guildData.generators.removeIf { it.id == ctx.generatorData.id }
        guildDao.save(ctx.guildData)
        ctx.guild.getVoiceChannelById(ctx.generatorData.id)?.delete()?.await()

        ctx.replyHandler.replyEmbed(Embeds.default("The generator has been deleted, to create a new one use `/generator create`"))
    }

    @SubCommand(
        name = "fallback-generator",
        description = "Sets a fallback generator in case one isn't able to create more vcs (category limit)"
    )
    suspend fun fallbackGenerator(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {
        val generators = ctx.guildData.generators.apply { removeIf { it.id == ctx.generatorData.id } }

        if (!premiumRequirementDetector.canUseFallbackGenerator(ctx.guildData)) {
            ctx.replyHandler.replyPremiumRequired()
            return
        }

        if (generators.isEmpty()) {
            ctx.replyHandler.replyEmbed(
                Embeds.error(
                    "You only have one generator in the server." +
                        "To have a fallback for that generator, create a new one with `/generator create`"
                )
            )
            return
        }

        ctx.replyHandler.replyWithSelectMenu(
            Embeds.selector(
                "**Choose the fallback generator for the ${ctx.generatorData.id.asChannelMention()} generator.**" +
                    "\nWhen that generator category gets filled (meaning 50 channels already exist in that category) Astro will move" +
                    "the users that try to join that generator to the fallback one."
            ),
            StringSelectMenu.create(InteractionIds.getRandom())
                .addOptions(generators.mapIndexed { index, generatorDto ->
                    val channel = ctx.guild.getVoiceChannelById(generatorDto.id)
                    SelectOption.of(channel?.name ?: "#Deleted", index.toString())
                        .withEmoji(Emojis.generator)
                        .withDescription(if (channel?.parentCategory != null) "From category ${channel.parentCategory!!.name}" else "Not in a category")
                })
                .setRequiredRange(0, 1)
                .setPlaceholder("Select the fallback generator")
                .build(),
            true
        ) {
            if (it.isEmpty()) {
                ctx.generatorData.fallbackId = null
                ctx.guildData.generators[ctx.generatorIndex] = ctx.generatorData
                guildDao.save(ctx.guildData)

                ctx.replyHandler.replyEmbed(Embeds.success("This generator doesn't have a fallback generator anymore."))
            } else {
                val fallbackGen = generators[it.first().toInt()]

                if (fallbackGen.fallbackId == ctx.generatorData.id) {
                    ctx.replyHandler.replyEmbed(
                        Embeds.error(
                            "The generator you selected (${fallbackGen.id.asChannelMention()}) to be the fallback of this current generator (${ctx.generatorData.id.asChannelMention()}), has as *its* fallback this current generator (yeah it's confusing)." +
                                "\n\n" +
                                "${ctx.generatorData.id.asChannelMention()} -- fallback --> ${fallbackGen.id.asChannelMention()}" +
                                "\n${fallbackGen.id.asChannelMention()} -- fallback --> ${ctx.generatorData.id.asChannelMention()}" +
                                "\n\nThat way an infinite loop could be caused, so that is not allowed."
                        )
                    )

                    return@replyWithSelectMenu
                }

                ctx.generatorData.fallbackId = fallbackGen.id
                ctx.guildData.generators[ctx.generatorIndex] = ctx.generatorData
                guildDao.save(ctx.guildData)

                ctx.replyHandler.replyEmbed(Embeds.success("This generator now has ${fallbackGen.id.asChannelMention()} as its fallback generator!"))
            }
        }
    }

    //////////
    /// VC ///
    //////////
    @SubCommand(
        name = "badwords",
        description = "Sets whether users can use badwords for their vc name",
        group = "vc",
        groupDescription = "0"
    )
    suspend fun badwords(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            type = OptionType.BOOLEAN,
            description = "Whether badwords are allowed in voice channel names"
        )
        allowed: Boolean
    ) {
        if (!allowed && !premiumRequirementDetector.canValidateBadwords(ctx.guildData)) {
            ctx.replyHandler.replyPremiumRequired()
            return
        }

        ctx.generatorData.commandsSettings.badwordsAllowed = allowed
        ctx.guildData.generators[ctx.generatorIndex] = ctx.generatorData
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(Embeds.success("Users are now ${allowed.asAbleOrUnable()} to use badwords with the `/name` command."))
    }

    @SubCommand(
        name = "bitrate",
        description = "Set the default, minimum and maximum bitrate for temporary vcs",
        group = "vc",
        groupDescription = "0"
    )
    suspend fun bitrate(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            type = OptionType.INTEGER,
            description = "The default bitrate for temporary voice channels (in kbps)",
            minValue = 8
        )
        default: Int,
        @CommandOption(
            type = OptionType.INTEGER,
            description = "The minimum bitrate owners can set for their temporary voice channel (in kbps)",
            minValue = 8
        )
        min: Int?,
        @CommandOption(
            type = OptionType.INTEGER,
            description = "The maximum bitrate owners can set for their temporary voice channel (in kbps)",
            minValue = 8
        )
        max: Int?
    ) {
        val guildDto = ctx.guildData
        val guildMax = ctx.guild.maxBitrate

        var currentMax = ctx.generatorData.commandsSettings.maxBitrate
        var currentMin = ctx.generatorData.commandsSettings.minBitrate

        val defaultComputed = default.times(1000)
        val minComputed = min?.times(1000)
        val maxComputed = min?.times(1000)

        val toCheck = listOf(Pair("default", defaultComputed), Pair("min", minComputed), Pair("max", maxComputed))
        toCheck.forEach {
            if (it.second == null)
                return@forEach

            if (it.second!! > guildMax) {
                ctx.replyHandler.replyEmbed(
                    Embeds.error(
                        "You provided a ${it.first} bitrate amount which is too high." +
                            "\nYour server supports a max bitrate of ${guildMax / 1000} kbps, provide an amount within that value."
                    )
                )
                return
            }
        }

        val currentDefault = defaultComputed
        if (minComputed != null)
            currentMin = minComputed
        if (maxComputed != null)
            currentMax = maxComputed

        if (currentMax != null && currentMin > currentMax)
            ctx.replyHandler.replyEmbed(
                Embeds.error(
                    "The minimum bitrate ($currentMin) must be lower than the maximum ($currentMax)" +
                        "\nMake sure to provide a minimum bitrate lower than the maximum (or a maximum bitrate greater than the minimum)."
                )
            )

        guildDto.generators[ctx.generatorIndex].bitrate = currentDefault
        if (currentMax != null) {
            guildDto.generators[ctx.generatorIndex].commandsSettings.maxBitrate = currentMax
        }
        guildDto.generators[ctx.generatorIndex].commandsSettings.minBitrate = currentMin

        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                "The bitrate settings for temporary voice channels now are:" +
                    "\n• Default > ${currentDefault / 1000} kbps" +
                    "\n• Maximum > ${(currentMax ?: guildMax) / 1000} kbps" +
                    "\n• Minimum > ${currentMin / 1000} kbps"
            )
        )
    }

    @SubCommand(
        name = "category",
        description = "Set in which category temporary vcs should get generated",
        group = "vc",
        groupDescription = "0"
    )
    suspend fun category(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            description = "The category where temporary voice channels should be generated, leave empty for no category",
            type = OptionType.CHANNEL,
            channelTypes = [ChannelType.CATEGORY]
        )
        category: GuildChannel?
    ) {
        ctx.guildData.generators[ctx.generatorIndex].category = category?.id
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                if (category == null)
                    "Temporary voice channels will be generated at the top of the server since you didn't provide a category in the options of this command."
                else
                    "Temporary voice channels will now be generated under the category ${category.asMention}."
            )
        )
    }

    @SubCommand(
        name = "limit",
        description = "Set the default, minimum and maximum user limit for temporary vcs",
        group = "vc",
        groupDescription = "0"
    )
    suspend fun limit(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            description = "The default user limit for temporary voice channels",
            type = OptionType.INTEGER,
            minValue = 0,
            maxValue = 99
        )
        default: Int,
        @CommandOption(
            description = "The minimum user limit owners can set for their temporary voice channel",
            type = OptionType.INTEGER,
            minValue = 0,
            maxValue = 99
        )
        min: Int?,
        @CommandOption(
            description = "The maximum user limit owners can set for their temporary voice channel",
            type = OptionType.INTEGER,
            minValue = 0,
            maxValue = 99
        )
        max: Int?
    ) {
        var currentMax = ctx.generatorData.commandsSettings.maxUserLimit
        var currentMin = ctx.generatorData.commandsSettings.minUserLimit

        val currentDefault = default
        if (min != null)
            currentMin = min
        if (max != null)
            currentMax = max

        if (currentMin > currentMax) {
            ctx.replyHandler.replyEmbed(
                Embeds.error(
                    "The minimum user limit ($currentMin) must be lower than the maximum ($currentMax)" +
                        "\nMake sure to provide a minimum user limit lower than the maximum (or a maximum user limit greater than the minimum)."
                )
            )
        }

        ctx.guildData.generators[ctx.generatorIndex].userLimit = currentDefault
        ctx.guildData.generators[ctx.generatorIndex].commandsSettings.maxUserLimit = currentMax
        ctx.guildData.generators[ctx.generatorIndex].commandsSettings.minUserLimit = currentMin

        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                "The user limit settings for temporary voice channels now are:" +
                    "\n• Default > $currentDefault" +
                    "\n• Maximum > $currentMax" +
                    "\n• Minimum > $currentMin"
            )
        )
    }

    @SubCommand(
        name = "name",
        description = "\"Set the name for temporary voice channels or for a specific state of temporary vcs",
        group = "vc",
        groupDescription = "0"
    )
    suspend fun name(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            description = "The name for temporary voice channels, see documentation for variables",
            type = OptionType.STRING,
            minLength = 2,
            maxLength = 500
        )
        name: String,
        @CommandOption(
            description = "The state of the voice channel for which the name is applied",
            type = OptionType.STRING,
            stringChoices = ["Any", "Locked", "Hidden"]
        )
        state: String?
    ) {
        if (!premiumRequirementDetector.canUseVCNameTemplate(ctx.guildData, name)) {
            ctx.replyHandler.replyEmbedAndComponent(
                embed = Embeds.error(
                    "Some variables you used in the name can only be used in premium servers." +
                        "\nSee documentation for variables with the button below."
                ),
                component = Buttons.Help.variables
            )
            return
        }

        when (state) {
            "locked" -> ctx.guildData.generators[ctx.generatorIndex].defaultLockedName = name
            "hidden" -> ctx.guildData.generators[ctx.generatorIndex].defaultHiddenName = name
            else -> ctx.guildData.generators[ctx.generatorIndex].defaultName = name
        }

        val stateReadableName = when (state) {
            "locked" -> "locked"
            "hidden" -> "hidden"
            else -> "default"
        }

        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbedAndComponent(
            Embeds.success(
                "The $stateReadableName name for temporary voice channels now is:" +
                    "\n`$name`" +
                    "\n\nYou can mix variables in the name to further personalize it, use the button below for documentation!"
            ),
            Buttons.Help.variables
        )
    }

    @SubCommand(
        name = "permissions",
        description = "Set from where temporary vcs will inherit the permissions",
        group = "vc",
        groupDescription = "0"
    )
    suspend fun permissions(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            name = "inherit-from",
            description = "From where temporary voice channels will inherit permissions from",
            type = OptionType.STRING,
        )
        inheritFrom: PermissionsInherited,
        @CommandOption(
            name = "target-role",
            description = "Astro will modify only the permissions of this role (on lock, hide, etc...)",
            type = OptionType.ROLE
        )
        targetRole: Role,
        @CommandOption(
            name = "moderator-role",
            description = "This role will be immune to all vc commands (lock, hide, etc...)",
            type = OptionType.ROLE
        )
        moderatorRole: Role?
    ) {
        ctx.guildData.generators[ctx.generatorIndex].permissionsInherited = inheritFrom
        ctx.guildData.generators[ctx.generatorIndex].permissionsTargetRole = targetRole.id
        ctx.guildData.generators[ctx.generatorIndex].permissionsImmuneRole = moderatorRole?.id

        guildDao.save(ctx.guildData)

        val permissionsResult = if (inheritFrom == PermissionsInherited.NONE)
            "Temporary voice channels will not inherit permissions from anywhere."
        else
            "Temporary voice channels will now inherit permissions from the ${inheritFrom.name.lowercase()}."

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                permissionsResult +
                    "\nCommands like `/lock` and `/hide` will modify only the permissions of ${targetRole.asMention}." +
                    if (moderatorRole != null) "\n(*${moderatorRole.asMention} is immune to all the voice channel commands*)" else ""
            )
        )
    }

    @SubCommand(
        name = "positions",
        description = "Set the exact place where temporary vcs will be created in a category",
        group = "vc",
        groupDescription = "0"
    )
    suspend fun position(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            description = "The exact position for temporary voice channels",
            type = OptionType.STRING
        )
        position: InitialPosition
    ) {
        ctx.guildData.generators[ctx.generatorIndex].initialPosition = position
        guildDao.save(ctx.guildData)

        val readablePosition = when (position) {
            InitialPosition.BEFORE -> "before the generator"
            InitialPosition.AFTER -> "after the generator"
            InitialPosition.BOTTOM -> "at the bottom of the category"
        }

        ctx.replyHandler.replyEmbed(
            Embeds.success("All temporary voice channels will be generated $readablePosition.")
        )
    }

    @SubCommand(
        name = "queue",
        description = "Enable queue mode which will fills existing temporary vcs before creating new ones",
        group = "vc",
        groupDescription = "0"
    )
    suspend fun queue(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            description = "Enable queue mode which will fills existing temporary vcs before creating new ones",
            type = OptionType.BOOLEAN
        )
        enabled: Boolean
    ) {
        ctx.guildData.generators[ctx.generatorIndex].queueMode = enabled
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                if (enabled)
                    "Now, when a user joins the generator, Astro will check if there are temporary voice channels that haven't reached the maximum users capacity and put users in it first before generating a new temporary voice channel."
                else
                    "Now Astro will always generate a new vc when a user joins the generator."
            )
        )
    }

    @SubCommand(
        name = "region",
        description = "Set the region for temporary vcs",
        group = "vc",
        groupDescription = "0"
    )
    suspend fun region(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
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
                        Emoji.fromUnicode(region.emoji!!)
                )
            })
            .setPlaceholder("Choose a region")
            .setRequiredRange(1, 1)
            .build()

        ctx.replyHandler.replyWithSelectMenu(
            Embeds.selector("Choose the region for the temporary voice channels"),
            selectMenu,
            true
        ) {
            val selectedRegion = regionsAvailable[it.first().toInt()]
            ctx.guild.getVoiceChannelById(ctx.generatorData.id)
                ?.manager
                ?.setRegion(selectedRegion)
                ?.await()

            ctx.replyHandler.replyEmbed(
                Embeds.success(
                    "Default region for temporary voice channels set to: ${selectedRegion.emoji ?: Emojis.region.formatted} ${selectedRegion.getName()}"
                )
            )
        }
    }

    @SubCommand(
        name = "rename-conditions",
        description = "Set when a vc name should update",
        group = "vc",
        groupDescription = "0"
    )
    suspend fun renameConditions(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            description = "When the voice channel get unlocked, locked or hidden",
            type = OptionType.BOOLEAN
        )
        stateChange: Boolean?,
        @CommandOption(
            description = "When the voice channel owner changes",
            type = OptionType.BOOLEAN
        )
        ownerChange: Boolean?,
        @CommandOption(
            description = "Whether Astro should update the name if the voice channel name has been manually changed",
            type = OptionType.BOOLEAN
        )
        renamed: Boolean?,
        @CommandOption(
            description = "When the owner activity changes",
            type = OptionType.BOOLEAN
        )
        activityChange: Boolean?
    ) {
        val defaultRenameConditions = RenameConditions()

        val newRenameConditions =
            RenameConditions(
                stateChange ?: defaultRenameConditions.stateChange,
                ownerChange ?: defaultRenameConditions.ownerChange,
                renamed ?: defaultRenameConditions.renamed,
                activityChange ?: defaultRenameConditions.activityChange
            )

        ctx.guildData.generators[ctx.generatorIndex].renameConditions = newRenameConditions
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                "The rename conditions now are:" +
                    "\n> State changes > ${newRenameConditions.stateChange.asOnOrOff()}" +
                    "\n> Owner changes > ${newRenameConditions.ownerChange.asOnOrOff()}" +
                    "\n> If already renamed by user > ${newRenameConditions.renamed.asOnOrOff()}" +
                    "\n> User activity changes > ${newRenameConditions.activityChange.asOnOrOff()}"
            )
        )
    }

    @SubCommand(
        name = "state",
        description = "Set the default state (unlocked, locked or hidden) for temporary vcs",
        group = "vc",
        groupDescription = "0"
    )
    suspend fun state(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            description = "The state of the voice channel for which the name is applied",
            type = OptionType.STRING,
            stringChoices = ["Unlocked", "Locked", "Hidden"]
        )
        state: String
    ) {
        val newState = VCState.valueOf(state.uppercase())

        ctx.guildData.generators[ctx.generatorIndex].initialState = newState
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                "Temporary voice channels will now be **${newState.name.lowercase()}** by default."
            )
        )
    }


    /////////////////
    /// OWNERSHIP ///
    /////////////////
    @SubCommand(
        name = "permissions",
        description = "Set specific permissions for the vc owners",
        group = "owner",
        groupDescription = "0"
    )
    suspend fun ownerPermissions(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {
        val voicePermissions = (Permission.getPermissions(Permission.ALL_VOICE_PERMISSIONS) + listOf(
            Permission.VIEW_CHANNEL,
            Permission.MANAGE_CHANNEL,
            Permission.MANAGE_PERMISSIONS,
            Permission.CREATE_INSTANT_INVITE
        ))
            .sorted()

        val textPermissions = listOf(
            Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_TTS,
            Permission.MESSAGE_MANAGE,
            Permission.MESSAGE_EMBED_LINKS,
            Permission.MESSAGE_ATTACH_FILES,
            Permission.MESSAGE_EXT_EMOJI,
            Permission.MESSAGE_EXT_STICKER,
            Permission.MESSAGE_HISTORY,
            Permission.MESSAGE_MENTION_EVERYONE,
            Permission.USE_APPLICATION_COMMANDS
        )

        val voiceSelectMenu = StringSelectMenu.create(InteractionIds.getRandom())
            .setRequiredRange(0, voicePermissions.size)
            .setPlaceholder("Select the voice permissions")
            .addOptions(voicePermissions.mapIndexed { index, permission ->
                SelectOption.of(
                    permission.getName(),
                    index.toString()
                )
            })
            .build()

        ctx.replyHandler.replyWithSelectMenu(
            Embeds.selector("Select all the **voice permissions** that the owner of a temporary voice channel should have"),
            voiceSelectMenu,
            true
        ) {
            val selectedPermissions =
                voicePermissions.filterIndexed { index, _ -> index.toString() in it }.toMutableList()

            val textSelectMenu = StringSelectMenu.create(InteractionIds.getRandom())
                .setRequiredRange(0, textPermissions.size)
                .setPlaceholder("Select the text permissions")
                .addOptions(textPermissions.mapIndexed { index, permission ->
                    SelectOption.of(
                        permission.getName(),
                        index.toString()
                    )
                })
                .build()

            ctx.replyHandler.replyWithSelectMenu(
                Embeds.selector("Select all the **text permissions** that the owner of a temporary voice channel should have"),
                textSelectMenu,
                true
            ) {
                selectedPermissions.addAll(textPermissions.filterIndexed { index, _ -> index.toString() in it })

                val permissionsRaw = Permission.getRaw(selectedPermissions)

                ctx.guildData.generators[ctx.generatorIndex].ownerPermissions = permissionsRaw
                guildDao.save(ctx.guildData)

                ctx.replyHandler.replyEmbed(
                    Embeds.success(
                        "Here is the list of the permissions that the owners of temporary voice channels will have:" +
                            "\n• ${selectedPermissions.joinToString("\n") { "• ${it.getName()}" }}"
                    )
                )
            }
        }
    }

    @SubCommand(
        name = "role",
        description = "Set a role that will be temporarily assigned to vc owners",
        group = "owner",
        groupDescription = "0"
    )
    suspend fun ownerRole(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            description = "The role Astro should temporarily assign to voice channel owners",
            type = OptionType.ROLE
        )
        role: Role?
    ) {
        if (role != null && !premiumRequirementDetector.canAssignTemporaryVCOwnerRole(ctx.guildData)) {
            ctx.replyHandler.replyPremiumRequired()
            return
        }

        if (role != null && !ctx.guild.selfMember.canInteract(role)) {
            ctx.replyHandler.replyEmbed(Embeds.requireRoleHierarchy(role.name))
            return
        }

        ctx.guildData.generators[ctx.generatorIndex].ownerRole = role?.id
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                if (role == null)
                    "Temporary voice channel owners will not get any temporary role."
                else
                    "Temporary voice channel owners will now get the ${role.asMention} role."
            )
        )
    }


    ////////////
    /// CHAT ///
    ////////////
    @SubCommand(
        name = "category",
        description = "Set the category where private text chats get generated",
        group = "chat",
        groupDescription = "0"
    )
    suspend fun chatCategory(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            description = "The category for private text chats",
            type = OptionType.CHANNEL,
            channelTypes = [ChannelType.CATEGORY]
        )
        category: GuildChannel?
    ) {
        ctx.guildData.generators[ctx.generatorIndex].chatCategory = category?.id
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                if (category != null)
                    "Private text chats will now be generated under the category ${category.asMention}."
                else
                    "Private text chats will be generated at the top of the server since you didn't provide a category."
            )
        )
    }

    @SubCommand(
        name = "creation",
        description = "Choose whether Astro should create private text chats or use the integrated voice text chats",
        group = "chat",
        groupDescription = "0"
    )
    suspend fun chatCreation(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            description = "Whether Astro should create separate private text chats",
            type = OptionType.BOOLEAN
        )
        create: Boolean
    ) {
        if (create && !premiumRequirementDetector.canCreatePrivateChatOnVCGeneration(ctx.guildData)) {
            ctx.replyHandler.replyPremiumRequired()
            return
        }

        ctx.guildData.generators[ctx.generatorIndex].autoChat = create
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                if (create)
                    "Astro will create separate private text chats for temporary voice channels."
                else
                    "Astro will use the integrated voice channel text chats."
            )
        )
    }

    @SubCommand(
        name = "message",
        description = "Create a message that will be sent in the private text chats",
        group = "chat",
        groupDescription = "0"
    )
    suspend fun chatMessage(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            description = "The kind of message to send in the private text chat",
            type = OptionType.STRING,
            stringChoices = ["None", "Normal", "Embed", "Interface"]
        )
        type: String
    ) {
        if (type != "none" && !premiumRequirementDetector.canSendMessageInVCChatOnVCGeneration(ctx.guildData)) {
            ctx.replyHandler.replyPremiumRequired()
            return
        }

        if ((type == "normal") || (type == "embed")) {
            val textInput = TextInput.create(InteractionIds.getRandom(), "content", TextInputStyle.PARAGRAPH)
                .setRequired(true)
                .setRequiredRange(1, 4000)
                .setPlaceholder("The text that Astro will send in private text chats of temporary voice channels")
                .build()

            val modal = Modal.create(InteractionIds.getRandom(), "Chat message content")
                .addActionRow(textInput)
                .build()

            ctx.replyHandler.replyWithModel(modal) {
                val content = it.getValue(textInput.id)!!.asString

                ctx.guildData.generators[ctx.generatorIndex].defaultChatText = content
                ctx.guildData.generators[ctx.generatorIndex].defaultChatTextEmbed = type == "embed"
                ctx.guildData.generators[ctx.generatorIndex].chatInterface = -1
                guildDao.save(ctx.guildData)

                it.replyEmbeds(
                    Embeds.success(
                        "This is the new message Astro will sent in private text chats of temporary voice channels\n > $content"
                            .take(MessageEmbed.DESCRIPTION_MAX_LENGTH)
                    )
                ).await()
            }
        } else if (type == "interface") {
            if (ctx.guildData.interfaces.isEmpty()) {
                ctx.replyHandler.replyEmbed(
                    Embeds.error(
                        "You don't have any interface in this server." +
                            "\nCreate an interface with `/interface create` and then run this command again."
                    )
                )
                return
            }

            val selectMenuBuilder = StringSelectMenu.create(InteractionIds.getRandom())
                .setPlaceholder("Select the interface")

            ctx.guildData.interfaces.mapIndexed { index, interfaceDto ->
                selectMenuBuilder.addOption(
                    interfaceDto.channelID.asChannelMention(),
                    index.toString(),
                    "${interfaceDto.buttons.size} actions - message id ${interfaceDto.messageID}",
                    Emojis.vcInterface
                )
            }

            ctx.replyHandler.replyWithSelectMenu(
                Embeds.selector("Select an interface that will be sent in the private text chat of temporary voice channels"),
                selectMenuBuilder.build(),
                true
            ) {
                ctx.guildData.generators[ctx.generatorIndex].chatInterface = it.first().toIntOrNull() ?: -1
                guildDao.save(ctx.guildData)

                ctx.replyHandler.replyEmbed(
                    Embeds.success(
                        "Users will get an interface in their private text chat by default."
                    )
                )
            }
        } else {
            ctx.guildData.generators[ctx.generatorIndex].chatInterface = -1
            ctx.guildData.generators[ctx.generatorIndex].defaultChatText = null
            guildDao.save(ctx.guildData)

            ctx.replyHandler.replyEmbed(
                Embeds.success(
                    "Users will not get any message in their private text chat on its creation."
                )
            )
        }
    }

    @SubCommand(
        name = "name",
        description = "The name for private text chats (see docs for variables)",
        group = "chat",
        groupDescription = "0"
    )
    suspend fun chatName(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            description = "The name for private text chats, see documentation for variables",
            type = OptionType.STRING,
            minLength = 2,
            maxLength = 500
        )
        name: String,
    ) {
        if (!premiumRequirementDetector.canUseVCNameTemplate(ctx.guildData, name)) {
            ctx.replyHandler.replyEmbedAndComponent(
                embed = Embeds.error(
                    "Some variables you used in the name can only be used in premium servers." +
                        "\nSee documentation for variables with the button below."
                ),
                component = Buttons.Help.variables
            )
            return
        }

        ctx.guildData.generators[ctx.generatorIndex].defaultChatName = name
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbedAndComponent(
            Embeds.success(
                "Private text chats will now have the following name:" +
                    "\n> *$name*" +
                    "\n\nYou can use ${Emojis.variables.formatted} variables to customize the name." +
                    "\nUse the button below to see the documentation"
            ),
            component = Buttons.Help.variables
        )
    }

    @SubCommand(
        name = "nsfw",
        description = "Sets the nsfw setting for private text chats",
        group = "chat",
        groupDescription = "0"
    )
    suspend fun chatNsfw(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            description = "Whether nsfw is enabled (true) or disabled (false)",
            type = OptionType.BOOLEAN
        )
        nsfw: Boolean
    ) {
        ctx.guildData.generators[ctx.generatorIndex].chatNsfw = nsfw
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                "Nsfw of private text chats is now ${nsfw.asEnabledOrDisabled()}."
            )
        )
    }

    @SubCommand(
        name = "permissions",
        description = "Set from which channel private text chats should inherit the permissions",
        group = "chat",
        groupDescription = "0"
    )
    suspend fun chatPermissions(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            name = "inherit-from",
            description = "The channel from which private text chats will inherit the permissions",
            type = OptionType.STRING,
        )
        inheritFrom: PermissionsInherited,
    ) {
        ctx.guildData.generators[ctx.generatorIndex].chatPermissionsInherited = inheritFrom
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                if (inheritFrom == PermissionsInherited.NONE)
                    "Private text chats will not inherit any permissions"
                else
                    "Private text chats will inherit permissions from the ${inheritFrom.name.lowercase()}"
            )
        )
    }

    @SubCommand(
        name = "slowmode",
        description = "Set the slowmode for private text chat",
        group = "chat",
        groupDescription = "0"
    )
    suspend fun chatSlowmode(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            description = "The slowmode time in seconds, 0 for no slowmode",
            type = OptionType.INTEGER,
            minValue = 0,
            maxValue = TextChannel.MAX_SLOWMODE.toLong()
        )
        seconds: Int
    ) {
        ctx.guildData.generators[ctx.generatorIndex].chatSlowmode = seconds

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                if (seconds != 0) "Private text chats slowmode set to $seconds seconds"
                else "Private text chats slowmode disabled",
            )
        )
    }

    @SubCommand(
        name = "topic",
        description = "Set the topic for private text chat",
        group = "chat",
        groupDescription = "0"
    )
    suspend fun chatTopic(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext,
        @CommandOption(
            description = "The topic for private text chat, leave empty for no topic",
            type = OptionType.STRING,
            minLength = 2,
            maxLength = 1024
        )
        topic: String?
    ) {
        ctx.guildData.generators[ctx.generatorIndex].chatTopic = topic
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                if (topic != null)
                    "The topic for the private text chats now is:\n> $topic"
                else
                    "Private text chats will not have a topic."
            )
        )
    }
}