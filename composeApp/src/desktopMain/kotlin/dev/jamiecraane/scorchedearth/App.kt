package dev.jamiecraane.scorchedearth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.cos
import kotlin.math.sin

@Composable
@Preview
fun App() {
    // Create a game instance to manage state
    val game = remember { ScorchedEarthGame() }

    // Track canvas size to detect changes
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    // Game loop using LaunchedEffect
    LaunchedEffect(Unit) {
        var lastTime = System.currentTimeMillis()
        while (true) {
            val currentTime = System.currentTimeMillis()
            val deltaTime = (currentTime - lastTime) / 1000f
            lastTime = currentTime

            game.update(deltaTime)
            delay(16) // ~60 FPS
        }
    }

    // Main game container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF87CEEB)) // Sky blue background
    ) {
        // Game canvas where all rendering happens
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Only update dimensions when canvas size actually changes
            if (canvasSize != size) {
                canvasSize = size
                game.updateDimensions(size.width, size.height)
            }

            // Draw the game elements
            drawGame(game)
        }

        // UI controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            // Display current player and wind
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Player: ${game.currentPlayerIndex + 1}", color = Color.White)
                Text("Wind: ${game.wind.toInt()} mph", color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Angle control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Angle: ${game.players[game.currentPlayerIndex].angle.toInt()}°",
                    modifier = Modifier.width(100.dp),
                    color = Color.White)
                Slider(
                    value = game.players[game.currentPlayerIndex].angle,
                    onValueChange = {
                        val players = game.players.toMutableList()
                        players[game.currentPlayerIndex] = players[game.currentPlayerIndex].copy(angle = it)
                        game.players = players
                    },
                    valueRange = 0f..90f,
                    modifier = Modifier.weight(1f)
                )
            }

            // Power control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Power: ${game.players[game.currentPlayerIndex].power.toInt()}",
                    modifier = Modifier.width(100.dp),
                    color = Color.White)
                Slider(
                    value = game.players[game.currentPlayerIndex].power,
                    onValueChange = {
                        val players = game.players.toMutableList()
                        players[game.currentPlayerIndex] = players[game.currentPlayerIndex].copy(power = it)
                        game.players = players
                    },
                    valueRange = 10f..100f,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fire button
            Button(
                onClick = {
                    val player = game.players[game.currentPlayerIndex]
                    game.fireProjectile(player.angle, player.power)
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = game.gameState == GameState.WAITING_FOR_PLAYER
            ) {
                Text("FIRE!")
            }
        }
    }
}

// Function to draw all game elements
private fun DrawScope.drawGame(game: ScorchedEarthGame) {
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
            val angleRadians = player.angle * Math.PI.toFloat() / 180f
            val cannonLength = 30f

            // For the blue player (index 1), we need to flip the angle to face left
            val adjustedAngleRadians = if (index == 1) {
                // Convert angle to face left: 180° - angle
                (180f - player.angle) * Math.PI.toFloat() / 180f
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
        }

        // Draw projectile if in flight
        game.projectile?.let { projectile ->
            drawCircle(
                color = Color.Black,
                radius = 5f,
                center = projectile.position
            )
        }

        // Draw explosion if it exists
        game.explosion?.let { explosion ->
            drawCircle(
                color = Color.Red,
                radius = explosion.radius,
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
