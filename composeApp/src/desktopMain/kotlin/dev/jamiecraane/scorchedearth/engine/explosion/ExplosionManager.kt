package dev.jamiecraane.scorchedearth.engine.explosion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import dev.jamiecraane.scorchedearth.engine.player.PlayerManager
import dev.jamiecraane.scorchedearth.engine.terrain.TerrainManager
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

    /**
     * Creates an explosion at the specified position.
     * @param position The position of the explosion
     * @param proj The projectile that caused the explosion (null for other explosion sources)
     * @param gameWidth Width of the game area
     * @param gameHeight Height of the game area
     */
    fun createExplosion(position: Offset, proj: Projectile? = null, gameWidth: Float, gameHeight: Float) {
        val explosionRadius = proj?.blastRadius ?: 90f
        explosion = Explosion(
            position = position,
            initialRadius = 10f, // Start with a small radius
            maxRadius = explosionRadius,
            timeRemaining = 0.5f // Half a second duration
        )

        // Deform terrain if the explosion is colliding with it
        if (terrainManager.isCollidingWithTerrain(position)) {
            terrainManager.deformTerrain(position, explosionRadius, gameWidth, gameHeight)
        }

        // Check for players within blast radius and apply damage
        applyBlastDamageToPlayers(position, explosionRadius, proj)
    }

    /**
     * Updates the explosion animation.
     * @param deltaTime Time elapsed since the last update in seconds
     */
    fun updateExplosion(deltaTime: Float) {
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
     * Applies damage to players within the blast radius.
     * Damage decreases with distance from explosion center.
     * @param explosionPosition The center of the explosion
     * @param blastRadius The radius of the explosion
     * @param projectile The projectile that caused the explosion (null for other explosion sources)
     * @return True if game over condition is reached
     */
    private fun applyBlastDamageToPlayers(explosionPosition: Offset, blastRadius: Float, projectile: Projectile? = null): Boolean {
        // Get damage values from projectile or use defaults
        val minDamage = projectile?.minDamage ?: 10
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
                // Using a linear falloff between maxDamage (direct hit) and minDamage (edge of blast)
                val distanceRatio = distance / blastRadius
                val damageFactor = 1.0f - distanceRatio

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
