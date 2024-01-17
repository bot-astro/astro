package space.astro.bot.core.ui

import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import space.astro.shared.core.util.extention.linkFromLink
import space.astro.shared.core.util.ui.Colors
import space.astro.shared.core.util.ui.Links

object Embeds {
    const val footer = "Use /help for support"

    fun default(description: String): MessageEmbed {
        return Embed(
            color = Colors.purple.rgb,
            description = description,
            footerText = footer
        )
    }

    fun error(description: String): MessageEmbed {
        return Embed(
            color = Colors.red.rgb,
            description = description,
            footerText = footer
        )
    }

    ////////////
    /// HELP ///
    ////////////

    val helpGeneral = Embed(
        color = Colors.purple.rgb,
        authorName = "Help panel",
        authorUrl = Links.WEBSITE,
        authorIcon = Links.LOGO,
        description = "Astro can be used to generate temporary voice channels & connect voice channels to roles!" +
                "\n\nTake a look at the ${Links.Documentation.DOCUMENTATION.linkFromLink("documentation")} for learning how to use Astro." +
                "\nUse the buttons below for other useful links!"
    )

    val helpPremium = Embed(

    )

    val helpVariables = Embed(

    )

    val helpTemporaryVoiceChannels = Embed(

    )

    val helpGenerators = Embed(

    )

    val helpInterfaces = Embed(

    )

    val helpTemplates = Embed(

    )

    val helpConnections = Embed(

    )

    /*
    fun dashboardSettings() : MessageEmbed {
        return Embed(
            color = Colors.purple.rgb,
            description = "You can manage Astro's settings on its ${"dashboard".linkFromName(Links.DASHBOARD)}!"
        )
    }
     */


    ////////////////////
    /// PREDASHBOARD ///
    ////////////////////

    val canceled = EmbedBuilder()
        .setColor(Colors.red)
        .setDescription("The action has been canceled so nothing has been modified")
        .setFooter(footer)
        .build()

    val timeExpired = EmbedBuilder()
        .setColor(Colors.red)
        .setDescription("You took too long to complete this action")
        .setFooter(footer)
        .build()

    fun selector(description: String) = EmbedBuilder()
        .setColor(Colors.purple)
        .setDescription(description)
        .setFooter(footer)
        .build()

    fun confirmation(description: String) = EmbedBuilder()
        .setColor(Colors.yellow)
        .setDescription(description)
        .setFooter(footer)
        .build()

    fun success(description: String): MessageEmbed {
        val builder = EmbedBuilder()
            .setColor(Colors.green)
            .setDescription(description)
            .setFooter(footer)

        return builder.build()
    }

    fun requireRoleHierarchy(roleName: String) = EmbedBuilder()
        .setColor(Colors.red)
        .setTitle("Cannot manage the role $roleName")
        .setDescription(
             "Make sure that the role you selected is below the Astro role in the *Server settings > Roles*" +
                    "\nFind out more about roles hierarchy [here](${Links.ExternalGuides.ROLE_HIERARCHY})."
        )
        .setFooter(footer)
        .build()
}