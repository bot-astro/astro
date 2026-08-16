package space.astro.shared.core.util.exceptions

class BotApiPermissionException(
    override val message: String?
) : Exception(message)