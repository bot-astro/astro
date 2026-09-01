package space.astro.shared.core.models.database.userSettings

data class UserSettingsData(
    var interfaceReplies: Boolean = true,
    var interfaceRepliesDeleteAfter: Long? = null
)
