package dev.jamiecraane.scorchedearth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.jamiecraane.scorchedearth.engine.GameState
import dev.jamiecraane.scorchedearth.engine.ScorchedEarthGame
import dev.jamiecraane.scorchedearth.inventory.InventoryButton
import dev.jamiecraane.scorchedearth.inventory.InventoryPopup
import kotlinx.coroutines.delay
// No need to import components from the same package

/**
 * Main game UI component that displays the game canvas and controls.
 *
 * @param game The game engine instance
 * @param initialCanvasSize The initial size of the canvas
 */
@Composable
fun GameUI(game: ScorchedEarthGame, initialCanvasSize: androidx.compose.ui.geometry.Size) {
    // Track canvas size to detect changes
    var canvasSize by remember { mutableStateOf(initialCanvasSize) }

    // Game loop using LaunchedEffect
    LaunchedEffect(Unit) {
        var lastTime = System.currentTimeMillis()
        while (true) {
            val currentTime = System.currentTimeMillis()
            val deltaTime = (currentTime - lastTime) / 1000f
            lastTime = currentTime

            game.update(deltaTime)
            delay(16) // ~60 FPS
        }
    }

    // Main game container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF87CEEB)) // Sky blue background
    ) {

        // Game canvas where all rendering happens
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Only update dimensions when canvas size actually changes
            if (canvasSize != size) {
                canvasSize = size
                game.updateDimensions(size.width, size.height)
            }

            // Draw the game elements
            drawGame(game)
        }

        // UI controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            // Display current player and wind
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Player: ${game.currentPlayerIndex + 1}", color = Color.White)
                Text("Wind: ${game.wind.toInt()} mph", color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Angle control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Angle: ${game.players[game.currentPlayerIndex].angle.toInt()}° (0=right)",
                    modifier = Modifier.width(150.dp),
                    color = Color.White)
                Slider(
                    value = game.players[game.currentPlayerIndex].angle,
                    onValueChange = {
                        val players = game.players.toMutableList()
                        players[game.currentPlayerIndex] = players[game.currentPlayerIndex].copy(angle = it)
                        game.players = players
                    },
                    valueRange = -90f..90f,
                    modifier = Modifier.weight(1f)
                )
            }

            // Power control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Power: ${game.players[game.currentPlayerIndex].power.toInt()}",
                    modifier = Modifier.width(100.dp),
                    color = Color.White)
                Slider(
                    value = game.players[game.currentPlayerIndex].power,
                    onValueChange = {
                        val players = game.players.toMutableList()
                        players[game.currentPlayerIndex] = players[game.currentPlayerIndex].copy(power = it)
                        game.players = players
                    },
                    valueRange = 10f..100f,
                    modifier = Modifier.weight(1f)
                )
            }

            // Missile type selection
            var showInventoryPopup by remember { mutableStateOf(false) }
            val currentPlayer = game.players[game.currentPlayerIndex]
            val currentMissile = currentPlayer.selectedProjectileType
            val currentMissileQuantity = currentPlayer.inventory.getItemQuantity(currentMissile)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Inventory: ",
                    modifier = Modifier.width(100.dp),
                    color = Color.White)

                // Button to show current missile and open popup
                InventoryButton(
                    currentMissile = currentMissile,
                    currentMissileQuantity = currentMissileQuantity,
                    onClick = { showInventoryPopup = true }
                )

                // Missile selection popup
                if (showInventoryPopup) {
                    InventoryPopup(
                        game = game,
                        onDismiss = { showInventoryPopup = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fire button
            var showNoMissilesMessage by remember { mutableStateOf(false) }

            Button(
                onClick = {
                    val player = game.players[game.currentPlayerIndex]
                    val success = game.fireProjectile(player.angle, player.power)
                    if (!success) {
                        showNoMissilesMessage = true
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = game.gameState == GameState.WAITING_FOR_PLAYER
            ) {
                Text("FIRE!")
            }

            // Show message if player doesn't have the selected missile
            if (showNoMissilesMessage) {
                Text(
                    text = "No ${game.players[game.currentPlayerIndex].selectedProjectileType.displayName} missiles left!",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
                )

                // Hide the message after a delay
                LaunchedEffect(showNoMissilesMessage) {
                    delay(2000)
                    showNoMissilesMessage = false
                }
            }
        }
    }
}
