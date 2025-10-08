package space.astro.api.central.configs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("discord.application")
class DiscordApplicationConfig {

    var id: String = "id"
    var token: String = "token"
    var secret: String = "secret"
    var redirectUri: String = "http://localhost:3000/callback/discord"
}
