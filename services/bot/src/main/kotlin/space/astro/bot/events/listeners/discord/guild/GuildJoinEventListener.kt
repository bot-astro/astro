package space.astro.bot.events.listeners.discord.guild

import dev.minn.jda.ktx.coroutines.await
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.ui.Messages
import space.astro.shared.core.lang.L
import space.astro.shared.core.models.analytics.GuildEventData
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.milliseconds

private val log = KotlinLogging.logger { }

@Component
class GuildJoinEventListener(
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    private companion object {
        // the audit log entry can show up a bit after the join event
        val AUDIT_LOG_ATTEMPT_DELAYS = listOf(2_000L, 4_000L, 8_000L)

        // an older BOT_ADD entry targeting Astro belongs to a previous join of the bot
        val MAX_ENTRY_AGE: Duration = Duration.ofMinutes(2)
        const val AUDIT_LOG_ENTRIES_LIMIT = 10

        val IGNORED_ERROR_RESPONSES =
            setOf(
                ErrorResponse.CANNOT_SEND_TO_USER,
                ErrorResponse.UNKNOWN_GUILD,
                ErrorResponse.UNKNOWN_USER,
                ErrorResponse.MISSING_ACCESS,
                ErrorResponse.MISSING_PERMISSIONS,
            )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }

    // The welcome message in the guild is disabled: spam-check bots flag it and kick Astro within seconds of joining
    @EventListener
    fun guildJoinReceived(event: GuildJoinEvent) {
        val guild = event.guild

        applicationEventPublisher.publishEvent(
            GuildEventData(
                guildId = guild.idLong,
                usersCount = guild.memberCount,
                locale = guild.locale.locale,
                action = GuildEventData.GuildEventAction.JOINED,
                timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString(),
            ),
        )

        scope.launch {
            try {
                sendInviterOnboarding(event.jda, guild)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorResponseException) {
                if (e.errorResponse in IGNORED_ERROR_RESPONSES) {
                    log.debug { "Inviter onboarding skipped in guild ${guild.id}: ${e.meaning}" }
                } else {
                    log.warn(e) { "Failed to send the inviter onboarding in guild ${guild.id}" }
                }
            } catch (e: Exception) {
                log.warn(e) { "Failed to send the inviter onboarding in guild ${guild.id}" }
            }
        }
    }

    private suspend fun sendInviterOnboarding(
        jda: JDA,
        guild: Guild,
    ) {
        val inviterId = findInviterId(jda, guild) ?: return

        val inviter = jda.retrieveUserById(inviterId).await()
        if (inviter.isBot) {
            return
        }

        // kicked or left while looking for the inviter
        if (jda.getGuildById(guild.idLong) == null) {
            return
        }

        val locale = L.closestSupportedLocale(guild.locale.locale)
        val privateChannel = inviter.openPrivateChannel().await()
        privateChannel.sendMessage(Messages.inviterOnboarding(guild, locale)).await()
    }

    /**
     * Id of the user that added Astro, null if the bot can't see the audit log or no recent entry shows up
     */
    private suspend fun findInviterId(
        jda: JDA,
        guild: Guild,
    ): Long? {
        for (attemptDelay in AUDIT_LOG_ATTEMPT_DELAYS) {
            delay(attemptDelay.milliseconds)

            if (jda.getGuildById(guild.idLong) == null || !guild.selfMember.hasPermission(Permission.VIEW_AUDIT_LOGS)) {
                return null
            }

            val now = OffsetDateTime.now(ZoneOffset.UTC)
            val entry =
                guild
                    .retrieveAuditLogs()
                    .type(ActionType.BOT_ADD)
                    .limit(AUDIT_LOG_ENTRIES_LIMIT)
                    .await()
                    .firstOrNull { it.targetIdLong == jda.selfUser.idLong && Duration.between(it.timeCreated, now) <= MAX_ENTRY_AGE }

            if (entry != null) {
                return entry.userIdLong
            }
        }

        return null
    }
}
