package dev.jamiecraane.scorchedearth.engine.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import dev.jamiecraane.scorchedearth.inventory.ProjectileType
import dev.jamiecraane.scorchedearth.inventory.ShieldType
import dev.jamiecraane.scorchedearth.model.Player
import dev.jamiecraane.scorchedearth.model.PlayerType
import kotlin.math.sqrt

/**
 * Manages player generation, positioning, and damage.
 */
class PlayerManager {
    // Players
    var players by mutableStateOf<List<Player>>(listOf())

    // Current player index
    var currentPlayerIndex by mutableStateOf(0)

    // Game dimensions for calculating fall damage
    private var gameHeight: Float = 1200f

    /**
     * Generates players with positions scaled to the current game dimensions.
     * @param width Width of the game area
     * @param height Height of the game area
     * @param numberOfPlayers Number of players to generate (2-10)
     * @return List of players
     */
    fun generatePlayers(width: Float, height: Float, numberOfPlayers: Int = 2): List<Player> {
        // Store game height for fall damage calculations
        this.gameHeight = height

        val baseY = height * 0.7f - 20f
        val players = mutableListOf<Player>()

        // Ensure number of players is within valid range
        val validNumberOfPlayers = numberOfPlayers.coerceIn(2, 10)

        // Generate colors for players
        val colors = listOf(
            Color.Red,
            Color.Blue,
            Color.Green,
            Color.Yellow,
            Color.Magenta,
            Color.Cyan,
            Color.White,
            Color(0xFFFF8000), // Orange
            Color(0xFF800080), // Purple
            Color(0xFF008080)  // Teal
        )

        // Calculate positions evenly distributed across the width
        for (i in 0 until validNumberOfPlayers) {
            // Calculate position based on player index
            // For 2 players: 10% and 90% of width
            // For more players: evenly distributed
            val xPosition = width * (0.1f + 0.8f * i.toFloat() / (validNumberOfPlayers - 1))

            players.add(
                Player(
                    position = Offset(xPosition, baseY),
                    color = colors[i % colors.size]
                ).apply {
                    inventory.addItem(ProjectileType.BABY_MISSILE, 10)
                    inventory.addItem(ProjectileType.LEAPFROG, 3)
                    inventory.addItem(ProjectileType.TRACER, 10)
                    inventory.addItem(ProjectileType.ROLLER, 2)
                    inventory.addItem(ShieldType.BASIC_SHIELD, 1)
                    selectedShieldType = ShieldType.BASIC_SHIELD
                }
            )
        }

        this.players = players
        return players
    }

    /**
     * Updates player positions to match the terrain height at their x-coordinates.
     * This ensures players always sit on top of the terrain.
     * @param getTerrainHeightAtX Function to get the terrain height at a specific x-coordinate
     * @param animate Whether to animate the falling (true) or update instantly (false)
     */
    fun updatePlayerPositions(getTerrainHeightAtX: (Float) -> Float, animate: Boolean = false) {
        val updatedPlayers = players.map { player ->
            // Find the terrain height at the player's x-coordinate
            val terrainHeight = getTerrainHeightAtX(player.position.x)
            // Calculate the target position (on top of the terrain)
            val targetPosition = Offset(player.position.x, terrainHeight - 15f)

            // Check if the player needs to fall (current y position is above the terrain)
            val needsToFall = player.position.y < targetPosition.y

            if (needsToFall && animate) {
                // Start falling animation
                player.copy(
                    isFalling = true,
                    fallStartPosition = player.position,
                    fallTargetPosition = targetPosition,
                    fallProgress = 0f
                )
            } else {
                // Immediately update position without animation
                player.copy(
                    position = targetPosition,
                    isFalling = false,
                    fallStartPosition = null,
                    fallTargetPosition = null,
                    fallProgress = 0f
                )
            }
        }

        players = updatedPlayers
    }

