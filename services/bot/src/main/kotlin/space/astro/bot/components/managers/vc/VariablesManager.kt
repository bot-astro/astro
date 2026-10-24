package space.astro.bot.components.managers.vc

import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import space.astro.shared.core.lang.L
import space.astro.shared.core.models.database.guildSettings.GeneratorSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VariablesManager {
    private val natoAlphabet =
        listOf(
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

    private val romanNumbers =
        listOf(
            "I", "II", "III", "IV", "V",
            "VI", "VII", "VIII", "IX", "X",
            "XI", "XII", "XIII", "XIV", "XV",
            "XVI", "XVII", "XVIII", "XIX", "XX",
            "XXI", "XXII", "XXIII", "XXIV", "XXV",
            "XXVI", "XXVII", "XXVIII", "XXIX", "XXX",
            "XXXI", "XXXII", "XXXIII", "XXXIV", "XXXV",
            "XXXVI", "XXXVII", "XXXVIII", "XXXIX", "XL",
            "XLI", "XLII", "XLIII", "XLIV", "XLV",
            "XLVI", "XLVII", "XLVIII", "XLIX", "L",
        )

    private val incrementalVariables = listOf("{n}", "{nato}", "{roman}")

    fun doesTemplateRequireVCPositionalData(template: String) = incrementalVariables.any { it in template.lowercase() }

    fun getNameTemplateForCreation(generatorSettings: GeneratorSettings): String =
        when (generatorSettings.initialState) {
            GeneratorSettings.VCState.UNLOCKED, GeneratorSettings.VCState.UNHIDDEN -> generatorSettings.defaultName
            GeneratorSettings.VCState.LOCKED -> generatorSettings.defaultLockedName
            GeneratorSettings.VCState.HIDDEN -> generatorSettings.defaultHiddenName
        } ?: generatorSettings.defaultName

    fun computeVcNameForCreation(
        template: String,
        locale: Locale,
        owner: Member,
        bitrate: Int,
        userLimit: Int,
        incrementalPosition: Int?,
    ): String {
        var name =
            parseOwner(template, owner)
                .let { parseActivity(it, owner) }
                .let { parseVcCreation(it, locale, bitrate, userLimit) }

        incrementalPosition?.also { name = parsePosition(name, it) }

        return formatChannelNameLength(name)
    }

    fun computeChatMessage(
        template: String,
        locale: Locale,
        owner: Member,
        temporaryVC: VoiceChannel,
    ): String {
        val message =
            parseOwner(template, owner)
                .let { parseActivity(it, owner) }
                .let { parseVc(it, temporaryVC, locale) }

        return (if (message.length < 2) message + "-".repeat(2) else message).take(2000)
    }

    fun computeVcNameForExisting(
        template: String,
        locale: Locale,
        owner: Member?,
        temporaryVC: VoiceChannel,
        incrementalPosition: Int?,
    ): String {
        var name =
            parseOwner(template, owner)
                .let { parseActivity(it, owner) }
                .let { parseVc(it, temporaryVC, locale) }

        incrementalPosition?.also { name = parsePosition(name, it) }

        return formatChannelNameLength(name)
    }

    fun computePrivateChatName(
        template: String,
        locale: Locale,
        owner: Member?,
        temporaryVC: VoiceChannel,
    ): String =
        parseOwner(template, owner)
            .let { parseActivity(it, owner) }
            .let { parseVc(it, temporaryVC, locale) }
            .let { formatChannelNameLength(it) }

    fun computeWaitingRoomName(
        template: String,
        locale: Locale,
        owner: Member?,
        temporaryVC: VoiceChannel,
        incrementalPosition: Int?,
    ): String {
        var name =
            parseOwner(template, owner)
                .let { parseActivity(it, owner) }
                .let { parseVc(it, temporaryVC, locale) }

        incrementalPosition?.also { name = parsePosition(name, it) }

        return formatChannelNameLength(name)
    }

    private fun parseOwner(
        template: String,
        owner: Member?,
    ): String =
        template
            .replace("{nickname}", owner?.effectiveName ?: "", true)
            .replace("{username}", owner?.user?.name ?: "", true)
            .replace("{mention}", owner?.asMention ?: "", true)
            .replace("{id}", owner?.id ?: "", true)

    private fun parseActivity(
        template: String,
        owner: Member?,
    ): String {
        val activity = owner?.activities?.firstOrNull { it.type != Activity.ActivityType.CUSTOM_STATUS }
        val timeFormat = SimpleDateFormat("HH:mm:ss")

        val emoji = activity?.emoji?.takeIf { it.type == Emoji.Type.UNICODE }?.name
        val start = activity?.timestamps?.start?.takeIf { it != 0L }?.let { timeFormat.format(Date(it)) }
        val end = activity?.timestamps?.end?.takeIf { it != 0L }?.let { timeFormat.format(Date(it)) }
        val type = activity?.type?.name?.lowercase()?.replaceFirstChar { it.uppercase() }

        return template
            .replace("{activity_name}", activity?.name ?: "", true)
            .replace("{activity_emoji}", emoji ?: "", true)
            .replace("{activity_start_time}", start ?: "", true)
            .replace("{activity_end_time}", end ?: "", true)
            .replace("{activity_type}", type ?: "", true)
            .replace("{activity_link}", activity?.url ?: "", true)
    }

    private fun parseVcCreation(
        template: String,
        locale: Locale,
        bitrate: Int,
        userLimit: Int,
    ): String =
        template
            .replace("{vc_bitrate}", (bitrate / 1000).toString(), true)
            .replace("{vc_userlimit}", userLimit.takeIf { it > 0 }?.toString() ?: L.t(locale, "vc.variables.no_limit"), true)
            .replace("{vc_users}", "1", true)

    private fun parseVc(
        template: String,
        vc: VoiceChannel,
        locale: Locale,
    ): String =
        template
            .replace("{vc_name}", vc.name, true)
            .replace("{vc_bitrate}", (vc.bitrate / 1000).toString(), true)
            .replace("{vc_userlimit}", vc.userLimit.takeIf { it > 0 }?.toString() ?: L.t(locale, "vc.variables.no_limit"), true)
            .replace("{vc_users}", vc.members.size.toString(), true)
            .replace("{vc_id}", vc.id, true)
            .replace("{vc_mention}", vc.asMention, true)

    private fun parsePosition(
        template: String,
        incrementalPosition: Int,
    ): String =
        template
            .replace("{n}", incrementalPosition.toString(), true)
            .replace("{nato}", natoAlphabet.getOrElse(incrementalPosition - 1) { incrementalPosition.toString() }, true)
            .replace("{roman}", romanNumbers.getOrElse(incrementalPosition - 1) { incrementalPosition.toString() }, true)

    private fun formatChannelNameLength(name: String): String {
        val formatted = name.take(100)
        return if (formatted.length < 2) formatted + "-".repeat(2) else formatted
    }
}
