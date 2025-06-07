package dev.jamiecraane.scorchedearth.inventory

import dev.jamiecraane.scorchedearth.inventory.ProjectileType

/**
 * Represents an item in a player's inventory.
 * @param type The type of projectile this item represents
 * @param quantity The number of this item in the inventory
 */
data class Item(
    val type: ProjectileType,
    var quantity: Int
)

/**
 * Represents a player's inventory of items.
 */
class Inventory {
    private val items = mutableMapOf<ProjectileType, Item>()

    /**
     * Adds the specified quantity of an item to the inventory.
     * @param type The type of projectile to add
     * @param quantity The quantity to add
     */
    fun addItem(type: ProjectileType, quantity: Int) {
        val existingItem = items[type]
        if (existingItem != null) {
            existingItem.quantity += quantity
        } else {
            items[type] = Item(type, quantity)
        }
    }

    /**
     * Removes the specified quantity of an item from the inventory.
     * @param type The type of projectile to remove
     * @param quantity The quantity to remove
     * @return True if the item was successfully removed, false if there weren't enough items
     */
    fun removeItem(type: ProjectileType, quantity: Int): Boolean {
        val existingItem = items[type] ?: return false

        if (existingItem.quantity < quantity) {
            return false
        }

        existingItem.quantity -= quantity

        if (existingItem.quantity <= 0) {
            items.remove(type)
        }

        return true
    }

    /**
     * Gets the quantity of a specific item type in the inventory.
     * @param type The type of projectile to check
     * @return The quantity of the item, or 0 if not present
     */
    fun getItemQuantity(type: ProjectileType): Int {
        return items[type]?.quantity ?: 0
    }

    /**
     * Gets all items in the inventory.
     * @return A list of all items
     */
    fun getAllItems(): List<Item> {
        return items.values.toList()
    }

    /**
     * Checks if the inventory has at least the specified quantity of an item.
     * @param type The type of projectile to check
     * @param quantity The minimum quantity required
     * @return True if the inventory has at least the specified quantity
     */
    fun hasItem(type: ProjectileType, quantity: Int = 1): Boolean {
        return getItemQuantity(type) >= quantity
    }
}
