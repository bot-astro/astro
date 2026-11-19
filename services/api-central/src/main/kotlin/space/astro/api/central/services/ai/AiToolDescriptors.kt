package space.astro.api.central.services.ai

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import kotlinx.serialization.json.JsonObject
import space.astro.api.central.models.ai.AiProposal
import space.astro.shared.core.models.database.AiSurface
import space.astro.shared.core.models.database.guildSettings.ConnectionData
import space.astro.shared.core.models.database.guildSettings.GeneratorSettings

data class AiToolContext(
    val guildId: String,
    val userId: String,
    val surface: AiSurface,
)

/**
 * @param text what the model reads as the tool result
 * @param proposal set by the write tools
 */
data class AiToolOutput(
    val text: String,
    val proposal: AiProposal? = null,
)

fun interface AiToolExecutor {
    fun execute(
        context: AiToolContext,
        name: String,
        args: JsonObject,
    ): AiToolOutput
}

/**
 * Definitions of the tools the model can call, they are executed by [AiTools]
 */
object AiToolDescriptors {
    private fun param(
        name: String,
        description: String,
        type: ToolParameterType = ToolParameterType.String,
    ) = ToolParameterDescriptor(name, description, type)

    private val generatorId = param("generator_id", "Id of the generator voice channel")
    private val interfaceId = param("interface_id", "Message id of the interface")
    private val templateId = param("template_id", "Id of the template")
    private val vcState =
        ToolParameterType.Enum(
            GeneratorSettings.VCState.entries
                .map { it.name }
                .toTypedArray(),
        )

    val GET_GUILD_OVERVIEW =
        ToolDescriptor(
            "get_guild_overview",
            "Overview of the Astro setup of the server: language, premium status, generators, interfaces, templates and voice roles counts",
        )
    val LIST_CHANNELS_AND_CATEGORIES =
        ToolDescriptor("list_channels_and_categories", "Lists the channels and categories of the server with their ids and types")
    val LIST_ROLES = ToolDescriptor("list_roles", "Lists the roles of the server with their ids")
    val LIST_GENERATORS =
        ToolDescriptor("list_generators", "Lists the generators: voice channels that create a temporary voice channel when joined")
    val GET_GENERATOR =
        ToolDescriptor("get_generator", "Every setting of a generator", requiredParameters = listOf(generatorId))
    val LIST_INTERFACES =
        ToolDescriptor("list_interfaces", "Lists the interfaces: messages with buttons to manage temporary voice channels")
    val GET_INTERFACE =
        ToolDescriptor("get_interface", "Buttons and message of an interface", requiredParameters = listOf(interfaceId))
    val LIST_TEMPLATES =
        ToolDescriptor(
            "list_templates",
            "Lists the templates: presets of name, limit and bitrate users can apply to their temporary voice channel",
        )
    val LIST_VOICE_ROLES =
        ToolDescriptor("list_voice_roles", "Lists the voice roles: roles given or removed when a user joins a voice channel")
    val GET_PREMIUM_STATUS =
        ToolDescriptor("get_premium_status", "Whether the server has premium and the limits of the free plan")

