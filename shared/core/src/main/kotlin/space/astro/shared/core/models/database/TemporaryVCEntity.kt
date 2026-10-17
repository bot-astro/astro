package space.astro.shared.core.models.database


data class TemporaryVCEntity(
    val id: String,
    var ownerID: String,
    val generatorID: String,

    var state: TemporaryVCState = TemporaryVCState.UNLOCKED,

    var nameChanges: Int = 0,
    var lastNameChange: Long? = null,
    var renamed: Boolean = false,

    var incrementalPosition: Int? = null,

    var chatID: String?,
    var chatLogs: Boolean = false,
    var chatNameChanges: Int = 0,
    var lastChatNameChange: Long? = null,

    var waitingID: String?,
    var waitingNameChanges: Int = 0,
    var lastWaitingNameChange: Long? = null,
) {
    enum class TemporaryVCState {
        UNLOCKED,
        LOCKED,
        HIDDEN,
        UNHIDDEN;
    }
}