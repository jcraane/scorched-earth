package dev.jamiecraane.scorchedearth.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.jamiecraane.scorchedearth.engine.explosion.ExplosionManager
import dev.jamiecraane.scorchedearth.engine.player.PlayerManager
import dev.jamiecraane.scorchedearth.engine.projectile.ProjectileManager
import dev.jamiecraane.scorchedearth.engine.terrain.TerrainManager
import dev.jamiecraane.scorchedearth.inventory.ProjectileType
import dev.jamiecraane.scorchedearth.inventory.ShieldType
import dev.jamiecraane.scorchedearth.shield.Shield
import dev.jamiecraane.scorchedearth.sky.SkyStyle
import dev.jamiecraane.scorchedearth.weather.WeatherManager
import dev.jamiecraane.scorchedearth.weather.WeatherType
import kotlin.random.Random

/**
 * Main game engine class that manages the game state and logic.
 * This class coordinates the different components of the game.
 */
class ScorchedEarthGame(private val numberOfPlayers: Int = 2, val totalRounds: Int = 1) {
    /**
     * Data class to store player data between rounds.
     */
    private data class PlayerData(
        val name: String,
        val type: dev.jamiecraane.scorchedearth.model.PlayerType,
        val money: Int,
        val inventory: dev.jamiecraane.scorchedearth.inventory.GenericInventory,
        val selectedProjectileType: ProjectileType
    )
    // Current round
    var currentRound by mutableStateOf(1)
    // Game dimensions - these will be updated when the canvas size changes
    var gameWidth by mutableStateOf(1600f)
    var gameHeight by mutableStateOf(1200f)

    // Game state
    var gameState by mutableStateOf(GameState.WAITING_FOR_PLAYER)

    // Sky style - determines the background gradient
    var skyStyle by mutableStateOf(SkyStyle.AFTERNOON)

    // Weather type - determines the weather effects
    var weatherTypeState by mutableStateOf(WeatherType.NONE)

    // Component managers
    private val terrainManager = TerrainManager()
    private val playerManager = PlayerManager()
    private val explosionManager = ExplosionManager(terrainManager, playerManager)
    private val projectileManager = ProjectileManager(terrainManager, playerManager, explosionManager)
    private val weatherManager = WeatherManager()

    // Flag to indicate if players have been set externally
    private var playersSetExternally = false

    init {
        // Initialize the game
        terrainManager.generateTerrain(gameWidth, gameHeight)
        // Only generate players if they haven't been set externally
        if (!playersSetExternally) {
            playerManager.generatePlayers(gameWidth, gameHeight, numberOfPlayers)
        }
        projectileManager.setGameDimensions(gameWidth, gameHeight)
        projectileManager.generateWind()

        // Initialize weather manager
        weatherManager.setGameDimensions(gameWidth, gameHeight)
    }

    /**
     * Updates the game dimensions and regenerates content accordingly.
     * Call this when the window/canvas size changes.
     */
    fun updateDimensions(width: Float, height: Float) {
        gameWidth = width
        gameHeight = height
        terrainManager.generateTerrain(width, height)

        // Store player data before regenerating players
        val playerData = playerManager.players.map { player ->
            PlayerData(
                name = player.name,
                type = player.type,
                money = player.money,
                inventory = player.inventory,
                selectedProjectileType = player.selectedProjectileType
            )
        }

        // Regenerate players
        playerManager.generatePlayers(width, height, numberOfPlayers)

        // Restore player data
        playerManager.players.forEachIndexed { index, player ->
            if (index < playerData.size) {
                val data = playerData[index]
                player.name = data.name
                player.type = data.type
                player.money = data.money

                // Copy inventory items
                data.inventory.getAllItems().forEach { item ->
                    player.inventory.addItem(item.type, item.quantity)
                }

                // Set selected projectile type
                player.selectedProjectileType = data.selectedProjectileType
            }
        }

        // Update player positions to stick to the terrain
        // Don't animate when resizing - instant repositioning
        playerManager.updatePlayerPositions(terrainManager::getTerrainHeightAtX, animate = false)

        // Reset projectile and explosion if they exist to prevent out-of-bounds issues
        projectileManager.reset()

        // Update game dimensions in projectile manager
        projectileManager.setGameDimensions(width, height)

        // Update game dimensions in weather manager
        weatherManager.setGameDimensions(width, height)
    }

    /**
     * Sets the weather type.
     * @param type The weather type to set
     */
    fun setWeatherType(type: WeatherType) {
        weatherTypeState = type
        weatherManager.setWeatherType(type)
    }

