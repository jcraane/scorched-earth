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
    // Game dimensions - these will be updated when the canvas size changes
    var gameWidth by mutableStateOf(1600f)
    var gameHeight by mutableStateOf(1200f)

    // Game state
    var gameState by mutableStateOf(GameState.WAITING_FOR_PLAYER)

    // Terrain data
    var terrain by mutableStateOf(generateTerrain(gameWidth, gameHeight))

    // Players
    var players by mutableStateOf(generatePlayers(gameWidth, gameHeight))

    // Current player index
    var currentPlayerIndex by mutableStateOf(0)

    // Environmental factors
    var wind by mutableStateOf(generateWind())

    // Projectile state
    var projectile by mutableStateOf<Projectile?>(null)

    /**
     * Updates the game dimensions and regenerates content accordingly.
     * Call this when the window/canvas size changes.
     */
    fun updateDimensions(width: Float, height: Float) {
        gameWidth = width
        gameHeight = height
        terrain = generateTerrain(width, height)
        players = generatePlayers(width, height)
        // Reset projectile if it exists to prevent out-of-bounds issues
        if (projectile != null) {
            projectile = null
            gameState = GameState.WAITING_FOR_PLAYER
        }
    }

    /**
     * Generates players with positions scaled to the current game dimensions.
     */
    private fun generatePlayers(width: Float, height: Float): List<Player> {
        val baseY = height * 0.7f - 20f
        return listOf(
            Player(
                position = Offset(width * 0.1f, baseY), // 10% from left edge
                color = androidx.compose.ui.graphics.Color.Red
            ),
            Player(
                position = Offset(width * 0.9f, baseY), // 10% from right edge
                color = androidx.compose.ui.graphics.Color.Blue
            )
        )
    }

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
    private fun generateTerrain(width: Float, height: Float): Path {
        val path = Path()
        val baseHeight = height * 0.7f
        val segments = 100
        val segmentWidth = width / segments

        // Start at the left edge
        path.moveTo(0f, height)
        path.lineTo(0f, baseHeight + Random.nextFloat() * 50f)

        // Generate terrain points
        for (i in 1..segments) {
            val x = i * segmentWidth
            val y = baseHeight + sin(i * 0.1).toFloat() * 50f + Random.nextFloat() * 30f
            path.lineTo(x, y)
        }

        // Close the path at the bottom
        path.lineTo(width, height)
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

            val newVelocity = Offset(
                proj.velocity.x + wind * deltaTime,
                proj.velocity.y + gravity * deltaTime
            )

            val newPosition = Offset(
                proj.position.x + newVelocity.x * deltaTime,
                proj.position.y + newVelocity.y * deltaTime
            )

            // Create a new projectile instance to trigger recomposition
            projectile = Projectile(
                position = newPosition,
                velocity = newVelocity
            )

            // Check for collision with terrain or boundaries using current game dimensions
            if (newPosition.x < 0 || newPosition.x > gameWidth || newPosition.y > gameHeight) {
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
    val position: Offset,
    val velocity: Offset
)
