package dev.jamiecraane.scorchedearth.model

import androidx.compose.ui.geometry.Offset
import dev.jamiecraane.scorchedearth.engine.ProjectileType

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
