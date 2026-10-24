package space.astro.bot.utils.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Icon
import java.net.URI

/**
 * Downloads the image at [url] and builds a JDA [Icon] from it
 *
 * @throws java.io.IOException if the image cannot be downloaded or read
 */
suspend fun iconFromUrl(url: String): Icon =
    withContext(Dispatchers.IO) {
        URI(url).toURL().openStream().use { Icon.from(it) }
    }
