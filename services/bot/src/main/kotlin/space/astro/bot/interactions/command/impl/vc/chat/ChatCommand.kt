package space.astro.bot.interactions.command.impl.vc.chat

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX

@Command(
    name = "chat",
    description = "Manage the private text chat of your VC",
    category = CommandCategory.VC,
    premium = true
)
class ChatCommand : AbstractCommand() {
    @SubCommand(
        name = "create",
        description = "Create a private text chat for your VC"
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
        description = "Delete the private text chat of your VC"
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

    @SubCommand(
        name = "logs",
        description = "Toggle logs for your VC text chat"
    )
    suspend fun logs(
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