    /**
     * Updates falling animation for all players.
     * @param deltaTime Time elapsed since the last update in seconds
     * @return True if any player is still falling, false otherwise
     */
    fun updateFallingPlayers(deltaTime: Float): Boolean {
        // Animation speed (adjust as needed)
        val fallSpeed = 2.0f

        // Check if any player is falling
        val anyPlayerFalling = players.any { it.isFalling }
        if (!anyPlayerFalling) {
            return false
        }

        // Update falling players
        val updatedPlayers = players.map { player ->
            if (player.isFalling && player.fallStartPosition != null && player.fallTargetPosition != null) {
                // Update fall progress
                val newProgress = (player.fallProgress + deltaTime * fallSpeed).coerceAtMost(1.0f)

                // Calculate new position using linear interpolation
                val newPosition = if (newProgress >= 1.0f) {
                    // Animation complete

                    // Calculate fall damage when animation completes
                    val fallHeight = player.fallTargetPosition!!.y - player.fallStartPosition!!.y

                    // Create updated player with fall animation complete
                    var updatedPlayer = player.copy(
                        position = player.fallTargetPosition!!,
                        isFalling = false,
                        fallStartPosition = null,
                        fallTargetPosition = null,
                        fallProgress = 0f
                    )

                    // Apply fall damage if applicable
                    if (fallHeight > 0) {
                        // Calculate fall damage - max 30 damage for full level height
                        // Damage decreases linearly with height
                        val damageRatio = fallHeight / gameHeight
                        val fallDamage = (30 * damageRatio).toInt()

                        if (fallDamage > 0) {
                            println("[DEBUG_LOG] Player ${player.name} took $fallDamage fall damage from height $fallHeight")
                            // Apply damage directly to the updated player
                            updatedPlayer = updatedPlayer.copy(
                                health = (updatedPlayer.health - fallDamage).coerceAtLeast(0)
                            )
                            println("[DEBUG_LOG] Player ${updatedPlayer.name} health after fall damage: ${updatedPlayer.health}")
                        }
                    }

                    updatedPlayer
                } else {
                    // Animation in progress
                    val startPos = player.fallStartPosition!!
                    val targetPos = player.fallTargetPosition!!
                    val x = startPos.x
                    val y = startPos.y + (targetPos.y - startPos.y) * newProgress

                    player.copy(
                        position = Offset(x, y),
                        fallProgress = newProgress
                    )
                }

                newPosition
            } else {
                player
            }
        }

        players = updatedPlayers

        // Return true if any player is still falling
        return players.any { it.isFalling }
    }

    // Track the elimination order
    private var eliminationCount = 0

    /**
     * Applies damage to a player. Players with 0 health are considered dead but remain in the list.
     * @param playerIndex The index of the player to damage
     * @param damage The amount of damage to apply
     * @return GameOver flag if all players except one are dead
     */
    fun applyDamageToPlayer(playerIndex: Int, damage: Int): Boolean {
        println("[DEBUG_LOG] PlayerManager.applyDamageToPlayer: playerIndex=$playerIndex, damage=$damage")
        println("[DEBUG_LOG] PlayerManager.applyDamageToPlayer: players before=${players.map { "${it.name}(${it.health})" }}")

        val updatedPlayers = players.toMutableList()

        // Check if the playerIndex is valid
        if (playerIndex < 0 || playerIndex >= updatedPlayers.size) {
            println("[DEBUG_LOG] PlayerManager.applyDamageToPlayer: Invalid player index")
            return false // Skip if index is out of bounds
        }

        val player = updatedPlayers[playerIndex]
        println("[DEBUG_LOG] PlayerManager.applyDamageToPlayer: Applying damage to ${player.name}, current health=${player.health}")

        // Check if player has an active shield
        var remainingDamage = damage
        val updatedPlayer = player.copy()

        if (player.hasActiveShield()) {
            println("[DEBUG_LOG] PlayerManager.applyDamageToPlayer: Player ${player.name} has an active shield with health ${player.activeShield!!.currentHealth}")

            // Apply damage to shield first
            remainingDamage = player.activeShield!!.applyDamage(damage)

            println("[DEBUG_LOG] PlayerManager.applyDamageToPlayer: Shield absorbed ${damage - remainingDamage} damage, remaining damage: $remainingDamage")

            // If shield is depleted, remove it
            if (player.activeShield!!.isDepleted()) {
                println("[DEBUG_LOG] PlayerManager.applyDamageToPlayer: Shield depleted, removing it")
                updatedPlayer.activeShield = null
            } else {
                // Copy the shield to the updated player
                updatedPlayer.activeShield = player.activeShield
            }
        }

        // Apply remaining damage to player health
        val newHealth = (player.health - remainingDamage).coerceAtLeast(0)
        updatedPlayer.health = newHealth

        println("[DEBUG_LOG] PlayerManager.applyDamageToPlayer: Applied $remainingDamage damage to player health")

        // Check if player was just eliminated
        val wasJustEliminated = player.health > 0 && newHealth == 0

        // If player was just eliminated, set elimination order
        if (wasJustEliminated) {
            updatedPlayer.eliminationOrder = eliminationCount++
            println("[DEBUG_LOG] PlayerManager.applyDamageToPlayer: Player ${player.name} eliminated, order=${updatedPlayer.eliminationOrder}")
        }

        updatedPlayers[playerIndex] = updatedPlayer
        println("[DEBUG_LOG] PlayerManager.applyDamageToPlayer: New health for ${player.name}=${newHealth}")

        // Update the players list
        players = updatedPlayers
        println("[DEBUG_LOG] PlayerManager.applyDamageToPlayer: players after=${players.map { "${it.name}(${it.health})" }}")

        // Count how many players are still alive
        val alivePlayers = players.count { it.health > 0 }
        println("[DEBUG_LOG] PlayerManager.applyDamageToPlayer: alivePlayers=$alivePlayers")

        // Check for game over condition - only one player alive
        val gameOver = alivePlayers <= 1

        // If only one player is left alive, mark them as the last one (winner)
        if (gameOver && alivePlayers == 1) {
            val winner = players.first { it.health > 0 }
            winner.eliminationOrder = players.size - 1 // Highest elimination order = winner
            println("[DEBUG_LOG] PlayerManager.applyDamageToPlayer: Winner is ${winner.name}")
        }

        println("[DEBUG_LOG] PlayerManager.applyDamageToPlayer: gameOver=$gameOver")
        return gameOver
    }