    val CREATE_VOICE_GENERATOR =
        ToolDescriptor(
            "create_voice_generator",
            "Proposes a new generator voice channel. Defaults are filled in, only set what the user asked for",
            requiredParameters = listOf(param("name", "Name of the generator voice channel, e.g. ➕ Create a VC")),
            optionalParameters =
                listOf(
                    param("category_id", "Category where the generator and its temporary voice channels are created"),
                    param("user_limit", "User limit of the temporary voice channels, 0 to 99, 0 means no limit", ToolParameterType.Integer),
                ),
        )
    val UPDATE_GENERATOR =
        ToolDescriptor(
            "update_generator",
            "Proposes changes to a generator, only the given settings change. " +
                "Names can use variables like {nickname}, {username}, {game}, {counter}",
            requiredParameters = listOf(generatorId),
            optionalParameters =
                listOf(
                    param("default_name", "Name of the temporary voice channels"),
                    param("default_locked_name", "Name of the temporary voice channels when locked"),
                    param("default_hidden_name", "Name of the temporary voice channels when hidden"),
                    param("user_limit", "User limit, 0 to 99, 0 means no limit", ToolParameterType.Integer),
                    param("bitrate", "Bitrate in bps, 8000 to 384000", ToolParameterType.Integer),
                    param("category_id", "Category of the temporary voice channels"),
                    param("initial_state", "State of new temporary voice channels", vcState),
                    param("queue_mode", "Users join a queue instead of creating a channel each", ToolParameterType.Boolean),
                    param("auto_chat", "Create a private text chat for each temporary voice channel (premium)", ToolParameterType.Boolean),
                    param("auto_waiting", "Create a waiting room for each temporary voice channel (premium)", ToolParameterType.Boolean),
                    param("owner_role_id", "Role given to the owner of a temporary voice channel (premium)"),
                    param("fallback_generator_id", "Generator used when this one cannot create channels (premium)"),
                ),
        )
    val DELETE_GENERATOR =
        ToolDescriptor("delete_generator", "Proposes deleting a generator and its voice channel", requiredParameters = listOf(generatorId))
    val CREATE_INTERFACE =
        ToolDescriptor(
            "create_interface",
            "Proposes sending a new interface with the default buttons in a text channel",
            requiredParameters = listOf(param("channel_id", "Text channel where the interface is sent")),
        )
    val SET_INTERFACE_BUTTONS =
        ToolDescriptor(
            "set_interface_buttons",
            "Proposes which buttons of an interface are enabled, the buttons not listed get disabled. " +
                "Get the button ids with get_interface",
            requiredParameters =
                listOf(
                    interfaceId,
                    param("enabled_button_ids", "Ids of the buttons to keep enabled", ToolParameterType.List(ToolParameterType.String)),
                ),
        )
    val CREATE_TEMPLATE =
        ToolDescriptor(
            "create_template",
            "Proposes a new template",
            requiredParameters = listOf(param("name", "Name of the template shown to users")),
            optionalParameters =
                listOf(
                    param("vc_name", "Name applied to the voice channel"),
                    param("vc_limit", "User limit applied, 0 to 99", ToolParameterType.Integer),
                    param("vc_bitrate", "Bitrate applied in bps, 8000 to 384000", ToolParameterType.Integer),
                    param(
                        "enabled_generator_ids",
                        "Generators where the template can be used, all when omitted",
                        ToolParameterType.List(ToolParameterType.String),
                    ),
                ),
        )
    val UPDATE_TEMPLATE =
        ToolDescriptor(
            "update_template",
            "Proposes changes to a template, only the given settings change",
            requiredParameters = listOf(templateId),
            optionalParameters =
                listOf(
                    param("name", "Name of the template shown to users"),
                    param("vc_name", "Name applied to the voice channel"),
                    param("vc_limit", "User limit applied, 0 to 99", ToolParameterType.Integer),
                    param("vc_bitrate", "Bitrate applied in bps, 8000 to 384000", ToolParameterType.Integer),
                ),
        )
    val CREATE_VOICE_ROLE =
        ToolDescriptor(
            "create_voice_role",
            "Proposes a voice role: a role changed when users join a voice channel",
            requiredParameters =
                listOf(
                    param("channel_id", "Voice channel (or category for all its voice channels)"),
                    param("role_id", "Role to change"),
                ),
            optionalParameters =
                listOf(
                    param(
                        "action",
                        "ASSIGN gives the role, REMOVE removes it, TOGGLE flips it. Default ASSIGN",
                        ToolParameterType.Enum(
                            ConnectionData.ConnectionAction.entries
                                .map { it.name }
                                .toTypedArray(),
                        ),
                    ),
                    param("permanent", "Keep the change after the user leaves the channel", ToolParameterType.Boolean),
                ),
        )
    val DELETE_VOICE_ROLE =
        ToolDescriptor(
            "delete_voice_role",
            "Proposes deleting a voice role",
            requiredParameters = listOf(param("channel_id", "Channel of the voice role"), param("role_id", "Role of the voice role")),
        )

    val all =
        listOf(
            GET_GUILD_OVERVIEW,
            LIST_CHANNELS_AND_CATEGORIES,
            LIST_ROLES,
            LIST_GENERATORS,
            GET_GENERATOR,
            LIST_INTERFACES,
            GET_INTERFACE,
            LIST_TEMPLATES,
            LIST_VOICE_ROLES,
            GET_PREMIUM_STATUS,
            CREATE_VOICE_GENERATOR,
            UPDATE_GENERATOR,
            DELETE_GENERATOR,
            CREATE_INTERFACE,
            SET_INTERFACE_BUTTONS,
            CREATE_TEMPLATE,
            UPDATE_TEMPLATE,
            CREATE_VOICE_ROLE,
            DELETE_VOICE_ROLE,
        )
}
