package space.astro.bot.interactions

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import org.springframework.stereotype.Component

@Component
class InteractionComponentBuilder {
    
    ///////////////
    /// BUTTONS ///
    ///////////////
    
    fun buttonWithLabel(
        action: InteractionAction,
        buttonStyle: ButtonStyle,
        label: String
    ) = Button.of(
        buttonStyle,
        action.id,
        label
    )

    fun buttonWithEmoji(
        action: InteractionAction,
        buttonStyle: ButtonStyle,
        emoji: Emoji
    ) = Button.of(
        buttonStyle,
        action.id,
        emoji
    )

    fun buttonWithLabelAndEmoji(
        action: InteractionAction,
        buttonStyle: ButtonStyle,
        label: String,
        emoji: Emoji
    ) = Button.of(
        buttonStyle,
        action.id,
        label,
        emoji
    )
    
    
    /////////////
    /// MENUS ///
    /////////////
    
    fun selectMenu(
        action: InteractionAction,
        options: List<SelectOption>,
        placeholder: String? = null,
        rangeMin: Int = 1,
        rangeMax: Int = 1,
    ) = StringSelectMenu.create(action.id)
        .setPlaceholder(placeholder)
        .setRequiredRange(rangeMin, rangeMax)
        .addOptions(options)
        .build()
}