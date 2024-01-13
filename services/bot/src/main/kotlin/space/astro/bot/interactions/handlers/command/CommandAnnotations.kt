package space.astro.bot.interactions.handlers.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.interactions.commands.OptionType
import org.springframework.stereotype.Component
import space.astro.bot.interactions.InteractionAction

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Component
annotation class Command(
    val name: String,
    val description: String = "N/A",
    val requiredPermissions: Array<Permission> = [],
    val category: CommandCategory = CommandCategory.ALL,
    val action: InteractionAction = InteractionAction.GENERIC
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
annotation class CommandOption(
    val name: String = "",
    val description: String = "",
    val type: OptionType,
    val channelTypes: Array<ChannelType> = [],
    val stringChoices: Array<String> = [],
    val minValue : Long = 0,
    val maxValue : Long = 0,
    val minLength : Int = 0,
    val maxLength : Int = 0,
    val autocomplete: Boolean = false
    //vararg val integerChoices: Int = [],
)
