package dev.jamiecraane.scorchedearth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Main application entry point.
 * This composable manages the game state and switches between the intro screen and game UI.
 */
@Composable
@Preview
fun App() {
    // State to track whether the game has started
    var gameStarted by remember { mutableStateOf(false) }

    // State to track the number of players
    var numberOfPlayers by remember { mutableStateOf(2) }

    // Track canvas size to detect changes
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Show intro screen if game hasn't started
    if (!gameStarted) {
        IntroScreen(
            onStartGame = { players ->
                numberOfPlayers = players
                gameStarted = true
            }
        )
    } else {
        // Game is started, create the game instance
        val game = remember(numberOfPlayers) { ScorchedEarthGame(numberOfPlayers) }

        // Game UI
        GameUI(game, canvasSize)
    }
}
