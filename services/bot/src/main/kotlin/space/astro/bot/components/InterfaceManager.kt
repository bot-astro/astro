package space.astro.bot.components

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.springframework.stereotype.Component
import space.astro.bot.ui.Emojis
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.models.database.guildSettings.InterfaceData
import space.astro.shared.core.models.database.guildSettings.InterfaceData.InterfaceButton
import java.time.Instant

@Component
class InterfaceManager {
    private val maxActionRows = 5
    private val maxButtonsPerRow = 5

    private fun defaultButton(
        id: String,
        emoji: CustomEmoji,
        row: Int,
        column: Int,
    ) = InterfaceButton(
        id = id,
        name = null,
        emoji = emoji.asMention,
        buttonStyleKey = ButtonStyle.SECONDARY.key,
        buttonDisabled = false,
        position = Pair(row, column),
    )

    private fun defaultButtons() =
        mutableListOf(
            defaultButton("button/vc/lock", Emojis.lock, 0, 0),
            defaultButton("button/vc/unlock", Emojis.unlock, 0, 1),
            defaultButton("button/vc/hide", Emojis.hide, 0, 2),
            defaultButton("button/vc/unhide", Emojis.unhide, 0, 3),
            defaultButton("button/vc/limit", Emojis.limit, 1, 0),
            defaultButton("button/vc/invite", Emojis.invite, 1, 1),
            defaultButton("button/vc/ban", Emojis.ban, 1, 2),
            defaultButton("button/vc/permit", Emojis.permit, 1, 3),
            defaultButton("button/vc/name", Emojis.name, 2, 0),
            defaultButton("button/vc/bitrate", Emojis.bitrate, 2, 1),
            defaultButton("button/vc/region", Emojis.region, 2, 2),
            defaultButton("button/vc/template", Emojis.template, 2, 3),
            defaultButton("button/vc/chat", Emojis.chat, 3, 0),
            defaultButton("button/vc/waiting-room", Emojis.waitingRoom, 3, 1),
            defaultButton("button/vc/claim", Emojis.owner, 3, 2),
            defaultButton("button/vc/transfer", Emojis.transferOwner, 3, 3),
        )

    private fun defaultEmbedStyle() =
        InterfaceData.EmbedStyle(
            url = null,
            title = "Voice channel interface",
            description = "Use the buttons below to manage your temporary voice channel",
            timestamp = null,
            color = 0x2B2D31,
            thumbnail = null,
            image = null,
            authorName = null,
            authorUrl = null,
            authorIconUrl = null,
            footer = null,
            footerIconUrl = null,
        )

    suspend fun createInterface(
        guild: Guild,
        channelId: String,
    ): InterfaceData {
        val channel =
            guild.getTextChannelById(channelId)
                ?: throw ANotFoundException("Text channel with id $channelId not found")

        return sendInterface(channel)
    }

    private suspend fun sendInterface(channel: TextChannel): InterfaceData {
        val interfaceData =
            InterfaceData(
                channelID = channel.id,
                // replaced with the real message id once the message is sent
                messageID = "0",
                buttons = defaultButtons(),
                embedStyle = defaultEmbedStyle(),
            )

        val message = channel.sendMessage(computeMessage(interfaceData)).await()
        interfaceData.messageID = message.id

        return interfaceData
    }

    suspend fun updateInterface(
        guild: Guild,
        interfaceData: InterfaceData,
    ) {
        val message = retrieveMessage(guild, interfaceData)

        message
            .editMessageEmbeds(computeEmbed(interfaceData))
            .setComponents(computeComponents(interfaceData))
            .await()
    }

    suspend fun deleteInterface(
        guild: Guild,
        channelId: String,
        messageId: String,
    ) {
        val channel =
            guild.getTextChannelById(channelId)
                ?: return

        try {
            channel.deleteMessageById(messageId).await()
        } catch (e: ErrorResponseException) {
            if (e.errorResponse == ErrorResponse.UNKNOWN_MESSAGE) {
                return
            }

            throw ABadRequestException(e.message ?: "Error from Discord API", e)
        }
    }

    private suspend fun retrieveMessage(
        guild: Guild,
        interfaceData: InterfaceData,
    ): Message {
        val channel =
            guild.getTextChannelById(interfaceData.channelID)
                ?: throw ANotFoundException("Text channel with id ${interfaceData.channelID} not found")

        if (!guild.selfMember.hasPermission(channel, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY)) {
            throw ABadRequestException("Cannot retrieve the interface message, missing permissions in the channel")
        }

        try {
            return channel.retrieveMessageById(interfaceData.messageID).await()
        } catch (e: ErrorResponseException) {
            if (e.errorResponse == ErrorResponse.UNKNOWN_MESSAGE) {
                throw ANotFoundException("Interface message with id ${interfaceData.messageID} not found", e)
            }

            throw ABadRequestException(e.message ?: "Error from Discord API", e)
        }
    }

    fun computeMessage(interfaceData: InterfaceData): MessageCreateData =
        MessageCreateBuilder()
            .setEmbeds(computeEmbed(interfaceData))
            .setComponents(computeComponents(interfaceData))
            .build()

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

    private fun computeComponents(interfaceData: InterfaceData): List<ActionRow> =
        (0 until maxActionRows).mapNotNull { row ->
            interfaceData.buttons
                .filter { it.position.first == row }
                .sortedBy { it.position.second }
                .take(maxButtonsPerRow)
                .map { computeButton(it) }
                .takeIf { it.isNotEmpty() }
                ?.let { ActionRow.of(it) }
        }

    private fun computeButton(button: InterfaceButton): Button =
        Button
            .of(
                ButtonStyle.fromKey(button.buttonStyleKey),
                button.id,
                button.name,
                button.emoji?.let { Emoji.fromFormatted(it) },
            ).withDisabled(button.buttonDisabled)
}
