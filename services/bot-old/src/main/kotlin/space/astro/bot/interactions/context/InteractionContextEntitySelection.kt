package space.astro.bot.interactions.context

import net.dv8tion.jda.api.entities.emoji.Emoji
import space.astro.bot.core.ui.Emojis

enum class InteractionContextEntitySelection(
    val entityName: String,
    val emoji: Emoji,
    val slashCommandPath: String,
    val buttonId: String? = null
) {
    TEMPLATE("Template", Emojis.template, "template create", null),
    GENERATOR("Generator", Emojis.generator, "generator create", null),
    CONNECTION("Connection", Emojis.voiceRole, "connection create", null),
    INTERFACE("Interface", Emojis.vcInterface, "interface create", null);
}