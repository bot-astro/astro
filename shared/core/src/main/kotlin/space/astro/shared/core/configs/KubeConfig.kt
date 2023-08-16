package space.astro.shared.core.configs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("kube")
class KubeConfig {

    var lifecycleAuthorization: String = "password"

}
