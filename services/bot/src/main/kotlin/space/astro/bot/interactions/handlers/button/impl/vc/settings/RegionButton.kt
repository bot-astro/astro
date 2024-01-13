package space.astro.bot.interactions.handlers.button.impl.vc.settings

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionComponentBuilder
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.button.Button
import space.astro.bot.interactions.handlers.button.ButtonRunnable
import space.astro.bot.models.discord.vc.VCOperationCTX

@Button(
    id = InteractionIds.Button.VC_REGION,
    action = InteractionAction.VC_REGION
)
class RegionButton(
    private val interactionComponentBuilder: InteractionComponentBuilder
) : space.astro.bot.interactions.handlers.button.AbstractButton() {
    @ButtonRunnable
    suspend fun run(
        event: ButtonInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        ctx.replyHandler.deferReply()

        val regions = ctx.guild.retrieveRegions(false).await()

        val regionSelectMenu = interactionComponentBuilder.selectMenu(
            id = InteractionIds.Menu.VC_REGION,
            placeholder = "Set the region for your VC",
            options = regions.map { region ->
                SelectOption.of(region.getName(), region.key)
                    .withEmoji(region.emoji?.let { Emoji.fromUnicode(it) })
            },
        )

        ctx.replyHandler.replyEmbedAndComponent(
            embed = Embeds.default("Set the region of your VC with the menu below"),
            component = regionSelectMenu
        )
    }
}