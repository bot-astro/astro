package space.astro.shared.core.components.sentry

import io.sentry.Hint
import io.sentry.SentryEvent
import io.sentry.SentryOptions
import org.springframework.stereotype.Component
import java.util.concurrent.RejectedExecutionException

@Component
class CustomBeforeSendCallback : SentryOptions.BeforeSendCallback {
    override fun execute(event: SentryEvent, hint: Hint): SentryEvent? {
        val e = event.throwable
        if (e is RejectedExecutionException) {
            return null
        }

        return event
    }
}