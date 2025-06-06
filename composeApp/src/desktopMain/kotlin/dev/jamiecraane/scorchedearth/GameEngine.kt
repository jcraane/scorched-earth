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
class ScorchedEarthGame(private val numberOfPlayers: Int = 2) {
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
    var players by mutableStateOf(generatePlayers(gameWidth, gameHeight, numberOfPlayers))

    // Current player index
    var currentPlayerIndex by mutableStateOf(0)

    // Environmental factors
    var wind by mutableStateOf(generateWind())

    // Projectile state
    var projectile by mutableStateOf<Projectile?>(null)

    // Explosion state
    var explosion by mutableStateOf<Explosion?>(null)

    // Mini-bombs state (for Funky Bomb and MIRV)
    var miniBombs by mutableStateOf<List<Projectile>>(listOf())

    /**
     * Updates the game dimensions and regenerates content accordingly.
     * Call this when the window/canvas size changes.
     */
    fun updateDimensions(width: Float, height: Float) {
        gameWidth = width
        gameHeight = height
        terrain = generateTerrain(width, height)
        players = generatePlayers(width, height, numberOfPlayers)
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

        // Reset mini-bombs if they exist
        if (miniBombs.isNotEmpty()) {
            miniBombs = listOf()
        }
    }

    /**
     * Generates players with positions scaled to the current game dimensions.
     * @param width Width of the game area
     * @param height Height of the game area
     * @param numberOfPlayers Number of players to generate (2-10)
     * @return List of players
     */
    private fun generatePlayers(width: Float, height: Float, numberOfPlayers: Int = 2): List<Player> {
        val baseY = height * 0.7f - 20f
        val players = mutableListOf<Player>()

        // Ensure number of players is within valid range
        val validNumberOfPlayers = numberOfPlayers.coerceIn(2, 10)

        // Generate colors for players
        val colors = listOf(
            androidx.compose.ui.graphics.Color.Red,
            androidx.compose.ui.graphics.Color.Blue,
            androidx.compose.ui.graphics.Color.Green,
            androidx.compose.ui.graphics.Color.Yellow,
            androidx.compose.ui.graphics.Color.Magenta,
            androidx.compose.ui.graphics.Color.Cyan,
            androidx.compose.ui.graphics.Color.White,
            androidx.compose.ui.graphics.Color(0xFFFF8000), // Orange
            androidx.compose.ui.graphics.Color(0xFF800080), // Purple
            androidx.compose.ui.graphics.Color(0xFF008080)  // Teal
        )

        // Calculate positions evenly distributed across the width
        for (i in 0 until validNumberOfPlayers) {
            // Calculate position based on player index
            // For 2 players: 10% and 90% of width
            // For more players: evenly distributed
            val xPosition = width * (0.1f + 0.8f * i.toFloat() / (validNumberOfPlayers - 1))

            players.add(
                Player(
                    position = Offset(xPosition, baseY),
                    color = colors[i % colors.size]
                ).apply {
                    // Add missiles to inventory
                    inventory.addItem(ProjectileType.BABY_MISSILE, 10)
                    inventory.addItem(ProjectileType.FUNKY_BOMB, 3)
                    inventory.addItem(ProjectileType.MIRV, 3)
                }
            )
        }

        return players
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

        // Update mini-bombs if they exist
        if (miniBombs.isNotEmpty()) {
            updateMiniBombs(deltaTime)
        }

        // Update explosion if it exists
        updateExplosion(deltaTime)
    }

