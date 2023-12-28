package space.astro.bot.interactions.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.OptionType
import org.springframework.stereotype.Component
import space.astro.bot.models.discord.vc.VCOperationCTX

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Component
annotation class Command(
    val name: String = "",
    val description: String = "N/A",
    val requiredPermissions: Array<Permission> = [],
    val category: CommandCategory = CommandCategory.ALL,
    val premium: Boolean = false
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class BaseCommand

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class SubCommand(
    val name: String,
    val description: String = "",
    val group: String = "",
    val groupDescription: String = "N/A",
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class VcInteractionContextInfo(
    val ownershipRequired: Boolean,
    val vcOperationOrigin: VCOperationCTX.VCOperationOrigin
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class CommandOption(
    val name: String = "",
    val description: String = "",
    val type: OptionType,
    // TODO: find better solution
    vararg val stringChoices: String = [],
    val minValue : Long = 0,
    val maxValue : Long = 0,
    val minLength : Int = 0,
    val maxLength : Int = 0,
    val autocomplete: Boolean = false
    // CAN'T USE VARARG TWICE
    //vararg val integerChoices: Int = [],
)
