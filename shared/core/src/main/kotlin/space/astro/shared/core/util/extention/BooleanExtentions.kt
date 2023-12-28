package space.astro.shared.core.util.extention

/**
 * Converts a boolean value to "true" or "false"
 */
fun Boolean.asTrueOrFalse() = if (this) "true" else "false"


/**
 * Converts a boolean value to "enabled" or "disabled"
 */
fun Boolean.asEnabledOrDisabled() = if (this) "enabled" else "disabled"

/**
 * Converts a boolean value to "active" or "inactive"
 */
fun Boolean.asActiveOrInactive() = if (this) "active" else "inactive"

/**
 * Converts a boolean value to "allowed" or "not allowed"
 */
fun Boolean.asAllowedOrNotAllowed() = if (this) "allowed" else "not allowed"

/**
 * Converts a boolean value to "able" or "unable"
 */
fun Boolean.asAbleOrUnable() = if (this) "able" else "unable"

/**
 * Converts a boolean value to "on" or "off"
 */
fun Boolean.asOnOrOff() = if (this) "on" else "off"