package space.astro.bot.interactions.command.impl.vc.settings

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionComponentBuilder
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX

@Command(
    name = "region",
    description = "Set the region for your VC",
    category = CommandCategory.VC
)
class RegionCommand(
    val interactionComponentBuilder: InteractionComponentBuilder
) : AbstractCommand() {
    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        event.deferReply(true).await()

        val regions = ctx.guild.retrieveRegions(false).await()

        val regionSelectMenu = interactionComponentBuilder.selectMenu(
            id = InteractionIds.Menu.VC_REGION,
            placeholder = "Set the region for your VC",
            options = regions.map { region ->
                SelectOption.of(region.getName(), region.key)
                    .withEmoji(region.emoji?.let { Emoji.fromUnicode(it) })
            },
        )

        event.hook.editOriginalEmbeds(Embeds.default("Set the region of your VC with the menu below"))
            .setActionRow(regionSelectMenu)
            .queue()
    }
}