package space.astro.bot.components.managers.vc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import space.astro.bot.exceptions.ConfigurationException
import space.astro.bot.interactions.InteractionException
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.models.database.ConfigurationErrorEntity
import space.astro.shared.core.models.database.TemporaryVCEntity
import space.astro.shared.core.repositories.redis.TemporaryVCRepository

@Component
class VCNameManager(
    private val temporaryVCRepository: TemporaryVCRepository,
) {
    /**
     * Renames a temporary vc, the change is applied once [VCOperationCTX.queueUpdatedManagers] is called
     *
     * TODO: refresh on state / owner / activity change with the rename conditions, incremental position updates (needs the position manager)
     * TODO: badwords validation, once the generator settings have a flag for it
     *
     * @throws InteractionException if the vc cannot be renamed right now
     * @throws ConfigurationException if the resulting name is not valid
     */
    suspend fun performVCRename(
        ctx: VCOperationCTX,
        newNameTemplate: String,
    ) {
        val entity = ctx.temporaryVCEntity

        if (!entity.canBeRenamed()) {
            throw InteractionException(
                "The voice channel has already been renamed twice in the last 10 minutes." +
                    "\nDiscord allows bots to change the name of a channel a maximum of two times every 10 minutes, " +
                    "so Astro cannot rename it again yet.",
            )
        }

        val newName =
            VariablesManager.computeVcNameForExisting(
                template = newNameTemplate,
                owner = ctx.temporaryVCOwner,
                temporaryVC = ctx.temporaryVC,
                incrementalPosition = entity.incrementalPosition,
            )

        if (ctx.temporaryVC.name == newName) {
            return
        }

        try {
            ctx.temporaryVCManager.setName(newName)
        } catch (e: IllegalArgumentException) {
            throw ConfigurationException(
                ConfigurationErrorEntity(
                    guildId = ctx.guild.id,
                    description = "Invalid temporary vc name: $newName",
                ),
            )
        }

        ctx.markTemporaryVCManagerAsUpdated()

        entity.registerUserRename()
        withContext(Dispatchers.IO) { temporaryVCRepository.save(ctx.guild.id, entity) }
    }

    private fun TemporaryVCEntity.canBeRenamed(): Boolean {
        val lastChange = lastNameChange ?: return true

        return System.currentTimeMillis() - lastChange > RATE_LIMIT_WINDOW_MS || nameChanges < MAX_NAME_CHANGES
    }

    private fun TemporaryVCEntity.registerUserRename() {
        val now = System.currentTimeMillis()
        val lastChange = lastNameChange

        nameChanges = if (lastChange == null || now - lastChange > RATE_LIMIT_WINDOW_MS) 1 else nameChanges + 1
        lastNameChange = now
        renamed = true
    }

    private companion object {
        const val RATE_LIMIT_WINDOW_MS = 600_000L
        const val MAX_NAME_CHANGES = 2
    }
}
