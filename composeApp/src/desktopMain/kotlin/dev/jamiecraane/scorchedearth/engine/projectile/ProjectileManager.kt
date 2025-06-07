package dev.jamiecraane.scorchedearth.engine.projectile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import dev.jamiecraane.scorchedearth.engine.explosion.ExplosionManager
import dev.jamiecraane.scorchedearth.engine.player.PlayerManager
import dev.jamiecraane.scorchedearth.engine.terrain.TerrainManager
import dev.jamiecraane.scorchedearth.inventory.ProjectileType
import dev.jamiecraane.scorchedearth.model.Projectile
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Manages projectile movement, collision, and special behaviors.
 */
class ProjectileManager(
    private val terrainManager: TerrainManager,
    private val playerManager: PlayerManager,
    private val explosionManager: ExplosionManager
) {
    // Projectile state
    var projectile by mutableStateOf<Projectile?>(null)

    // Mini-bombs state (for Funky Bomb)
    var miniBombs by mutableStateOf<List<Projectile>>(listOf())

    // Constants for game physics
    private val rollerMinSpeedThreshold = 10.0f // Minimum speed for roller before it explodes

    // Flag to track if the current player has already fired a tracer in this turn
    private var hasPlayerFiredTracerThisTurn by mutableStateOf(false)

    // Environmental factors
    var wind by mutableStateOf(0f)

    // Game dimensions
    private var gameWidth = 0f
    private var gameHeight = 0f

    /**
     * Sets the game dimensions.
     * @param width Width of the game area
     * @param height Height of the game area
     */
    fun setGameDimensions(width: Float, height: Float) {
        gameWidth = width
        gameHeight = height
    }

    /**
     * Generates a random wind speed and direction.
     * @return Wind speed in pixels per second (negative = left, positive = right)
     */
    fun generateWind(): Float {
        wind = Random.nextFloat() * 20f - 10f
        return wind
    }

    /**
     * Fires a projectile with the given angle and power.
     * @param angle Angle in degrees (-90 to 90, where 0 = horizontal, -90 = down, 90 = up)
     * @param power Power factor (0-100)
     * @return True if the projectile was fired successfully, false otherwise
     */
    fun fireProjectile(angle: Float, power: Float): Boolean {
        val player = playerManager.getCurrentPlayer()

        println("[DEBUG_LOG] Attempting to fire projectile: player=${player.name}, type=${player.type}, projectile=${player.selectedProjectileType.displayName}")
        println("[DEBUG_LOG] Player inventory: ${player.inventory.getAllItems().joinToString { "${it.type.displayName}(${it.quantity})" }}")

        // Check if player has the selected projectile type in inventory
        if (!player.inventory.hasItem(player.selectedProjectileType)) {
            println("[DEBUG_LOG] Fire failed: No ${player.selectedProjectileType.displayName} in inventory")
            return false
        }

        // If firing a non-tracer projectile, reset the tracer flag
        // This ensures that if a player fires a tracer after a non-tracer in the same turn,
        // it's treated as the first tracer of the turn
        if (player.selectedProjectileType != ProjectileType.TRACER) {
            hasPlayerFiredTracerThisTurn = false
            println("[DEBUG_LOG] Firing non-tracer projectile, resetting tracer flag")
        } else {
            println("[DEBUG_LOG] Firing tracer projectile, tracer already fired this turn: $hasPlayerFiredTracerThisTurn")
        }

        // Remove one projectile from inventory
        player.inventory.removeItem(player.selectedProjectileType, 1)

        // Rotate the angle by 90 degrees to make 0 point right, -90 point down, and 90 point up
        val rotatedAngle = angle + 90f
        val angleRadians = rotatedAngle * PI.toFloat() / 180f

        // Determine direction multiplier based on player position
        // Players on the left half of the screen fire right (positive direction)
        // Players on the right half of the screen fire left (negative direction)
        val playerX = player.position.x
        val screenMidpoint = gameWidth / 2
        val directionMultiplier = if (playerX < screenMidpoint) 1f else -1f

        val powerMultiplieFactor = 12f
        val velocity = Offset(
            cos(angleRadians) * power * powerMultiplieFactor * directionMultiplier,
            -sin(angleRadians) * power * powerMultiplieFactor
        )

        projectile = Projectile(
            position = Offset(player.position.x, player.position.y - 20f),
            velocity = velocity,
            type = player.selectedProjectileType,
            trail = listOf() // Initialize with empty trail
        )

        return true
    }

    /**
     * Updates the projectile position and checks for collisions.
     * @param deltaTime Time elapsed since the last update in seconds
     * @return True if the turn should end, false otherwise
     */
    fun updateProjectile(deltaTime: Float): Boolean {
        var turnEnded = false

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

            // Special handling for MIRV - check if it should split at apex
            if (proj.type == ProjectileType.MIRV) {
                // Check if MIRV has reached its apex (velocity.y becomes positive, meaning it's falling)
                val wasRising = proj.velocity.y <= 0
                val nowFalling = newVelocity.y > 0

                if (wasRising && nowFalling) {
                    // MIRV has reached its apex - split into sub-projectiles
                    generateMIRVSubProjectiles(newPosition, proj, newVelocity)
                    turnEnded = endProjectileFlight()
                    return@let
                }
            }

            // Create a new projectile instance to trigger recomposition
            // Add current position to the trail
            // For Tracers, keep the entire trail without limiting length
            // For other projectiles, maintain max trail length
            val updatedTrail = if (proj.type == ProjectileType.TRACER) {
                proj.trail + proj.position
            } else {
                (proj.trail + proj.position).takeLast(proj.maxTrailLength)
            }

            projectile = Projectile(
                position = newPosition,
                velocity = newVelocity,
                type = proj.type,
                trail = updatedTrail,
                bounceCount = proj.bounceCount,
                maxBounces = proj.maxBounces,
                isRolling = proj.isRolling, // Preserve rolling state
                rollingDistance = proj.rollingDistance,
                maxRollingDistance = proj.maxRollingDistance
            )

            // Check for collision with boundaries
            if (newPosition.x < 0 || newPosition.x > gameWidth || newPosition.y > gameHeight) {
                // For MIRV, don't explode on boundary collision - just remove it
                if (proj.type == ProjectileType.MIRV || proj.type == ProjectileType.TRACER) {
                    turnEnded = endProjectileFlight()
                } else if (proj.type == ProjectileType.LEAPFROG && proj.bounceCount < proj.maxBounces) {
                    // Leapfrog bounces off boundaries
                    handleLeapfrogBounce(newPosition, proj, newVelocity, true)
                } else {
                    explosionManager.createExplosion(newPosition, projectile, gameWidth, gameHeight)
                    turnEnded = endProjectileFlight()
                }
                return@let
            }

            // Special handling for Roller projectiles
            if (proj.type == ProjectileType.ROLLER) {
                if (proj.isRolling) {
                    // Already rolling - continue rolling physics
                    handleRollerRolling(newPosition, proj, newVelocity, deltaTime)
                } else {
                    // Not rolling yet - check if it should start rolling
                    if (terrainManager.isCollidingWithTerrain(newPosition)) {
                        handleRollerStart(newPosition, proj, newVelocity)
                    }
                }
                return@let
            }

            // Check for collision with terrain (for non-roller projectiles)
            if (terrainManager.isCollidingWithTerrain(newPosition)) {
                // For MIRV and TRACER, don't explode on terrain collision - just remove it
                if (proj.type == ProjectileType.MIRV || proj.type == ProjectileType.TRACER) {
                    turnEnded = endProjectileFlight()
                } else if (proj.type == ProjectileType.LEAPFROG && proj.bounceCount < proj.maxBounces) {
                    // Leapfrog bounces off terrain
                    handleLeapfrogBounce(newPosition, proj, newVelocity, false)
                } else {
                    explosionManager.createExplosion(newPosition, projectile, gameWidth, gameHeight)
                    turnEnded = endProjectileFlight()
                }
                return@let
            }

            // Check for collision with players
            for ((index, player) in playerManager.players.withIndex()) {
                if (playerManager.isCollidingWithPlayer(newPosition, player)) {
                    // For MIRV and TRACER, don't explode on player collision - just remove it
                    if (proj.type == ProjectileType.MIRV || proj.type == ProjectileType.TRACER) {
                        turnEnded = endProjectileFlight()
                    } else {
                        // Create explosion at player's position
                        explosionManager.createExplosion(player.position, projectile, gameWidth, gameHeight)

                        // Apply direct hit damage (use projectile's maxDamage for direct hit)
                        val damage = if (proj.type == ProjectileType.ROLLER && proj.isRolling) {
                            (projectile?.maxDamage ?: 100) * 1.5f.toInt() // 50% more damage when rolling
                        } else {
                            projectile?.maxDamage ?: 100
                        }

                        playerManager.applyDamageToPlayer(index, damage)

                        turnEnded = endProjectileFlight()
                    }
                    return@let
                }
            }
        }

        return turnEnded
    }

    /**
     * Updates the mini-bombs positions and checks for collisions.
     * @param deltaTime Time elapsed since the last update in seconds
     * @return True if the turn should end, false otherwise
     */
    fun updateMiniBombs(deltaTime: Float): Boolean {
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
                terrainManager.isCollidingWithTerrain(newPosition)) {
                // Create an explosion for the mini-bomb with full blast radius
                explosionManager.createExplosion(
                    newPosition,
                    Projectile(
                        position = newPosition,
                        velocity = newVelocity,
                        type = bomb.type,
                        damageOuterBlastRadius = bomb.damageOuterBlastRadius / 2, // Reduced damage for mini-bombs
                        maxDamage = bomb.maxDamage / 2,
                        blastRadius = bomb.blastRadius // Keep full blast radius
                    ),
                    gameWidth,
                    gameHeight
                )
                continue // Skip adding this bomb to the updated list
            }

            // Check for collision with players
            var hitPlayer = false
            for ((index, player) in playerManager.players.withIndex()) {
                if (playerManager.isCollidingWithPlayer(newPosition, player)) {
                    // Create explosion at player's position with full blast radius
                    explosionManager.createExplosion(
                        player.position,
                        Projectile(
                            position = newPosition,
                            velocity = newVelocity,
                            type = bomb.type,
                            damageOuterBlastRadius = bomb.damageOuterBlastRadius / 2,
                            maxDamage = bomb.maxDamage / 2,
                            blastRadius = bomb.blastRadius // Keep full blast radius
                        ),
                        gameWidth,
                        gameHeight
                    )

                    // Apply direct hit damage (use half of mini-bomb's maxDamage for direct hit)
                    val damage = bomb.maxDamage / 2
                    playerManager.applyDamageToPlayer(index, damage)

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
                        damageOuterBlastRadius = bomb.damageOuterBlastRadius,
                        maxDamage = bomb.maxDamage,
                        blastRadius = bomb.blastRadius,
                        trail = updatedTrail
                    )
                )
            }
        }

        // Update the mini-bombs list
        miniBombs = updatedMiniBombs

        // If all mini-bombs have exploded, advance to the next player
        if (miniBombs.isEmpty() && projectile == null) {
            // All projectiles and mini-bombs are gone, move to next player
            playerManager.nextPlayer()
            // Reset the tracer flag for the new player
            hasPlayerFiredTracerThisTurn = false
            println("[DEBUG_LOG] Turn ended after mini-bombs, switching to next player")
            return true
        }

        return false
    }

    /**
     * Handles the bouncing behavior of the Leapfrog projectile.
     * @param position The current position of the projectile
     * @param proj The current projectile
     * @param velocity The current velocity of the projectile
     * @param isBoundaryCollision True if the collision is with a boundary, false if with terrain
     */
    private fun handleLeapfrogBounce(position: Offset, proj: Projectile, velocity: Offset, isBoundaryCollision: Boolean) {
        // Create a small explosion at the bounce point with reduced blast radius
        val bounceExplosionProjectile = Projectile(
            position = position,
            velocity = velocity,
            type = proj.type,
            blastRadius = proj.blastRadius * 0.5f, // Half the normal blast radius
            damageOuterBlastRadius = proj.damageOuterBlastRadius / 2, // Reduced damage for bounce explosions
            maxDamage = proj.maxDamage / 2
        )
        explosionManager.createExplosion(position, bounceExplosionProjectile, gameWidth, gameHeight)

        // Calculate new velocity based on collision type
        val newVelocity = if (isBoundaryCollision) {
            // Boundary collision - reflect based on which boundary was hit
            when {
                position.x <= 0 || position.x >= gameWidth -> Offset(-velocity.x * 0.8f, velocity.y * 0.8f) // X-boundary: reverse X velocity
                else -> Offset(velocity.x * 0.8f, -velocity.y * 0.8f) // Y-boundary: reverse Y velocity
            }
        } else {
            // Terrain collision - reflect upward with some randomness
            val bounceAngle = -PI.toFloat() / 2 + (Random.nextFloat() - 0.5f) * PI.toFloat() / 4 // Mostly upward with some variation
            val speed = sqrt(velocity.x * velocity.x + velocity.y * velocity.y) * 0.8f // Reduce speed by 20% each bounce

            Offset(
                cos(bounceAngle) * speed * (if (velocity.x > 0) 1f else -1f), // Maintain horizontal direction
                sin(bounceAngle) * speed
            )
        }

        // Create a new projectile with incremented bounce count
        projectile = Projectile(
            position = position,
            velocity = newVelocity,
            type = proj.type,
            trail = proj.trail,
            bounceCount = proj.bounceCount + 1,
            maxBounces = proj.maxBounces
        )

        // If this was the last allowed bounce, create a final explosion
        if (proj.bounceCount + 1 >= proj.maxBounces) {
            // Create a larger final explosion with increased blast radius and damage
            val finalExplosionProjectile = Projectile(
                position = position,
                velocity = newVelocity,
                type = proj.type,
                blastRadius = proj.blastRadius * 2.0f, // Double the normal blast radius
                damageOuterBlastRadius = proj.damageOuterBlastRadius * 2, // Double the damage
                maxDamage = proj.maxDamage * 2
            )
            explosionManager.createExplosion(position, finalExplosionProjectile, gameWidth, gameHeight)
            endProjectileFlight()
        }
    }

    /**
     * Handles the initial impact of a Roller projectile with terrain.
     * @param position The current position of the projectile
     * @param proj The current projectile
     * @param velocity The current velocity of the projectile
     */
    private fun handleRollerStart(position: Offset, proj: Projectile, velocity: Offset) {
        // Get terrain height at the impact position
        val terrainHeight = terrainManager.getTerrainHeightAt(position.x) ?: return

        // Position the Roller slightly above the terrain surface
        val rollerRadius = 5f
        val adjustedY = terrainHeight - rollerRadius

        // Create a new position that's on the terrain surface
        val adjustedPosition = Offset(position.x, adjustedY)

        // Direction is determined by the original horizontal direction
        val horizontalDirection = if (velocity.x > 0) 1f else -1f

        // Start with a much lower initial rolling speed to make physics more realistic
        val initialRollingSpeed = 15f // Fixed initial speed that can be slowed down by friction

        // Create simple rolling velocity - just horizontal movement
        val rollingVelocity = Offset(
            horizontalDirection * initialRollingSpeed,
            0f // No vertical component initially
        )

        println("[ROLLER DEBUG] Starting to roll with speed: $initialRollingSpeed")

        // Switch to rolling mode
        projectile = Projectile(
            position = adjustedPosition,
            velocity = rollingVelocity,
            type = proj.type,
            trail = proj.trail,
            isRolling = true, // Now in rolling mode
            rollingDistance = 0f,
            maxRollingDistance = 300f // Reduced maximum rolling distance
        )
    }

    /**
     * Handles the rolling behavior of the Roller projectile.
     * @param position The current position of the projectile
     * @param proj The current projectile
     * @param velocity The current velocity of the projectile
     * @param deltaTime Time elapsed since the last update in seconds
     */
    private fun handleRollerRolling(position: Offset, proj: Projectile, velocity: Offset, deltaTime: Float) {
        // Calculate how far the roller has moved in this step
        val distanceMoved = sqrt(
            (position.x - proj.position.x) * (position.x - proj.position.x) +
                (position.y - proj.position.y) * (position.y - proj.position.y)
        )

        // Update total rolling distance
        val newRollingDistance = proj.rollingDistance + distanceMoved

        // Calculate current horizontal speed (ignore vertical component for rolling)
        val currentSpeed = abs(velocity.x)

        println("[ROLLER DEBUG] Current speed: $currentSpeed, Threshold: $rollerMinSpeedThreshold, Distance: $newRollingDistance")

        // Check for explosion conditions FIRST
        if (currentSpeed < rollerMinSpeedThreshold) {
            println("[ROLLER DEBUG] Speed below threshold ($currentSpeed < $rollerMinSpeedThreshold), exploding")
            explosionManager.createExplosion(position, proj, gameWidth, gameHeight)
            endProjectileFlight()
            return
        }

        if (newRollingDistance >= proj.maxRollingDistance) {
            println("[ROLLER DEBUG] Maximum distance reached, exploding")
            explosionManager.createExplosion(position, proj, gameWidth, gameHeight)
            endProjectileFlight()
            return
        }

        // Calculate new x-position based on velocity
        val newX = position.x + velocity.x * deltaTime

        // Check if roller has gone off screen
        if (newX < 0 || newX > gameWidth) {
            println("[ROLLER DEBUG] Off screen, exploding")
            explosionManager.createExplosion(position, proj, gameWidth, gameHeight)
            endProjectileFlight()
            return
        }

        // Get terrain height at the new x-position
        val terrainHeight = terrainManager.getTerrainHeightAt(newX) ?: run {
            explosionManager.createExplosion(position, proj, gameWidth, gameHeight)
            endProjectileFlight()
            return
        }

        // Position the Roller on the terrain surface
        val rollerRadius = 5f
        val newY = terrainHeight - rollerRadius
        val newPosition = Offset(newX, newY)

        // Get terrain slope at new position
        val terrainSlope = terrainManager.getTerrainSlopeAt(newPosition)

        // Check if in a valley
        if (terrainManager.isInValley(newPosition, terrainSlope)) {
            println("[ROLLER DEBUG] In valley, exploding")
            explosionManager.createExplosion(newPosition, proj, gameWidth, gameHeight)
            endProjectileFlight()
            return
        }

        // Calculate the horizontal direction based on current velocity
        val currentHorizontalDirection = if (velocity.x > 0) 1f else -1f

        // Apply physics effects to speed with stronger effects:
        // 1. Gravity effect on slopes
        val slopeEffect = -terrainSlope * 3.0f // Strong slope effect

        // 2. Rolling friction (much stronger to ensure it slows down)
        val friction = 4.0f // Strong friction per second

        // Calculate new speed
        var newSpeed = currentSpeed + slopeEffect * deltaTime - friction * deltaTime

        // Handle direction changes on steep uphill slopes
        var finalHorizontalDirection = currentHorizontalDirection
        if (newSpeed < 0 && terrainSlope > 0.2f) {
            // Rolling backwards due to gravity on slope
            newSpeed = abs(newSpeed) * 0.3f // Much reduced speed when rolling back
            finalHorizontalDirection = -currentHorizontalDirection
            println("[ROLLER DEBUG] Rolling backwards due to slope")
        }

        // Ensure speed doesn't go negative
        newSpeed = newSpeed.coerceAtLeast(0f)

        println("[ROLLER DEBUG] New speed after physics: $newSpeed (slope: $terrainSlope, friction applied)")

        // Create new rolling velocity
        val rollingVelocity = Offset(
            finalHorizontalDirection * newSpeed,
            0f // Keep rolling on surface
        )

        // Continue rolling
        projectile = Projectile(
            position = newPosition,
            velocity = rollingVelocity,
            type = proj.type,
            trail = proj.trail,
            isRolling = true,
            rollingDistance = newRollingDistance,
            maxRollingDistance = proj.maxRollingDistance
        )
    }

    /**
     * Generates mini-bombs from a Funky Bomb explosion.
     * @param position The position of the parent explosion
     * @param parentProjectile The parent projectile (Funky Bomb)
     */
    fun generateMiniBombs(position: Offset, parentProjectile: Projectile) {
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
                damageOuterBlastRadius = parentProjectile.damageOuterBlastRadius / 3, // Reduced damage
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
     * Generates sub-projectiles from a MIRV at its highest point.
     * @param position The position of the parent MIRV
     * @param parentProjectile The parent MIRV projectile
     * @param parentVelocity The velocity of the parent MIRV at split time
     */
    private fun generateMIRVSubProjectiles(position: Offset, parentProjectile: Projectile, parentVelocity: Offset) {
        val numberOfSubProjectiles = 6 // Number of sub-projectiles
        val newMiniBombs = mutableListOf<Projectile>()

        // Calculate the original projectile's direction angle
        val originalAngleRadians = atan2(parentVelocity.y, parentVelocity.x)
        val originalAngleDegrees = originalAngleRadians * 180f / PI.toFloat()

        // Set spread angle for sub-projectiles
        val spreadAngle = 40f // Total spread angle in degrees
        val angleStep = spreadAngle / (numberOfSubProjectiles - 1)

        for (i in 0 until numberOfSubProjectiles) {
            // Calculate angle variation for this sub-projectile (centered around original angle)
            val angleVariation = -spreadAngle/2 + i * angleStep

            // Add a small random component to make trajectories more varied
            val randomVariation = Random.nextFloat() * 10f - 5f // ±5 degrees random variation

            // Calculate final angle in radians (maintain original direction but add variation)
            val finalAngleRadians = (originalAngleDegrees + angleVariation + randomVariation) * PI.toFloat() / 180f

            // Calculate velocity with a base speed slightly higher than the parent
            val speedMultiplier = 0.8f + Random.nextFloat() * 0.4f // 80-120% of parent speed
            val parentSpeed = sqrt(parentVelocity.x * parentVelocity.x + parentVelocity.y * parentVelocity.y)
            val speed = parentSpeed * speedMultiplier

            val velocity = Offset(
                cos(finalAngleRadians) * speed,
                sin(finalAngleRadians) * speed
            )

            // Create a sub-projectile with slightly reduced damage
            val subProjectile = Projectile(
                position = position.copy(), // Copy to avoid reference issues
                velocity = velocity,
                type = parentProjectile.type,
                damageOuterBlastRadius = parentProjectile.damageOuterBlastRadius / 2,
                maxDamage = parentProjectile.maxDamage / 2,
                blastRadius = parentProjectile.blastRadius * 2.0f, // Tripled blast radius at highest point
                trail = listOf() // Start with empty trail
            )

            newMiniBombs.add(subProjectile)
        }

        // Add the new sub-projectiles to the existing ones
        miniBombs = miniBombs + newMiniBombs
    }

    /**
     * Ends the projectile flight and switches to the next player if no mini-bombs are in flight.
     * For tracer projectiles, allows the player to fire again unless they've already fired a tracer this turn.
     * @return True if the turn ended, false otherwise
     */
    private fun endProjectileFlight(): Boolean {
        val wasTracer = projectile?.type == ProjectileType.TRACER
        projectile = null

        // Only end the turn if there are no mini-bombs in flight
        if (miniBombs.isEmpty()) {
            // For tracers, handle turn transition differently
            if (wasTracer && !hasPlayerFiredTracerThisTurn) {
                // First tracer fired this turn - allow player to fire again
                hasPlayerFiredTracerThisTurn = true
                println("[DEBUG_LOG] Tracer fired, player can fire again")
                return false
            } else {
                // Not a tracer or player has already fired a tracer - switch to next player
                playerManager.nextPlayer()
                // Reset the tracer flag for the new player
                hasPlayerFiredTracerThisTurn = false
                println("[DEBUG_LOG] Turn ended, switching to next player")
                return true
            }
        } else {
            // Keep the game state as PROJECTILE_IN_FLIGHT while mini-bombs are active
            // Don't advance to next player yet - we'll do that when mini-bombs are done
            return false
        }
    }

    /**
     * Resets the projectile and mini-bombs.
     */
    fun reset() {
        projectile = null
        miniBombs = listOf()
        hasPlayerFiredTracerThisTurn = false
    }
}
