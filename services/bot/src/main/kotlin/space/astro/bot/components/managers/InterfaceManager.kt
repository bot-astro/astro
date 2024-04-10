package space.astro.bot.components.managers

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.PermissionException
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Component
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.InteractionComponentBuilder
import space.astro.bot.interactions.InteractionIds
import space.astro.shared.core.models.database.InterfaceButton
import space.astro.shared.core.models.database.InterfaceData
import java.time.Instant

private val log = KotlinLogging.logger {  }

@org.springframework.stereotype.Component
class InterfaceManager(
    private val interactionComponentBuilder: InteractionComponentBuilder
) {
    private val MAX_ACTION_ROWS = 5
    private val MAX_COMPONENTS = Message.MAX_COMPONENT_COUNT
    private val MAX_BUTTONS_PER_COMPONENT = Component.Type.BUTTON.maxPerRow

    fun getDefaultInterfaceButtons() = mutableListOf(
        InterfaceButton.fromButton(
            interactionComponentBuilder.buttonWithEmoji(InteractionIds.Button.VC_LOCK, ButtonStyle.SECONDARY, Emojis.lock),
            Pair(0, 0)
        ),
        InterfaceButton.fromButton(
            interactionComponentBuilder.buttonWithEmoji(InteractionIds.Button.VC_UNLOCK, ButtonStyle.SECONDARY, Emojis.unlock),
            Pair(0, 1)
        ),
        InterfaceButton.fromButton(
            interactionComponentBuilder.buttonWithEmoji(InteractionIds.Button.VC_HIDE, ButtonStyle.SECONDARY, Emojis.hide),
            Pair(0, 2)
        ),
        InterfaceButton.fromButton(
            interactionComponentBuilder.buttonWithEmoji(InteractionIds.Button.VC_UNHIDE, ButtonStyle.SECONDARY, Emojis.unhide),
            Pair(0, 3)
        ),

        InterfaceButton.fromButton(
            interactionComponentBuilder.buttonWithEmoji(InteractionIds.Button.VC_LIMIT, ButtonStyle.SECONDARY, Emojis.limit),
            Pair(1, 0)
        ),
        InterfaceButton.fromButton(
            interactionComponentBuilder.buttonWithEmoji(InteractionIds.Button.VC_INVITE, ButtonStyle.SECONDARY, Emojis.invite),
            Pair(1, 1)
        ),
        InterfaceButton.fromButton(
            interactionComponentBuilder.buttonWithEmoji(InteractionIds.Button.VC_BAN, ButtonStyle.SECONDARY, Emojis.ban),
            Pair(1, 2)
        ),
        InterfaceButton.fromButton(
            interactionComponentBuilder.buttonWithEmoji(InteractionIds.Button.VC_PERMIT, ButtonStyle.SECONDARY, Emojis.permit),
            Pair(1, 3)
        ),

        InterfaceButton.fromButton(
            interactionComponentBuilder.buttonWithEmoji(InteractionIds.Button.VC_NAME, ButtonStyle.SECONDARY, Emojis.name),
            Pair(2, 0)
        ),
        InterfaceButton.fromButton(
            interactionComponentBuilder.buttonWithEmoji(InteractionIds.Button.VC_BITRATE, ButtonStyle.SECONDARY, Emojis.bitrate),
            Pair(2, 1)
        ),
        InterfaceButton.fromButton(
            interactionComponentBuilder.buttonWithEmoji(InteractionIds.Button.VC_REGION, ButtonStyle.SECONDARY, Emojis.region),
            Pair(2, 2)
        ),
        InterfaceButton.fromButton(
            interactionComponentBuilder.buttonWithEmoji(InteractionIds.Button.VC_TEMPLATE, ButtonStyle.SECONDARY, Emojis.template),
            Pair(2, 3)
        ),

        InterfaceButton.fromButton(
            interactionComponentBuilder.buttonWithEmoji(InteractionIds.Button.VC_CHAT, ButtonStyle.SECONDARY, Emojis.chat),
            Pair(3, 0)
        ),
        InterfaceButton.fromButton(
            interactionComponentBuilder.buttonWithEmoji(InteractionIds.Button.VC_WAITING_ROOM, ButtonStyle.SECONDARY, Emojis.waiting),
            Pair(3, 1)
        ),
        InterfaceButton.fromButton(
            interactionComponentBuilder.buttonWithEmoji(InteractionIds.Button.VC_CLAIM, ButtonStyle.SECONDARY, Emojis.owner),
            Pair(3, 2)
        ),
        InterfaceButton.fromButton(
            interactionComponentBuilder.buttonWithEmoji(InteractionIds.Button.VC_TRANSFER, ButtonStyle.SECONDARY, Emojis.transfer),
            Pair(3, 3)
        ),
    )

    suspend fun createInterface(channel: TextChannel): InterfaceData {
        val interfaceData = InterfaceData(
            channelID = channel.id,
            messageID = "temp_id",
            buttons = getDefaultInterfaceButtons()
        )

        log.info { "creating interface: $interfaceData" }

        val interfaceMessage = channel.sendMessage(computeMessage(interfaceData)).await()
        interfaceData.messageID = interfaceMessage.id

        return  interfaceData
    }

    suspend fun sendInterface(channel: TextChannel, interfaceData: InterfaceData): InterfaceData {
        val interfaceMessage = channel.sendMessage(computeMessage(interfaceData)).await()
        interfaceData.messageID = interfaceMessage.id
        interfaceData.channelID = channel.id

        return interfaceData
    }

    fun computeMessage(interfaceData: InterfaceData): MessageCreateData {
        return MessageCreateBuilder()
            .setEmbeds(computeEmbed(interfaceData))
            .setComponents(computeComponents(interfaceData))
            .build()
    }

    private fun computeEmbed(interfaceData: InterfaceData): MessageEmbed {
        val embedStyle = interfaceData.embedStyle

        return Embed {
            color = embedStyle.color

            author {
                name = embedStyle.authorName
                iconUrl = embedStyle.authorIconUrl
                url = embedStyle.authorUrl
            }

            title = embedStyle.title
            url = embedStyle.url
            description = embedStyle.description
            thumbnail = embedStyle.thumbnail
            image = embedStyle.image

            embedStyle.timestamp?.also {
                timestamp = Instant.ofEpochMilli(it)
            }

            embedStyle.footer?.also {
                footer {
                    name = it
                    iconUrl = embedStyle.footerIconUrl
                }
            }
        }
    }

    private fun computeComponents(interfaceData: InterfaceData): List<LayoutComponent> {
        val components: MutableList<LayoutComponent> = mutableListOf()

        for (i in 0 until MAX_COMPONENTS) {
            interfaceData.buttons
                .filter { it.position.first == i }
                .sortedBy { it.position.second }
                .map { computeButton(it) }
                .onEach {
                    log.info { "Computed button: ${it.id} ${it.label} ${it.emoji}" }
                }
                .takeIf { it.isNotEmpty() }
                ?.take(MAX_BUTTONS_PER_COMPONENT)
                ?.also {
                    components.add(ActionRow.of(it))
                }
        }

        log.info { "creating interface with components: ${components}" }

        return components
    }


    private fun computeButton(button: InterfaceButton): Button {
        return Button.of(
            ButtonStyle.fromKey(button.buttonStyleKey),
            button.id,
            button.name,
            button.emoji?.let { Emoji.fromFormatted(it) }
        ).withDisabled(button.buttonDisabled)
    }

    /**
     * @throws NoSuchElementException
     * @throws PermissionException
     */
    suspend fun updateInterface(guild: Guild, interfaceData: InterfaceData): Message {
        val interfaceMessage = retrieveMessage(guild, interfaceData)
        return interfaceMessage.editMessageEmbeds(computeEmbed(interfaceData))
            .setComponents(formatActionRows(interfaceData.buttons)).await()
    }

    /**
     * @throws NoSuchElementException
     * @throws PermissionException
     */
    suspend fun updateEmbed(guild: Guild, interfaceData: InterfaceData): Message {
        val interfaceMessage = retrieveMessage(guild, interfaceData)
        return interfaceMessage.editMessageEmbeds(computeEmbed(interfaceData)).await()
    }

    /**
     * @throws NoSuchElementException
     * @throws PermissionException
     */
    private suspend fun retrieveMessage(guild: Guild, interfaceData: InterfaceData): Message {
        try {
            val channel = guild.getTextChannelById(interfaceData.channelID) ?: throw NoSuchElementException()
            if (guild.selfMember.hasPermission(channel, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY))
                return channel.retrieveMessageById(interfaceData.messageID).await()
            else
                throw PermissionException("Cannot retrieve interface message, missing permissions in the channel")
        } catch (e: ErrorResponseException) {
            if (e.errorResponse == ErrorResponse.UNKNOWN_MESSAGE)
                throw NoSuchElementException()
            else
                throw PermissionException("Cannot retrieve interface message, missing permissions in the channel")
        }
    }

    fun calculateNewButtonPosition(buttons: MutableList<InterfaceButton>): Pair<Int, Int>? {
        return try {
            val row = buttons.sortedByDescending { it.position.first }.groupBy { it.position.first }
                .filter { it.value.size < MAX_BUTTONS_PER_COMPONENT }.maxOf { it.key }
            val buttonsOfRow = buttons.filter { it.position.first == row }.sortedBy { it.position.second }

            for (i in 0 until MAX_BUTTONS_PER_COMPONENT)
                if (buttonsOfRow.none { it.position.second == i })
                    return Pair(row, i)

            null
        } catch (e: NoSuchElementException) {
            val indexes = listOf(0, 1, 2, 3, 4)
            for (i in 0 until MAX_ACTION_ROWS) {
                val buttonsOfRow = buttons.filter { it.position.first == i }.sortedBy { it.position.second }
                if (buttonsOfRow.size < MAX_ACTION_ROWS)
                    return Pair(i, indexes.first { index -> buttonsOfRow.none { it.position.second == index } })
            }
            null
        }
    }

    fun orderButtons(buttons: MutableList<InterfaceButton>): MutableList<InterfaceButton> {
        for (i in 0 until MAX_ACTION_ROWS * MAX_BUTTONS_PER_COMPONENT)
            buttons.getOrNull(i)?.position = Pair(i / 5, i % 5)

        return buttons
    }

    private fun formatActionRows(buttons: List<InterfaceButton>): MutableList<ActionRow> {
        val formattedActionRows: MutableList<ActionRow> = mutableListOf()

        for (i in 0 until MAX_ACTION_ROWS) {
            val buttonForIActionRow = buttons.filter { it.position.first == i }
            if (buttonForIActionRow.isNotEmpty()) {
                val orderedButtons = buttonForIActionRow.sortedBy { it.position.second }
                formattedActionRows.add(ActionRow.of(orderedButtons.map { formatButton(it) }))
            }
        }

        return formattedActionRows
    }


    fun formatButton(button: InterfaceButton, customId: String? = null) = Button.of(
        ButtonStyle.fromKey(button.buttonStyleKey),
        customId ?: button.id,
        button.name,
        if (button.emoji != null)
            Emoji.fromFormatted(button.emoji!!)
        else null
    ).withDisabled(button.buttonDisabled)
}