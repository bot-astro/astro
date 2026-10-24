package space.astro.shared.core.models.database

import space.astro.shared.core.models.database.ConfigurationErrorEntity.Guide

/**
 * Codes of [ConfigurationErrorEntity], each one maps to the translation key `configuration_error.<code in lowercase>`
 *
 * @param guide The guide that explains how to fix the error, if any
 * @param premiumRequired Whether the error is caused by a premium requirement
 */
enum class ConfigurationErrorCode(
    val guide: Guide? = null,
    val premiumRequired: Boolean = false,
) {
    /** args: encounteredIn, details */
    UNKNOWN,

    /** args: permission, channelId */
    MISSING_PERMISSION,
    ROLE_HIERARCHY,

    /** args: name */
    INVALID_VC_NAME,
    MAX_CHANNELS_PRIVATE_CHAT,
    MAX_CHANNELS_WAITING_ROOM,

    /** args: manager, message */
    UPDATE_MANAGER_FAILED,

    /** args: generator */
    GENERATOR_CATEGORY_FULL(Guide.GENERATOR),

    /** args: generator */
    GENERATOR_TARGET_ROLE_MISSING(Guide.GENERATOR),

    /** args: encounteredIn */
    PREMIUM_VARIABLES(Guide.GENERATOR, premiumRequired = true),
    PREMIUM_MAX_CONNECTIONS(Guide.VOICE_ROLE, premiumRequired = true),
    PREMIUM_MAX_GENERATORS(Guide.GENERATOR, premiumRequired = true),
    PREMIUM_FALLBACK_GENERATOR(Guide.GENERATOR, premiumRequired = true),
    PREMIUM_AUTO_PRIVATE_CHAT(Guide.GENERATOR, premiumRequired = true),
    PREMIUM_AUTO_WAITING_ROOM(Guide.GENERATOR, premiumRequired = true),
    PREMIUM_CREATION_CHAT_MESSAGE(Guide.GENERATOR, premiumRequired = true),
    PREMIUM_OWNER_ROLE(Guide.GENERATOR, premiumRequired = true),
    ;

    val translationKey = "configuration_error.${name.lowercase()}"
}
