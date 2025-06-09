package dev.jamiecraane.scorchedearth.weather

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders weather effects on the game canvas.
 */
object WeatherRenderer {
    /**
     * Renders weather effects based on the current weather type.
     * @param drawScope The DrawScope to draw on
     * @param weatherManager The WeatherManager containing weather state
     * @param wind The current wind value (negative = left, positive = right)
     */
    fun render(drawScope: DrawScope, weatherManager: WeatherManager, wind: Float) {
        when (weatherManager.weatherTypeState) {
            WeatherType.RAIN -> renderRain(drawScope, weatherManager, wind)
            else -> {} // No rendering for other weather types
        }
    }

    /**
     * Renders rain drops.
     * @param drawScope The DrawScope to draw on
     * @param weatherManager The WeatherManager containing rain state
     * @param wind The current wind value (negative = left, positive = right)
     */
    private fun renderRain(drawScope: DrawScope, weatherManager: WeatherManager, wind: Float) {
        val rainColor = Color(0x99AADDFF) // Semi-transparent light blue

        for (drop in weatherManager.rainDrops) {
            // Use the raindrop's angle property which represents its fall direction
            val angle = drop.angle

            // Calculate the end point of the main raindrop based on its length and angle
            val endX = drop.position.x + cos(angle) * drop.length
            val endY = drop.position.y + sin(angle) * drop.length

            // Draw the rain drop as a line
            drawScope.drawLine(
                color = rainColor,
                start = drop.position,
                end = Offset(endX, endY),
                strokeWidth = 1.5f
            )
        }
    }
}
