package dev.jamiecraane.scorchedearth.inventory

/**
 * Common interface for all item types in the game.
 */
interface ItemType {
    /**
     * The display name of the item shown to the player.
     */
    val displayName: String

    /**
     * The cost of the item in the shop.
     */
    val cost: Int

    /**
     * The quantity of items purchased at once.
     */
    val purchaseQuantity: Int
}
