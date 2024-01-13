package space.astro.bot.interactions

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu.SelectTarget
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.modals.Modal
import org.springframework.stereotype.Component

@Component
class InteractionComponentBuilder {
    
    ///////////////
    /// BUTTONS ///
    ///////////////
    
    fun buttonWithLabel(
        id: String,
        buttonStyle: ButtonStyle,
        label: String
    ) = Button.of(
        buttonStyle,
        id,
        label
    )

    fun buttonWithEmoji(
        id: String,
        buttonStyle: ButtonStyle,
        emoji: Emoji
    ) = Button.of(
        buttonStyle,
        id,
        emoji
    )

    fun buttonWithLabelAndEmoji(
        id: String,
        buttonStyle: ButtonStyle,
        label: String,
        emoji: Emoji
    ) = Button.of(
        buttonStyle,
        id,
        label,
        emoji
    )
    
    
    /////////////
    /// MENUS ///
    /////////////
    
    fun selectMenu(
        id: String,
        options: List<SelectOption>,
        placeholder: String? = null,
        rangeMin: Int = 1,
        rangeMax: Int = 1,
    ) = StringSelectMenu.create(id)
        .setPlaceholder(placeholder)
        .setRequiredRange(rangeMin, rangeMax)
        .addOptions(options)
        .build()

    fun entitySelectMenu(
        id: String,
        entityTypes: List<SelectTarget>,
        placeholder: String? = null,
        rangeMin: Int = 1,
        rangeMax: Int = 1
    ) = EntitySelectMenu.create(id, entityTypes)
        .setPlaceholder(placeholder)
        .setRequiredRange(rangeMin, rangeMax)
        .build()


    //////////////
    /// MODALS ///
    //////////////
    fun modalWithTextInput(
        id: String,
        title: String,
        textInput: TextInput
    ) = Modal.create(id, title)
        .addActionRow(textInput)
        .build()
}