    /**
     * Updates the game state for each frame.
     * @param deltaTime Time elapsed since the last update in seconds
     */
    fun update(deltaTime: Float) {
        when (gameState) {
            GameState.PROJECTILE_IN_FLIGHT -> {
                val turnEnded = projectileManager.updateProjectile(deltaTime)

                // Check if projectile is null (impact occurred) but game state hasn't changed
                // This handles the case where a tracer projectile impacts but turnEnded is false
                if (projectileManager.projectile == null && projectileManager.miniBombs.isEmpty()) {
                    // No projectiles in flight, check if round is over
                    val alivePlayers = players.count { it.health > 0 }
                    if (alivePlayers <= 1) {
                        // Round is over, show statistics
                        prepareNextRound()
                    } else if (!turnEnded) {
                        // If turnEnded is false, it means the player can fire again (e.g., after firing a tracer)
                        gameState = GameState.WAITING_FOR_PLAYER
                    }
                } else if (turnEnded) {
                    // Normal turn end handling
                    // Check if round is over (only one player alive)
                    val alivePlayers = players.count { it.health > 0 }
                    if (alivePlayers <= 1) {
                        // Round is over, show statistics
                        prepareNextRound()
                    } else {
                        gameState = GameState.WAITING_FOR_PLAYER
                    }
                }

                // Update mini-bombs if they exist
                if (projectileManager.miniBombs.isNotEmpty()) {
                    val miniBombsTurnEnded = projectileManager.updateMiniBombs(deltaTime)
                    if (miniBombsTurnEnded) {
                        // Check if round is over (only one player alive)
                        val alivePlayers = players.count { it.health > 0 }
                        if (alivePlayers <= 1) {
                            // Round is over, show statistics
                            prepareNextRound()
                        } else {
                            gameState = GameState.WAITING_FOR_PLAYER
                        }
                    }
                }
            }
            GameState.WAITING_FOR_PLAYER -> {
                // Check if current player is CPU
                val currentPlayer = playerManager.getCurrentPlayer()
                if (currentPlayer.type == dev.jamiecraane.scorchedearth.model.PlayerType.CPU) {
                    println("[DEBUG_LOG] Detected CPU player's turn: ${currentPlayer.name}, transitioning to AIMING state")
                    // CPU player's turn - set game state to AIMING to prevent multiple shots
                    gameState = GameState.AIMING
                }
            }
            GameState.AIMING -> {
                // This state is used to prevent CPU from firing multiple shots in a single frame
                // CPU player logic is handled in the CPUPlayerController
            }
            GameState.ROUND_STATISTICS -> {
                // Statistics screen is displayed, waiting for user to click "Next Round"
                // No updates needed here
            }
            else -> {} // No updates needed for other states
        }

        // Update explosion if it exists
        explosionManager.updateExplosion(deltaTime)

        // Update falling players animation
        playerManager.updateFallingPlayers(deltaTime)

        // Update weather effects
        weatherManager.update(deltaTime, wind)

        // Check for lightning strikes hitting players
        if (weatherTypeState == WeatherType.LIGHTNING && weatherManager.lightning != null) {
            println("[DEBUG_LOG] Checking for lightning hits, weatherType=$weatherTypeState, lightning=${weatherManager.lightning}")
            // Check each player
            players.forEachIndexed { index, player ->
                // Only check players that are still alive
                println("[DEBUG_LOG] Checking player $index (${player.name}): health=${player.health}, position=${player.position}")
                if (player.health > 0) {
                    val isHit = weatherManager.isPlayerHitByLightning(player.position, index)
                    println("[DEBUG_LOG] Player $index (${player.name}) is hit by lightning: $isHit")
                    if (isHit) {
                        // Apply lightning damage (10 damage)
                        println("[DEBUG_LOG] Applying ${weatherManager.lightning!!.damage} damage to player $index (${player.name})")
                        val beforeHealth = player.health
                        playerManager.applyDamageToPlayer(index, weatherManager.lightning!!.damage)
                        val afterHealth = players[index].health
                        println("[DEBUG_LOG] Player $index (${player.name}) health changed from $beforeHealth to $afterHealth")
                        println("[DEBUG_LOG] Players list after damage: ${players.map { "${it.name}(${it.health})" }}")
                    }
                }
            }
        } else {
            if (weatherManager.lightning != null) {
                println("[DEBUG_LOG] Lightning exists but weatherType is not LIGHTNING: weatherType=$weatherTypeState")
            }
        }
    }

