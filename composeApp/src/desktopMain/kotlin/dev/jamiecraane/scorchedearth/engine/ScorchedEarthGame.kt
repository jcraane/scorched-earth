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

/**
 * Main game engine class that manages the game state and logic.
 * This class coordinates the different components of the game.
 */
class ScorchedEarthGame(private val numberOfPlayers: Int = 2, val totalRounds: Int = 1) {
    // Current round
    var currentRound by mutableStateOf(1)
    // Game dimensions - these will be updated when the canvas size changes
    var gameWidth by mutableStateOf(1600f)
    var gameHeight by mutableStateOf(1200f)

    // Game state
    var gameState by mutableStateOf(GameState.WAITING_FOR_PLAYER)

    // Sky style - determines the background gradient
    var skyStyle by mutableStateOf(SkyStyle.AFTERNOON)

    // Component managers
    private val terrainManager = TerrainManager()
    private val playerManager = PlayerManager()
    private val explosionManager = ExplosionManager(terrainManager, playerManager)
    private val projectileManager = ProjectileManager(terrainManager, playerManager, explosionManager)

    init {
        // Initialize the game
        terrainManager.generateTerrain(gameWidth, gameHeight)
        playerManager.generatePlayers(gameWidth, gameHeight, numberOfPlayers)
        projectileManager.setGameDimensions(gameWidth, gameHeight)
        projectileManager.generateWind()
    }

    /**
     * Updates the game dimensions and regenerates content accordingly.
     * Call this when the window/canvas size changes.
     */
    fun updateDimensions(width: Float, height: Float) {
        gameWidth = width
        gameHeight = height
        terrainManager.generateTerrain(width, height)

        // Store player names and types before regenerating players
        val playerNames = playerManager.players.map { it.name }
        val playerTypes = playerManager.players.map { it.type }

        // Regenerate players
        playerManager.generatePlayers(width, height, numberOfPlayers)

        // Restore player names and types
        playerManager.players.forEachIndexed { index, player ->
            if (index < playerNames.size) {
                player.name = playerNames[index]
                player.type = playerTypes[index]
            }
        }

        // Update player positions to stick to the terrain
        // Don't animate when resizing - instant repositioning
        playerManager.updatePlayerPositions(terrainManager::getTerrainHeightAtX, animate = false)

        // Reset projectile and explosion if they exist to prevent out-of-bounds issues
        projectileManager.reset()

        // Update game dimensions in projectile manager
        projectileManager.setGameDimensions(width, height)
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
     */
    fun prepareNextRound() {
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
            terrainManager.generateTerrain(gameWidth, gameHeight)

            // Regenerate players with the same names and types
            val playerNames = playerManager.players.map { it.name }
            val playerTypes = playerManager.players.map { it.type }

            playerManager.generatePlayers(gameWidth, gameHeight, numberOfPlayers)

            // Restore player names and types
            playerManager.players.forEachIndexed { index, player ->
                if (index < playerNames.size) {
                    player.name = playerNames[index]
                    player.type = playerTypes[index]
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
        set(value) { playerManager.players = value }

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
}
