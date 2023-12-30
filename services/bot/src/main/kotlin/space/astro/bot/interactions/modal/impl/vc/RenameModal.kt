package space.astro.bot.interactions.button.impl.vc

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.modal.AbstractModal
import space.astro.bot.interactions.button.Button
import space.astro.bot.interactions.button.ButtonRunnable
import space.astro.bot.interactions.command.VcInteractionContextInfo
import space.astro.bot.models.discord.vc.VCOperationCTX

@Button(
    id = InteractionIds.Button.VC_LOCK
)
class RenameModal : AbstractModal() {

    @ButtonRunnable
    fun run(
        event: ButtonInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {

    }
}