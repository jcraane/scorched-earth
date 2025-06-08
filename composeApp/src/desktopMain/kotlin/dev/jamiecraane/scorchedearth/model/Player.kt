package dev.jamiecraane.scorchedearth.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import dev.jamiecraane.scorchedearth.inventory.ProjectileType
import dev.jamiecraane.scorchedearth.inventory.GenericInventory
import dev.jamiecraane.scorchedearth.inventory.ShieldType
import dev.jamiecraane.scorchedearth.inventory.ItemType
import dev.jamiecraane.scorchedearth.shield.Shield

/**
 * Defines the type of player (human or CPU).
 */
enum class PlayerType {
    HUMAN,
    CPU
}

/**
 * Represents a player in the game.
 */
data class Player(
    val position: Offset,
    val color: Color,
    var name: String = "",
    var health: Int = 100,
    var angle: Float = 0f,
    var power: Float = 50f,
    var selectedProjectileType: ProjectileType = ProjectileType.BABY_MISSILE,
    var money: Int = 10000,
    val inventory: GenericInventory = GenericInventory(),
    var type: PlayerType = PlayerType.HUMAN,
    // Animation properties
    var isFalling: Boolean = false,
    var fallStartPosition: Offset? = null,
    var fallTargetPosition: Offset? = null,
    var fallProgress: Float = 0f,
    // Elimination tracking
    var eliminationOrder: Int = -1, // -1 means not eliminated yet
    // Shield properties
    var activeShield: Shield? = null
) {
    /**
     * Gets the selected shield type from the inventory.
     * @return The selected shield type, or null if no shield is selected
     */
    fun getSelectedShieldType(): ShieldType? {
        return inventory.getSelectedShield() as? ShieldType
    }

    /**
     * Toggles the selection of a shield in the inventory.
     * @param shieldType The shield type to toggle
     * @return True if the shield is now selected, false if it was deselected
     */
    fun toggleShieldSelection(shieldType: ShieldType): Boolean {
        return inventory.toggleShieldSelection(shieldType)
    }

    /**
     * Checks if a shield is currently selected in the inventory.
     * @param shieldType The shield type to check
     * @return True if the specified shield is selected
     */
    fun isShieldSelected(shieldType: ShieldType): Boolean {
        return inventory.isShieldSelected(shieldType)
    }

    /**
     * Activates the selected shield if it's available in the inventory.
     * @return True if shield was activated, false otherwise
     */
    fun activateShield(): Boolean {
        val shieldType = getSelectedShieldType() ?: return false

        // Check if player has this shield type in inventory
        if (!inventory.hasItem(shieldType)) {
            return false
        }

        // Remove shield from inventory
        if (!inventory.removeItem(shieldType, 1)) {
            return false
        }

        // Activate shield
        activeShield = Shield(shieldType)
        return true
    }

    /**
     * Deactivates the current shield.
     */
    fun deactivateShield() {
        activeShield = null
    }

    /**
     * Checks if the player has an active shield.
     */
    fun hasActiveShield(): Boolean {
        return activeShield != null && !activeShield!!.isDepleted()
    }
}
