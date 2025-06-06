package dev.jamiecraane.scorchedearth

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import dev.jamiecraane.scorchedearth.engine.ScorchedEarthGame
import kotlin.math.cos
import kotlin.math.sin

/**
 * Extension function to draw all game elements on the canvas.
 */
fun DrawScope.drawGame(game: ScorchedEarthGame) {
    // Scale all game elements to match the canvas size
    scale(scaleX = 1f, scaleY = 1f) {
        // Draw terrain
        drawPath(
            path = game.terrain,
            color = Color(0xFF8B4513), // Brown color for terrain
            style = Stroke(width = 2f)
        )
        drawPath(
            path = game.terrain,
            color = Color(0xFF8B4513).copy(alpha = 0.5f) // Semi-transparent fill
        )

        // Draw players (tanks)
        game.players.forEachIndexed { index, player ->
            // Draw tank body
            drawCircle(
                color = player.color,
                radius = 15f,
                center = player.position
            )

            // Draw tank cannon
            // Rotate the angle by 90 degrees to make 0 point right, -90 point up, and 90 point down
            val rotatedAngle = player.angle + 90f
            val angleRadians = rotatedAngle * Math.PI.toFloat() / 180f
            val cannonLength = 30f

            // Determine the direction based on player position
            val isOnRightSide = player.position.x > game.gameWidth / 2

            // For players on the right side, we need to flip the angle to face left
            val adjustedAngleRadians = if (isOnRightSide) {
                // Convert angle to face left: 180° - rotatedAngle
                (180f - rotatedAngle) * Math.PI.toFloat() / 180f
            } else {
                angleRadians
            }

            drawLine(
                color = player.color,
                start = player.position,
                end = Offset(
                    player.position.x + cos(adjustedAngleRadians) * cannonLength,
                    player.position.y - sin(adjustedAngleRadians) * cannonLength
                ),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )

            // Highlight current player
            if (index == game.currentPlayerIndex) {
                drawCircle(
                    color = Color.Yellow,
                    radius = 20f,
                    center = player.position,
                    style = Stroke(width = 2f)
                )
            }

            // Draw player health
            // Calculate health bar width based on health percentage
            val maxHealthBarWidth = 80f
            val healthBarHeight = 14f
            val healthPercentage = player.health / 100f
            val healthBarWidth = maxHealthBarWidth * healthPercentage

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
