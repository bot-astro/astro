package space.astro.bot.exceptions

import space.astro.shared.core.models.database.ConfigurationErrorCode
import space.astro.shared.core.models.database.ConfigurationErrorEntity

/**
 * Configuration errors reported when a guild uses something that requires premium
 */
object PremiumConfigurationErrors {
    private fun premiumRequired(
        guildId: String,
        code: ConfigurationErrorCode,
        args: Map<String, String> = emptyMap(),
    ) = ConfigurationException(ConfigurationErrorEntity(guildId = guildId, code = code, args = args))

    fun premiumVariables(
        guildId: String,
        encounteredIn: String,
    ) = premiumRequired(guildId, ConfigurationErrorCode.PREMIUM_VARIABLES, mapOf("encounteredIn" to encounteredIn))

    fun maximumAmountOfConnections(guildId: String) = premiumRequired(guildId, ConfigurationErrorCode.PREMIUM_MAX_CONNECTIONS)

    fun maximumAmountOfGenerators(guildId: String) = premiumRequired(guildId, ConfigurationErrorCode.PREMIUM_MAX_GENERATORS)

    fun fallbackGenerator(guildId: String) = premiumRequired(guildId, ConfigurationErrorCode.PREMIUM_FALLBACK_GENERATOR)

    fun autoPrivateChat(guildId: String) = premiumRequired(guildId, ConfigurationErrorCode.PREMIUM_AUTO_PRIVATE_CHAT)

    fun autoWaitingRoom(guildId: String) = premiumRequired(guildId, ConfigurationErrorCode.PREMIUM_AUTO_WAITING_ROOM)

    fun creationChatMessage(guildId: String) = premiumRequired(guildId, ConfigurationErrorCode.PREMIUM_CREATION_CHAT_MESSAGE)

    fun ownerRole(guildId: String) = premiumRequired(guildId, ConfigurationErrorCode.PREMIUM_OWNER_ROLE)
}
