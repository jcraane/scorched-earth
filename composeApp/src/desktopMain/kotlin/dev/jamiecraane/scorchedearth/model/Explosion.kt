package dev.jamiecraane.scorchedearth.model

import androidx.compose.ui.geometry.Offset

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
