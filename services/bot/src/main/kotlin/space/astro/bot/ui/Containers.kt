package space.astro.bot.ui

import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.container.ContainerChildComponent
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.section.SectionAccessoryComponent
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import java.awt.Color

/**
 * Components v2 building blocks, every message of the bot (except the voice channel interfaces, which keep
 * using embeds) should be built from these so the look can be changed from a single place
 */
object Containers {
    val purple: Color = Color.decode("#B125EA")
    private val red = Color.decode("#F04747")

    /**
     * Container with a purple accent, [description] is rendered as markdown text followed by [children]
     */
    fun default(
        description: String,
        vararg children: ContainerChildComponent,
    ): Container = container(purple, TextDisplay.of(description), *children)

    fun error(
        description: String,
        vararg children: ContainerChildComponent,
    ): Container = container(red, TextDisplay.of(description), *children)

    fun container(
        color: Color?,
        vararg children: ContainerChildComponent,
    ): Container = Container.of(children.toList()).withAccentColor(color)

    /**
     * Same as [container] but takes a raw color as returned by JDA, the default role color (no color) falls back to [purple]
     */
    fun container(
        rawColor: Int,
        vararg children: ContainerChildComponent,
    ): Container = container(if (rawColor == Role.DEFAULT_COLOR_RAW) purple else Color(rawColor), *children)

    fun text(content: String): TextDisplay = TextDisplay.of(content)

    /**
     * Big heading, to be used as the first element of a container
     */
    fun title(content: String): TextDisplay = TextDisplay.of("## $content")

    /**
     * Titled block of text, replaces the embed fields
     */
    fun field(
        name: String,
        value: String,
    ): TextDisplay = TextDisplay.of("### $name\n$value")

    /**
     * Small grayed out text, replaces the embed footers
     */
    fun footer(content: String): TextDisplay = TextDisplay.of("-# $content")

    fun divider(spacing: Separator.Spacing = Separator.Spacing.SMALL): Separator = Separator.createDivider(spacing)

    /**
     * Text with an accessory (thumbnail or button) displayed on its side
     */
    fun section(
        accessory: SectionAccessoryComponent,
        vararg content: String,
    ): Section = Section.of(accessory, content.map { TextDisplay.of(it) })

    /**
     * Message made of [components], to be used when sending outside of an interaction response
     */
    fun message(vararg components: MessageTopLevelComponent): MessageCreateData =
        MessageCreateBuilder()
            .setComponents(components.toList())
            .build()
}
