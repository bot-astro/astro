package space.astro.bot.interactions.handlers.modal.impl.vc

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import space.astro.bot.components.managers.vc.VCNameManager
import space.astro.bot.core.exceptions.VcOperationException
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.modal.AbstractModal
import space.astro.bot.interactions.handlers.modal.Modal
import space.astro.bot.interactions.handlers.modal.ModalRunnable
import space.astro.bot.models.discord.vc.VCOperationCTX

@Modal(
    id = InteractionIds.Modal.VC_NAME,
    action = InteractionAction.VC_NAME
)
class NameModal(
    private val vcNameManager: VCNameManager
) : AbstractModal() {

    companion object {
        const val NAME_TEXT_INPUT_ID = "name"
    }

    @ModalRunnable
    suspend fun run(
        event: ModalInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        val name = event.getValue(NAME_TEXT_INPUT_ID)?.asString

        if (name == null) {
            ctx.replyHandler.replyEmbed(Embeds.error("Please provide a valid name"))
            return
        }

        try {
            vcNameManager.performVCRename(ctx.vcOperationCTX, name)

            ctx.vcOperationCTX.queueUpdatedManagers()

            ctx.replyHandler.replyEmbed(
                Embeds.default(
                    "${Emojis.name.formatted} Name of your VC set to `$name`!"
                )
            )
        } catch (e: VcOperationException) {
            ctx.replyHandler.replyEmbed(
                Embeds.error(
                when (e.reason) {
                    VcOperationException.Reason.CANNOT_USE_BADWORDS -> "Badwords have been detected in the name you provided and this server doesn't allow them!"
                    VcOperationException.Reason.RENAME_IS_RATE_LIMITED -> "The voice channel has already been renamed twice in the last 10 minutes." +
                            "\nSince the Discord update that took place the 29th of May 2020, bots are allowed to change channels names a maximum of two times every 10 minutes." +
                            "\nFor that reason Astro will not be able to always update the channel name as it should." +
                            "\n\nHere is the full announcement made by @Mason, a member of the Discord Staff:" +
                            "\n> Hi everyone--wanted to make official note of the rate limit change on channel updates that was deployed yesterday. While most of you won't notice anything at all, there are known and unknown use cases that this effects.\n" +
                            "\n" +
                            "> **The new rate limit for channel NAME AND TOPIC updates is 2 updates per 10 minutes, per channel.** (Sorry that was unclear at first)\n" +
                            "\n" +
                            "> This is reflected in the normal rate limits headers so no library changes should need to happen.\n" +
                            "\n" +
                            "> For a bit of context, frequent channel updates have significant impact on our infrastructure and performance, especially in the case of large guilds and mobile clients, hence the limit changes.\n" +
                            "\n" +
                            "> I apologize that this went out without a prior announcement. While in general we do not announce rate limit changes as they are dynamically handled by libraries, this change was significant enough to warrant one. If crucial functionality has broken for your implementation because of these changes, please feel free to reach out to me directly (@Mason in the Discord Developers server) so that I can understand!" +
                            "\n\nYou can find this announcement by joining the [Discord Developers Server](https://discord.gg/discord-developers) and navigating to [this link](https://discord.com/channels/613425648685547541/697138785317814292/715995470048264233)."
                    else -> throw e
                }
            ))
        }
    }
}