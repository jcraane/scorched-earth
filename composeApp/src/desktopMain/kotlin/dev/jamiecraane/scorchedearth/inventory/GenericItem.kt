package dev.jamiecraane.scorchedearth.inventory

/**
 * Represents a generic item in a player's inventory.
 * @param type The type of item this represents
 * @param quantity The number of this item in the inventory
 */
data class GenericItem(
    val type: ItemType,
    var quantity: Int
)

/**
 * Represents a player's inventory of items.
 */
class GenericInventory {
    private val items = mutableMapOf<ItemType, GenericItem>()

    /**
     * Adds the specified quantity of an item to the inventory.
     * @param type The type of item to add
     * @param quantity The quantity to add
     */
    fun addItem(type: ItemType, quantity: Int) {
        val existingItem = items[type]
        if (existingItem != null) {
            existingItem.quantity += quantity
        } else {
            items[type] = GenericItem(type, quantity)
        }
    }

    /**
     * Removes the specified quantity of an item from the inventory.
     * @param type The type of item to remove
     * @param quantity The quantity to remove
     * @return True if the item was successfully removed, false if there weren't enough items
     */
    fun removeItem(type: ItemType, quantity: Int): Boolean {
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
     * @param type The type of item to check
     * @return The quantity of the item, or 0 if not present
     */
    fun getItemQuantity(type: ItemType): Int {
        return items[type]?.quantity ?: 0
    }

    /**
     * Gets all items in the inventory.
     * @return A list of all items
     */
    fun getAllItems(): List<GenericItem> {
        return items.values.toList()
    }

    /**
     * Checks if the inventory has at least the specified quantity of an item.
     * @param type The type of item to check
     * @param quantity The minimum quantity required
     * @return True if the inventory has at least the specified quantity
     */
    fun hasItem(type: ItemType, quantity: Int = 1): Boolean {
        return getItemQuantity(type) >= quantity
    }
}
