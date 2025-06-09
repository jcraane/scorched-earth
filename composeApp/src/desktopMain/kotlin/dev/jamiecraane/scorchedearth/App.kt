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
import dev.jamiecraane.scorchedearth.terrain.TerrainStyleSelector
import dev.jamiecraane.scorchedearth.weather.WeatherType
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

    // State to track the current player index for inventory selection
    var currentInventoryPlayerIndex by remember { mutableStateOf(0) }

    // State to track the list of players with their inventories
    var players by remember { mutableStateOf(listOf<dev.jamiecraane.scorchedearth.model.Player>()) }

    // State to track the selected sky style
    var selectedSkyStyle by remember { mutableStateOf(SkyStyleSelector.getDefault()) }

    // State to track the selected terrain style
    var selectedTerrainStyle by remember { mutableStateOf(TerrainStyleSelector.getDefault()) }

    // State to track the terrain variance
    var terrainVariance by remember { mutableStateOf(25) }

    // State to track the number of rounds
    var numberOfRounds by remember { mutableStateOf(1) }

    // State to track the weather type
    var weatherType by remember { mutableStateOf(WeatherType.NONE) }

    // Track canvas size to detect changes
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    when (currentScreen) {
        Screen.INTRO -> {
            IntroScreen(
                onStartGame = { players, skyStyle, terrainStyle, variance, rounds, weather ->
                    numberOfPlayers = players
                    selectedSkyStyle = skyStyle
                    selectedTerrainStyle = terrainStyle
                    terrainVariance = variance
                    numberOfRounds = rounds
                    weatherType = weather
                    currentScreen = Screen.PLAYER_NAMES
                }
            )
        }

        Screen.PLAYER_NAMES -> {
            PlayerNameInputScreen(
                numberOfPlayers = numberOfPlayers,
                onComplete = { setups ->
                    playerSetups = setups

                    // Initialize players list with player setups
                    val game = ScorchedEarthGame(numberOfPlayers, numberOfRounds)
                    game.players.forEachIndexed { index, player ->
                        if (index < playerSetups.size) {
                            player.name = playerSetups[index].name
                            player.type = playerSetups[index].type
                        }
                    }
                    players = game.players.toList()

                    // Reset current inventory player index
                    currentInventoryPlayerIndex = 0

                    // Transition to inventory screen
                    currentScreen = Screen.INVENTORY
                }
            )
        }

        Screen.INVENTORY -> {
            PlayerInventoryScreen(
                players = players,
                currentPlayerIndex = currentInventoryPlayerIndex,
                onComplete = { updatedPlayers ->
                    // Update the players list with the modified players from the temporary game
                    players = updatedPlayers

                    // Move to next player or start game if all players have selected items
                    if (currentInventoryPlayerIndex < players.size - 1) {
                        // Move to next player
                        currentInventoryPlayerIndex++
                    } else {
                        // All players have selected their items, start the game
                        currentScreen = Screen.GAME
                    }
                }
            )
        }

        Screen.GAME -> {
            // Game is started, create the game instance with the players from inventory selection
            val game = remember(players, selectedSkyStyle, selectedTerrainStyle, terrainVariance, numberOfRounds) {
                ScorchedEarthGame(players.size, numberOfRounds).apply {
                    // Set the players from inventory selection
                    this.players = players.toMutableList()

                    // Set the sky style
                    skyStyle = selectedSkyStyle.toSkyStyle()

                    // Set the terrain style
                    setTerrainStyle(selectedTerrainStyle.toTerrainStyle())

                    // Set the weather type
                    setWeatherType(weatherType)

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
    INVENTORY,
    GAME
}
