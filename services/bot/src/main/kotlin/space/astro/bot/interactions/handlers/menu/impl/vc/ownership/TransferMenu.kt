package space.astro.bot.interactions.handlers.menu.impl.vc.ownership

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import space.astro.bot.components.managers.vc.VCOwnershipManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.menu.AbstractMenu
import space.astro.bot.interactions.handlers.menu.Menu
import space.astro.bot.interactions.handlers.menu.MenuRunnable
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.TemporaryVCDao

@Menu(
    id = InteractionIds.Menu.VC_TRANSFER,
    action = InteractionAction.VC_TRANSFER
)
class TransferMenu(
    private val vcOwnershipManager: VCOwnershipManager,
    private val temporaryVCDao: TemporaryVCDao
) : AbstractMenu() {
    @MenuRunnable
    suspend fun run(
        event: StringSelectInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        val member = event.values.firstOrNull()?.let { ctx.guild.getMemberById(it) }

        if (member == null) {
            event.hook.editOriginalEmbeds(Embeds.error("The user you provided is not in this server!"))
                .queue()

            return
        }

        if (member.user.isBot) {
            event.hook.editOriginalEmbeds(Embeds.error("Cannot transfer the ownership to a bot"))
                .queue()

            return
        }

        if (member.voiceState!!.channel?.id != ctx.vcOperationCTX.temporaryVC.id) {
            event.hook.editOriginalEmbeds(Embeds.error("The user you provided is not in your voice channel!"))
                .queue()

            return
        }

        event.deferReply(true).await()

        vcOwnershipManager.changeOwner(ctx.vcOperationCTX, member)
        ctx.vcOperationCTX.queueUpdatedManagers()
        temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)
        vcOwnershipManager.handleOwnerRoleMigration(ctx.vcOperationCTX, ctx.member, member)

        event.hook.editOriginalEmbeds(Embeds.default("Ownership transferred to ${member.asMention}"))
            .queue()
    }
}