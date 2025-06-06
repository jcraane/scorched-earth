package dev.jamiecraane.scorchedearth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ComposeAppDesktopTest {

    @Test
    fun example() {
        assertEquals(3, 1 + 2)
    }

    @Test
    fun testScorchedEarthGameInitialization() {
        // This test verifies that a ScorchedEarthGame can be initialized without throwing exceptions
        val game = ScorchedEarthGame()

        // Verify that the game and its properties are properly initialized
        assertNotNull(game.terrain)
        assertNotNull(game.terrainHeights)
        assertNotNull(game.players)

        // Verify that the terrain heights map is not empty
        assert(game.terrainHeights.isNotEmpty()) { "Terrain heights map should not be empty" }

        println("[DEBUG_LOG] ScorchedEarthGame initialized successfully")
        println("[DEBUG_LOG] Terrain heights size: ${game.terrainHeights.size}")
    }
}
