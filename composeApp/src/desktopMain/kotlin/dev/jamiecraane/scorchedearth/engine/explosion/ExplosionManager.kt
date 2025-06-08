package dev.jamiecraane.scorchedearth.engine.explosion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import dev.jamiecraane.scorchedearth.engine.player.PlayerManager
import dev.jamiecraane.scorchedearth.engine.terrain.TerrainManager
import dev.jamiecraane.scorchedearth.inventory.ProjectileType
import dev.jamiecraane.scorchedearth.model.Explosion
import dev.jamiecraane.scorchedearth.model.Projectile
import kotlin.math.sqrt

/**
 * Manages explosion effects, animations, and damage.
 */
class ExplosionManager(
    private val terrainManager: TerrainManager,
    private val playerManager: PlayerManager
) {
    // Explosion state
    var explosion by mutableStateOf<Explosion?>(null)

    // For Death's Head two-stage explosion
    private var isDeathsHeadFirstStage = false
    private var deathsHeadProjectile: Projectile? = null
    private var deathsHeadPosition: Offset? = null
    private var deathsHeadGameWidth: Float = 0f
    private var deathsHeadGameHeight: Float = 0f
    private var deathsHeadDelayTimer: Float = 0f
    private var isWaitingForSecondExplosion: Boolean = false

    /**
     * Creates an explosion at the specified position.
     * @param position The position of the explosion
     * @param proj The projectile that caused the explosion (null for other explosion sources)
     * @param gameWidth Width of the game area
     * @param gameHeight Height of the game area
     */
    fun createExplosion(position: Offset, proj: Projectile? = null, gameWidth: Float, gameHeight: Float) {
        // Determine explosion radius based on projectile type
        val explosionRadius = if (proj?.type == ProjectileType.DEATHS_HEAD && !isDeathsHeadFirstStage) {
            // Special handling for Death's Head projectile - first stage
            // Store information for the second stage
            isDeathsHeadFirstStage = true
            deathsHeadProjectile = proj
            deathsHeadPosition = position
            deathsHeadGameWidth = gameWidth
            deathsHeadGameHeight = gameHeight

            // Use half the blast radius for first explosion
            proj.blastRadius / 2
        } else {
            // Normal explosion for other projectiles
            proj?.blastRadius ?: 90f
        }

        // Create the explosion
        explosion = Explosion(
            position = position,
            initialRadius = 10f, // Start with a small radius
            maxRadius = explosionRadius,
            timeRemaining = 0.5f // Half a second duration
        )

        // Deform terrain if the explosion is colliding with it
        if (terrainManager.isCollidingWithTerrain(position)) {
            terrainManager.deformTerrain(position, explosionRadius, gameWidth, gameHeight)

            // Update player positions to account for terrain deformation
            // Use animation for falling players
            playerManager.updatePlayerPositions(terrainManager::getTerrainHeightAtX, animate = true)
        }

        // Check for players within blast radius and apply damage
        applyBlastDamageToPlayers(position, explosionRadius, proj)
    }

    /**
     * Updates the explosion animation.
     * @param deltaTime Time elapsed since the last update in seconds
     */
    fun updateExplosion(deltaTime: Float) {
        // Handle the delay between Death's Head explosions
        if (isWaitingForSecondExplosion) {
            deathsHeadDelayTimer -= deltaTime
            if (deathsHeadDelayTimer <= 0) {
                isWaitingForSecondExplosion = false

                // Create the second explosion with full blast radius
                val position = deathsHeadPosition!!
                val proj = deathsHeadProjectile!!

                // Create a new explosion at the same position with full blast radius
                explosion = Explosion(
                    position = position,
                    initialRadius = 10f, // Start small again for better visual effect
                    maxRadius = proj.blastRadius,
                    timeRemaining = 0.5f // Half a second duration
                )

                // Apply terrain deformation and damage for the second explosion
                if (terrainManager.isCollidingWithTerrain(position)) {
                    terrainManager.deformTerrain(position, proj.blastRadius, deathsHeadGameWidth, deathsHeadGameHeight)
                    playerManager.updatePlayerPositions(terrainManager::getTerrainHeightAtX, animate = true)
                }

                // Apply damage for the second explosion
                applyBlastDamageToPlayers(position, proj.blastRadius, proj)

                // Clear stored Death's Head data
                deathsHeadProjectile = null
                deathsHeadPosition = null
            }
            return
        }

        explosion?.let { exp ->
            val newTimeRemaining = exp.timeRemaining - deltaTime
            if (newTimeRemaining <= 0) {
                // Explosion is finished
                explosion = null

                // Check if this was the first stage of a Death's Head explosion
                if (isDeathsHeadFirstStage && deathsHeadProjectile != null && deathsHeadPosition != null) {
                    // Set up for the second explosion after a delay
                    isDeathsHeadFirstStage = false
                    isWaitingForSecondExplosion = true
                    deathsHeadDelayTimer = 0.3f // 0.3 seconds delay between explosions
                }
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
     * Applies damage to players within the blast radius.
     * Damage decreases with distance from explosion center.
     * @param explosionPosition The center of the explosion
     * @param blastRadius The radius of the explosion
     * @param projectile The projectile that caused the explosion (null for other explosion sources)
     * @return True if game over condition is reached
     */
    private fun applyBlastDamageToPlayers(explosionPosition: Offset, blastRadius: Float, projectile: Projectile? = null): Boolean {
        // Get damage values from projectile or use defaults
        val minDamage = projectile?.damageOuterBlastRadius ?: 10
        val maxDamage = projectile?.maxDamage ?: 100
        var gameOver = false

        // Check each player
        playerManager.players.forEachIndexed { index, player ->
            // Calculate distance from player to explosion
            val distance = sqrt(
                (player.position.x - explosionPosition.x) * (player.position.x - explosionPosition.x) +
                (player.position.y - explosionPosition.y) * (player.position.y - explosionPosition.y)
            )

            // Only apply damage if player is within blast radius
            if (distance <= blastRadius) {
                // Calculate damage based on distance (closer = more damage)
                // Using a quadratic falloff for less severe damage reduction
                val distanceRatio = distance / blastRadius
                val damageFactor = (1.0f - distanceRatio) * (1.0f - distanceRatio) // Quadratic falloff

                // Calculate damage: at center (distanceRatio=0) = maxDamage, at edge (distanceRatio=1) = minDamage
                val damage = (minDamage + (maxDamage - minDamage) * damageFactor).toInt()

                // Apply damage if it's greater than 0
                if (damage > 0) {
                    val result = playerManager.applyDamageToPlayer(index, damage)
                    if (result) {
                        gameOver = true
                    }
                }
            }
        }

        return gameOver
    }
}
