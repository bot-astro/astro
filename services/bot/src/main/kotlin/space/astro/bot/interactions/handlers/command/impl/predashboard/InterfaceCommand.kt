package space.astro.bot.interactions.handlers.command.impl.predashboard

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.sharding.ShardManager
import space.astro.bot.components.managers.InterfaceManager
import space.astro.bot.components.managers.PremiumRequirementDetector
import space.astro.bot.core.ui.Buttons
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.InterfaceSettingsInteractionContext
import space.astro.bot.interactions.context.SettingsInteractionContext
import space.astro.bot.interactions.handlers.command.*
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.models.database.EmbedStyle
import space.astro.shared.core.models.database.InterfaceButton
import space.astro.shared.core.util.extention.asChannelMention
import space.astro.shared.core.util.extention.asMessageMarkdownLink
import space.astro.shared.core.util.extention.asTrueOrFalse
import space.astro.shared.core.util.extention.lowercaseAndCapitalize
import java.awt.Color
import java.text.ParseException
import java.text.SimpleDateFormat

@Command(
    name = "interface",
    description = "Create, edit and delete interfaces",
    requiredPermissions = [Permission.MANAGE_CHANNEL],
    category = CommandCategory.SETTINGS,
    action = InteractionAction.SETTINGS
)
class InterfaceCommand(
    private val premiumRequirementDetector: PremiumRequirementDetector,
    private val interfaceManager: InterfaceManager,
    private val guildDao: GuildDao,
    private val shardManager: ShardManager
) : AbstractCommand() {

    /////////////
    /// BASIC ///
    /////////////
    @SubCommand(
        name = "create",
        description = "Creates an interface that can be used to manage temporary voice channels"
    )
    suspend fun create(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext,
        @CommandOption(
            description = "The channel where the interface will get created",
            type = OptionType.CHANNEL,
            channelTypes = [ChannelType.TEXT]
        )
        channel: GuildChannel
    ) {
        if (!premiumRequirementDetector.canCreateInterface(ctx.guildData)) {
            ctx.replyHandler.replyEmbedAndComponent(
                embed = Embeds.error(
                    "There is already 1 Interface setup in this server." +
                            "\nPremium is required to have more than 1 Interface." +
                            "\nPossible solutions:" +
                            "\n• Get ${Emojis.premium.formatted}" +
                            "\n• Delete an existing interface with `/interface delete`"),
                component = Buttons.premium
            )
            return
        }

        ctx.replyHandler.deferReply()

        val interfaceData = interfaceManager.createInterface(channel as TextChannel)

        ctx.guildData.interfaces.add(interfaceData)
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbedAndComponent(
            embed = Embeds.success(
            "Interface created in ${channel.asMention} (${"message".asMessageMarkdownLink(ctx.guildId, channel.id, interfaceData.messageID)})"
            ),
            component = Buttons.Help.interfaces
        )
    }

    @SubCommand(
        name = "delete",
        description = "Deletes an interface"
    )
    suspend fun delete(
        event: SlashCommandInteractionEvent,
        ctx: InterfaceSettingsInteractionContext
    ) {
        val deletedInterface = ctx.guildData.interfaces.removeAt(ctx.interfaceIndex)

        try {
            ctx.guild.getTextChannelById(deletedInterface.channelID)
                ?.deleteMessageById(deletedInterface.messageID)?.await()
        } catch (e: ErrorResponseException) {
            if (e.errorResponse != ErrorResponse.UNKNOWN_CHANNEL && e.errorResponse != ErrorResponse.MISSING_ACCESS && e.errorResponse != ErrorResponse.UNKNOWN_MESSAGE)
                throw e
        }

        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                "The interface has been deleted." +
                "\nIf you want to create a new interface use `/interface create`."
            )
        )
    }

    ////////////
    /// EDIT ///
    ////////////
    @SubCommand(
        name = "button",
        description = "Adds a button to an interface",
        group = "add",
        groupDescription = "0"
    )
    suspend fun addButton(
        event: SlashCommandInteractionEvent,
        ctx: InterfaceSettingsInteractionContext,
        @CommandOption(
            description = "The name of the button, leave blank for no name",
            type = OptionType.STRING
        )
        name: String?,
        @CommandOption(
            description = "The emoji of the button, leave blank for no emoji",
            type = OptionType.STRING
        )
        emoji: String?,
        @CommandOption(
            description = "The style of the button",
            type = OptionType.STRING,
            stringChoices = ["Green", "Blue", "Red", "Gray"]
        )
        color: String?,
        @CommandOption(
            description = "If the button should be disabled",
            type = OptionType.BOOLEAN,
        )
        disabled: Boolean?
    ) {
        if (name == null && emoji == null) {
            ctx.replyHandler.replyEmbed(Embeds.error(
                "You didn't provide neither a name or an emoji for the button" +
                "\nRerun this command and provide at least a name or an emoji."
            ))
            return
        }

        val buttonStyle = when(color) {
            "blue" -> ButtonStyle.PRIMARY.key
            "green" -> ButtonStyle.SUCCESS.key
            "red" -> ButtonStyle.DANGER.key
            "gray" -> ButtonStyle.SECONDARY.key
            else -> ButtonStyle.PRIMARY.key
        }

        val usedInteractionIds = ctx.interfaceData.buttons.map { it.id }
        val availableInterfaceButtons = interfaceManager.defaultInterfaceButtons.filter { it.id !in usedInteractionIds }.take(25)

        if (availableInterfaceButtons.isEmpty()) {
            ctx.replyHandler.replyEmbed(Embeds.error("You already added all possible actions to this interface!"))
            return
        }

        val selectMenu = StringSelectMenu.create(InteractionIds.getRandom())
            .addOptions(availableInterfaceButtons.mapIndexed { index, action ->
                SelectOption.of(action.name?.takeIf { it.isNotEmpty() } ?: action.id.split("?").first(), index.toString()).apply {
                    if (action.emoji != null)
                        withEmoji(Emoji.fromFormatted(action.emoji!!))
                }
            })
            .setPlaceholder("Select action command")
            .build()

        ctx.replyHandler.replyWithSelectMenu(
            Embeds.selector("Select the action that this interface button will execute when triggered."),
            selectMenu,
            true
        ) {
            val interfaceButtonSelected = availableInterfaceButtons[it[0].toInt()]
            val position = interfaceManager.calculateNewButtonPosition(ctx.interfaceData.buttons)
                ?: run {
                    ctx.replyHandler.replyEmbed(
                        Embeds.error(
                            "The interface already has 25 buttons, you can't add more." +
                            "\nDelete a button from the interface with `/interface delete button` then re-use this command to add a new button."
                        )
                    )
                    return@replyWithSelectMenu
                }

            interfaceButtonSelected.name = name
            interfaceButtonSelected.emoji = emoji
            interfaceButtonSelected.position = position
            interfaceButtonSelected.buttonStyleKey = buttonStyle
            interfaceButtonSelected.buttonDisabled = disabled ?: false

            ctx.interfaceData.buttons.add(interfaceButtonSelected)

            try {
                interfaceManager.updateInterface(ctx.guild, ctx.interfaceData)

                ctx.guildData.interfaces[ctx.interfaceIndex] = ctx.interfaceData
                guildDao.save(ctx.guildData)

                ctx.replyHandler.replyEmbed(
                    Embeds.success(
                        "A new button has been added to the ${ctx.interfaceData.asMarkdownLink(ctx.guildId)}."
                    )
                )
            } catch (e: Exception) {
                ctx.replyHandler.replyEmbed(Embeds.error(
                    "Either the interface doesn't exist or Astro doesn't have permissions to access it" +
                    "\nMake sure Astro has permissions in ${ctx.interfaceData.channelID.asChannelMention()} and that the interface still exists, otherwise use `/interface edit channel` to re-create it."
                ))
            }
        }
    }

    @SubCommand(
        name = "button",
        description = "Deletes a button from an interface",
        group = "remove",
        groupDescription = "0"
    )
    suspend fun deleteButton(
        event: SlashCommandInteractionEvent,
        ctx: InterfaceSettingsInteractionContext
    ) {
        val selectMenu = StringSelectMenu.create(InteractionIds.getRandom())
            .addOptions(ctx.interfaceData.buttons.mapIndexed { index, b ->
                SelectOption.of(b.name?.takeIf { it.isNotEmpty() } ?: b.id.split("?").first(), index.toString())
                    .withEmoji(if (b.emoji != null) Emoji.fromFormatted(b.emoji!!) else null)
            })
            .setPlaceholder("Choose a button to remove")
            .build()

        ctx.replyHandler.replyWithSelectMenu(
            Embeds.selector(
                "Select the button to remove from the ${ctx.interfaceData.asMarkdownLink(ctx.guildId)}"
            ),
            selectMenu,
            true
        ) {
            ctx.interfaceData.buttons.removeAt(it[0].toInt())

            try {
                interfaceManager.updateInterface(ctx.guild, ctx.interfaceData)
                ctx.guildData.interfaces[ctx.interfaceIndex] = ctx.interfaceData
                guildDao.save(ctx.guildData)

                ctx.replyHandler.replyEmbed(
                    Embeds.success(
                        "The button has been removed from the ${ctx.interfaceData.asMarkdownLink(ctx.guildId)}"
                    )
                )
            } catch (e: Exception) {
                ctx.replyHandler.replyEmbed(Embeds.error(
                    "Either the interface doesn't exist or Astro doesn't have permissions to access it" +
                    "\nMake sure Astro has permissions in ${ctx.interfaceData.channelID.asChannelMention()} and that the interface still exists, otherwise use `/interface edit channel` to re-create it."
                ))
            }
        }
    }

    @SubCommand(
        name = "button-order",
        description = "Changes the order of the buttons in an interface",
        group = "edit",
        groupDescription = "0"
    )
   suspend fun editButtonOrder(
        event: SlashCommandInteractionEvent,
        ctx: InterfaceSettingsInteractionContext,
        @CommandOption(
            description = "Orders the buttons automatically",
            type = OptionType.BOOLEAN
        )
        auto: Boolean?
    ) {
        if (!premiumRequirementDetector.canEditInterfaceButtonOrder(ctx.guildData)) {
            ctx.replyHandler.replyPremiumRequired()
            return
        }

        val interfaceButtons = ctx.interfaceData.buttons

        val orderedButtons = if (auto == true) {
            interfaceManager.orderButtons(interfaceButtons)
        } else {
            val newButtons = mutableListOf<InterfaceButton>()

            for (i in 0 until 5) {
                if (interfaceButtons.isEmpty())
                    break

                val minActionsAmountForRow = (5 - (((5 - (i + 1)) * 5) - (interfaceButtons.size - newButtons.size))).coerceAtLeast(0)

                val selectMenu = StringSelectMenu.create(InteractionIds.getRandom())
                    .addOptions(interfaceButtons.mapIndexed { index, b ->
                        SelectOption.of(b.name ?: b.id, index.toString())
                            .withEmoji(if (b.emoji != null) Emoji.fromFormatted(b.emoji!!) else null)
                    })
                    .setRequiredRange(minActionsAmountForRow, 5)
                    .build()
                val cancelButton = Buttons.cancel()

                ctx.replyHandler.reply(
                    embed = Embeds.selector(
                        "You are currently selecting the buttons that will go on a specific row.\n\nSelect the buttons for the ${i+1} row"
                    ),
                    components = listOf(
                        ActionRow.of(selectMenu),
                        ActionRow.of(cancelButton)
                    )
                )

                val newEvent = withTimeoutOrNull(60000) {
                    shardManager.await<GenericComponentInteractionCreateEvent> {
                        (it is StringSelectInteractionEvent && it.componentId == selectMenu.id)
                                || (it is ButtonInteractionEvent && it.componentId == cancelButton.id)
                    }
                }

                if (newEvent == null) {
                    ctx.replyHandler.replyEmbed(Embeds.canceled)
                    return
                }

                ctx.replyHandler.setCallbacksFromComponentEvent(newEvent)

                if (newEvent is ButtonInteractionEvent) {
                    ctx.replyHandler.replyEmbed(Embeds.canceled)
                    return
                }

                val buttonIndexes = (newEvent as StringSelectInteractionEvent).values.map { it.toInt() }

                val buttons = mutableListOf<Button>()
                for (aIndex in buttonIndexes) {
                    val button = interfaceButtons[aIndex]
                    buttons.add(interfaceManager.formatButton(button, "itemp${button.id}"))
                }

                for (index in buttonIndexes.indices) {
                    ctx.replyHandler.replyEmbedAndComponents(
                        embed = Embeds.selector(
                            "You are now selecting the column order of the buttons of a row.\n\nSelect the button for the ${index+1} column"
                        ),
                        components = buttons
                    )
                    val btnIds = buttons.map { bns -> bns.id }
                    val secondEvent = withTimeoutOrNull(60000) {
                        shardManager.await<ButtonInteractionEvent> {
                            it.componentId in btnIds
                        }
                    } ?: run {
                        ctx.replyHandler.replyEmbed(Embeds.timeExpired)
                        return
                    }

                    ctx.replyHandler.setCallbacksFromComponentEvent(newEvent)

                    val buttonID = secondEvent.componentId

                    buttons.removeIf { it.id == buttonID }
                    val selectedActionIndex = interfaceButtons.indexOfFirst { it.id == buttonID.drop(5) }
                    val selectedAction = interfaceButtons[selectedActionIndex]
                    newButtons.add(selectedAction)
                    newButtons[newButtons.size - 1].position = Pair(i, index)
                    interfaceButtons.remove(selectedAction)
                    delay(1000)
                }
            }

            newButtons
        }

        ctx.interfaceData.buttons = orderedButtons.toMutableList()
        try {
            interfaceManager.updateInterface(ctx.guild, ctx.interfaceData)

            ctx.guildData.interfaces[ctx.interfaceIndex] = ctx.interfaceData
            guildDao.save(ctx.guildData)

            ctx.replyHandler.replyEmbed(
                Embeds.success(
                    "The buttons order has been updated, check the ${ctx.interfaceData.asMarkdownLink(ctx.guildId)}."
                )
            )
        } catch (e: Exception) {
            ctx.replyHandler.replyEmbed(Embeds.error(
                "Either the interface doesn't exist or Astro doesn't have permissions to access it" +
                "\nMake sure Astro has permissions in ${ctx.interfaceData.channelID.asChannelMention()} and that the interface still exists, otherwise use `/interface edit channel` to re-create it."
            ))
        }
    }

    @SubCommand(
        name = "button",
        description = "Edit a button of an interface",
        group = "edit",
        groupDescription = "0"
    )
    suspend fun editButton(
        event: SlashCommandInteractionEvent,
        ctx: InterfaceSettingsInteractionContext,
        @CommandOption(
            description = "The name of the button, leave blank for no name",
            type = OptionType.STRING
        )
        name: String?,
        @CommandOption(
            description = "The emoji of the button, leave blank for no emoji",
            type = OptionType.STRING
        )
        emoji: String?,
        @CommandOption(
            description = "The style of the button",
            type = OptionType.STRING,
            stringChoices = ["Green", "Blue", "Red", "Gray"]
        )
        color: String?,
        @CommandOption(
            description = "If the button should be disabled",
            type = OptionType.BOOLEAN,
        )
        disabled: Boolean?
    ) {
        if (name == null && emoji == null) {
            ctx.replyHandler.replyEmbed(
                Embeds.error(
                    "You didn't provide neither a name or an emoji for the button" +
                    "\nRerun this command and provide at least a name or an emoji."
                )
            )
            return
        }

        val buttonStyle = when(color) {
            "blue" -> ButtonStyle.PRIMARY.key
            "green" -> ButtonStyle.SUCCESS.key
            "red" -> ButtonStyle.DANGER.key
            "gray" -> ButtonStyle.SECONDARY.key
            else -> ButtonStyle.PRIMARY.key
        }

        val interfaceButtonsExisting = interfaceManager.defaultInterfaceButtons.take(25)
        val buttonSelectMenu = StringSelectMenu
            .create(InteractionIds.getRandom())
            .addOptions(interfaceButtonsExisting.map { a ->
                SelectOption.of(a.name?.takeIf { it.isNotEmpty() } ?: a.id.split("?").first(), a.id)
                    .let {
                        if (a.emoji != null)
                            it.withEmoji(Emoji.fromFormatted(a.emoji!!))
                        else
                            it
                    }
            })
            .setPlaceholder("Select an action for the button")
            .build()

        ctx.replyHandler.replyWithSelectMenu(
            Embeds.selector("Select an action for the button to edit."),
            buttonSelectMenu,
            true
        ) { actionSelection ->
            val actionId = actionSelection[0]

            val buttonToModifySelectMenu = StringSelectMenu.create(InteractionIds.getRandom())
                .addOptions(ctx.interfaceData.buttons.mapIndexed { index, ib ->
                    SelectOption.of(ib.name?.takeIf { it.isNotEmpty() } ?: ib.id.split("?").first(), index.toString())
                        .withEmoji(if (ib.emoji != null) Emoji.fromFormatted(ib.emoji!!) else null)
                })
                .setPlaceholder("Select the button to modify")
                .build()

            ctx.replyHandler.replyWithSelectMenu(
                Embeds.selector(
                    "Select the button to which you want to apply the following settings:" +
                            "\n• Action > `$actionId`" +
                            "\n• Name > `${name ?: "no name"}`" +
                            "\n• Emoji > `${emoji ?: "no emoji"}`" +
                            "\n• Style > `${ButtonStyle.fromKey(buttonStyle).name.lowercaseAndCapitalize()}`" +
                            "\n• Disabled > `${(disabled ?: false).asTrueOrFalse()}`"
                ),
                buttonToModifySelectMenu,
                true
            ) {
                val buttonIndexToModify = it[0].toInt()

                ctx.interfaceData.buttons[buttonIndexToModify].id = actionId
                ctx.interfaceData.buttons[buttonIndexToModify].buttonStyleKey = buttonStyle
                if (name != null)
                    ctx.interfaceData.buttons[buttonIndexToModify].name = name
                if (emoji != null)
                    ctx.interfaceData.buttons[buttonIndexToModify].emoji = emoji
                if (disabled != null)
                    ctx.interfaceData.buttons[buttonIndexToModify].buttonDisabled = disabled

                ctx.guildData.interfaces[ctx.interfaceIndex] = ctx.interfaceData
                guildDao.save(ctx.guildData)

                try {
                    interfaceManager.updateInterface(ctx.guild, ctx.interfaceData)

                    ctx.replyHandler.replyEmbed(
                        Embeds.success(
                            "The button of the ${ctx.interfaceData.asMarkdownLink(ctx.guildId)} has been edited."
                        )
                    )
                } catch (e: Exception) {
                    ctx.replyHandler.replyEmbed(Embeds.error(
                        "Either the interface doesn't exist or Astro doesn't have permissions to access it" +
                        "\nMake sure Astro has permissions in ${ctx.interfaceData.channelID.asChannelMention()} and that the interface still exists, otherwise use `/interface edit channel` to re-create it."
                    ))
                }
            }
        }
    }

    @SubCommand(
        name = "channel",
        description = "Moves an interface to another channel",
        group = "edit",
        groupDescription = "0"
    )
    suspend fun editChannel(
        event: SlashCommandInteractionEvent,
        ctx: InterfaceSettingsInteractionContext,
        @CommandOption(
            description = "Moves an interface to another channel",
            type = OptionType.CHANNEL,
            channelTypes = [ChannelType.TEXT]
        )
        channel: GuildChannel
    ) {
        try {
            ctx.guild.getTextChannelById(ctx.interfaceData.channelID)?.deleteMessageById(ctx.interfaceData.messageID)?.await()
        } catch (_: Exception) {}

        val interfaceData = interfaceManager.sendInterface((channel as TextChannel), ctx.interfaceData)
        ctx.guildData.interfaces[ctx.interfaceIndex] = interfaceData
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                "The channel for the ${interfaceData.asMarkdownLink(ctx.guildId)} has been changed to ${channel.asMention}."
            ))
    }

    @SubCommand(
        name = "message",
        description = "Modify the message content and style of an interface",
        group = "edit",
        groupDescription = "0"
    )
    suspend fun editMessage(
        event: SlashCommandInteractionEvent,
        ctx: InterfaceSettingsInteractionContext,
        @CommandOption(
            description = "Remove a section of the interface",
            type = OptionType.STRING,
            stringChoices = [
                "Color", "Author", "Author_url",
                "Author_image", "Title", "Title_url",
                "Description", "Thumbnail", "Image",
                "Timestamp", "Footer", "Footer_image"
            ]
        )
        remove: String?,
        @CommandOption(
            description = "The color (hexadecimal format #f4e5aa) for the message",
            type = OptionType.STRING
        )
        color: String?,
        @CommandOption(
            description = "The author of the message",
            type = OptionType.STRING
        )
        author: String?,
        @CommandOption(
            name = "author_url",
            description = "The link for the message author",
            type = OptionType.STRING
        )
        authorUrl: String?,
        @CommandOption(
            name = "author_image",
            description = "The image link for the message author",
            type = OptionType.STRING
        )
        authorImage: String?,
        @CommandOption(
            description = "The title for the message",
            type = OptionType.STRING
        )
        title: String?,
        @CommandOption(
            name = "title_url",
            description = "The url for the message title",
            type = OptionType.STRING
        )
        titleUrl: String?,
        @CommandOption(
            description = "The description for the message",
            type = OptionType.STRING
        )
        description: String?,
        @CommandOption(
            description = "The thumbnail link for the message",
            type = OptionType.STRING
        )
        thumbnail: String?,
        @CommandOption(
            description = "The image link for the message",
            type = OptionType.STRING
        )
        image: String?,
        @CommandOption(
            description = "The timestamp (yyyy-mm-dd:hh:ss) for the message",
            type = OptionType.STRING
        )
        timestamp: String?,
        @CommandOption(
            description = "The footer for the message",
            type = OptionType.STRING
        )
        footer: String?,
        @CommandOption(
            name = "footer_image",
            description = "The image for the message footer",
            type = OptionType.STRING
        )
        footerImage: String?
    ) {
        if (!premiumRequirementDetector.canEditInterfaceMessage(ctx.guildData)) {
            ctx.replyHandler.replyPremiumRequired()
            return
        }

        val defaultEmbedStyle = EmbedStyle()

        if (remove != null) {
            when (remove) {
                "title" -> ctx.interfaceData.embedStyle.title = null
                "title_url" -> ctx.interfaceData.embedStyle.url = null
                "description" -> ctx.interfaceData.embedStyle.description = null
                "timestamp" -> ctx.interfaceData.embedStyle.timestamp = null
                "color" -> ctx.interfaceData.embedStyle.color = defaultEmbedStyle.color
                "thumbnail" -> ctx.interfaceData.embedStyle.thumbnail = null
                "image" -> ctx.interfaceData.embedStyle.image = null
                "author" -> ctx.interfaceData.embedStyle.authorName = null
                "author_url" -> ctx.interfaceData.embedStyle.authorUrl = null
                "author_image" -> ctx.interfaceData.embedStyle.authorIconUrl = null
                "footer" -> ctx.interfaceData.embedStyle.footer = null
                "footer_image" -> ctx.interfaceData.embedStyle.footerIconUrl = null
            }
        }

        val errors = mutableListOf<String>()

        if (title != null)
            ctx.interfaceData.embedStyle.title = title
        if (titleUrl != null)
            ctx.interfaceData.embedStyle.url = titleUrl
        if (description != null)
            ctx.interfaceData.embedStyle.description = description
        if (timestamp != null) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd:HH:mm")
            try {
                val date = dateFormat.parse(timestamp)
                ctx.interfaceData.embedStyle.timestamp = date.time
            } catch (e: ParseException) {
                errors.add("The timestamp wasn't correctly formatted")
            }
        }
        if (color != null) {
            try {
                val hexColor = Color.decode(color)
                ctx.interfaceData.embedStyle.color = hexColor.rgb
            } catch (e: NumberFormatException) {
                errors.add("The color was not valid hexadecimal color")
            }
        }
        if (thumbnail != null)
            ctx.interfaceData.embedStyle.thumbnail = thumbnail
        if (image != null)
            ctx.interfaceData.embedStyle.image = image
        if (author != null)
            ctx.interfaceData.embedStyle.authorName = author
        if (authorUrl != null)
            ctx.interfaceData.embedStyle.authorUrl = authorUrl
        if (authorImage != null)
            ctx.interfaceData.embedStyle.authorIconUrl = authorImage
        if (footer != null)
            ctx.interfaceData.embedStyle.footer = footer
        if (footerImage != null)
            ctx.interfaceData.embedStyle.footerIconUrl = footerImage

        interfaceManager.updateEmbed(ctx.guild, ctx.interfaceData)
        ctx.guildData.interfaces[ctx.interfaceIndex] = ctx.interfaceData
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(Embeds.success(
            if (errors.isEmpty())
                "The ${ctx.interfaceData.asMarkdownLink(ctx.guildId)} message has been updated successfully."
            else
                "The ${ctx.interfaceData.asMarkdownLink(ctx.guildId)} message has been updated but Astro encountered some errors:" +
                        "\n• ${errors.joinToString("\n• ")}"
        ))
    }
}