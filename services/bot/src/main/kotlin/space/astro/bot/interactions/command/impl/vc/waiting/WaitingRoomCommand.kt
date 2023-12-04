package space.astro.bot.interactions.command.impl.vc.waiting

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX

@Command(
    name = "waiting-room",
    description = "Manage the waiting room of your VC",
    category = CommandCategory.VC,
    premium = true
)
class WaitingRoomCommand : AbstractCommand() {
    @SubCommand(
        name = "create",
        description = "Create a waiting room for your VC"
    )
    suspend fun create(
        event: SlashCommandInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        TODO()
    }

    @SubCommand(
        name = "delete",
        description = "Delete the waiting room of your VC"
    )
    suspend fun delete(
        event: SlashCommandInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        TODO()
    }
}