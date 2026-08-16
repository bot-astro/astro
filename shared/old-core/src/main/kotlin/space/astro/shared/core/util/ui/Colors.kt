package space.astro.shared.core.util.ui

import java.awt.Color
import kotlin.random.Random

/**
 * Utility class for Astro most used colors
 */
object Colors {
    val rose: Color = Color.decode("#E24462")

    /**
     * Primary color
     */
    val purple: Color = Color.decode("#B125EA")
    val purpleBlue: Color = Color.decode("#7F52FF")

    val green: Color = Color.decode("#43B581")
    val yellow: Color = Color.decode("#FFD56C")
    val red: Color = Color.decode("#F04747")
    val gray: Color = Color.decode("#697380")

    /**
     * @return completely random [Color]
     */
    fun random(): Color {
        val random = Random
        return Color(random.nextInt(256), random.nextInt(256), random.nextInt(256))
    }
}