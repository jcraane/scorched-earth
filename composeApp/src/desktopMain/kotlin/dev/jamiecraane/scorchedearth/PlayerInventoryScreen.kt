package dev.jamiecraane.scorchedearth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jamiecraane.scorchedearth.engine.ScorchedEarthGame
import dev.jamiecraane.scorchedearth.inventory.InventoryPopup
import dev.jamiecraane.scorchedearth.model.Player
import dev.jamiecraane.scorchedearth.model.PlayerType

/**
 * Screen for players to select items from the inventory before the game starts.
 *
 * @param players The list of players in the game
 * @param currentPlayerIndex The index of the current player selecting items
 * @param onComplete Callback when all players have selected their items
 */
@Composable
fun PlayerInventoryScreen(
    players: List<Player>,
    currentPlayerIndex: Int,
    onComplete: () -> Unit
) {
    // Create a temporary game instance just for inventory management
    val tempGame = remember(players, currentPlayerIndex) {
        ScorchedEarthGame(players.size, 1).apply {
            // Copy players to the game
            this.players = players.toMutableList()
            // Set current player index
            this.currentPlayerIndex = currentPlayerIndex
        }
    }

    // State to control inventory popup visibility
    var showInventory by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E), // Dark blue
                        Color(0xFF3949AB)  // Medium blue
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Select Your Items",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Player info
            Text(
                text = "${players[currentPlayerIndex].name}'s Turn",
                color = Color.White,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Money info
            Text(
                text = "Money: $${players[currentPlayerIndex].money}",
                color = Color.Yellow,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Instructions
            Text(
                text = "Select and purchase the items you want to use in the game.",
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Note: Continue button removed as it's now part of the inventory popup
        }

        // Show inventory popup
        if (showInventory) {
            InventoryPopup(
                game = tempGame,
                onDismiss = { showInventory = false },
                showBuyButton = true,
                isLastPlayer = currentPlayerIndex == players.size - 1,
                onNext = onComplete
            )
        }
    }
}
