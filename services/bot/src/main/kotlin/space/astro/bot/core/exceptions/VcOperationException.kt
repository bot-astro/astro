package space.astro.bot.core.exceptions

class VcOperationException(val reason: Reason): Exception() {
    enum class Reason {
        CANNOT_USE_BADWORDS,
        RENAME_IS_RATE_LIMITED,

        CANNOT_BAN_IMMUNE_ROLE
    }
}