    /**
     * Updates the mini-bombs positions and checks for collisions.
     * @param deltaTime Time elapsed since the last update in seconds
     */
    private fun updateMiniBombs(deltaTime: Float) {
        val updatedMiniBombs = mutableListOf<Projectile>()

        for (bomb in miniBombs) {
            // Update mini-bomb position based on velocity and gravity
            val gravity = 9.8f * 30f // Scaled gravity

            val newVelocity = Offset(
                bomb.velocity.x + wind * deltaTime,
                bomb.velocity.y + gravity * deltaTime
            )

            val newPosition = Offset(
                bomb.position.x + newVelocity.x * deltaTime,
                bomb.position.y + newVelocity.y * deltaTime
            )

            // Add current position to the trail and maintain max trail length
            val updatedTrail = (bomb.trail + bomb.position).takeLast(bomb.maxTrailLength)

            // Check for collision with boundaries, terrain, or players
            if (newPosition.x < 0 || newPosition.x > gameWidth || newPosition.y > gameHeight ||
                isCollidingWithTerrain(newPosition)) {
                // Create an explosion for the mini-bomb with full blast radius
                createExplosion(
                    newPosition,
                    Projectile(
                        position = newPosition,
                        velocity = newVelocity,
                        type = bomb.type,
                        minDamage = bomb.minDamage / 2, // Reduced damage for mini-bombs
                        maxDamage = bomb.maxDamage / 2,
                        blastRadius = bomb.blastRadius // Keep full blast radius
                    )
                )
                continue // Skip adding this bomb to the updated list
            }

            // Check for collision with players
            var hitPlayer = false
            for ((index, player) in players.withIndex()) {
                if (isCollidingWithPlayer(newPosition, player)) {
                    // Create explosion at player's position with full blast radius
                    createExplosion(
                        player.position,
                        Projectile(
                            position = newPosition,
                            velocity = newVelocity,
                            type = bomb.type,
                            minDamage = bomb.minDamage / 2,
                            maxDamage = bomb.maxDamage / 2,
                            blastRadius = bomb.blastRadius // Keep full blast radius
                        )
                    )

                    // Apply direct hit damage (use half of mini-bomb's maxDamage for direct hit)
                    val damage = bomb.maxDamage / 2
                    applyDamageToPlayer(index, damage)

                    hitPlayer = true
                    break
                }
            }

            if (!hitPlayer) {
                // If no collision, update the mini-bomb
                updatedMiniBombs.add(
                    Projectile(
                        position = newPosition,
                        velocity = newVelocity,
                        type = bomb.type,
                        minDamage = bomb.minDamage,
                        maxDamage = bomb.maxDamage,
                        blastRadius = bomb.blastRadius,
                        trail = updatedTrail
                    )
                )
            }
        }

        // Update the mini-bombs list
        miniBombs = updatedMiniBombs

        // If all mini-bombs are gone and we're still in PROJECTILE_IN_FLIGHT state,
        // end the turn and move to the next player
        if (miniBombs.isEmpty() && gameState == GameState.PROJECTILE_IN_FLIGHT && projectile == null) {
            gameState = GameState.WAITING_FOR_PLAYER
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size
        }
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
            // Add current position to the trail and maintain max trail length
            val updatedTrail = (proj.trail + proj.position).takeLast(proj.maxTrailLength)

            // Check if this is a MIRV that should split
            if (proj.type == ProjectileType.MIRV) {
                // Check if MIRV is at its apex (vertical velocity changes from negative to positive)
                val isAtApex = proj.velocity.y < 0 && newVelocity.y > 0

                // Check if MIRV is near a target (within a certain distance of terrain or player)
                val distanceToTerrain = getDistanceToTerrain(newPosition)
                val distanceToNearestPlayer = getDistanceToNearestPlayer(newPosition)

                // Only consider "near target" if the projectile has traveled some distance
                // This prevents immediate explosion right after firing
                val hasMinimumTravel = proj.trail.size >= 5
                val isNearTarget = hasMinimumTravel && (distanceToTerrain < 100f || distanceToNearestPlayer < 100f)

                // Split the MIRV if it's at its apex or near a target
                if (isAtApex || isNearTarget) {
                    // Generate MIRV sub-projectiles
                    generateMIRVSubProjectiles(newPosition, proj, newVelocity)

                    // Create a small explosion at the split point
                    val smallExplosion = Explosion(
                        position = newPosition,
                        initialRadius = 5f,
                        maxRadius = 30f,
                        timeRemaining = 0.3f
                    )
                    explosion = smallExplosion

                    // End the parent projectile's flight
                    endProjectileFlight()
                    return@let
                }
            }

            projectile = Projectile(
                position = newPosition,
                velocity = newVelocity,
                type = proj.type,
                trail = updatedTrail
            )

            // Check for collision with boundaries
            if (newPosition.x < 0 || newPosition.x > gameWidth || newPosition.y > gameHeight) {
                createExplosion(newPosition, projectile)
                endProjectileFlight()
                return@let
            }

            // Check for collision with terrain
            if (isCollidingWithTerrain(newPosition)) {
                createExplosion(newPosition, projectile)
                endProjectileFlight()
                return@let
            }

            // Check for collision with players
            for ((index, player) in players.withIndex()) {
                if (isCollidingWithPlayer(newPosition, player)) {
                    // Create explosion at player's position
                    createExplosion(player.position, projectile)

                    // Apply direct hit damage (use projectile's maxDamage for direct hit)
                    val damage = projectile?.maxDamage ?: 100
                    applyDamageToPlayer(index, damage)

                    endProjectileFlight()
                    return@let
                }
            }
        }
    }

    /**
     * Gets the distance from a position to the nearest terrain point.
     * @param position The position to check
     * @return The distance to the nearest terrain point
     */
    private fun getDistanceToTerrain(position: Offset): Float {
        // Get the terrain height at this x-coordinate
        val terrainHeight = getTerrainHeightAtX(position.x)

        // Calculate vertical distance to terrain
        return kotlin.math.abs(position.y - terrainHeight)
    }

    /**
     * Gets the distance from a position to the nearest player.
     * @param position The position to check
     * @return The distance to the nearest player
     */
    private fun getDistanceToNearestPlayer(position: Offset): Float {
        var minDistance = Float.MAX_VALUE

        for (player in players) {
            val distance = kotlin.math.sqrt(
                (position.x - player.position.x) * (position.x - player.position.x) +
                (position.y - player.position.y) * (position.y - player.position.y)
            )

            if (distance < minDistance) {
                minDistance = distance
            }
        }

        return minDistance
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
     * @param proj The projectile that caused the explosion (null for other explosion sources)
     */
    private fun createExplosion(position: Offset, proj: Projectile? = null) {
        val explosionRadius = proj?.blastRadius ?: 90f
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
        applyBlastDamageToPlayers(position, explosionRadius, proj)

        // Generate mini-bombs if this is a Funky Bomb
        if (proj?.type == ProjectileType.FUNKY_BOMB) {
            generateMiniBombs(position, proj)
        }
    }

    /**
     * Generates mini-bombs from a Funky Bomb explosion.
     * @param position The position of the parent explosion
     * @param parentProjectile The parent projectile (Funky Bomb)
     */
    private fun generateMiniBombs(position: Offset, parentProjectile: Projectile) {
        val numberOfMiniBombs = Random.nextInt(5, 10) // Random number of mini-bombs
        val newMiniBombs = mutableListOf<Projectile>()

        for (i in 0 until numberOfMiniBombs) {
            // Generate random angle and power for each mini-bomb
            val angle = Random.nextFloat() * 2 * PI.toFloat()
            val power = Random.nextFloat() * 200f + 100f // Random power between 100 and 300

            // Calculate velocity based on angle and power
            val velocity = Offset(
                cos(angle) * power,
                -sin(angle) * power
            )

            // Create a mini-bomb with reduced damage but same blast radius as parent
            // Calculate initial position offset from explosion center to spread mini-bombs further
            val initialOffset = Offset(
                cos(angle) * 50f, // Spread mini-bombs 50 pixels from explosion center
                -sin(angle) * 50f
            )
            val offsetPosition = Offset(
                position.x + initialOffset.x,
                position.y + initialOffset.y
            )

            val miniBomb = Projectile(
                position = offsetPosition, // Start from offset position
                velocity = velocity,
                type = parentProjectile.type, // Same type as parent
                minDamage = parentProjectile.minDamage / 3, // Reduced damage
                maxDamage = parentProjectile.maxDamage / 3,
                blastRadius = parentProjectile.blastRadius, // Same blast radius as parent
                trail = listOf() // Start with empty trail
            )

            newMiniBombs.add(miniBomb)
        }

        // Add the new mini-bombs to the existing ones
        miniBombs = miniBombs + newMiniBombs
    }

    /**
     * Generates sub-projectiles from a MIRV at its apex or near target.
     * @param position The position where the MIRV splits
     * @param parentProjectile The parent projectile (MIRV)
     * @param parentVelocity The velocity of the parent projectile at split time
     */
    private fun generateMIRVSubProjectiles(position: Offset, parentProjectile: Projectile, parentVelocity: Offset) {
        val numberOfSubProjectiles = 5 // Fixed number of sub-projectiles for predictability
        val newSubProjectiles = mutableListOf<Projectile>()

        // Calculate spread angle based on parent velocity direction
        val parentAngle = kotlin.math.atan2(-parentVelocity.y, parentVelocity.x)

        // Calculate spread angles for sub-projectiles (in a cone pattern)
        val spreadAngleRange = PI.toFloat() / 3f // 60 degrees spread
        val angleStep = spreadAngleRange / (numberOfSubProjectiles - 1)

        // Calculate base velocity magnitude (slightly less than parent)
        val baseVelocityMagnitude = kotlin.math.sqrt(parentVelocity.x * parentVelocity.x + parentVelocity.y * parentVelocity.y) * 0.8f

        for (i in 0 until numberOfSubProjectiles) {
            // Calculate angle for this sub-projectile
            // Center sub-projectile continues on parent trajectory, others spread out
            val angleOffset = -spreadAngleRange / 2f + i * angleStep
            val angle = parentAngle + angleOffset

            // Calculate velocity based on angle and base velocity
            val velocity = Offset(
                cos(angle) * baseVelocityMagnitude,
                -sin(angle) * baseVelocityMagnitude
            )

            // Create a sub-projectile with reduced damage and blast radius
            val subProjectile = Projectile(
                position = position, // All start from the same position where MIRV splits
                velocity = velocity,
                type = parentProjectile.type, // Same type as parent
                minDamage = parentProjectile.minDamage / 2, // Reduced damage
                maxDamage = parentProjectile.maxDamage / 2,
                blastRadius = parentProjectile.blastRadius / 2, // Reduced blast radius
                trail = listOf() // Start with empty trail
            )

            newSubProjectiles.add(subProjectile)
        }

        // Add the new sub-projectiles to the existing mini-bombs list
        miniBombs = miniBombs + newSubProjectiles
    }

    /**
     * Applies damage to players within the blast radius.
     * Damage decreases with distance from explosion center.
     * @param explosionPosition The center of the explosion
     * @param blastRadius The radius of the explosion
     * @param projectile The projectile that caused the explosion (null for other explosion sources)
     */
    private fun applyBlastDamageToPlayers(explosionPosition: Offset, blastRadius: Float, projectile: Projectile? = null) {
        // Get damage values from projectile or use defaults
        val minDamage = projectile?.minDamage ?: 10
        val maxDamage = projectile?.maxDamage ?: 100

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
                // Using a linear falloff between maxDamage (direct hit) and minDamage (edge of blast)
                val distanceRatio = distance / blastRadius
                val damageFactor = 1.0f - distanceRatio

                // Calculate damage: at center (distanceRatio=0) = maxDamage, at edge (distanceRatio=1) = minDamage
                val damage = (minDamage + (maxDamage - minDamage) * damageFactor).toInt()

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
     * Ends the projectile flight and switches to the next player if no mini-bombs are in flight.
     */
    private fun endProjectileFlight() {
        projectile = null

        // Only end the turn if there are no mini-bombs in flight
        if (miniBombs.isEmpty()) {
            gameState = GameState.WAITING_FOR_PLAYER
            // Switch to next player
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size
        } else {
            // Keep the game state as PROJECTILE_IN_FLIGHT while mini-bombs are active
            gameState = GameState.PROJECTILE_IN_FLIGHT
        }
    }

    /**
     * Attempts to purchase a missile for the current player.
     * @param projectileType The type of projectile to purchase
     * @return True if the purchase was successful, false if the player doesn't have enough money
     */
    fun purchaseMissile(projectileType: ProjectileType): Boolean {
        val player = players[currentPlayerIndex]

        // Check if player has enough money
        if (player.money < projectileType.cost) {
            return false
        }

        // Create a new player with updated money
        val updatedPlayer = player.copy(
            money = player.money - projectileType.cost
        )

        // Add the missile to the player's inventory
        updatedPlayer.inventory.addItem(projectileType, 1)

        // Update the players list
        val updatedPlayers = players.toMutableList()
        updatedPlayers[currentPlayerIndex] = updatedPlayer
        players = updatedPlayers

        return true
    }

    /**
     * Fires a projectile with the given angle and power.
     * @param angle Angle in degrees (0 = right, 90 = up)
     * @param power Power factor (0-100)
     * @return True if the projectile was fired successfully, false otherwise
     */
    fun fireProjectile(angle: Float, power: Float): Boolean {
        val player = players[currentPlayerIndex]

        // Check if player has the selected projectile type in inventory
        if (!player.inventory.hasItem(player.selectedProjectileType)) {
            return false
        }

        // Remove one projectile from inventory
        player.inventory.removeItem(player.selectedProjectileType, 1)

        val angleRadians = angle * PI.toFloat() / 180f

        // Determine direction multiplier based on player position
        // Players on the left half of the screen fire right (positive direction)
        // Players on the right half of the screen fire left (negative direction)
        val playerX = player.position.x
        val screenMidpoint = gameWidth / 2
        val directionMultiplier = if (playerX < screenMidpoint) 1f else -1f

        val powerMultiplieFactor = 12f
        val velocity = Offset(
            cos(angleRadians) * power * powerMultiplieFactor * directionMultiplier, // Increased from 2f to 8f
            -sin(angleRadians) * power * powerMultiplieFactor // Increased from 2f to 8f
        )

        projectile = Projectile(
            position = Offset(player.position.x, player.position.y - 20f),
            velocity = velocity,
            type = player.selectedProjectileType,
            trail = listOf() // Initialize with empty trail
        )

        gameState = GameState.PROJECTILE_IN_FLIGHT
        return true
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
 * Defines the different types of projectiles available in the game.
 */
enum class ProjectileType(
    val displayName: String,
    val minDamage: Int,
    val maxDamage: Int,
    val blastRadius: Float,
    val cost: Int
) {
    BABY_MISSILE("Baby Missile", 10, 30, 60f, 250),
    SMALL_MISSILE("Small Missile", 20, 50, 90f, 1000),
    BIG_MISSILE("Big Missile", 30, 75, 200f, 2500),
    DEATHS_HEAD("Death's Head", 50, 100, 300f, 5000),
    NUCLEAR_BOMB("Nuclear Bomb", 75, 150, 700f, 10000),
    FUNKY_BOMB("Funky Bomb", 25, 60, 150f, 3000),
    MIRV("MIRV", 35, 70, 120f, 4000)
}

/**
 * Represents a player in the game.
 */
data class Player(
    val position: Offset,
    val color: androidx.compose.ui.graphics.Color,
    var health: Int = 100,
    var angle: Float = 45f,
    var power: Float = 50f,
    var selectedProjectileType: ProjectileType = ProjectileType.BABY_MISSILE,
    var money: Int = 5000,
    val inventory: Inventory = Inventory()
)

/**
 * Represents a projectile in flight.
 * @param position Current position of the projectile
 * @param velocity Current velocity of the projectile
 * @param type The type of projectile
 * @param minDamage Minimum damage dealt at the outer edge of the blast radius
 * @param maxDamage Maximum damage dealt on direct hit
 * @param blastRadius Radius of the explosion when the projectile hits
 * @param trail List of previous positions to create a trail effect
 * @param maxTrailLength Maximum number of positions to keep in the trail
 */
data class Projectile(
    val position: Offset,
    val velocity: Offset,
    val type: ProjectileType,
    val minDamage: Int = type.minDamage,
    val maxDamage: Int = type.maxDamage,
    val blastRadius: Float = type.blastRadius,
    val trail: List<Offset> = listOf(),
    val maxTrailLength: Int = 15
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
