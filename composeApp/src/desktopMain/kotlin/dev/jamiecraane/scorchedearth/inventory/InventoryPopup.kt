package dev.jamiecraane.scorchedearth.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.jamiecraane.scorchedearth.engine.ProjectileType
import dev.jamiecraane.scorchedearth.engine.ScorchedEarthGame
import kotlinx.coroutines.delay

/**
 * Popup to display the inventory and allow purchasing missiles.
 */
@Composable
fun InventoryPopup(
    game: ScorchedEarthGame,
    onDismiss: () -> Unit
) {
    Popup(
        alignment = Alignment.Companion.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier.Companion
                .width(600.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Companion.DarkGray.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.Companion.padding(16.dp)
            ) {
                Text(
                    text = "Select Item",
                    color = Color.Companion.White,
                    modifier = Modifier.Companion.padding(bottom = 8.dp)
                )

                // Display player's money
                Text(
                    text = "Money: $${game.players[game.currentPlayerIndex].money}",
                    color = Color.Companion.Yellow,
                    fontSize = 16.sp,
                    modifier = Modifier.Companion.padding(bottom = 16.dp)
                )

                // Message to show after purchase attempt
                var purchaseMessage by remember { mutableStateOf<String?>(null) }

                // Show purchase message if it exists
                purchaseMessage?.let { message ->
                    Text(
                        text = message,
                        color = if (message.contains("Success")) Color.Companion.Green else Color.Companion.Red,
                        modifier = Modifier.Companion.padding(bottom = 8.dp)
                    )

                    // Clear message after a delay
                    LaunchedEffect(purchaseMessage) {
                        delay(2000)
                        purchaseMessage = null
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ProjectileType.entries.toTypedArray()) { projectileType ->
                        val currentPlayer = game.players[game.currentPlayerIndex]
                        val quantity = currentPlayer.inventory.getItemQuantity(projectileType)
                        val canAfford = currentPlayer.money >= projectileType.cost

                        MissileItem(
                            projectileType = projectileType,
                            isSelected = currentPlayer.selectedProjectileType == projectileType,
                            quantity = quantity,
                            canAfford = canAfford,
                            onClick = {
                                // Only allow selection if player has this missile type
                                if (quantity > 0) {
                                    val players = game.players.toMutableList()
                                    players[game.currentPlayerIndex] = players[game.currentPlayerIndex].copy(
                                        selectedProjectileType = projectileType
                                    )
                                    game.players = players
                                    onDismiss()
                                }
                            },
                            onBuy = {
                                val success = game.purchaseMissile(projectileType)
                                if (success) {
                                    purchaseMessage = "Success! Purchased ${projectileType.displayName}"
                                } else {
                                    purchaseMessage = "Not enough money to buy ${projectileType.displayName}"
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
