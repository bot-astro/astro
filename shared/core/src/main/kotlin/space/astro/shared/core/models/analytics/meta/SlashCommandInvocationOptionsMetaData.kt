package space.astro.shared.core.models.analytics.meta

import space.astro.shared.core.models.analytics.AnalyticsEventMetaData
import space.astro.shared.core.models.analytics.meta.structure.OptionPair

data class SlashCommandInvocationOptionsMetaData(
    val options: List<OptionPair>
) : AnalyticsEventMetaData
