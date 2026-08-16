package space.astro.shared.core.components.managers

import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import space.astro.shared.core.models.database.GeneratorData
import space.astro.shared.core.models.database.TemporaryVCData
import space.astro.shared.core.models.database.VCState
import space.astro.shared.core.util.extention.capitalize
import space.astro.shared.core.util.ui.TextUtils
import java.text.SimpleDateFormat
import java.util.*

object VariablesManager {
    private val premiumVariablesRegex = """\{(n|nato|roman|activity_[^\s\\}]+)}""".toRegex()

    private object Alphabets {
        val nateAlphabet = listOf(
            "Alpha", "Bravo", "Charlie", "Delta", "Echo",
            "Foxtrot", "Golf", "Hotel", "India", "Juliet",
            "Kilo", "Lima", "Mike", "November", "Oscar",
            "Papa", "Quebec", "Romeo", "Sierra", "Tango",
            "Uniform", "Victor", "Whiskey", "X-Ray", "Yankee",
            "Zulu",
            "Alpha 2", "Bravo 2", "Charlie 2", "Delta 2", "Echo 2",
            "Foxtrot", "Golf", "Hotel", "India", "Juliet 2",
            "Kilo", "Lima", "Mike", "November", "Oscar 2",
            "Papa", "Quebec", "Romeo", "Sierra", "Tango 2",
            "Uniform", "Victor", "Whiskey", "X-Ray 2", "Yankee 2",
            "Zulu",
        )

        val romanNumbers = listOf(
            "I", "II", "III", "IV", "V",
            "VI", "VII", "VIII", "XI", "X",
            "XI", "XII", "XIII", "XIV", "XV",
            "XVI", "XVII", "XVIII", "XIX", "XX",
            "XXI", "XXII", "XXIII", "XXIV", "XXV",
            "XXVI", "XXVII", "XXVII", "XXIX", "XXX",
            "XXXI", "XXXII", "XXXIII", "XXXIV", "XXXV",
            "XXXVI", "XXXVII", "XXXVIII", "XXXIX", "XL",
            "XLI", "XLII", "XLIII", "XLIV", "XLV",
            "XLVI", "XLVII", "XLVIII", "XLIX", "L",
        )
    }

    @Suppress("UNUSED")
    private object Variables {
        val owner = listOf("{nickname}", "{username}", "{mention}", "{id}")
        val activity = listOf("{activity_name}", "{activity_emoji}", "{activity_start_time}", "{activity_end_time}", "{activity_type}", "{activity_link}")
        val voiceChannel = listOf("{vc_name}", "{vc_bitrate}", "{vc_userlimit}", "{vc_users}", "{vc_id}", "{vc_mention}")
        val incremental = listOf("{n}", "{nato}", "{roman}")
    }

    private object Parsers {
        fun owner(template: String, owner: Member?): String {
            return template
                .replace("{nickname}", owner?.effectiveName ?: "", true)
                .replace("{username}", owner?.user?.name ?: "", true)
                .replace("{mention}", owner?.asMention ?: "", true)
                .replace("{id}", owner?.id ?: "", true)
        }

        fun activity(template: String, owner: Member?): String {
            val activity = owner?.activities?.firstOrNull { it.type != Activity.ActivityType.CUSTOM_STATUS }

            return if (activity == null) {
                template
                    .replace("{activity_name}", "", true)
                    .replace("{activity_emoji}", "", true)
                    .replace("{activity_start_time}", "", true)
                    .replace("{activity_end_time}", "", true)
                    .replace("{activity_type}", "", true)
                    .replace("{activity_link}", "", true)
            } else {
                val activityTimeFormat = SimpleDateFormat("HH:mm:ss")

                val activityName = activity.name
                val activityEmoji = activity.emoji
                    ?.takeIf { it.type == Emoji.Type.UNICODE }
                    ?.name
                    ?: ""

                val activityStartTime = activity.timestamps
                    ?.start
                    ?.takeIf { it != 0L }
                    ?.let { activityTimeFormat.format(Date(it)) }
                    ?: ""

                val activityEndTime = activity.timestamps
                    ?.end
                    ?.takeIf { it != 0L }
                    ?.let { activityTimeFormat.format(Date(it)) }
                    ?: ""

                val activityType = activity.type
                val activityTypeName = activityType
                    .takeIf { it != Activity.ActivityType.CUSTOM_STATUS }
                    ?.name
                    ?.capitalize()
                    ?: ""

                val activityLink = activity.url ?: ""

                template
                    .replace("{activity_name}", activityName, true)
                    .replace("{activity_emoji}", activityEmoji, true)
                    .replace("{activity_start_time}", activityStartTime, true)
                    .replace("{activity_end_time}", activityEndTime, true)
                    .replace("{activity_type}", activityTypeName, true)
                    .replace("{activity_link}", activityLink, true)
            }
        }

