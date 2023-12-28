package space.astro.bot.interactions.command

import mu.KotlinLogging
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.*
import space.astro.bot.interactions.InteractionContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.*

abstract class AbstractCommand : ICommand {

    private val log = KotlinLogging.logger {}

    final override val data: CommandData
    override var id: Long? = null
    private val requiredPermissions: Array<Permission>

    final override val commands: MutableMap<String, Pair<KFunction<*>, List<String>>> =
        mutableMapOf()

    final override val category: CommandCategory

    init {
        val reflectedClass = this::class
        val commandAnnotation = reflectedClass.findAnnotation<Command>()
            ?: throw UnsupportedOperationException("Missing command annotation on class extending AbstractCommand!")

        requiredPermissions = commandAnnotation.requiredPermissions

        val commandName = commandAnnotation.name.ifEmpty {
            commandAnnotation::class.simpleName!!.removeSuffix("Command").lowercase()
        }
        val commandDescription = commandAnnotation.description
        data = Commands.slash(commandName, commandDescription)
        category = commandAnnotation.category

        reflectedClass.memberFunctions.forEach { function ->
            val functionBaseCommandAnnotation = function.findAnnotation<BaseCommand>()
            val functionSubCommandAnnotation = function.findAnnotation<SubCommand>()
            when {
                functionBaseCommandAnnotation != null -> {
                    val options = parseOptions(function)
                    data.addOptions(options)
                    commands[commandName] = function to options.map { it.name }
                }

                functionSubCommandAnnotation != null -> {
                    val hasSubCommandGroup = functionSubCommandAnnotation.group.isNotEmpty()

                    val fullCommandName = if (hasSubCommandGroup) {
                        "$commandName.${functionSubCommandAnnotation.group}.${functionSubCommandAnnotation.name}"
                    } else {
                        "$commandName.${functionSubCommandAnnotation.name}"
                    }

                    val options = parseOptions(function)
                    val subCommandData = SubcommandData(
                        functionSubCommandAnnotation.name,
                        functionSubCommandAnnotation.description
                    ).addOptions(options)

                    if (hasSubCommandGroup) {
                        val subCommandGroup = data.subcommandGroups.firstOrNull {
                            it.name == functionSubCommandAnnotation.group
                        }

                        if (subCommandGroup != null) {
                            subCommandGroup.addSubcommands(subCommandData)
                        } else {
                            val subCommandGroupData = SubcommandGroupData(
                                functionSubCommandAnnotation.group,
                                functionSubCommandAnnotation.groupDescription,
                            ).addSubcommands(subCommandData)
                            data.addSubcommandGroups(subCommandGroupData)
                        }
                    } else {
                        data.addSubcommands(subCommandData)
                    }

                    commands[fullCommandName] = function to options.map { it.name }
                }
            }
        }
    }

    private fun parseOptions(
        function: KFunction<*>
    ): List<OptionData> {
        if (function.parameters.size < 3) {
            throw UnsupportedOperationException("Function ${function.name} does not have at least two parameters!")
        }

        if (!(function.parameters[1].type.classifier as KClass<*>).isSubclassOf(SlashCommandInteractionEvent::class)) {
            throw UnsupportedOperationException("First parameter of ${function.name} must be a SlashCommandInteractionEvent parameter!")
        }

        if (!(function.parameters[2].type.classifier as KClass<*>).isSubclassOf(InteractionContext::class)) {
            throw UnsupportedOperationException("Second parameter of ${function.name} must be a subtype of InteractionContext!")
        }

        val options = mutableListOf<OptionData>()
        var allowNonOptions = true
        for (i in 3 until function.parameters.size) {
            val parameter = function.parameters[i]
            val type = parameter.type.classifier as KClass<*>
            val commandOptionAnnotation = parameter.findAnnotation<CommandOption>()
            if (commandOptionAnnotation == null) {
                require(allowNonOptions) {
                    "Parameter ${parameter.name} in function " +
                            "${function.name} must be annotated as @CommandOption!"
                }
            } else {
                allowNonOptions = false
                val name = commandOptionAnnotation.name.ifEmpty { parameter.name!!.lowercase() }
                if (type.java.isEnum || commandOptionAnnotation.stringChoices.isNotEmpty()) {
                    val choices = if (type.java.isEnum) {
                        type.java.enumConstants.map {
                            Choice(
                                it.toString(),
                                (it as Enum<*>).name
                            )
                        }
                    } else {
                        commandOptionAnnotation.stringChoices.map {
                            Choice(
                                it,
                                it
                            )
                        }
                    }
                    options.add(
                        OptionData(
                            OptionType.STRING,
                            name,
                            commandOptionAnnotation.description,
                            !parameter.type.isMarkedNullable
                        ).addChoices(
                            choices
                        )
                    )
                } else {
                    require(commandOptionAnnotation.type != OptionType.UNKNOWN) {
                        "Option type was not specifically declared for ${parameter.name}!"
                    }
                    val optionData = OptionData(
                        commandOptionAnnotation.type,
                        name,
                        commandOptionAnnotation.description,
                        !parameter.type.isMarkedNullable,
                        commandOptionAnnotation.autocomplete
                    )
                    if (commandOptionAnnotation.minValue != 0L) {
                        optionData.setMinValue(commandOptionAnnotation.minValue)
                    }
                    if (commandOptionAnnotation.maxValue != 0L) {
                        optionData.setMaxValue(commandOptionAnnotation.maxValue)
                    }
                    if (commandOptionAnnotation.minLength != 0) {
                        optionData.setMinLength(commandOptionAnnotation.minLength)
                    }
                    if (commandOptionAnnotation.maxLength != 0) {
                        optionData.setMaxLength(commandOptionAnnotation.maxLength)
                    }
                    options.add(
                        optionData
                    )
                }
            }
        }
        return options
    }

    override fun handleAutoComplete(event: CommandAutoCompleteInteractionEvent) {
        log.warn { "AutoComplete not implemented for ${event.name}" }
    }
}
