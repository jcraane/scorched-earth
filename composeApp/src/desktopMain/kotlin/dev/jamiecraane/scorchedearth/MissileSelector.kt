package dev.jamiecraane.scorchedearth

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.jamiecraane.scorchedearth.engine.ScorchedEarthGame
import dev.jamiecraane.scorchedearth.inventory.InventoryButton
import dev.jamiecraane.scorchedearth.inventory.InventoryPopup

/**
 * Control for selecting a missile type from the inventory.
 */
@Composable
fun MissileSelector(game: ScorchedEarthGame) {
    var showInventoryPopup by remember { mutableStateOf(false) }
    val currentPlayer = game.players[game.currentPlayerIndex]
    val currentMissile = currentPlayer.selectedProjectileType
    val currentMissileQuantity = currentPlayer.inventory.getItemQuantity(currentMissile)

    Row(
        modifier = Modifier.Companion.fillMaxWidth(),
        verticalAlignment = Alignment.Companion.CenterVertically
    ) {
        Text(
            "Inventory: ",
            modifier = Modifier.Companion.width(100.dp),
            color = Color.Companion.White
        )

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
}
