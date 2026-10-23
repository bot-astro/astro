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
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.springframework.stereotype.Component
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.models.database.guildSettings.InterfaceData
import space.astro.shared.core.models.database.guildSettings.InterfaceData.InterfaceButton
import java.time.Instant

@Component
class InterfaceManager {
    private val maxActionRows = 5
    private val maxButtonsPerRow = 5

    // TODO: move button ids and emojis to a shared place once interactions are ported to the new bot
    private fun defaultButton(
        id: String,
        emojiName: String,
        emojiId: Long,
        row: Int,
        column: Int,
    ) = InterfaceButton(
        id = id,
        name = null,
        emoji = "<:$emojiName:$emojiId>",
        buttonStyleKey = ButtonStyle.SECONDARY.key,
        buttonDisabled = false,
        position = Pair(row, column),
    )

    private fun defaultButtons() =
        mutableListOf(
            defaultButton("button/vc/lock", "lock", 1029831342575325225, 0, 0),
            defaultButton("button/vc/unlock", "unlock", 1029831345884647554, 0, 1),
            defaultButton("button/vc/hide", "hide", 1029831340595626056, 0, 2),
            defaultButton("button/vc/unhide", "unhide", 1029831344278220833, 0, 3),
            defaultButton("button/vc/limit", "limit", 1029823585801478154, 1, 0),
            defaultButton("button/vc/invite", "invite", 1029823584287338496, 1, 1),
            defaultButton("button/vc/ban", "ban", 1029823553077510327, 1, 2),
            defaultButton("button/vc/permit", "permit", 1029823602071191563, 1, 3),
            defaultButton("button/vc/name", "name", 1029823591329579039, 2, 0),
            defaultButton("button/vc/bitrate", "bitrate", 1029823554574884997, 2, 1),
            defaultButton("button/vc/region", "region", 1029823605346930809, 2, 2),
            defaultButton("button/vc/template", "template", 1038794746249875567, 2, 3),
            defaultButton("button/vc/chat", "chat", 1029823561063464960, 3, 0),
            defaultButton("button/vc/waiting-room", "waiting", 1146421350819450920, 3, 1),
            defaultButton("button/vc/claim", "owner", 1030227494630076448, 3, 2),
            defaultButton("button/vc/transfer", "transfer_owner", 1030227495892557826, 3, 3),
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

    private fun computeMessage(interfaceData: InterfaceData): MessageCreateData =
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
