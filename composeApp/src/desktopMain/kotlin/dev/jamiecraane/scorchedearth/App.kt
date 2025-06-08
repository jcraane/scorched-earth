package dev.jamiecraane.scorchedearth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import dev.jamiecraane.scorchedearth.engine.CPUPlayerController
import dev.jamiecraane.scorchedearth.engine.ScorchedEarthGame
import dev.jamiecraane.scorchedearth.gameui.GameUI
import dev.jamiecraane.scorchedearth.sky.SkyStyleSelector
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

    // State to track player setups (name and type)
    var playerSetups by remember { mutableStateOf(listOf<PlayerSetup>()) }

    // State to track the selected sky style
    var selectedSkyStyle by remember { mutableStateOf(SkyStyleSelector.getDefault()) }

    // State to track the terrain variance
    var terrainVariance by remember { mutableStateOf(25) }

    // State to track the number of rounds
    var numberOfRounds by remember { mutableStateOf(1) }

    // Track canvas size to detect changes
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    when (currentScreen) {
        Screen.INTRO -> {
            IntroScreen(
                onStartGame = { players, skyStyle, variance, rounds ->
                    numberOfPlayers = players
                    selectedSkyStyle = skyStyle
                    terrainVariance = variance
                    numberOfRounds = rounds
                    currentScreen = Screen.PLAYER_NAMES
                }
            )
        }

        Screen.PLAYER_NAMES -> {
            PlayerNameInputScreen(
                numberOfPlayers = numberOfPlayers,
                onComplete = { setups ->
                    playerSetups = setups
                    currentScreen = Screen.GAME
                }
            )
        }

        Screen.GAME -> {
            // Game is started, create the game instance
            val game = remember(numberOfPlayers, playerSetups, selectedSkyStyle, terrainVariance, numberOfRounds) {
                ScorchedEarthGame(numberOfPlayers, numberOfRounds).apply {
                    // Set player names and types
                    players.forEachIndexed { index, player ->
                        if (index < playerSetups.size) {
                            player.name = playerSetups[index].name
                            player.type = playerSetups[index].type
                        }
                    }

                    // Set the sky style
                    skyStyle = selectedSkyStyle.toSkyStyle()

                    // Set the terrain variance and regenerate terrain
                    setTerrainVariance(terrainVariance)
                }
            }

            // Create CPU player controller
            val cpuController = remember(game) {
                CPUPlayerController(game)
            }

            // Game UI
            GameUI(
                game = game,
                initialCanvasSize = canvasSize,
                cpuController = cpuController,
                onBackToIntro = { currentScreen = Screen.INTRO }
            )
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
