package space.astro.bot.interactions.context

import space.astro.bot.models.discord.vc.VCOperationCTX

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class VcInteractionContextInfo(
    val ownershipRequired: Boolean,
    val vcOperationOrigin: VCOperationCTX.VCOperationOrigin
)