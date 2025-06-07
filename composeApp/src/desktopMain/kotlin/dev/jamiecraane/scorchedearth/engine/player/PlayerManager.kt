package dev.jamiecraane.scorchedearth.engine.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import dev.jamiecraane.scorchedearth.inventory.ProjectileType
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

    /**
     * Generates players with positions scaled to the current game dimensions.
     * @param width Width of the game area
     * @param height Height of the game area
     * @param numberOfPlayers Number of players to generate (2-10)
     * @return List of players
     */
    fun generatePlayers(width: Float, height: Float, numberOfPlayers: Int = 2): List<Player> {
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
     */
    fun updatePlayerPositions(getTerrainHeightAtX: (Float) -> Float) {
        val updatedPlayers = players.map { player ->
            // Find the terrain height at the player's x-coordinate
            val terrainHeight = getTerrainHeightAtX(player.position.x)

            // Create a new player with updated y-position
            // Subtract a small offset to place the player visibly on top of the terrain
            player.copy(position = Offset(player.position.x, terrainHeight - 15f))
        }

        players = updatedPlayers
    }

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

        // Apply damage
        val newHealth = (player.health - damage).coerceAtLeast(0)
        updatedPlayers[playerIndex] = player.copy(health = newHealth)
        println("[DEBUG_LOG] PlayerManager.applyDamageToPlayer: New health for ${player.name}=${newHealth}")

        // Update the players list
        players = updatedPlayers
        println("[DEBUG_LOG] PlayerManager.applyDamageToPlayer: players after=${players.map { "${it.name}(${it.health})" }}")

        // Count how many players are still alive
        val alivePlayers = players.count { it.health > 0 }
        println("[DEBUG_LOG] PlayerManager.applyDamageToPlayer: alivePlayers=$alivePlayers")

        // Check for game over condition - only one player alive
        val gameOver = alivePlayers <= 1
        println("[DEBUG_LOG] PlayerManager.applyDamageToPlayer: gameOver=$gameOver")
        return gameOver
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
}
