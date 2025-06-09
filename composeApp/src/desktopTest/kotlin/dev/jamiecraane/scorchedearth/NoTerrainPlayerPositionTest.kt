package dev.jamiecraane.scorchedearth

import androidx.compose.ui.geometry.Offset
import dev.jamiecraane.scorchedearth.engine.player.PlayerManager
import dev.jamiecraane.scorchedearth.engine.terrain.TerrainManager
import dev.jamiecraane.scorchedearth.model.Player
import kotlin.test.Test
import kotlin.test.assertEquals

class NoTerrainPlayerPositionTest {

    @Test
    fun testPlayerPositionWithNoTerrain() {
        // Create a TerrainManager instance
        val terrainManager = TerrainManager()

        // Set game dimensions
        val gameWidth = 1000f
        val gameHeight = 800f

        // Generate terrain initially
        terrainManager.generateTerrain(gameWidth, gameHeight)

        // Clear the terrain heights map to simulate no terrain
        val emptyTerrainHeights = mapOf<Float, Float>()
        terrainManager.terrainHeights = emptyTerrainHeights

        // Verify that getTerrainHeightAtX returns gameHeight when no terrain is present
        val terrainHeight = terrainManager.getTerrainHeightAtX(500f)
        assertEquals(gameHeight, terrainHeight, "When no terrain is present, getTerrainHeightAtX should return gameHeight")

        // Create a PlayerManager instance
        val playerManager = PlayerManager()

        // Generate a player
        playerManager.generatePlayers(gameWidth, gameHeight, 1)

        // Set initial player position
        val initialPlayer = playerManager.players[0]
        val players = listOf(
            initialPlayer.copy(position = Offset(500f, 100f)) // Position player above where terrain would be
        )
        playerManager.players = players

        // Update player positions based on terrain
        playerManager.updatePlayerPositions(terrainManager::getTerrainHeightAtX, animate = false)

        // Verify that the player is positioned at the bottom of the screen (minus the 15f offset)
        val expectedY = gameHeight - 15f // PlayerManager subtracts 15f to position player on top of terrain
        assertEquals(expectedY, playerManager.players[0].position.y, "Player should be positioned at the bottom of the screen when no terrain is present")

        println("[DEBUG_LOG] Test completed - player is positioned at the bottom of the screen when no terrain is present")
    }
}
