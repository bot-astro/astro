package space.astro.bot.command.interactions

import java.util.UUID

object MessageComponentUtils {
    fun generateId() = UUID.randomUUID().toString()
}