package space.astro.bot.components

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.container.ContainerChildComponent
import net.dv8tion.jda.api.components.mediagallery.MediaGallery
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
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
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder
import net.dv8tion.jda.api.utils.messages.MessageEditData
import org.springframework.stereotype.Component
import space.astro.bot.ui.Containers
import space.astro.bot.ui.Emojis
import space.astro.bot.ui.Links
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.lang.L
import space.astro.shared.core.models.database.guildSettings.InterfaceData
import space.astro.shared.core.models.database.guildSettings.InterfaceData.InterfaceButton
import java.time.Instant
import java.util.Locale

@Component
class InterfaceManager {
    private val maxActionRows = 5
    private val maxButtonsPerRow = 5

    private fun defaultButton(
        locale: Locale,
        id: String,
        emoji: CustomEmoji,
        row: Int,
        column: Int,
    ) = InterfaceButton(
        id = id,
        name = L.t(locale, "interface.button.${id.substringAfterLast('/')}").padCenter(),
        emoji = emoji.asMention,
        buttonStyleKey = ButtonStyle.SECONDARY.key,
        buttonDisabled = false,
        position = Pair(row, column),
    )

    // buttons are as wide as their label, the invisible padding makes them all look the same size
    private fun String.padCenter(width: Int = 9): String {
        val pad = (width - length).coerceAtLeast(0)
        return "\u2800".repeat(pad / 2) + this + "\u2800".repeat(pad - pad / 2)
    }

    private fun defaultButtons(locale: Locale) =
        mutableListOf(
            defaultButton(locale, "button/vc/lock", Emojis.lock, 0, 0),
            defaultButton(locale, "button/vc/unlock", Emojis.unlock, 0, 1),
            defaultButton(locale, "button/vc/hide", Emojis.hide, 0, 2),
            defaultButton(locale, "button/vc/unhide", Emojis.unhide, 0, 3),
            defaultButton(locale, "button/vc/limit", Emojis.limit, 1, 0),
            defaultButton(locale, "button/vc/invite", Emojis.invite, 1, 1),
            defaultButton(locale, "button/vc/ban", Emojis.ban, 1, 2),
            defaultButton(locale, "button/vc/permit", Emojis.permit, 1, 3),
            defaultButton(locale, "button/vc/name", Emojis.name, 2, 0),
            defaultButton(locale, "button/vc/bitrate", Emojis.bitrate, 2, 1),
            defaultButton(locale, "button/vc/region", Emojis.region, 2, 2),
            defaultButton(locale, "button/vc/template", Emojis.template, 2, 3),
            defaultButton(locale, "button/vc/chat", Emojis.chat, 3, 0),
            defaultButton(locale, "button/vc/waiting-room", Emojis.waitingRoom, 3, 1),
            defaultButton(locale, "button/vc/claim", Emojis.owner, 3, 2),
            defaultButton(locale, "button/vc/transfer", Emojis.transferOwner, 3, 3),
        )

    private fun defaultEmbedStyle(
        locale: Locale,
        showInvite: Boolean,
    ) = InterfaceData.EmbedStyle(
        url = Links.WEBSITE,
        title = null,
        description = L.t(locale, "interface.description"),
        timestamp = null,
        color = Containers.purple.rgb,
        thumbnail = null,
        image = null,
        authorName = L.t(locale, "interface.author"),
        authorUrl = if (showInvite) Links.invite("interface") else Links.WEBSITE,
        authorIconUrl = Links.LOGO,
        footer = L.t(locale, "interface.footer") + if (showInvite) " • " + L.t(locale, "interface.invite") else "",
        footerIconUrl = null,
    )

    suspend fun createInterface(
        guild: Guild,
        channelId: String,
        locale: Locale,
        showInvite: Boolean,
    ): InterfaceData {
        val channel =
            guild.getTextChannelById(channelId)
                ?: throw ANotFoundException("Text channel with id $channelId not found")

        return sendInterface(channel, locale, showInvite)
    }

    private suspend fun sendInterface(
        channel: TextChannel,
        locale: Locale,
        showInvite: Boolean,
    ): InterfaceData {
        val interfaceData =
            InterfaceData(
                channelID = channel.id,
                // replaced with the real message id once the message is sent
                messageID = "0",
                buttons = defaultButtons(locale),
                embedStyle = defaultEmbedStyle(locale, showInvite),
                componentsV2 = true,
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
            .editMessage(computeEdit(interfaceData))
            .await()
    }

    suspend fun moveInterface(
        guild: Guild,
        interfaceData: InterfaceData,
        channelId: String,
        locale: Locale,
    ): InterfaceData {
        val channel =
            guild.getTextChannelById(channelId)
                ?: throw ANotFoundException("Text channel with id $channelId not found")

        val movedInterface =
            interfaceData.copy(
                channelID = channel.id,
                // replaced with the real message id once the message is sent
                messageID = "0",
            )

        val message = channel.sendMessage(computeMessage(movedInterface)).await()
        movedInterface.messageID = message.id

        try {
            deleteInterface(guild, interfaceData.channelID, interfaceData.messageID, locale)
        } catch (_: Exception) {
            // the old message is not worth failing the move, the new one is already sent
        }

        return movedInterface
    }

    suspend fun deleteInterface(
        guild: Guild,
        channelId: String,
        messageId: String,
        locale: Locale,
    ) {
        val channel =
            guild.getTextChannelById(channelId)
                ?: return

        try {
            channel.deleteMessageById(messageId).reason(L.t(locale, "reason.dashboard.interface_delete")).await()
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
        if (interfaceData.componentsV2) {
            MessageCreateBuilder()
                .useComponentsV2()
                .setComponents(computeContainer(interfaceData))
                .build()
        } else {
            MessageCreateBuilder()
                .useComponentsV2(false)
                .setEmbeds(computeEmbed(interfaceData))
                .setComponents(computeComponents(interfaceData))
                .build()
        }

    private fun computeEdit(interfaceData: InterfaceData): MessageEditData =
        if (interfaceData.componentsV2) {
            MessageEditBuilder()
                .useComponentsV2()
                .setComponents(computeContainer(interfaceData))
                .build()
        } else {
            MessageEditBuilder()
                .useComponentsV2(false)
                .setEmbeds(computeEmbed(interfaceData))
                .setComponents(computeComponents(interfaceData))
                .build()
        }

    private fun computeContainer(interfaceData: InterfaceData): Container {
        val style = interfaceData.embedStyle
        val children = mutableListOf<ContainerChildComponent>()

        style.authorName?.let { name ->
            children += TextDisplay.of("**" + (style.authorUrl?.let { "[$name]($it)" } ?: name) + "**")
        }
        style.title?.let { children += TextDisplay.of("### $it") }
        style.description?.takeIf { it.isNotBlank() }?.let { children += TextDisplay.of(it) }
        style.image?.let { children += MediaGallery.of(MediaGalleryItem.fromUrl(it)) }
        children += computeComponents(interfaceData)
        style.footer?.let { children += TextDisplay.of("-# $it") }

        return Containers.container(style.color, *children.toTypedArray())
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
