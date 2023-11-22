package space.astro.bot.listeners.member

import net.dv8tion.jda.api.events.user.update.UserUpdateActivitiesEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class MemberUpdateActivitiesEventListener {

    @EventListener
    fun receiveMemberUpdateActivitiesEvent(event: UserUpdateActivitiesEvent) {
        // TODO: if in premium guild update voice channel name from variables
    }
}