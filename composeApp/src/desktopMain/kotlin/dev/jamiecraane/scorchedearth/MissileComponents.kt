package dev.jamiecraane.scorchedearth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

/**
 * Button to display the current missile and open the inventory popup.
 */
@Composable
fun InventoryButton(
    currentMissile: ProjectileType,
    currentMissileQuantity: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.DarkGray.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${currentMissile.displayName} (Damage: ${currentMissile.minDamage}-${currentMissile.maxDamage})",
                color = Color.White
            )

            // Display quantity
            Text(
                text = "Qty: $currentMissileQuantity",
                color = if (currentMissileQuantity > 0) Color.White else Color.Red
            )
        }
    }
}

/**
 * Popup to display the inventory and allow purchasing missiles.
 */
@Composable
fun InventoryPopup(
    game: ScorchedEarthGame,
    onDismiss: () -> Unit
) {
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier
                .width(400.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.DarkGray.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Item",
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

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

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
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

/**
 * Composable function to display a missile item in the grid.
 */
@Composable
fun MissileItem(
    projectileType: ProjectileType,
    isSelected: Boolean,
    quantity: Int = 0,
    canAfford: Boolean = true,
    onClick: () -> Unit,
    onBuy: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp) // Fixed height instead of aspect ratio
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) Color.Yellow else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF444444) else Color(0xFF333333)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween, // Changed to SpaceBetween
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top section with icon and name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Missile icon (simple colored circle for now)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(getMissileColor(projectileType), RoundedCornerShape(50))
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Missile name
                Text(
                    text = projectileType.displayName,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            // Middle section with details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Damage info
                Text(
                    text = "DMG: ${projectileType.minDamage}-${projectileType.maxDamage}",
                    color = Color.LightGray,
                    fontSize = 10.sp
                )

                // Cost info
                Text(
                    text = "Cost: $${projectileType.cost}",
                    color = Color.LightGray,
                    fontSize = 10.sp
                )

                // Quantity
                Text(
                    text = "Qty: $quantity",
                    color = if (quantity > 0) Color.White else Color.Red,
                    fontSize = 10.sp
                )
            }

            // Bottom section with buy button
            if (onBuy != null) {
                Button(
                    onClick = onBuy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canAfford) Color(0xFF4CAF50) else Color(0xFF888888)
                    ),
                    enabled = canAfford
                ) {
                    Text(
                        text = if (canAfford) "BUY" else "NO $",
                        fontSize = 10.sp,
                        color = if (canAfford) Color.White else Color.DarkGray
                    )
                }
            }
        }
    }
}

/**
 * Returns a color for the missile based on its type.
 */
fun getMissileColor(projectileType: ProjectileType): Color {
    return when (projectileType) {
        ProjectileType.BABY_MISSILE -> Color(0xFF8BC34A)  // Light Green
        ProjectileType.SMALL_MISSILE -> Color(0xFFFFEB3B)  // Yellow
        ProjectileType.BIG_MISSILE -> Color(0xFFFF9800)  // Orange
        ProjectileType.DEATHS_HEAD -> Color(0xFFE91E63)  // Pink
        ProjectileType.NUCLEAR_BOMB -> Color(0xFFF44336)  // Red
        ProjectileType.FUNKY_BOMB -> Color(0xFF9C27B0)  // Purple
        ProjectileType.MIRV -> Color(0xFF9D50B0)
    }
}
