package space.astro.shared.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("kube")
data class KubeProperties(
    val lifecycleAuthKey: String,
)
