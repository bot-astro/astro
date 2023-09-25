package space.astro.bot.managers.interfaces

import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Component
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import space.astro.shared.core.models.database.InterfaceButton
import space.astro.shared.core.models.database.InterfaceData
import java.time.Instant

object InterfaceManager {
    private val MAX_COMPONENTS = Message.MAX_COMPONENT_COUNT
    private val MAX_BUTTONS_PER_COMPONENT = Component.Type.BUTTON.maxPerRow

    fun computeMessage(interfaceData: InterfaceData): MessageCreateData {
        return MessageCreateBuilder()
            .setEmbeds(computeEmbed(interfaceData))
            .setComponents(computeComponents(interfaceData))
            .build()
    }

    private fun computeEmbed(interfaceData: InterfaceData): MessageEmbed {
        val embedStyle = interfaceData.embedStyle

        return Embed(
            // TODO: fields = formatEmbedFieldsFromButtons(interfaceDto)
        ) {
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
                .takeIf { it.isNotEmpty() }
                ?.take(MAX_BUTTONS_PER_COMPONENT)
                ?.also {
                    components.add(ActionRow.of(it))
                }
        }

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
}