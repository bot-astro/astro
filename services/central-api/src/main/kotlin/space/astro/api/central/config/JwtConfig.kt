package space.astro.api.central.config

import io.jsonwebtoken.io.Decoders
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("jwt")
class JwtConfig {
    var key = ""

    fun getDecodedKey() : ByteArray? {
        return Decoders.BASE64.decode(this.key)
    }
}
