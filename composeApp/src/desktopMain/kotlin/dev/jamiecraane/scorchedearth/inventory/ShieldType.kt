package dev.jamiecraane.scorchedearth.inventory

/**
 * Defines the different types of shields available in the game.
 */
enum class ShieldType(
    override val displayName: String,
    val maxHealth: Int,
    override val cost: Int,
    override val purchaseQuantity: Int = 1,
) : ItemType {
    BASIC_SHIELD("Basic Shield", 150, 2000, purchaseQuantity = 1)
    // More shield types can be added later
}
