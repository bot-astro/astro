package space.astro.bot.services

import org.springframework.stereotype.Service
import space.astro.shared.core.models.influx.ConfigurationErrorData
import space.astro.shared.core.util.extention.asChannelMention

@Service
class ConfigurationErrorService {
    ///////////////
    /// GENERIC ///
    ///////////////

    fun unknown(guildId: String, encounteredIn: String) = ConfigurationErrorData(
        guildId = guildId,
        description = "An unknown error occurred!" +
                "\nEncountered in: $encounteredIn"
    )

    ///////////////////
    /// PERMISSIONS ///
    ///////////////////
//    fun missingBotPermissions(guildId: String, permissions: List<Permission>, requiredFor: String) = ConfigurationErrorData(
//        guildId = guildId,
//        description = "Astro is missing the following permissions: ${permissions.joinToString(", ") { it.getName() }}" +
//                "\nRequired for: $requiredFor"
//    )

    ///////////////
    /// PREMIUM ///
    ///////////////
    fun premiumVariables(guildId: String, encounteredIn: String) = ConfigurationErrorData(
        guildId = guildId,
        description = "Your are trying to use ultimate variables and your server isn't upgraded to ultimate!" +
                "\nEncountered in: $encounteredIn",
        premiumRequired = true,
        guide = ConfigurationErrorData.Guide.GENERATOR
    )

    fun premiumRequiredForBadwordsValidation(guildId: String) = ConfigurationErrorData(
        guildId = guildId,
        description = "Your generator has badwords validation enabled, but ultimate is required to use it." +
                "\nEither upgrade to ultimate or disable badwords validation!",
        premiumRequired = true,
        guide = ConfigurationErrorData.Guide.GENERATOR
    )

    fun maximumAmountOfConnections(guildId: String) = ConfigurationErrorData(
        guildId = guildId,
        description = "Your server has exceeded the maximum amount of voice roles." +
                "\nEither upgrade to ultimate or delete one or more voice role!",
        premiumRequired = true,
        guide = ConfigurationErrorData.Guide.VOICE_ROLE
    )

    fun maximumAmountOfInterfaces(guildId: String) = ConfigurationErrorData(
        guildId = guildId,
        description = "Your server has exceeded the maximum amount of interfaces." +
                "\nEither upgrade to ultimate or delete one or more interfaces!",
        premiumRequired = true,
        guide = ConfigurationErrorData.Guide.INTERFACE
    )

    fun maximumAmountOfGenerator(guildId: String) = ConfigurationErrorData(
        guildId = guildId,
        description = "Your server has exceeded the maximum amount of temporary VC generators." +
                "\nEither upgrade to ultimate or delete one or more generators!",
        premiumRequired = true,
        guide = ConfigurationErrorData.Guide.GENERATOR
    )

    fun premiumFallbackGenerator(guildId: String) = ConfigurationErrorData(
        guildId = guildId,
        description = "Your generator has a fallback generator configured, but ultimate is required to use it." +
                "\nEither upgrade to ultimate or remove the fallback generator!",
        premiumRequired = true,
        guide = ConfigurationErrorData.Guide.GENERATOR
    )

    fun premiumRequiredForAutoPrivateChatCreation(guildId: String)  = ConfigurationErrorData(
        guildId = guildId,
        description = "Your generator has automatic private chat creation enabled, but ultimate is required to use it." +
                "\nEither upgrade to ultimate or disable private chat creation!",
        premiumRequired = true,
        guide = ConfigurationErrorData.Guide.GENERATOR
    )

    fun premiumRequiredForAutoWaitingRoomCreation(guildId: String)  = ConfigurationErrorData(
        guildId = guildId,
        description = "Your generator has automatic waiting room creation enabled, but ultimate is required to use it." +
                "\nEither upgrade to ultimate or disable waiting room creation!",
        premiumRequired = true,
        guide = ConfigurationErrorData.Guide.GENERATOR
    )

    fun premiumRequiredForAutoChatMessageOnCreation(guildId: String) = ConfigurationErrorData(
        guildId = guildId,
        description = "Your generator has a default creation message, but ultimate is required to use it." +
                "\nEither upgrade to ultimate or remove it!",
        premiumRequired = true,
        guide = ConfigurationErrorData.Guide.GENERATOR
    )

    fun premiumRequiredForOwnerRole(guildId: String) = ConfigurationErrorData(
        guildId = guildId,
        description = "Your generator has an owner role configured, but ultimate is required to use it." +
                "\nEither upgrade to ultimate or remove it!",
        premiumRequired = true,
        guide = ConfigurationErrorData.Guide.GENERATOR
    )

    /////////////////
    /// GENERATOR ///
    /////////////////

    fun missingGeneratorTargetRole(guildId: String, generatorName: String) = ConfigurationErrorData(
        guildId = guildId,
        description = "The generator $generatorName has a target role set but Astro couldn't find that role in your server!",
        guide = ConfigurationErrorData.Guide.GENERATOR
    )

    fun missingFallbackGenerator(guildId: String, encounteredIn: String) = ConfigurationErrorData(
        guildId = guildId,
        description = "Your category for temporary VCs is full and you haven't configured a fallback generator." +
                "\nThis means Astro could not generate a temporary VC because the category was already full, consider creating a fallback generator." +
                "\nEncountered in: $encounteredIn",
        guide = ConfigurationErrorData.Guide.GENERATOR
    )

    //////////////////////////
    /// CHANNEL PROPERTIES ///
    //////////////////////////

    fun maximumAmountOfChannelsReached(guildId: String, encounteredIn: String) = ConfigurationErrorData(
        guildId = guildId,
        description = "Your server has reached the maximum amount of channels" +
                "\nEncountered in: $encounteredIn",
    )

    fun invalidChannelName(guildId: String, encounteredIn: String) = ConfigurationErrorData(
        guildId = guildId,
        description = "An invalid channel name has been found in your configuration!" +
                "\nEncountered in: $encounteredIn",
    )

    fun missingChannelParent(guildId: String, requiredFor: String) = ConfigurationErrorData(
        guildId = guildId,
        description = "A category is misconfigured or missing" +
                "\nRequired for: $requiredFor",
    )

    /////////////////
    /// INTERFACE ///
    /////////////////
    fun invalidOldInterface(guildId: String, channelId: String) = ConfigurationErrorData(
        guildId = guildId,
        description = "This server has an old interface in ${channelId.asChannelMention()}." +
                "\nDelete it and create a new one to avoid potential issues.",
        guide = ConfigurationErrorData.Guide.INTERFACE
    )
}