package dev.jamiecraane.scorchedearth.model

import androidx.compose.ui.geometry.Offset
import dev.jamiecraane.scorchedearth.inventory.ProjectileType

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
 * @param bounceCount Number of times the projectile has bounced (for Leapfrog)
 * @param maxBounces Maximum number of bounces allowed (for Leapfrog)
 * @param isRolling Whether the projectile is currently rolling (for Roller)
 * @param rollingDistance Distance the projectile has rolled so far (for Roller)
 * @param maxRollingDistance Maximum distance the projectile can roll (for Roller)
 */
data class Projectile(
    val position: Offset,
    val velocity: Offset,
    val type: ProjectileType,
    val minDamage: Int = type.minDamage,
    val maxDamage: Int = type.maxDamage,
    val blastRadius: Float = type.blastRadius,
    val trail: List<Offset> = listOf(),
    val maxTrailLength: Int = 15,
    val bounceCount: Int = 0,
    val maxBounces: Int = 5,
    val isRolling: Boolean = false,
    val rollingDistance: Float = 0f,
    val maxRollingDistance: Float = 500f
)
