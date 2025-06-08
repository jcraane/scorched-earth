package dev.jamiecraane.scorchedearth.gameui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import kotlinx.coroutines.delay

/**
 * Fire button with error message for when player doesn't have the selected missile.
 */
@Composable
fun FireButton(
    game: ScorchedEarthGame,
    modifier: Modifier = Modifier.Companion,
) {
    var showNoMissilesMessage by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
        modifier = modifier
    ) {
        Button(
            onClick = {
                val player = game.players[game.currentPlayerIndex]
                val success = game.fireProjectile(player.angle, player.power)
                if (!success) {
                    showNoMissilesMessage = true
                }
            },
            enabled = game.gameState == GameState.WAITING_FOR_PLAYER
        ) {
            Text("FIRE!")
        }

        // Show message if player doesn't have the selected missile
        if (showNoMissilesMessage) {
            Text(
                text = "No ${game.players[game.currentPlayerIndex].selectedProjectileType.displayName} missiles left!",
                color = Color.Companion.Red,
                modifier = Modifier.Companion.padding(top = 4.dp)
            )

            // Hide the message after a delay
            LaunchedEffect(showNoMissilesMessage) {
                delay(2000)
                showNoMissilesMessage = false
            }
        }
    }
}
