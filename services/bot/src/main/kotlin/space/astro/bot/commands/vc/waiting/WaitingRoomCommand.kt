package space.astro.bot.commands.vc.waiting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VCWaitingRoomManager
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.CommandLocalization
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Containers
import space.astro.shared.core.repositories.redis.TemporaryVCRepository

@Component
class WaitingRoomCommand(
    contextResolverFactory: VcContextResolverFactory,
    private val temporaryVCRepository: TemporaryVCRepository,
    private val vcWaitingRoomManager: VCWaitingRoomManager,
) : SlashHandler<VcInteractionContext> {
    companion object {
        private const val CREATE = "create"
        private const val DELETE = "delete"
    }

    override val data =
        Commands
            .slash("waiting-room", CommandLocalization.description("waiting-room"))
            .setContexts(InteractionContextType.GUILD)
            .addSubcommands(
                SubcommandData(CREATE, CommandLocalization.description("waiting-room.create")),
                SubcommandData(DELETE, CommandLocalization.description("waiting-room.delete")),
            )
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY_EPHEMERAL)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE,
        )

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        when (event.subcommandName) {
            CREATE -> create(ctx)
            DELETE -> delete(ctx)
        }
    }

    private suspend fun create(ctx: VcInteractionContext) {
        val vcCtx = ctx.vcOperationCTX
        val existingWaitingRoom = vcCtx.waitingRoom

        if (existingWaitingRoom != null) {
            ctx.responder.error("vc.waiting_room.already_exists", "room" to existingWaitingRoom.asMention)
            return
        }

        val entity = vcCtx.temporaryVCEntity
        val waitingRoom =
            vcWaitingRoomManager.create(ctx.member, vcCtx.generatorSettings, vcCtx.temporaryVC, entity.incrementalPosition, ctx.locale)
        entity.waitingID = waitingRoom.id
        entity.waitingNameChanges = 0
        entity.lastWaitingNameChange = null
        withContext(Dispatchers.IO) { temporaryVCRepository.save(ctx.guildId, entity) }

        ctx.responder.reply(Containers.default(ctx.t("vc.waiting_room.created", "room" to waitingRoom.asMention)))
    }

    private suspend fun delete(ctx: VcInteractionContext) {
        val vcCtx = ctx.vcOperationCTX
        val entity = vcCtx.temporaryVCEntity

        vcCtx.waitingRoom
            ?.delete()
            ?.reason("user requested waiting room deletion")
            ?.queue()
        entity.waitingID = null
        withContext(Dispatchers.IO) { temporaryVCRepository.save(ctx.guildId, entity) }

        ctx.responder.reply(Containers.default(ctx.t("vc.waiting_room.deleted")))
    }
}