    /**
     * Sets the terrain variance and regenerates the terrain.
     * @param variance The new terrain variance value (0-100)
     */
    fun setTerrainVariance(variance: Int) {
        terrainManager.setTerrainVariance(variance, gameWidth, gameHeight)
        // Don't animate when changing terrain variance - instant repositioning
        playerManager.updatePlayerPositions(terrainManager::getTerrainHeightAtX, animate = false)
    }

    /**
     * Attempts to purchase a missile for the current player.
     * @param projectileType The type of projectile to purchase
     * @return True if the purchase was successful, false if the player doesn't have enough money
     */
    fun purchaseMissile(projectileType: ProjectileType): Boolean {
        return playerManager.purchaseMissile(projectileType)
    }

    /**
     * Attempts to purchase a shield for the current player.
     * @param shieldType The type of shield to purchase
     * @return True if the purchase was successful, false if the player doesn't have enough money
     */
    fun purchaseShield(shieldType: ShieldType): Boolean {
        return playerManager.purchaseShield(shieldType)
    }

    /**
     * Toggles the selection of a shield type for the current player.
     * Also automatically activates the shield when selected and deactivates when deselected.
     * @param shieldType The type of shield to toggle
     * @return True if the shield is now selected, false if it was deselected
     */
    fun selectShield(shieldType: ShieldType): Boolean {
        val currentPlayer = playerManager.getCurrentPlayer()

        // First check if this shield is already selected
        val wasSelected = currentPlayer.isShieldSelected(shieldType)

        // If it was selected, deactivate it first
        if (wasSelected) {
            currentPlayer.deactivateShield()
        }

        // Now toggle the selection state
        val isSelected = currentPlayer.toggleShieldSelection(shieldType)

        // Activate shield if it's now selected
        if (isSelected) {
            // Check if player has this shield type in inventory
            if (currentPlayer.inventory.hasItem(shieldType)) {
                // Don't remove from inventory, just activate
                currentPlayer.activeShield = Shield(shieldType)
            }
        }

        return isSelected
    }

    /**
     * Activates the selected shield for the current player.
     * @return True if the shield was activated, false otherwise
     */
    fun activateShield(): Boolean {
        val currentPlayer = playerManager.getCurrentPlayer()
        return currentPlayer.activateShield()
    }

    /**
     * Deactivates the current player's shield.
     */
    fun deactivateShield() {
        val currentPlayer = playerManager.getCurrentPlayer()
        currentPlayer.deactivateShield()
    }

    /**
     * Fires a projectile with the given angle and power.
     * @param angle Angle in degrees (-90 to 90, where 0 = horizontal, -90 = down, 90 = up)
     * @param power Power factor (0-100)
     * @return True if the projectile was fired successfully, false otherwise
     */
    fun fireProjectile(angle: Float, power: Float): Boolean {
        val success = projectileManager.fireProjectile(angle, power)
        if (success) {
            gameState = GameState.PROJECTILE_IN_FLIGHT
        }
        return success
    }

    /**
     * Prepares to transition to the next round by showing round statistics.
     * This is called when a round ends (only one player left alive).
     * Awards money to players based on their position at the end of the round.
     */
    fun prepareNextRound() {
        // Award money based on player position
        val alivePlayers = players.filter { it.health > 0 }
        val eliminatedPlayers = players.filter { it.health == 0 }.sortedBy { it.eliminationOrder }

        println("[DEBUG_LOG] prepareNextRound: alivePlayers=${alivePlayers.map { "${it.name}(${it.health})" }}")
        println("[DEBUG_LOG] prepareNextRound: eliminatedPlayers=${eliminatedPlayers.map { "${it.name}(${it.health}, order=${it.eliminationOrder})" }}")

        // Update player money based on position
        val updatedPlayers = players.toMutableList()

        // First, handle eliminated players
        eliminatedPlayers.forEachIndexed { index, player ->
            // Calculate money: (num players - position) * 1000
            // Position is 0-based, so the first eliminated player has position 0
            val moneyForPosition = (players.size - index) * 1000

            println("[DEBUG_LOG] prepareNextRound: Eliminated player ${player.name} at position $index, money calculation: (${players.size} - $index) * 1000 = $moneyForPosition")

            // Only award money if it's positive
            if (moneyForPosition > 0) {
                // Find the player in the original list and update their money
                val playerIndex = players.indexOf(player)
                if (playerIndex >= 0) {
                    val updatedPlayer = updatedPlayers[playerIndex].copy(
                        money = updatedPlayers[playerIndex].money + moneyForPosition
                    )
                    updatedPlayers[playerIndex] = updatedPlayer
                    println("[DEBUG_LOG] Player ${player.name} earned $moneyForPosition money for position ${index + 1}")
                }
            }
        }

        // Then, handle alive players (winners)
        // There should be only one alive player at this point
        if (alivePlayers.isNotEmpty()) {
            // The winner gets (num players - position) * 1000 money
            // where position is the number of eliminated players
            val winner = alivePlayers.first()
            val winnerPosition = eliminatedPlayers.size
            val moneyForWinner = (players.size - winnerPosition) * 1000

            println("[DEBUG_LOG] prepareNextRound: Winner ${winner.name} at position $winnerPosition, money calculation: (${players.size} - $winnerPosition) * 1000 = $moneyForWinner")

            if (moneyForWinner > 0) {
                val winnerIndex = players.indexOf(winner)
                if (winnerIndex >= 0) {
                    val updatedWinner = updatedPlayers[winnerIndex].copy(
                        money = updatedPlayers[winnerIndex].money + moneyForWinner
                    )
                    updatedPlayers[winnerIndex] = updatedWinner
                    println("[DEBUG_LOG] Player ${winner.name} earned $moneyForWinner money for winning")
                }
            }
        }

        // Update the players list
        players = updatedPlayers

        // Change game state to show statistics
        gameState = GameState.ROUND_STATISTICS
    }

