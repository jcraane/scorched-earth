package dev.jamiecraane.scorchedearth.inventory

import androidx.compose.ui.graphics.Color

/**
 * Defines the different types of shields available in the game.
 */
enum class ShieldType(
    override val displayName: String,
    val maxHealth: Int,
    override val cost: Int,
    override val purchaseQuantity: Int = 1,
    val color: Color = Color.Blue
) : ItemType {
    BASIC_SHIELD("Basic Shield", 150, 2000, purchaseQuantity = 1),
    HEAVY_SHIELD("Heavy Shield", 250, 6000, purchaseQuantity = 1, color = Color.Yellow),
    FORCE_SHIELD("Force Shield", 500, 8000, purchaseQuantity = 1, color = Color.Green)
}