    /**
     * Resets the elimination tracking for a new round.
     */
    fun resetEliminationTracking() {
        eliminationCount = 0
        players = players.map {
            it.copy(eliminationOrder = -1)
        }
    }

    /**
     * Checks if a point is colliding with a player.
     * @param position The position to check
     * @param player The player to check collision with
     * @return True if the position is within the player's collision radius
     */
    fun isCollidingWithPlayer(position: Offset, player: Player): Boolean {
        val playerRadius = 15f // Same as the radius used for drawing
        val distance = sqrt(
            (position.x - player.position.x) * (position.x - player.position.x) +
            (position.y - player.position.y) * (position.y - player.position.y)
        )
        return distance <= playerRadius
    }

    /**
     * Advances to the next player.
     */
    fun nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
    }

    /**
     * Gets the current player.
     * @return The current player
     */
    fun getCurrentPlayer(): Player {
        return players[currentPlayerIndex]
    }

    /**
     * Attempts to purchase a missile for the current player.
     * @param projectileType The type of projectile to purchase
     * @return True if the purchase was successful, false if the player doesn't have enough money
     */
    fun purchaseMissile(projectileType: ProjectileType): Boolean {
        val player = players[currentPlayerIndex]

        // Check if player has enough money
        if (player.money < projectileType.cost) {
            return false
        }

        // Create a new player with updated money
        val updatedPlayer = player.copy(
            money = player.money - projectileType.cost
        )

        // Add the missile to the player's inventory
        updatedPlayer.inventory.addItem(projectileType, projectileType.purchaseQuantity)

        // Update the players list
        val updatedPlayers = players.toMutableList()
        updatedPlayers[currentPlayerIndex] = updatedPlayer
        players = updatedPlayers

        return true
    }

    /**
     * Attempts to purchase a shield for the current player.
     * @param shieldType The type of shield to purchase
     * @return True if the purchase was successful, false if the player doesn't have enough money
     */
    fun purchaseShield(shieldType: ShieldType): Boolean {
        val player = players[currentPlayerIndex]

        // Check if player has enough money
        if (player.money < shieldType.cost) {
            return false
        }

        // Create a new player with updated money
        val updatedPlayer = player.copy(
            money = player.money - shieldType.cost
        )

        // Add the shield to the player's inventory
        updatedPlayer.inventory.addItem(shieldType, shieldType.purchaseQuantity)

        // Update the players list
        val updatedPlayers = players.toMutableList()
        updatedPlayers[currentPlayerIndex] = updatedPlayer
        players = updatedPlayers

        return true
    }
}
