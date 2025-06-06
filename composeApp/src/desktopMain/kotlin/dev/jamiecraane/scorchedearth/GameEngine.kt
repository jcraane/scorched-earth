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

    // Terrain height data for collision detection
    var terrainHeights by mutableStateOf<Map<Float, Float>>(mapOf())

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

    // Explosion state
    var explosion by mutableStateOf<Explosion?>(null)

    /**
     * Updates the game dimensions and regenerates content accordingly.
     * Call this when the window/canvas size changes.
     */
    fun updateDimensions(width: Float, height: Float) {
        gameWidth = width
        gameHeight = height
        terrain = generateTerrain(width, height)
        players = generatePlayers(width, height)
        // Update player positions to stick to the terrain
        updatePlayerPositions()
        // Reset projectile and explosion if they exist to prevent out-of-bounds issues
        if (projectile != null) {
            projectile = null
            gameState = GameState.WAITING_FOR_PLAYER
        }
        // Reset explosion if it exists
        if (explosion != null) {
            explosion = null
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

        // Create a map to store terrain heights
        val heights = mutableMapOf<Float, Float>()

        // Start at the left edge
        val startY = baseHeight + Random.nextFloat() * 50f
        path.moveTo(0f, height)
        path.lineTo(0f, startY)
        heights[0f] = startY

        // Generate terrain points
        for (i in 1..segments) {
            val x = i * segmentWidth
            val y = baseHeight + sin(i * 0.1).toFloat() * 50f + Random.nextFloat() * 30f
            path.lineTo(x, y)
            heights[x] = y
        }

        // Close the path at the bottom
        path.lineTo(width, height)
        path.close()

        // Store the terrain heights for collision detection
        terrainHeights = heights

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

        // Update explosion if it exists
        updateExplosion(deltaTime)
    }

    /**
     * Updates player positions to match the terrain height at their x-coordinates.
     * This ensures players always sit on top of the terrain.
     */
    private fun updatePlayerPositions() {
        val updatedPlayers = players.map { player ->
            // Find the terrain height at the player's x-coordinate
            val terrainHeight = getTerrainHeightAtX(player.position.x)

            // Create a new player with updated y-position
            // Subtract a small offset to place the player visibly on top of the terrain
            player.copy(position = Offset(player.position.x, terrainHeight - 15f))
        }

        players = updatedPlayers
    }

    /**
     * Gets the terrain height at a specific x-coordinate using interpolation.
     * @param x The x-coordinate to get the height for
     * @return The interpolated height at the given x-coordinate
     */
    private fun getTerrainHeightAtX(x: Float): Float {
        // Find the closest x-coordinates in our terrain height map
        val terrainXCoords = terrainHeights.keys.toList().sorted()

        // If position is outside the terrain bounds, return a default value
        if (x < terrainXCoords.first()) {
            return terrainHeights[terrainXCoords.first()] ?: (gameHeight * 0.7f)
        }

        if (x > terrainXCoords.last()) {
            return terrainHeights[terrainXCoords.last()] ?: (gameHeight * 0.7f)
        }

        // Find the two closest x-coordinates
        val lowerX = terrainXCoords.filter { it <= x }.maxOrNull() ?: return gameHeight * 0.7f
        val upperX = terrainXCoords.filter { it >= x }.minOrNull() ?: return gameHeight * 0.7f

        // Get the heights at those coordinates
        val lowerY = terrainHeights[lowerX] ?: return gameHeight * 0.7f
        val upperY = terrainHeights[upperX] ?: return gameHeight * 0.7f

        // Interpolate to find the terrain height at the exact x-coordinate
        return if (upperX == lowerX) {
            lowerY
        } else {
            lowerY + (upperY - lowerY) * (x - lowerX) / (upperX - lowerX)
        }
    }

    /**
     * Updates the explosion animation.
     * @param deltaTime Time elapsed since the last update in seconds
     */
    private fun updateExplosion(deltaTime: Float) {
        explosion?.let { exp ->
            val newTimeRemaining = exp.timeRemaining - deltaTime
            if (newTimeRemaining <= 0) {
                // Explosion is finished
                explosion = null
            } else {
                // Calculate the progress of the animation (0.0 to 1.0)
                val initialTime = 0.5f // Same as in createExplosion
                val progress = 1.0f - (newTimeRemaining / initialTime)

                // Calculate the new radius based on the progress
                val newRadius = exp.initialRadius + (exp.maxRadius - exp.initialRadius) * progress

                // Update explosion time remaining and radius
                explosion = exp.copy(
                    timeRemaining = newTimeRemaining,
                    currentRadius = newRadius
                )
            }
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

            // Check for collision with boundaries
            if (newPosition.x < 0 || newPosition.x > gameWidth || newPosition.y > gameHeight) {
                createExplosion(newPosition)
                endProjectileFlight()
                return@let
            }

            // Check for collision with terrain
            if (isCollidingWithTerrain(newPosition)) {
                createExplosion(newPosition)
                endProjectileFlight()
                return@let
            }

            // Check for collision with players
            for ((index, player) in players.withIndex()) {
                if (isCollidingWithPlayer(newPosition, player)) {
                    // Create explosion at player's position
                    createExplosion(player.position)

                    // Apply direct hit damage (100 health)
                    applyDamageToPlayer(index, 100)

                    endProjectileFlight()
                    return@let
                }
            }
        }
    }

    /**
     * Applies damage to a player and removes them if health reaches 0.
     * @param playerIndex The index of the player to damage
     * @param damage The amount of damage to apply
     */
    private fun applyDamageToPlayer(playerIndex: Int, damage: Int) {
        val updatedPlayers = players.toMutableList()

        // Check if the playerIndex is valid
        if (playerIndex < 0 || playerIndex >= updatedPlayers.size) {
            return // Skip if index is out of bounds
        }

        val player = updatedPlayers[playerIndex]

        // Apply damage
        val newHealth = (player.health - damage).coerceAtLeast(0)
        updatedPlayers[playerIndex] = player.copy(health = newHealth)

        // Check if player is eliminated
        if (newHealth <= 0) {
            // Remove the player from the list
            updatedPlayers.removeAt(playerIndex)

            // Adjust current player index if necessary
            if (currentPlayerIndex >= updatedPlayers.size) {
                currentPlayerIndex = 0
            } else if (playerIndex <= currentPlayerIndex && currentPlayerIndex > 0) {
                currentPlayerIndex--
            }

            // Check for game over condition
            if (updatedPlayers.size <= 1) {
                gameState = GameState.GAME_OVER
            }
        }

        players = updatedPlayers
    }
    /**
     * Checks if a point is colliding with the terrain.
     * @param position The position to check
     * @return True if the position is below the terrain surface
     */
    private fun isCollidingWithTerrain(position: Offset): Boolean {
        // Find the closest x-coordinates in our terrain height map
        val terrainXCoords = terrainHeights.keys.toList().sorted()

        // If position is outside the terrain bounds, no collision
        if (position.x < terrainXCoords.first() || position.x > terrainXCoords.last()) {
            return false
        }

        // Find the two closest x-coordinates
        val lowerX = terrainXCoords.filter { it <= position.x }.maxOrNull() ?: return false
        val upperX = terrainXCoords.filter { it >= position.x }.minOrNull() ?: return false

        // Get the heights at those coordinates
        val lowerY = terrainHeights[lowerX] ?: return false
        val upperY = terrainHeights[upperX] ?: return false

        // Interpolate to find the terrain height at the exact x-coordinate
        val terrainHeight = if (upperX == lowerX) {
            lowerY
        } else {
            lowerY + (upperY - lowerY) * (position.x - lowerX) / (upperX - lowerX)
        }

        // Check if the position is below the terrain surface
        return position.y >= terrainHeight
    }

    /**
     * Checks if a point is colliding with a player.
     * @param position The position to check
     * @param player The player to check collision with
     * @return True if the position is within the player's collision radius
     */
    private fun isCollidingWithPlayer(position: Offset, player: Player): Boolean {
        val playerRadius = 15f // Same as the radius used for drawing
        val distance = kotlin.math.sqrt(
            (position.x - player.position.x) * (position.x - player.position.x) +
            (position.y - player.position.y) * (position.y - player.position.y)
        )
        return distance <= playerRadius
    }

    /**
     * Creates an explosion at the specified position.
     * @param position The position of the explosion
     */
    private fun createExplosion(position: Offset) {
        val explosionRadius = 90f // 3 times bigger than the original 30f
        explosion = Explosion(
            position = position,
            initialRadius = 10f, // Start with a small radius
            maxRadius = explosionRadius,
            timeRemaining = 0.5f // Half a second duration
        )

        // Deform terrain if the explosion is colliding with it
        if (isCollidingWithTerrain(position)) {
            deformTerrain(position, explosionRadius)
        }

        // Check for players within blast radius and apply damage
        applyBlastDamageToPlayers(position, explosionRadius)
    }

    /**
     * Applies damage to players within the blast radius.
     * Damage decreases with distance from explosion center.
     * @param explosionPosition The center of the explosion
     * @param blastRadius The radius of the explosion
     */
    private fun applyBlastDamageToPlayers(explosionPosition: Offset, blastRadius: Float) {
        // Check each player
        players.forEachIndexed { index, player ->
            // Calculate distance from player to explosion
            val distance = kotlin.math.sqrt(
                (player.position.x - explosionPosition.x) * (player.position.x - explosionPosition.x) +
                (player.position.y - explosionPosition.y) * (player.position.y - explosionPosition.y)
            )

            // Only apply damage if player is within blast radius
            if (distance <= blastRadius) {
                // Calculate damage based on distance (closer = more damage)
                // Using a linear falloff: damage = maxDamage * (1 - distance/radius)
                val distanceRatio = distance / blastRadius
                val damageFactor = 1.0f - distanceRatio

                // Maximum damage at center is 100, minimum at edge is 0
                val damage = (100 * damageFactor).toInt()

                // Apply damage if it's greater than 0
                if (damage > 0) {
                    applyDamageToPlayer(index, damage)
                }
            }
        }
    }

    /**
     * Deforms the terrain at the explosion point based on the blast radius.
     * @param position The position of the explosion
     * @param blastRadius The radius of the explosion
     */
    private fun deformTerrain(position: Offset, blastRadius: Float) {
        // Create a copy of the terrain heights map
        val newTerrainHeights = terrainHeights.toMutableMap()

        // Get all x-coordinates in the terrain
        val terrainXCoords = terrainHeights.keys.toList().sorted()

        // Calculate the deformation for each x-coordinate within the blast radius
        for (x in terrainXCoords) {
            // Calculate horizontal distance from explosion center
            val horizontalDistance = kotlin.math.abs(x - position.x)

            // Only deform terrain within the blast radius
            if (horizontalDistance <= blastRadius) {
                // Get current height at this x-coordinate
                val currentHeight = terrainHeights[x] ?: continue

                // Get vertical distance from explosion to terrain
                val verticalDistance = kotlin.math.max(0f, currentHeight - position.y)

                // Calculate actual distance from explosion center to terrain point
                val actualDistance = kotlin.math.sqrt(horizontalDistance * horizontalDistance + verticalDistance * verticalDistance)

                // Only deform if the actual distance is within the blast radius
                if (actualDistance <= blastRadius) {
                    // Calculate deformation amount based on distance from explosion center
                    // Using a parabolic falloff for more realistic crater shape: (1-(d/r)²)
                    val distanceRatio = actualDistance / blastRadius
                    val deformationFactor = 1.0f - (distanceRatio * distanceRatio)

                    // Scale the deformation by the blast radius
                    val deformationAmount = blastRadius * deformationFactor * 0.8f

                    // Apply deformation (raise the terrain value, which means digging a crater)
                    // The y-coordinate increases downward in the canvas
                    val newHeight = currentHeight + deformationAmount

                    // Update the height map
                    newTerrainHeights[x] = newHeight
                }
            }
        }

        // Update the terrain heights map
        terrainHeights = newTerrainHeights

        // Regenerate the terrain path with the new heights
        terrain = regenerateTerrainPath(gameWidth, gameHeight)

        // Update player positions to stick to the terrain
        updatePlayerPositions()
    }

    /**
     * Regenerates the terrain path using the current terrain heights.
     * @param width Width of the game area
     * @param height Height of the game area
     * @return A Path object representing the terrain
     */
    private fun regenerateTerrainPath(width: Float, height: Float): Path {
        val path = Path()

        // Get all x-coordinates in the terrain sorted
        val terrainXCoords = terrainHeights.keys.toList().sorted()

        if (terrainXCoords.isEmpty()) {
            return generateTerrain(width, height)
        }

        // Start at the left edge
        path.moveTo(0f, height)
        path.lineTo(terrainXCoords.first(), terrainHeights[terrainXCoords.first()] ?: 0f)

        // Connect all terrain points
        for (x in terrainXCoords.drop(1)) {
            path.lineTo(x, terrainHeights[x] ?: 0f)
        }

        // Close the path at the bottom
        path.lineTo(width, height)
        path.close()

        return path
    }

    /**
     * Ends the projectile flight and switches to the next player.
     */
    private fun endProjectileFlight() {
        projectile = null
        gameState = GameState.WAITING_FOR_PLAYER
        // Switch to next player
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
    }

    /**
     * Fires a projectile with the given angle and power.
     * @param angle Angle in degrees (0 = right, 90 = up)
     * @param power Power factor (0-100)
     */
    fun fireProjectile(angle: Float, power: Float) {
        val player = players[currentPlayerIndex]
        val angleRadians = angle * PI.toFloat() / 180f

        // Determine direction multiplier based on player
        // Player 0 (red, left side) fires right (positive direction)
        // Player 1 (blue, right side) fires left (negative direction)
        val directionMultiplier = if (currentPlayerIndex == 0) 1f else -1f

        val powerMultiplieFactor = 12f
        val velocity = Offset(
            cos(angleRadians) * power * powerMultiplieFactor * directionMultiplier, // Increased from 2f to 8f
            -sin(angleRadians) * power * powerMultiplieFactor // Increased from 2f to 8f
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

/**
 * Represents an explosion when a projectile hits something.
 */
data class Explosion(
    val position: Offset,
    val initialRadius: Float = 10f,
    val maxRadius: Float,
    val currentRadius: Float = initialRadius,
    val timeRemaining: Float
)
