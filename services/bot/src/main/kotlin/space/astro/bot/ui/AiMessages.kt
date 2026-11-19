package space.astro.bot.ui

import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.container.ContainerChildComponent
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import space.astro.bot.components.ai.AiDiffEntry
import space.astro.bot.components.ai.AiProposal
import space.astro.bot.components.ai.AiTurnResponse
import space.astro.shared.core.lang.L
import java.util.Locale

/**
 * Messages of the AI assistant threads
 */
object AiMessages {
    // Discord caps the text of all the text displays of a message together
    private const val TEXT_BUDGET = 3900
    private const val MIN_REPLY_TEXT = 100
    private const val QUOTE_MAX = 200
    private const val PROPOSAL_MAX = 600
    private const val PROPOSAL_SUMMARY_MAX = 150
    private const val PROPOSAL_FOOTER_RESERVE = 60
    private const val MAX_PROPOSALS = 5
    private const val MAX_DIFF_LINES = 8
    private const val DIFF_VALUE_MAX = 80

    /**
     * First message of a thread opened without a prompt
     */
    fun welcome(locale: Locale): MessageCreateData =
        Containers.message(
            Containers.default(L.t(locale, "ai.thread.welcome", "emoji" to Emojis.chat.formatted)),
            ActionRow.of(Buttons.aiMessage(locale)),
        )

    fun error(
        locale: Locale,
        key: String,
    ): MessageCreateData = Containers.message(Containers.error(L.t(locale, key)))

    /**
     * Answer of a turn: the text, a card with Approve / Cancel for every proposal and the button to keep talking
     *
     * [quote] is the message of the user, shown on top when it is not visible in the chat
     */
    fun reply(
        locale: Locale,
        response: AiTurnResponse,
        premium: Boolean,
        quote: String?,
    ): MessageCreateData {
        if (response.limitReached) {
            val key = if (premium) "ai.limit_reached.premium" else "ai.limit_reached.free"
            val container = Containers.default(L.t(locale, key, "emoji" to Emojis.premium.formatted))

            return if (premium) {
                Containers.message(container)
            } else {
                Containers.message(container, ActionRow.of(Buttons.ultimate(locale)))
            }
        }

        val proposals = response.proposals.take(MAX_PROPOSALS)
        val quoteText = quote?.let { "> ${fit(sanitize(it).replace(WHITESPACE, " "), QUOTE_MAX)}" }
        val textBudget =
            (TEXT_BUDGET - proposals.size * (PROPOSAL_MAX + PROPOSAL_FOOTER_RESERVE) - (quoteText?.length ?: 0))
                .coerceAtLeast(MIN_REPLY_TEXT)
        val text =
            response.text
                .takeIf { it.isNotBlank() }
                ?.let { fit(sanitize(it.trim()), textBudget) }
                ?: if (proposals.isEmpty()) L.t(locale, "ai.reply.empty") else null
        val body = listOfNotNull(quoteText, text).joinToString("\n\n")

        val components = mutableListOf<MessageTopLevelComponent>()
        if (body.isNotEmpty()) {
            components += Containers.default(body)
        }
        proposals.forEach { components += proposal(locale, it) }
        components += ActionRow.of(Buttons.aiMessage(locale))

        return Containers.message(*components.toTypedArray())
    }

    private fun proposal(
        locale: Locale,
        proposal: AiProposal,
    ): Container {
        val children = mutableListOf<ContainerChildComponent>(Containers.text(proposalText(locale, proposal)))

        proposal.expiresAtEpochSeconds()?.let {
            children += Containers.footer(L.t(locale, "ai.reply.expires", "time" to "<t:$it:R>"))
        }
        children += ActionRow.of(Buttons.aiApprove(locale, proposal.id), Buttons.aiCancel(locale, proposal.id))

        return Containers.container(Containers.purple, *children.toTypedArray())
    }

    internal fun proposalText(
        locale: Locale,
        proposal: AiProposal,
    ): String {
        val lines =
            proposal.diff
                .take(MAX_DIFF_LINES)
                .map { diffLine(locale, it) }
                .toMutableList()
        if (proposal.diff.size > MAX_DIFF_LINES) {
            lines += L.t(locale, "ai.proposal.more_changes", "count" to proposal.diff.size - MAX_DIFF_LINES)
        }

        val title = "### ${fit(sanitize(proposal.summary), PROPOSAL_SUMMARY_MAX)}"

        return fit((listOf(title) + lines).joinToString("\n"), PROPOSAL_MAX)
    }

    private fun diffLine(
        locale: Locale,
        entry: AiDiffEntry,
    ): String {
        val empty = L.t(locale, "ai.proposal.empty_value")
        val before = entry.before?.takeIf { it.isNotBlank() }?.let { fit(sanitize(it), DIFF_VALUE_MAX) } ?: empty
        val after = entry.after?.takeIf { it.isNotBlank() }?.let { fit(sanitize(it), DIFF_VALUE_MAX) } ?: empty

        return "**${sanitize(entry.field)}**: $before → $after"
    }

    /**
     * Removes the buttons of the proposal card that contains [buttonId] and shows [status] in their place,
     * everything else of the message stays as it is
     */
    fun resolveProposal(
        components: List<MessageTopLevelComponent>,
        buttonId: String,
        status: String,
    ): List<MessageTopLevelComponent> =
        components.map { component ->
            val isCard =
                component is Container &&
                    component.components.any { child ->
                        child is ActionRow && child.buttons.any { it.customId == buttonId }
                    }

            if (component is Container && isCard) {
                component.withComponents(component.components.filterNot { it is ActionRow } + Containers.footer(status))
            } else {
                component
            }
        }

    /**
     * Text from the model or the user can't ping anyone: a zero width space breaks every mention
     */
    internal fun sanitize(text: String): String = text.replace("@", "@​")

    internal fun fit(
        text: String,
        max: Int,
    ): String {
        if (text.length <= max) {
            return text
        }

        val cut = text.take(max - 1)

        // never leave half of an emoji
        return (if (cut.last().isHighSurrogate()) cut.dropLast(1) else cut) + "…"
    }

    private val WHITESPACE = Regex("\\s+")
}
