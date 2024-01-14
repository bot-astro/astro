package space.astro.bot.services

import net.dv8tion.jda.api.Permission
import org.springframework.stereotype.Service
import space.astro.shared.core.models.influx.ConfigurationErrorData
import space.astro.shared.core.util.extention.asChannelMention

@Service
class ConfigurationErrorService {
    ///////////////
    /// GENERIC ///
    ///////////////

    fun unknownError(encounteredIn: String) = ConfigurationErrorData(
        description = "An unknown error occurred!" +
                "\nEncountered in: $encounteredIn"
    )

    ///////////////////
    /// PERMISSIONS ///
    ///////////////////
    fun missingBotPermissions(permissions: List<Permission>, requiredFor: String) = ConfigurationErrorData(
        description = "Astro is missing the following permissions: ${permissions.joinToString(", ") { it.getName() }}" +
                "\nRequired for: $requiredFor"
    )

    ///////////////
    /// PREMIUM ///
    ///////////////
    fun premiumVariables(encounteredIn: String) = ConfigurationErrorData(
        description = "Your are trying to use premium variables and your server isn't premium!" +
                "\nEncountered in: $encounteredIn",
    )

    fun premiumRequiredForBadwordsValidation() = ConfigurationErrorData(
        description = "Your generator has badwords validation enabled, but premium is required to use it." +
                "\nEither upgrade to premium or disable badwords validation!"
    )

    fun maximumAmountOfConnections() = ConfigurationErrorData(
        description = "Your server has exceeded the maximum amount of connections." +
                "\nEither upgrade to premium or delete one or more connection!"
    )

    fun maximumAmountOfInterfaces() = ConfigurationErrorData(
        description = "Your server has exceeded the maximum amount of interfaces." +
                "\nEither upgrade to premium or delete one or more interface!"
    )

    fun maximumAmountOfGenerator() = ConfigurationErrorData(
        description = "Your server has exceeded the maximum amount of temporary VC generators." +
                "\nEither upgrade to premium or delete one or more generators!"
    )

    fun premiumFallbackGenerator() = ConfigurationErrorData(
        description = "Your generator has a fallback generator configured, but premium is required to use it." +
                "\nEither upgrade to premium or remove the fallback generator from the configuration!"
    )

    fun premiumRequiredForAutoPrivateChatCreation()  = ConfigurationErrorData(
        description = "Your generator has automatic private chat creation enabled, but premium is required to use it." +
                "\nEither upgrade to premium or disable private chat creation!"
    )

    fun premiumRequiredForAutoWaitingRoomCreation()  = ConfigurationErrorData(
        description = "Your generator has automatic waiting room creation enabled, but premium is required to use it." +
                "\nEither upgrade to premium or disable waiting room creation!"
    )

    fun premiumRequiredForAutoChatMessageOnCreation() = ConfigurationErrorData(
        description = "Your generator has a default creation message, but premium is required to use it." +
                "\nEither upgrade to premium or remove it!"
    )

    fun premiumRequiredForOwnerRole() = ConfigurationErrorData(
        description = "Your generator has an owner role configured, but premium is required to use it." +
                "\nEither upgrade to premium or remove it!"
    )

    /////////////////
    /// GENERATOR ///
    /////////////////

    fun missingGeneratorTargetRole(generatorName: String) = ConfigurationErrorData(
        description = "The generator $generatorName has a target role set but Astro couldn't find that role in your server!"
    )

    fun missingFallbackGenerator(encounteredIn: String) = ConfigurationErrorData(
        description = "Your category for temporary VCs is full and you haven't configured a fallback generator." +
                "\nThis means Astro could not generate a temporary VC because the category was already full, consider creating a fallback generator." +
                "\nEncountered in: $encounteredIn"
    )

    //////////////////////////
    /// CHANNEL PROPERTIES ///
    //////////////////////////

    fun maximumAmountOfChannelsReached(encounteredIn: String) = ConfigurationErrorData(
        description = "Your server has reached the maximum amount of channels" +
                "\nEncountered in: $encounteredIn",
    )

    fun invalidChannelName(encounteredIn: String) = ConfigurationErrorData(
        description = "An invalid channel name has been found in your configuration!" +
                "\nEncountered in: $encounteredIn",
    )

    fun missingChannelParent(requiredFor: String) = ConfigurationErrorData(
        description = "A category is misconfigured or missing" +
                "\nRequired for: $requiredFor",
    )

    /////////////////
    /// INTERFACE ///
    /////////////////
    fun invalidOldInterface(channelId: String) = ConfigurationErrorData(
        description = "This server has an old interface in ${channelId.asChannelMention()}." +
                "\nDelete it and create a new one to avoid issues."
    )
}