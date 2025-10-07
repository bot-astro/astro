package space.astro.shared.core.models.topgg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
data class TopggVoteResponse(
    @JsonProperty("created_at")
    val createdAt: Instant,
)