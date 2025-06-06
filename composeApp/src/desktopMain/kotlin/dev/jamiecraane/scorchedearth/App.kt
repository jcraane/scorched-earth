package dev.jamiecraane.scorchedearth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import dev.jamiecraane.scorchedearth.engine.ScorchedEarthGame
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Main application entry point.
 * This composable manages the game state and switches between the intro screen, player name input screen, and game UI.
 */
@Composable
@Preview
fun App() {
    // State to track the current screen
    var currentScreen by remember { mutableStateOf(Screen.INTRO) }

    // State to track the number of players
    var numberOfPlayers by remember { mutableStateOf(2) }

    // State to track player names
    var playerNames by remember { mutableStateOf(listOf<String>()) }

    // Track canvas size to detect changes
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    when (currentScreen) {
        Screen.INTRO -> {
            IntroScreen(
                onStartGame = { players ->
                    numberOfPlayers = players
                    currentScreen = Screen.PLAYER_NAMES
                }
            )
        }

        Screen.PLAYER_NAMES -> {
            PlayerNameInputScreen(
                numberOfPlayers = numberOfPlayers,
                onComplete = { names ->
                    playerNames = names
                    currentScreen = Screen.GAME
                }
            )
        }

        Screen.GAME -> {
            // Game is started, create the game instance
            val game = remember(numberOfPlayers, playerNames) {
                ScorchedEarthGame(numberOfPlayers).apply {
                    // Set player names
                    players.forEachIndexed { index, player ->
                        if (index < playerNames.size) {
                            player.name = playerNames[index]
                        }
                    }
                }
            }

            // Game UI
            GameUI(game, canvasSize)
        }
    }
}

/**
 * Enum representing the different screens in the application.
 */
enum class Screen {
    INTRO,
    PLAYER_NAMES,
    GAME
}
