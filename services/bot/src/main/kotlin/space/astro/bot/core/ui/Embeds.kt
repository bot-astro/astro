package space.astro.bot.core.ui

import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
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

    fun helpGeneral(
        shardId: Int,
        shardCount: Int,
        podId: Int,
        podCount: Int
    ) = Embed {
        color = Colors.purple.rgb
        title = "${Emojis.help.formatted} Help - General"
        description = "Astro is the most complete and unique bot for temporary voice channels and voice roles!" +
                "\n\n**Server requirements**" +
                "\n> • Your server must not have more than 50 bots, this is a limit set by Discord" +
                "\n> • You need the `Manage channels` permission to configure Astro for your server" +
                "\n\n**Bot permissions**" +
                "\n> Astro requires `Administrator` permissions by default to work and ensure a great user experience for the average Discord users." +
                "\n> " +
                "\n> If you own a professional server and cannot give that permission to Astro for security reasons, you can disable this requirement via the command `/settings admin-permission`." +
                "\n> You may ask for guidelines in the [Support Server](${Links.SUPPORT_SERVER}) regarding the permissions that Astro needs in order to work, but in depth support is not provided for that." +
                "\n\n**Command permissions**" +
                "\n> To manage which roles or users can use specific Astro commands you can use [Discord command permissions](${Links.ExternalGuides.COMMAND_PERMISSIONS})." +
                "\n\n**See reported errors**" +
                "\n> You can view all the errors that Astro encountered when working in your server with the command `/settings errors`." +
                "\n> If something doesn't seem to work properly always make sure to check those errors to make sure the issue is not on your end."
        footer {
            name = "For help regarding other features of Astro see the buttons below or /help commands • shard $shardId/$shardCount • pod $podId/$podCount"
        }
    }

    val helpPremium = Embed {
        color = Colors.purple.rgb
        title = "${Emojis.premium.formatted} Help - Premium"
        description = "If you own a professional server Astro premium is exactly what you need to take your voice channel system to the next level!" +
                "\n\n**Features**" +
                "\n> • Unlimited generators (instead of 3)" +
                "\n> • Unlimited interfaces (instead of 1)" +
                "\n> • Unlimited templates (instead of 3)" +
                "\n> • Unlimited connections (instead of 1)" +
                "\n> • Enable fallback generators" +
                "\n> • Use premium variables in voice channel names" +
                "\n> • Edit message and buttons of interfaces" +
                "\n> • Prevent badwords from being used in voice channel names (english only)" +
                "\n> • Enable waiting rooms and private text chats for each voice channel" +
                "\n> • Specific role for voice channel owners" +
                "\n> • Send custom messages or interfaces on voice channel creation" +
                "\n> • Dedicated support in the [Support Server](${Links.SUPPORT_SERVER})" +
                "\n\n**Get premium**" +
                "\n> You can get premium for your server directly on Discord via [the App Directory](${Links.APP_DIRECTORY_PREMIUM})." +
                "\n\n**FAQ**" +
                "\n> Premium is completely managed by Discord, check out [their FAQ](${Links.ExternalGuides.PREMIUM_FAQ}) for any other question." +
                "\n\n**Old subscribers**" +
                "\n> If you are a long time subscriber that got premium trough the Astro website, you can manage your subscription [at this link](${Links.OLD_PREMIUM})."
        footer {
            name = "For help regarding other features of Astro see the buttons below or /help commands"
        }
    }

    val helpVariables = Embed {
        color = Colors.purple.rgb
        title = "${Emojis.variables.formatted} Help - Variables"
        description = "Channel names and messages configured via Astro can make use of variables to be dynamic." +
                "\nHere is a list of all the variables that Astro supports" +
                "\n\n**Voice channel owner**" +
                "\n> `{nickname}` • The nickname (or username if missing) of the owner" +
                "\n> `{username}` • The username of the owner" +
                "\n> `{mention}` • The mention (@name) of the owner" +
                "\n> `{id}` • The ID of the owner" +
                "\n> `{activity_name}` • The name of the activity of the owner (the game name for example)" +
                "\n> `{activity_emoji}` • An emoji representing the activity" +
                "\n> `{activity_start_time}` • The time at which the owner started the activity" +
                "\n> `{activity_end_time}` • The time at which the activity will end or has ended" +
                "\n> `{activity_type}` • The type of the activity (playing, watching, listening, etc...)" +
                "\n> `{activity_link}` • The link of the twitch stream if the activity is a twitch stream" +
                "\n\n**Voice channel properties**" +
                "\n> `{vc_name}` • The name of the vc" +
                "\n> `{vc_bitrate}` • The bitrate of the vc" +
                "\n> `{vc_userlimit}` • The user limit of the vc" +
                "\n> `{vc_users}` • The amount of users in the vc" +
                "\n> `{vc_id}` • The id of the vc" +
                "\n> `{vc_mention}` • The vc as a mention (#voice-channel)" +
                "\n\n**Incremental values** • ${Emojis.premium.formatted} Premium" +
                "\n> `{n}` • A simple incremental number (1, 2, 3...)" +
                "\n> `{nato}` • Nato incremental alphabet (Alpha, Bravo, Charlie...)" +
                "\n> `{roman}` • Roman incremental numbers (I, II, III, IV...)" +
                "\n> " +
                "\n> **How do incremental values work?**" +
                "\n> Considering the new Discord Limitation which permits to change a voice channel name only 2 times per every 10 minutes, this feature works a bit differently that what you might expect:" +
                "\n> " +
                "\n> The voice channels will be generated incrementally: the bot will find the first missing voice channel and put the new voice channel in its place." +
                "\n> " +
                "\n> Example: We have 5 VCs generated by Astro incrementally [VC #1, VC#2, VC #3, VC #4 and VC#5], now let's say VC #3 gets deleted, what Astro will **not do** is rename every single VC to the proper name, so VC#4 to VC #3 and so on, **instead** the next time a VC will be created it will not be VC #6 but VC #3 so that the gap gets filled." +
                "\n\n**Why sometimes channel names are not updated?**" +
                "\n> This is because of the Discord limitation described above (voice channel name can be changed only 2 times per every 10 minutes)." +
                "\n> Because of that voice channel names may not be always precise if frequent changes occur." +
                "\n\n**Examples**" +
                "\n> Here is a simple example on how you could make use of variables in some common configuration commands:" +
                "\n> ```" +
                "\n> /generator vc name name: {nato} - {nickname} VC" +
                "\n> ```" +
                "\n> ```" +
                "\n> /generator chat name name: Chat for {vc_name}" +
                "\n> ```" +
                "\n> ```" +
                "\n> /generator-waiting name name: Waiting room for {vc_name}" +
                "\n> ```"
        footer {
            name = "For help regarding other features of Astro see the buttons below or /help commands"
        }
    }

    val helpGenerators = Embed {
        color = Colors.purple.rgb
        title = "${Emojis.generator.formatted} Help - Generators"
        description = "A generator is a voice channel that when joined by a user creates a temporary voice channel for that user." +
                "\nThe temporary voice channel will then be deleted when empty or filled only with bots." +
                "\n\n**Creating a generator**" +
                "\n> Create a generator via `/generator create`." +
                "\n> You can edit anything you need in the generator, the name, the category, etc..." +
                "\n\n**Voice channel settings**" +
                "\n> Here is the list of commands that you can use to change the settings of the generated temporary voice channels:" +
                "\n> `/generator vc category`" +
                "\n> `/generator vc position`" +
                "\n> `/generator vc limit`" +
                "\n> `/generator vc bitrate`" +
                "\n> `/generator vc region`" +
                "\n\n**Voice channel name**" +
                "\n> Use `/generator vc name` to change the default name for voice channels." +
                "\n> See `/help variables` for dynamic names." +
                "\n> That command also accepts a `state`, meaning you can have different names depending on the state (default, locked or hidden)." +
                "\n> " +
                "\n> You can also configure when a channel should be renamed with the command `/generator vc rename-conditions`." +
                "\n> These are the available options:" +
                "\n> • `state-change` • update when the channel gets unlocked, locked or hidden" +
                "\n> • `owner-change` • update when the owner of the channel changes" +
                "\n> • `renamed` • update even if it has already been manually renamed by the owner with `/name`" +
                "\n> • `activity-change` • update when the activity of the owner of the channel changes" +
                "\n\n**Voice channel permissions**" +
                "\n> Voice channel inherit all permissions by the generator by default. You can change this behaviour using `/generator vc permissions`." +
                "\n> " +
                "\n> That command also asks for a `target role`, which can be considered the main role of your server. It will be the role that gets modified when the channel gets locked, hidden, etc..." +
                "\n> " +
                "\n> You can also provide a `moderator role` which will be immune to all commands (`/ban` and `/lock` for example) and will always be able to claim ownership of the channels (`/claim`)." +
                "\n\n**Text chat settings**" +
                "\n> Astro can send a message in the integrated text chat of the temporary voice channels automatically when they get created." +
                "\n> You can configure the message with `/generator chat message`, it can include variables and you can even send an Interface in them!" +
                "\n> " +
                "\n> Astro can also create a separate text channel for each temporary voice channel created. Those separate channels are only visible to the users inside the related voice channel and will get deleted when the voice channel gets deleted." +
                "\n> Here are the commands to customise those text channels:" +
                "\n> `/generator chat category`" +
                "\n> `/generator chat name` • See `/help variables` for dynamic names" +
                "\n> `/generator chat nsfw`" +
                "\n> `/generator chat slowmode`" +
                "\n> `/generator chat topic`" +
                "\n> `/generator chat permissions`" +
                "\n\n**Ownership settings**" +
                "\n> `/generator owner permissions` • specify permissions for voice channel owners" +
                "\n> `/generator owner role` • configure a role that gets given to all voice channel owners" +
                "\n\n**Queue mode**" +
                "\n> With `/generator vc queue` enabled, Astro will try to fill existing channels before creating new ones." +
                "\n> Useful feature when channels have a user limit set and you need to make sure that channels are all filled with the maximum amount of users." +
                "\n\n**Fallback generator**" +
                "\n> When the generator category fills up (gets to 50 channels) users that join the generator will be moved to the fallback generator set with `/generator fallback-generator`." +
                "\n\n**Resetting temporary voice channels**" +
                "\n> If Astro is not generating correct names for temporary voice channels, for example when using incremental variables, run the `/settings reset-temporary-voice-channels` command."
        footer {
            name = "For help regarding other features of Astro see the buttons below or /help commands"
        }
    }

    val helpInterfaces = Embed {
        color = Colors.purple.rgb
        title = "${Emojis.vcInterface.formatted} Help - Interfaces"
        description = "An interface is a message that allows users to manage their voice channel quickly via simple buttons." +
                "\n\n**Creating interfaces**" +
                "\n> Use the command `/interface create` to create a new interface." +
                "\n> It will ask for a channel and you can choose the one you want." +
                "\n\n**Changing channel or re-sending an interface**" +
                "\n> If you mistakenly deleted an interface message or simply wanna change the channel use `/interface edit channel`." +
                "\n\n**Adding, modifying and removing buttons**" +
                "\n> You can add and remove buttons from specific interfaces using the command `/interface add button` and `/interface remove button`." +
                "\n> To modify a specific button use `/interface edit button`." +
                "\n\n**Modifying the order of the buttons**" +
                "\n> Use `/interface edit button-order` to change the order of the buttons. That command also has an `automatic` option to automatically arrange buttons." +
                "\n\n**Deleting an interface**" +
                "\n> You can delete an interface at any time via `/interface delete`." +
                "\n\n**Example of custom interface**" +
                "\n> The image below is an example of a custom interface:"
        image = Links.EXAMPLE_CUSTOM_INTERFACE
        footer {
            name = "For help regarding other features of Astro see the buttons below or /help commands"
        }
    }

    val helpTemplates = Embed {
        color = Colors.purple.rgb
        title = "${Emojis.template.formatted} Help - Templates"
        description = "Templates encapsulate a set of voice channel settings that can be applied all at once on temporary voice channels." +
                "\n\nThis is useful for example in gaming servers where users might be playing many different games." +
                "\nHaving a template for each game would allow to make the voice channel more fitting to that game, for example with a specific name or user limit." +
                "\n\n**Using templates**" +
                "\n> Users can use templates either via `/template` which provides autocompletion for which templates are available, and via the apposite ${Emojis.template.formatted} interface button." +
                "\n\n**Creating and editing templates**" +
                "\n> You can create a template with `/template create`." +
                "\n> Each template has a name, which is the recognizable name of the template that users will end up using." +
                "\n> You can then customise the voice channel settings of that template with all the `/template edit`... commands." +
                "\n\n**Allow templates on specific generators**" +
                "\n> To make a template usable only in channels created by a specific generator use `/template enabled-generators`." +
                "\n\n**Deleting templates**" +
                "\n> Delete a template at any time via `/template delete`"
        footer {
            name = "For help regarding other features of Astro see the buttons below or /help commands"
        }
    }

    val helpConnections = Embed {
        color = Colors.purple.rgb
        title = "${Emojis.connection.formatted} Help - Connections"
        description = "Connections allow to connect voice channels to roles." +
                "\nSo that when a user joins a specific voice channel he gets a specific role." +
                "\n\n**Channel**" +
                "\n> You can create a connection on any kind of channel and even categories." +
                "\n> You can also create a connection on a generator to achieve the connection behaviour for all temporary voice channels created by that generator." +
                "\n\n**Action**" +
                "\n> Every connection has a specific action, which can be one of the following:" +
                "\n> `Assign` • assigns the role when joining the voice channel" +
                "\n> `Remove` • removes the role when joining the voice channel" +
                "\n> `Toggle` • `Assign` when the user doesn't have the role, and `Remove` otherwise" +
                "\n> " +
                "\n> Every action can also be marked as `permanent`, when that is the case Astro will not revert it back when the user leaves the voice channel" +
                "\n\n**Creating connections**" +
                "\n> To create a connection use `/connection create`." +
                "\n\n**Editing a connection**" +
                "\n> You can edit the channel, role or action of a connection with `/connection edit`." +
                "\n\n**Deleting a connection**" +
                "\n> To delete a connection use `/connection delete`." +
                "\n\n**Example**" +
                "\n> In the example below the user gets a specific role when joining the voice channel that allows him to see some hidden channels."
        image = Links.EXAMPLE_CONNECTION
        footer {
            name = "For help regarding other features of Astro see the buttons below or /help commands"
        }
    }

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