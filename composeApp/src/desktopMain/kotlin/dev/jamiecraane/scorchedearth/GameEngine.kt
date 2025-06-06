package dev.jamiecraane.scorchedearth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Main game engine class that manages the game state and logic.
 */
class ScorchedEarthGame {
    // Game state
    var gameState by mutableStateOf(GameState.WAITING_FOR_PLAYER)

    // Terrain data
    var terrain by mutableStateOf(generateTerrain(800, 600))

    // Players
    var players by mutableStateOf(listOf(
        Player(position = Offset(100f, 0f), color = androidx.compose.ui.graphics.Color.Red),
        Player(position = Offset(700f, 0f), color = androidx.compose.ui.graphics.Color.Blue)
    ))

    // Current player index
    var currentPlayerIndex by mutableStateOf(0)

    // Environmental factors
    var wind by mutableStateOf(generateWind())

    // Projectile state
    var projectile by mutableStateOf<Projectile?>(null)

    /**
     * Generates a random wind speed and direction.
     * @return Wind speed in pixels per second (negative = left, positive = right)
     */
    private fun generateWind(): Float {
        return Random.nextFloat() * 20f - 10f
    }

    /**
     * Generates procedural terrain using a simple algorithm.
     * @param width Width of the game area
     * @param height Height of the game area
     * @return A Path object representing the terrain
     */
    private fun generateTerrain(width: Int, height: Int): Path {
        val path = Path()
        val baseHeight = height * 0.7f
        val segments = 100
        val segmentWidth = width.toFloat() / segments

        // Start at the left edge
        path.moveTo(0f, height.toFloat())
        path.lineTo(0f, baseHeight + Random.nextFloat() * 50f)

        // Generate terrain points
        for (i in 1..segments) {
            val x = i * segmentWidth
            val y = baseHeight + sin(i * 0.1).toFloat() * 50f + Random.nextFloat() * 30f
            path.lineTo(x, y)
        }

        // Close the path at the bottom
        path.lineTo(width.toFloat(), height.toFloat())
        path.close()

        return path
    }

    /**
     * Updates the game state for each frame.
     * @param deltaTime Time elapsed since the last update in seconds
     */
    fun update(deltaTime: Float) {
        when (gameState) {
            GameState.PROJECTILE_IN_FLIGHT -> updateProjectile(deltaTime)
            else -> {} // No updates needed for other states
        }
    }

    /**
     * Updates the projectile position and checks for collisions.
     * @param deltaTime Time elapsed since the last update in seconds
     */
    private fun updateProjectile(deltaTime: Float) {
        projectile?.let { proj ->
            // Update projectile position based on velocity and gravity
            val gravity = 9.8f * 30f // Scaled gravity

            proj.velocity = Offset(
                proj.velocity.x + wind * deltaTime,
                proj.velocity.y + gravity * deltaTime
            )

            proj.position = Offset(
                proj.position.x + proj.velocity.x * deltaTime,
                proj.position.y + proj.velocity.y * deltaTime
            )

            // Check for collision with terrain or boundaries
            // This will be implemented in Phase 3

            // For now, just check if projectile is out of bounds
            if (proj.position.x < 0 || proj.position.x > 800 || proj.position.y > 600) {
                projectile = null
                gameState = GameState.WAITING_FOR_PLAYER
                // Switch to next player
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size
            }
        }
    }

    /**
     * Fires a projectile with the given angle and power.
     * @param angle Angle in degrees (0 = right, 90 = up)
     * @param power Power factor (0-100)
     */
    fun fireProjectile(angle: Float, power: Float) {
        val player = players[currentPlayerIndex]
        val angleRadians = angle * PI.toFloat() / 180f
        val velocity = Offset(
            cos(angleRadians) * power * 2f,
            -sin(angleRadians) * power * 2f
        )

        projectile = Projectile(
            position = Offset(player.position.x, player.position.y - 20f),
            velocity = velocity
        )

        gameState = GameState.PROJECTILE_IN_FLIGHT
    }
}

/**
 * Represents the current state of the game.
 */
enum class GameState {
    WAITING_FOR_PLAYER,
    AIMING,
    PROJECTILE_IN_FLIGHT,
    GAME_OVER
}

/**
 * Represents a player in the game.
 */
data class Player(
    val position: Offset,
    val color: androidx.compose.ui.graphics.Color,
    var health: Int = 100,
    var angle: Float = 45f,
    var power: Float = 50f
)

/**
 * Represents a projectile in flight.
 */
data class Projectile(
    var position: Offset,
    var velocity: Offset
)
