package space.astro.api.central.models.topgg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class TopggWebhookEvent(
    val userId: String
)
