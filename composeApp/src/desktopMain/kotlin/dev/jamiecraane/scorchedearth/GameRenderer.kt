package dev.jamiecraane.scorchedearth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import dev.jamiecraane.scorchedearth.engine.ScorchedEarthGame
import dev.jamiecraane.scorchedearth.sky.SkyStyle
import dev.jamiecraane.scorchedearth.sky.Star
import dev.jamiecraane.scorchedearth.weather.WeatherRenderer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Store stars for the night sky
private var stars by mutableStateOf<List<Star>>(listOf())
// Track when stars were last generated to avoid regenerating them too frequently
private var lastStarGenerationTime = 0L
// Track when stars were last updated to control flickering rate
private var lastStarUpdateTime = 0L

/**
 * Extension function to draw all game elements on the canvas.
 */
fun DrawScope.drawGame(game: ScorchedEarthGame) {
    // Scale all game elements to match the canvas size
    scale(scaleX = 1f, scaleY = 1f) {
        // Draw sky with gradient based on current sky style
        drawRect(
            brush = game.skyStyle.createGradientBrush(),
            size = size
        )

        // Draw sun if the sky style has one (sunrise or sunset)
        game.skyStyle.createSun(size.width, size.height)?.let { sun ->
            // Draw the sun as a filled circle
            drawCircle(
                color = sun.color,
                radius = sun.radius,
                center = Offset(sun.x, sun.y)
            )
        }

        // Draw stars if night sky is selected
        if (game.skyStyle == SkyStyle.NIGHT) {
            // Generate stars if they don't exist or if it's been a while since they were last generated
            val currentTime = System.currentTimeMillis()
            if (stars.isEmpty() || currentTime - lastStarGenerationTime > 30000) { // Regenerate stars every 30 seconds
                stars = SkyStyle.generateStars(size.width, size.height, 150)
                lastStarGenerationTime = currentTime
            }

            // Update star brightness periodically to create flickering effect
            if (currentTime - lastStarUpdateTime > 100) { // Update every 100ms
                SkyStyle.updateStarBrightness(stars)
                lastStarUpdateTime = currentTime
            }

            // Draw each star
            stars.forEach { star ->
                drawCircle(
                    color = Color.White.copy(alpha = star.brightness),
                    radius = star.size,
                    center = Offset(star.x, star.y)
                )
            }
        }

        // Draw weather effects
        println("[DEBUG_LOG] GameRenderer.drawGame: Drawing weather effects, weatherType=${game.weather.weatherTypeState}, hasLightning=${game.weather.lightning != null}")
        WeatherRenderer.render(this, game.weather, game.wind)

        // Draw terrain
        drawPath(
            path = game.terrain,
            color = game.terrainStyle.color, // Use terrain style color
            style = Stroke(width = 2f)
        )
        drawPath(
            path = game.terrain,
            color = game.terrainStyle.color.copy(alpha = 1.0f) // Fully opaque fill
        )

        // Draw players (tanks) - only render players that are alive (health > 0)
        game.players.forEachIndexed { index, player ->
            // Skip rendering dead players
            if (player.health <= 0) return@forEachIndexed

            // Determine the direction based on player position
            val isOnRightSide = player.position.x > game.gameWidth / 2

            // Rotate the angle by 90 degrees to make 0 point right, -90 point up, and 90 point down
            val rotatedAngle = player.angle + 90f
            val angleRadians = rotatedAngle * Math.PI.toFloat() / 180f

            // For players on the right side, we need to flip the angle to face left
            val adjustedAngleRadians = if (isOnRightSide) {
                // Convert angle to face left: 180° - rotatedAngle
                (180f - rotatedAngle) * Math.PI.toFloat() / 180f
            } else {
                angleRadians
            }

            // Tank dimensions
            val tankWidth = 30f
            val tankHeight = 15f
            val turretRadius = 10f
            val cannonLength = 25f
            val trackHeight = 5f

            // Tank position (center of the tank body)
            val tankX = player.position.x
            val tankY = player.position.y

            // Draw tank tracks (two rectangles)
            val trackColor = Color.DarkGray

            // Left track
            drawRect(
                color = trackColor,
                topLeft = Offset(tankX - tankWidth / 2, tankY + tankHeight / 2 - trackHeight),
                size = Size(tankWidth, trackHeight)
            )

            // Draw tank body (hull)
            drawRect(
                color = player.color,
                topLeft = Offset(tankX - tankWidth / 2, tankY - tankHeight / 2),
                size = Size(tankWidth, tankHeight)
            )

            // Draw tank turret (circle on top of the body)
            drawCircle(
                color = player.color.copy(alpha = 0.8f),
                radius = turretRadius,
                center = Offset(tankX, tankY - 2f)
            )

            // Draw tank cannon
            drawLine(
                color = player.color.copy(alpha = 0.9f),
                start = Offset(tankX, tankY - 2f),
                end = Offset(
                    tankX + cos(adjustedAngleRadians) * cannonLength,
                    (tankY - 2f) - sin(adjustedAngleRadians) * cannonLength
                ),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )

            // Highlight current player
            if (index == game.currentPlayerIndex) {
                drawRect(
                    color = Color.Yellow,
                    topLeft = Offset(tankX - tankWidth / 2 - 3f, tankY - tankHeight / 2 - 3f),
                    size = Size(tankWidth + 6f, tankHeight + trackHeight + 6f),
                    style = Stroke(width = 2f)
                )
            }

            // Draw shield if active
            if (player.hasActiveShield()) {
                val shieldRadius = 40f // Larger than the tank
                val shieldHealthPercentage = player.activeShield!!.getHealthPercentage()
                val shieldColor = player.activeShield!!.type.color

                // Draw outer ring (non-transparent)
                drawCircle(
                    color = shieldColor,
                    radius = shieldRadius,
                    center = Offset(tankX, tankY),
                    style = Stroke(width = 3f)
                )

                // Draw inner shield (transparent based on health)
                drawCircle(
                    color = shieldColor.copy(alpha = shieldHealthPercentage * 0.5f), // More transparent as health decreases
                    radius = shieldRadius - 3f,
                    center = Offset(tankX, tankY)
                )
            }

            // Draw player health
            // Calculate health bar width based on health percentage
            val maxHealthBarWidth = 80f
            val healthBarHeight = 14f
            val healthPercentage = player.health / 100f
            val healthBarWidth = maxHealthBarWidth * healthPercentage

            println("[DEBUG_LOG] GameRenderer: Drawing health bar for player ${player.name}, health=${player.health}, percentage=$healthPercentage, width=$healthBarWidth")

            // Draw health background (red)
            drawRect(
                color = Color.Red,
                topLeft = Offset(player.position.x - maxHealthBarWidth / 2, player.position.y - 36f),
                size = Size(maxHealthBarWidth, healthBarHeight)
            )

            // Draw health foreground (green)
            drawRect(
                color = Color.Green,
                topLeft = Offset(player.position.x - maxHealthBarWidth / 2, player.position.y - 36f),
                size = Size(healthBarWidth, healthBarHeight)
            )

            // Draw shield health bar if player has an active shield
            player.activeShield?.let { shield ->
                // Calculate shield health bar width based on shield health percentage
                val shieldHealthPercentage = shield.getHealthPercentage()
                val shieldHealthBarWidth = maxHealthBarWidth * shieldHealthPercentage

                // Draw shield health background (darker blue)
                drawRect(
                    color = Color.Blue.copy(alpha = 0.5f),
                    topLeft = Offset(player.position.x - maxHealthBarWidth / 2, player.position.y + 22f), // Position below the player
                    size = Size(maxHealthBarWidth, healthBarHeight)
                )

                // Draw shield health foreground (brighter blue)
                drawRect(
                    color = shield.type.color,
                    topLeft = Offset(player.position.x - maxHealthBarWidth / 2, player.position.y + 22f), // Position below the player
                    size = Size(shieldHealthBarWidth, healthBarHeight)
                )
            }
        }

        // Draw projectile if in flight
        game.projectile?.let { projectile ->
            // Draw the trail with fade-out effect
            projectile.trail.forEachIndexed { index, position ->
                // Calculate alpha based on position in trail (older positions are more transparent)
                val alpha = (index + 1).toFloat() / projectile.trail.size.toFloat()

                drawCircle(
                    color = Color.Gray.copy(alpha = alpha * 0.7f),
                    radius = 3f + (2f * alpha), // Smaller circles for older positions
                    center = position
                )
            }

            // Draw the actual projectile
            drawCircle(
                color = Color.Black,
                radius = 5f,
                center = projectile.position
            )
        }

        game.miniBombs.forEach { miniBomb ->
            // Draw the trail with fade-out effect
            miniBomb.trail.forEachIndexed { index, position ->
                // Calculate alpha based on position in trail (older positions are more transparent)
                val alpha = (index + 1).toFloat() / miniBomb.trail.size.toFloat()

                drawCircle(
                    color = Color.Gray.copy(alpha = alpha * 0.7f),
                    radius = 2f + (1f * alpha), // Smaller circles for older positions
                    center = position
                )
            }

            // Draw the actual mini-bomb projectile
            drawCircle(
                color = Color.Red, // Different color to distinguish from main projectile
                radius = 3f,
                center = miniBomb.position
            )
        }

        // Draw explosion if it exists
        game.explosion?.let { explosion ->
            // Create radial gradient from light (hot) center to dark red outer
            val gradientBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFF99), // Light yellow-white (hottest center)
                    Color(0xFFFFCC00), // Bright yellow-orange
                    Color(0xFFFF6600), // Orange
                    Color(0xFFFF3300), // Red-orange
                    Color(0xFFCC0000), // Dark red
                    Color(0xFF990000)  // Very dark red (outer edge)
                ),
                center = explosion.position,
                radius = explosion.currentRadius
            )

            drawCircle(
                brush = gradientBrush,
                radius = explosion.currentRadius,
                center = explosion.position
            )
        }

        // Draw wind indicator
        val windIndicatorX = size.width / 2
        val windIndicatorY = 50f
        val windStrength = game.wind.coerceIn(-10f, 10f)
        val windLineLength = windStrength * 5f

        drawLine(
            color = Color.White,
            start = Offset(windIndicatorX - windLineLength, windIndicatorY),
            end = Offset(windIndicatorX + windLineLength, windIndicatorY),
            strokeWidth = 2f
        )

        // Wind direction arrow
        if (windStrength != 0f) {
            val arrowX = if (windStrength > 0) windIndicatorX + windLineLength else windIndicatorX - windLineLength
            val arrowSize = 10f
            val arrowDirection = if (windStrength > 0) 0f else 180f
            val arrowRadians = arrowDirection * Math.PI.toFloat() / 180f

            drawLine(
                color = Color.White,
                start = Offset(arrowX, windIndicatorY),
                end = Offset(
                    arrowX - cos(arrowRadians + 0.5f) * arrowSize,
                    windIndicatorY - sin(arrowRadians + 0.5f) * arrowSize
                ),
                strokeWidth = 2f
            )

            drawLine(
                color = Color.White,
                start = Offset(arrowX, windIndicatorY),
                end = Offset(
                    arrowX - cos(arrowRadians - 0.5f) * arrowSize,
                    windIndicatorY - sin(arrowRadians - 0.5f) * arrowSize
                ),
                strokeWidth = 2f
            )
        }
    }
}
