package dev.jamiecraane.scorchedearth.shield

import dev.jamiecraane.scorchedearth.inventory.ShieldType

/**
 * Represents an active shield with its current health and properties.
 * @param type The type of shield
 * @param currentHealth The current health of the shield (starts at max health)
 */
data class Shield(
    val type: ShieldType,
    var currentHealth: Int = type.maxHealth
) {
    /**
     * Calculates the shield's health percentage (0.0 to 1.0).
     * Used for rendering transparency.
     */
    fun getHealthPercentage(): Float {
        return currentHealth.toFloat() / type.maxHealth.toFloat()
    }

    /**
     * Applies damage to the shield.
     * @param damage The amount of damage to apply
     * @return The remaining damage that wasn't absorbed by the shield (0 if shield absorbed all damage)
     */
    fun applyDamage(damage: Int): Int {
        // If damage is less than or equal to current health, shield absorbs all damage
        if (damage <= currentHealth) {
            currentHealth -= damage
            return 0
        }

        // If damage exceeds current health, shield absorbs what it can and returns remaining damage
        val remainingDamage = damage - currentHealth
        currentHealth = 0
        return remainingDamage
    }

    /**
     * Checks if the shield is depleted (health <= 0).
     */
    fun isDepleted(): Boolean {
        return currentHealth <= 0
    }
}
