package space.astro.shared.core.util.extention

/**
 * Checks whether this [String] is a valid Discord snowflake
 */
fun String.isValidSnowflake() = (this.length == 19 || this.length == 18 || this.length == 17) && this.toLongOrNull() != null

/**
 * Returns a user mention from a string id: <@string>
 */
fun String.asUserMention() = "<@$this>"

/**
 * Returns a role mention from a string id: <@&string>
 */
fun String.asRoleMention() = "<@&$this>"

/**
 * Returns a channel mention from a string id: <#string>
 */
fun String.asChannelMention() = "<#$this>"

/**
 * Returns a markdown link with this String as its name
 */
fun String.linkFromName(url: String) = "[$this]($url)"

/**
 * Returns a markdown link with this String as its link
 */
fun String.linkFromLink(name: String) = "[$name]($this)"

/**
 * Returns a relative timestamp from a long: <t:long:style>
 */
fun Long.asTimestamp(style: Char = 'f') = "<t:${(this / 1000)}:$style>"

/**
 * Returns a relative timestamp from a long: <t:long:R>
 */
fun Long.asRelativeTimestamp() = asTimestamp('R')

/**
 * Returns a relative timestamp from a long and the current time: <t:current-time + long:R>
 */
fun Long.asRelativeTimestampFromNow() = "<t:${(System.currentTimeMillis() + this) / 1000}:R>"

/**
 * Creates a link with this String as the name and a message link as the value: https://discord.com/channels/$guildId/$channelId/$messageId
 */
fun String.asMessageMarkdownLink(guildId: String, channelId: String, messageId: String) = "[$this](https://discord.com/channels/$guildId/$channelId/$messageId)"