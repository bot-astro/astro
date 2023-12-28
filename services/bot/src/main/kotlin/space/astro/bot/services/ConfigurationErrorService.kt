package space.astro.bot.services

import org.springframework.stereotype.Service
import space.astro.bot.models.error.ConfigurationErrorDto

@Service
class ConfigurationErrorService {
    ///////////////
    /// GENERIC ///
    ///////////////

    fun unknownError(encounteredIn: String) = ConfigurationErrorDto(
        description = "An unknown error occured!" +
                "\nEncountered in: $encounteredIn"
    )

    ///////////////
    /// PREMIUM ///
    ///////////////
    fun premiumVariables(encounteredIn: String) = ConfigurationErrorDto(
        description = "Your are trying to use premium variables and your server isn't premium!" +
                "\nEncountered in: $encounteredIn",
    )

    fun premiumRequiredForBadwordsValidation() = ConfigurationErrorDto(
        description = "Your generator has badwords validation enabled, but premium is required to use it." +
                "\nEither upgrade to premium or disable badwords validation!"
    )

    fun maximumAmountOfGenerator() = ConfigurationErrorDto(
        description = "Your server has exceeded the maximum amount of temporary VC generators." +
                "\nEither upgrade to premium or delete one or more generators!"
    )

    fun premiumFallbackGenerator() = ConfigurationErrorDto(
        description = "Your generator has a fallback generator configured, but premium is required to use it." +
                "\nEither upgrade to premium or remove the fallback generator from the configuration!"
    )

    fun premiumRequiredForAutoPrivateChatCreation()  = ConfigurationErrorDto(
        description = "Your generator has automatic private chat creation enabled, but premium is required to use it." +
                "\nEither upgrade to premium or disable private chat creation!"
    )

    fun premiumRequiredForAutoWaitingRoomCreation()  = ConfigurationErrorDto(
        description = "Your generator has automatic waiting room creation enabled, but premium is required to use it." +
                "\nEither upgrade to premium or disable waiting room creation!"
    )

    fun premiumRequiredForAutoChatMessageOnCreation() = ConfigurationErrorDto(
        description = "Your generator has a default creation message, but premium is required to use it." +
                "\nEither upgrade to premium or remove it!"
    )

    fun premiumRequiredForOwnerRole() = ConfigurationErrorDto(
        description = "Your generator has an owner role configured, but premium is required to use it." +
                "\nEither upgrade to premium or remove it!"
    )

    /////////////////
    /// GENERATOR ///
    /////////////////

    fun missingGeneratorTargetRole(generatorName: String) = ConfigurationErrorDto(
        description = "The generator $generatorName has a target role set but Astro couldn't find that role in your server!"
    )

    fun missingFallbackGenerator(encounteredIn: String) = ConfigurationErrorDto(
        description = "Your category for temporary VCs is full and you haven't configured a fallback generator." +
                "\nThis means Astro could not generate a temporary VC because the category was already full, consider creating a fallback generator." +
                "\nEncountered in: $encounteredIn"
    )

    //////////////////////////
    /// CHANNEL PROPERTIES ///
    //////////////////////////

    fun maximumAmountOfChannelsReached(encounteredIn: String) = ConfigurationErrorDto(
        description = "Your server has reached the maximum amount of channels" +
                "\nEncountered in: $encounteredIn",
    )

    fun invalidChannelName(encounteredIn: String) = ConfigurationErrorDto(
        description = "An invalid channel name has been found in your configuration!" +
                "\nEncountered in: $encounteredIn",
    )

    fun missingChannelParent(requiredFor: String) = ConfigurationErrorDto(
        description = "A category is misconfigured or missing" +
                "\nRequired for: $requiredFor",
    )
}