        fun vcCreation(template: String, bitrate: Int, userLimit: Int): String {
            return template
                .replace("{vc_bitrate}", (bitrate / 1000).toString(), true)
                .replace("{vc_userlimit}", userLimit.takeIf { it > 0 }?.toString() ?: "no limit", true)
                .replace("{vc_users}", "1", true)
        }

        fun vc(template: String, vc: VoiceChannel): String {
            return template
                .replace("{vc_name}", vc.name, true)
                .replace("{vc_bitrate}", (vc.bitrate / 1000).toString(), true)
                .replace("{vc_userlimit}", vc.userLimit.takeIf { it > 0 }?.toString() ?: "no limit", true)
                .replace("{vc_users}", vc.members.size.toString(), true)
                .replace("{vc_id}", vc.id, true)
                .replace("{vc_mention}", vc.asMention, true)
        }

        fun position(template: String, incrementalPosition: Int) : String {
            return template
                .replace("{n}", (incrementalPosition).toString(), true)
                .replace("{nato}", Alphabets.nateAlphabet[incrementalPosition - 1], true)
                .replace("{roman}", Alphabets.romanNumbers[incrementalPosition - 1], true)
        }
    }

    private object Formatters {
        fun formatChannelNameLength(name: String): String {
            var formatted = name.take(100)
            if (formatted.length < 2)
                formatted += "-".repeat(2)

            return formatted
        }
    }

    object Checkers {
        fun containsPremiumVariable(name: String): Boolean {
            return premiumVariablesRegex.containsMatchIn(name)
        }

        fun containsBadwords(name: String): Boolean {
            return name.split(" ").any { TextUtils.badwords.contains(it.lowercase().trim()) }
        }
    }


    fun getNameTemplateForCreation(generatorData: GeneratorData) = when (generatorData.initialState) {
        VCState.UNLOCKED -> generatorData.defaultName
        VCState.UNHIDDEN -> generatorData.defaultName
        VCState.LOCKED -> generatorData.defaultLockedName
        VCState.HIDDEN -> generatorData.defaultHiddenName
    } ?: generatorData.defaultName

    fun getNameTemplateForRefresh(temporaryVCData: TemporaryVCData, generatorData: GeneratorData) = when (temporaryVCData.state) {
        VCState.UNLOCKED -> generatorData.defaultName
        VCState.UNHIDDEN -> generatorData.defaultName
        VCState.LOCKED -> generatorData.defaultLockedName
        VCState.HIDDEN -> generatorData.defaultHiddenName
    } ?: generatorData.defaultName

    fun doesTemplateRequireVCPositionalData(name: String) =
        Variables.incremental.any { it in name.lowercase() }

    fun computeVcNameForCreation(
        template: String,
        owner: Member,
        bitrate: Int,
        userLimit: Int,
        incrementalPosition: Int?
    ): String {
        var name = Parsers.owner(template, owner)
            .let { Parsers.activity(it, owner) }
            .let { Parsers.vcCreation(it, bitrate, userLimit) }

        incrementalPosition?.also {
            name = Parsers.position(name, it)
        }

        return Formatters.formatChannelNameLength(name)
    }

    fun computeVcNameForExisting(
        template: String,
        owner: Member?,
        temporaryVC: VoiceChannel,
        incrementalPosition: Int?
    ): String {
        var name = Parsers.owner(template, owner)
            .let { Parsers.activity(it, owner) }
            .let { Parsers.vc(it, temporaryVC) }

        incrementalPosition?.also {
            name = Parsers.position(name, it)
        }

        return Formatters.formatChannelNameLength(name)
    }


    fun computePrivateChatName(
        template: String,
        owner: Member?,
        temporaryVC: VoiceChannel
    ): String {
        return Parsers.owner(template, owner)
            .let { Parsers.activity(it, owner) }
            .let { Parsers.vc(it, temporaryVC) }
            .let {
                Formatters.formatChannelNameLength(it)
            }
    }

    fun computeWaitingRoomName(
        template: String,
        owner: Member?,
        temporaryVC: VoiceChannel,
        incrementalPosition: Int?
    ): String {
        @Suppress("DUPLICATE")
        var name = Parsers.owner(template, owner)
            .let { Parsers.activity(it, owner) }
            .let { Parsers.vc(it, temporaryVC) }

        incrementalPosition?.also {
            name = Parsers.position(name, it)
        }

        return Formatters.formatChannelNameLength(name)
    }

    fun computeChatMessage(
        template: String,
        owner: Member,
        temporaryVC: VoiceChannel
    ): String {
        var name = Parsers.owner(template, owner)
            .let { Parsers.activity(it, owner) }
            .let { Parsers.vc(it, temporaryVC) }

        if (name.length < 2)
            name += "-".repeat(2)

        return name.take(2000)
    }
}