package dev.jamiecraane.scorchedearth.engine

import androidx.compose.ui.geometry.Offset
import dev.jamiecraane.scorchedearth.model.Player
import kotlin.math.atan2
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Controller for CPU players that handles AI decision making.
 */
class CPUPlayerController(private val game: ScorchedEarthGame) {

    /**
     * Makes a decision for the CPU player and updates their angle and power.
     * @param player The CPU player to make a decision for
     * @return True if a decision was made successfully
     */
    fun makeDecision(player: Player): Boolean {
        println("[DEBUG_LOG] CPU making decision for player: ${player.name}")

        // Find the nearest opponent to target
        val target = findNearestOpponent(player)

        if (target == null) {
            println("[DEBUG_LOG] CPU couldn't find a target")
            return false
        }

        println("[DEBUG_LOG] CPU found target: ${target.name} at position ${target.position}")

        // Calculate angle and power to hit the target
        val (angle, power) = calculateShotParameters(player, target)
        println("[DEBUG_LOG] CPU calculated parameters: angle=$angle, power=$power")

        // Update the player's angle and power
        updatePlayerParameters(player, angle, power)
        println("[DEBUG_LOG] CPU selected projectile: ${player.selectedProjectileType.displayName}")

        return true
    }

    /**
     * Finds the nearest opponent to the given player.
     * @param player The player to find an opponent for
     * @return The nearest opponent, or null if no opponents are found
     */
    private fun findNearestOpponent(player: Player): Player? {
        var nearestOpponent: Player? = null
        var shortestDistance = Float.MAX_VALUE

        for (opponent in game.players) {
            // Skip the current player
            if (opponent == player) continue

            // Calculate distance to opponent
            val distance = calculateDistance(player.position, opponent.position)

            // Update nearest opponent if this one is closer
            if (distance < shortestDistance) {
                shortestDistance = distance
                nearestOpponent = opponent
            }
        }

        return nearestOpponent
    }

    /**
     * Calculates the distance between two positions.
     * @param pos1 The first position
     * @param pos2 The second position
     * @return The distance between the positions
     */
    private fun calculateDistance(pos1: Offset, pos2: Offset): Float {
        val dx = pos2.x - pos1.x
        val dy = pos2.y - pos1.y
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Calculates the angle and power needed to hit the target.
     * @param player The player making the shot
     * @param target The target player
     * @return A Pair of angle and power values
     */
    private fun calculateShotParameters(player: Player, target: Player): Pair<Float, Float> {
        // Calculate basic direction to target
        val dx = target.position.x - player.position.x
        val dy = target.position.y - player.position.y

        // Calculate base angle (in radians)
        val baseAngle = atan2(-dy, dx) // Negative dy because y-axis is inverted in UI coordinates

        // Convert to degrees and adjust for the game's angle system
        // In the game, 0 degrees is horizontal right, -90 is down, 90 is up
        var angleDegrees = (baseAngle * 180 / PI).toFloat() - 90

        // Adjust angle based on distance and wind
        val distance = calculateDistance(player.position, target.position)
        val windAdjustment = game.wind * 0.5f // Wind adjustment factor

        // Add some randomness to make CPU less perfect
        val randomAngleAdjustment = Random.nextFloat() * 10f - 5f // ±5 degrees
        angleDegrees += randomAngleAdjustment

        // Adjust for wind
        angleDegrees += windAdjustment

        // Clamp angle to valid range (-90 to 90)
        angleDegrees = angleDegrees.coerceIn(-90f, 90f)

        // Calculate power based on distance
        // Base power calculation - adjust these values based on game testing
        val basePower = (distance / 20f).coerceIn(20f, 100f)

        // Add some randomness to power
        val randomPowerAdjustment = Random.nextFloat() * 10f - 5f // ±5 power
        var power = basePower + randomPowerAdjustment

        // Clamp power to valid range (10 to 100)
        power = power.coerceIn(10f, 100f)

        return Pair(angleDegrees, power)
    }

    /**
     * Updates the player's angle and power parameters.
     * @param player The player to update
     * @param angle The new angle
     * @param power The new power
     */
    private fun updatePlayerParameters(player: Player, angle: Float, power: Float) {
        player.angle = angle
        player.power = power

        // Select a projectile type based on what's available in inventory
        selectProjectileType(player)
    }

    /**
     * Selects an appropriate projectile type based on what's available in the player's inventory.
     * @param player The player to select a projectile for
     */
    private fun selectProjectileType(player: Player) {
        // Get all available projectile types in inventory
        val availableTypes = ProjectileType.values().filter {
            player.inventory.hasItem(it)
        }

        if (availableTypes.isEmpty()) {
            // No projectiles available, keep current selection
            return
        }

        // For now, just select a random available projectile type
        player.selectedProjectileType = availableTypes.random()
    }
}
