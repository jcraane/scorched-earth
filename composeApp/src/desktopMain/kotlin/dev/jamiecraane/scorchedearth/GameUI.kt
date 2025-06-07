package dev.jamiecraane.scorchedearth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.jamiecraane.scorchedearth.engine.GameState
import dev.jamiecraane.scorchedearth.engine.ScorchedEarthGame
import dev.jamiecraane.scorchedearth.sky.SkyStyle
import kotlinx.coroutines.delay

// No need to import components from the same package

/**
 * Main game UI component that displays the game canvas and controls.
 *
 * @param game The game engine instance
 * @param initialCanvasSize The initial size of the canvas
 * @param cpuController Optional CPU player controller for handling AI turns
 * @param onBackToIntro Callback to navigate back to the intro screen
 */
@Composable
fun GameUI(
    game: ScorchedEarthGame,
    initialCanvasSize: androidx.compose.ui.geometry.Size,
    cpuController: dev.jamiecraane.scorchedearth.engine.CPUPlayerController? = null,
    onBackToIntro: () -> Unit = {},
) {
    // Track canvas size to detect changes
    var canvasSize by remember { mutableStateOf(initialCanvasSize) }

    // State to force recomposition for star animation
    var animationTrigger by remember { mutableLongStateOf(0L) }

    // Game loop using LaunchedEffect
    LaunchedEffect(Unit) {
        var lastTime = System.currentTimeMillis()
        while (true) {
            val currentTime = System.currentTimeMillis()
            val deltaTime = (currentTime - lastTime) / 1000f
            lastTime = currentTime

            game.update(deltaTime)

            // Force recomposition for star animation when night sky is active
            if (game.skyStyle == SkyStyle.NIGHT) {
                animationTrigger = currentTime
            }

            // Handle CPU player turns
            if (game.gameState == GameState.AIMING &&
                game.players.isNotEmpty() &&
                game.currentPlayerIndex < game.players.size
            ) {

                val currentPlayer = game.players[game.currentPlayerIndex]

                // Check if current player is CPU and we have a CPU controller
                if (currentPlayer.type == dev.jamiecraane.scorchedearth.model.PlayerType.CPU && cpuController != null) {
                    println(
                        "[DEBUG_LOG] CPU player turn: ${currentPlayer.name}, Inventory: ${
                            currentPlayer.inventory.getAllItems()
                                .joinToString { "${it.type.displayName}(${it.quantity})" }
                        }"
                    )

                    // Make CPU decision
                    if (cpuController.makeDecision(currentPlayer)) {
                        println("[DEBUG_LOG] CPU decision made: angle=${currentPlayer.angle}, power=${currentPlayer.power}, projectile=${currentPlayer.selectedProjectileType.displayName}")

                        // Add a small delay to make CPU turns visible
                        delay(500)

                        // Fire projectile with CPU-determined angle and power
                        val success = game.fireProjectile(currentPlayer.angle, currentPlayer.power)
                        println("[DEBUG_LOG] CPU fire result: $success")
                    } else {
                        println("[DEBUG_LOG] CPU couldn't make a decision, skipping turn")

                        // If CPU couldn't make a decision (e.g., no valid targets), skip turn
                        game.gameState = GameState.WAITING_FOR_PLAYER
                        game.currentPlayerIndex = (game.currentPlayerIndex + 1) % game.players.size
                    }
                } else {
                    // Not a CPU player, reset to waiting state
                    game.gameState = GameState.WAITING_FOR_PLAYER
                }
            }

            delay(16) // ~60 FPS
        }
    }

    // State for confirmation dialog
    var showConfirmationDialog by remember { mutableStateOf(false) }

    // Main game container
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Game canvas where all rendering happens - placed first so it's at the bottom layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Only update dimensions when canvas size actually changes
            if (canvasSize != size) {
                canvasSize = size
                game.updateDimensions(size.width, size.height)
            }

            // Draw the game elements
            // Access animationTrigger to ensure recomposition when stars need to animate
            if (game.skyStyle == SkyStyle.NIGHT) {
                // This read access ensures recomposition when animationTrigger changes
                val l_ = animationTrigger
            }

            drawGame(game)
        }

        Header(
            currentRound = game.currentRound,
            onBackButtonClick = { showConfirmationDialog = true },
            transitionToNextRoundClick = {
                game.prepareNextRound()
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        ConfirmationDialog(
            show = showConfirmationDialog,
            onConfirmClick = {
                showConfirmationDialog = false
                onBackToIntro()
            },
            onDismissClick = { showConfirmationDialog = false }
        )

        // Round statistics dialog
        if (game.gameState == GameState.ROUND_STATISTICS) {
            RoundStatistics(
                currentRound = game.currentRound,
                players = game.players,
                onNextCLick = { game.transitionToNextRound() }
            )
        }

        // Game over statistics dialog
        if (game.gameState == GameState.GAME_OVER) {
            GameStatistics(
                totalRounds = game.totalRounds,
                players = game.players,
                onBackToIntroClick = onBackToIntro
            )
        }

        PlayerControls(
            game,
            Modifier.align(Alignment.BottomCenter)
        )
    }
}
