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
        println("[DEBUG_LOG] WeatherRenderer.render called, weatherType=${weatherManager.weatherTypeState}, hasLightning=${weatherManager.lightning != null}")

        when (weatherManager.weatherTypeState) {
            WeatherType.RAIN -> renderRain(drawScope, weatherManager, wind)
            WeatherType.LIGHTNING -> renderLightning(drawScope, weatherManager)
            else -> {
                println("[DEBUG_LOG] No weather effect to render")
            } // No rendering for other weather types
        }
    }

    /**
     * Renders lightning strikes.
     * @param drawScope The DrawScope to draw on
     * @param weatherManager The WeatherManager containing lightning state
     */
    private fun renderLightning(drawScope: DrawScope, weatherManager: WeatherManager) {
        println("[DEBUG_LOG] renderLightning called, weatherType=${weatherManager.weatherTypeState}")

        // Only render if there's an active lightning strike
        val lightning = weatherManager.lightning
        if (lightning == null) {
            println("[DEBUG_LOG] No active lightning to render")
            return
        }

        println("[DEBUG_LOG] Rendering lightning at x=${lightning.strikePosition.x}, spread=${lightning.spread}")

        // Lightning colors
        val lightningCoreColor = Color(0xFFFFFFFF) // Bright white core
        val lightningOuterColor = Color(0xAAB0E0FF) // Light blue outer glow

        // Use the actual ground position where lightning hits the terrain
        val groundPosition = lightning.groundPosition
        val startX = lightning.strikePosition.x

        // Draw the main lightning bolt (zigzag line from top to ground)
        val segments = 8 // Number of zigzag segments
        val segmentHeight = groundPosition.y / segments

        var currentX = startX
        var currentY = 0f

        // Draw the lightning bolt with a zigzag pattern
        for (i in 0 until segments) {
            val nextY = currentY + segmentHeight
            // Random zigzag offset, more pronounced in the middle segments
            val zigzagFactor = if (i > 1 && i < segments - 2) 20f else 10f
            val nextX = currentX + (Math.random().toFloat() - 0.5f) * zigzagFactor

            // Draw outer glow (thicker line)
            drawScope.drawLine(
                color = lightningOuterColor,
                start = Offset(currentX, currentY),
                end = Offset(nextX, nextY),
                strokeWidth = 6f
            )

            // Draw inner core (thinner, brighter line)
            drawScope.drawLine(
                color = lightningCoreColor,
                start = Offset(currentX, currentY),
                end = Offset(nextX, nextY),
                strokeWidth = 2.5f
            )

            currentX = nextX
            currentY = nextY
        }

        // Draw impact area on the ground
        val impactRadius = lightning.spread / 2

        // Draw outer glow of impact
        drawScope.drawCircle(
            color = lightningOuterColor.copy(alpha = 0.6f),
            radius = impactRadius,
            center = Offset(currentX, groundPosition.y)
        )

        // Draw inner bright impact
        drawScope.drawCircle(
            color = lightningCoreColor.copy(alpha = 0.8f),
            radius = impactRadius * 0.6f,
            center = Offset(currentX, groundPosition.y)
        )
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