    /**
     * Transitions to the next round, resetting player health and positions.
     * This is called after the statistics screen is shown.
     */
    fun transitionToNextRound() {
        if (currentRound < totalRounds) {
            currentRound++

            // Regenerate terrain
            terrainManager.setTerrainVariance(25 + Random.nextInt(50), gameWidth, gameHeight)
            terrainManager.generateTerrain(gameWidth, gameHeight)

            // Store player data before regenerating players
            val playerData = playerManager.players.map { player ->
                PlayerData(
                    name = player.name,
                    type = player.type,
                    money = player.money,
                    inventory = player.inventory,
                    selectedProjectileType = player.selectedProjectileType
                )
            }

            playerManager.generatePlayers(gameWidth, gameHeight, numberOfPlayers)

            // Restore player data
            playerManager.players.forEachIndexed { index, player ->
                if (index < playerData.size) {
                    val data = playerData[index]
                    player.name = data.name
                    player.type = data.type
                    player.money = data.money

                    // Copy inventory items
                    data.inventory.getAllItems().forEach { item ->
                        player.inventory.addItem(item.type, item.quantity)
                    }

                    // Set selected projectile type
                    player.selectedProjectileType = data.selectedProjectileType
                }
            }

            // Update player positions to stick to the terrain
            // Don't animate when transitioning to a new round - instant repositioning
            playerManager.updatePlayerPositions(terrainManager::getTerrainHeightAtX, animate = false)

            // Reset projectiles and explosions
            projectileManager.reset()

            // Reset elimination tracking for the new round
            playerManager.resetEliminationTracking()

            // Reset game state
            gameState = GameState.WAITING_FOR_PLAYER
        } else {
            // This was the last round, go to game over
            gameState = GameState.GAME_OVER
        }
    }

    // Getters for accessing the components from outside

    /**
     * Gets the current terrain path.
     */
    val terrain get() = terrainManager.terrain

    /**
     * Gets the terrain heights map.
     */
    val terrainHeights get() = terrainManager.terrainHeights

    /**
     * Gets the current players.
     */
    var players
        get() = playerManager.players
        set(value) {
            playersSetExternally = true
            playerManager.players = value
        }

    /**
     * Gets the current player index.
     */
    var currentPlayerIndex
        get() = playerManager.currentPlayerIndex
        set(value) { playerManager.currentPlayerIndex = value }

    /**
     * Gets the current projectile.
     */
    val projectile get() = projectileManager.projectile

    /**
     * Gets the current mini-bombs.
     */
    val miniBombs get() = projectileManager.miniBombs

    /**
     * Gets the current explosion.
     */
    val explosion get() = explosionManager.explosion

    /**
     * Gets the current wind.
     */
    val wind get() = projectileManager.wind

    /**
     * Gets the weather manager.
     */
    val weather get() = weatherManager

    /**
     * Advances to the next player, skipping dead players.
     * @return True if an alive player was found, false if all players are dead
     */
    fun nextPlayer(): Boolean {
        return playerManager.nextPlayer()
    }
}
