package dev.jamiecraane.scorchedearth.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import dev.jamiecraane.scorchedearth.engine.ScorchedEarthGame
import kotlinx.coroutines.delay

/**
 * Popup to display the inventory and allow purchasing items.
 */
@Composable
fun InventoryPopup(
    game: ScorchedEarthGame,
    onDismiss: () -> Unit
) {
    // Tab state
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier
                .width(600.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.DarkGray.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Inventory",
                        color = Color.White
                    )

                    // Close button
                    Text(
                        text = "✕",
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable { onDismiss() }
                    )
                }

                // Display player's money
                Text(
                    text = "Money: $${game.players[game.currentPlayerIndex].money}",
                    color = Color.Yellow,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Message to show after purchase attempt
                var purchaseMessage by remember { mutableStateOf<String?>(null) }

                // Show purchase message if it exists
                purchaseMessage?.let { message ->
                    Text(
                        text = message,
                        color = if (message.contains("Success")) Color.Green else Color.Red,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Clear message after a delay
                    LaunchedEffect(purchaseMessage) {
                        delay(2000)
                        purchaseMessage = null
                    }
                }

                // Tabs for different item categories
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.DarkGray,
                    contentColor = Color.White
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Weapons") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Shields") }
                    )
                }

                when (selectedTabIndex) {
                    0 -> {
                        // Weapons tab
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            items(ProjectileType.entries.toTypedArray()) { projectileType ->
                                val currentPlayer = game.players[game.currentPlayerIndex]
                                val quantity = currentPlayer.inventory.getItemQuantity(projectileType)
                                val canAfford = currentPlayer.money >= projectileType.cost

                                InventoryItem(
                                    itemType = projectileType,
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
                    1 -> {
                        // Shields tab
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            items(ShieldType.entries.toTypedArray()) { shieldType ->
                                val currentPlayer = game.players[game.currentPlayerIndex]
                                val quantity = currentPlayer.inventory.getItemQuantity(shieldType)
                                val canAfford = currentPlayer.money >= shieldType.cost
                                val isSelected = currentPlayer.inventory.isShieldSelected(shieldType)

                                InventoryItem(
                                    itemType = shieldType,
                                    isSelected = isSelected,
                                    quantity = quantity,
                                    canAfford = canAfford,
                                    onClick = {
                                        // Toggle shield selection if player has this shield type
                                        if (quantity > 0) {
                                            // Toggle shield selection
                                            game.selectShield(shieldType)
                                        }
                                    },
                                    onBuy = {
                                        val success = game.purchaseShield(shieldType)
                                        if (success) {
                                            purchaseMessage = "Success! Purchased ${shieldType.displayName}"
                                        } else {
                                            purchaseMessage = "Not enough money to buy ${shieldType.displayName}"
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
