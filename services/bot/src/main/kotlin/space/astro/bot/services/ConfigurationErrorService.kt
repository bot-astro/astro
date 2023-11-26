package space.astro.bot.services

import org.springframework.stereotype.Service
import space.astro.bot.models.error.ConfigurationErrorDto

@Service
class ConfigurationErrorService {
    fun premiumVariables() = ConfigurationErrorDto(
        type = ConfigurationErrorDto.ConfigurationErrorType.PREMIUM,
        description = "Your are trying to use premium variables and your server isn't premium!",
    )
}