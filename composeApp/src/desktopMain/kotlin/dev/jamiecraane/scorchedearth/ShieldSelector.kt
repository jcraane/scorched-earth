package dev.jamiecraane.scorchedearth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Button
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
import dev.jamiecraane.scorchedearth.inventory.ShieldType

/**
 * Control for selecting and activating a shield.
 */
@Composable
fun ShieldSelector(game: ScorchedEarthGame) {
    val currentPlayer = game.players[game.currentPlayerIndex]
    val hasActiveShield = currentPlayer.hasActiveShield()
    val shieldHealth = currentPlayer.activeShield?.currentHealth ?: 0
    val maxShieldHealth = currentPlayer.activeShield?.type?.maxHealth ?: 0
    val shieldHealthPercentage = if (maxShieldHealth > 0) (shieldHealth.toFloat() / maxShieldHealth.toFloat()) * 100f else 0f

    // Track if shield inventory popup is shown
    var showShieldInventory by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Shield: ",
            modifier = Modifier.width(100.dp),
            color = Color.White
        )

        if (hasActiveShield) {
            // Show active shield with health percentage
            Text(
                "Active (${shieldHealthPercentage.toInt()}%)",
                color = Color.Blue
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Button to deactivate shield
            Button(onClick = { game.deactivateShield() }) {
                Text("Deactivate")
            }
        } else {
            // Show shield selection button
            val shieldType = currentPlayer.selectedShieldType
            val shieldQuantity = shieldType?.let { currentPlayer.inventory.getItemQuantity(it) } ?: 0

            if (shieldType != null && shieldQuantity > 0) {
                // Show selected shield and quantity
                Text(
                    "${shieldType.displayName} (${shieldQuantity})",
                    color = Color.White
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Button to activate shield
                Button(onClick = { game.activateShield() }) {
                    Text("Activate")
                }
            } else {
                // No shield selected or no shields in inventory
                Text(
                    "No shield selected",
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Button to open shield inventory
            Button(onClick = { showShieldInventory = true }) {
                Text("Select")
            }
        }
    }

    // Shield inventory popup
    if (showShieldInventory) {
        ShieldInventoryPopup(
            game = game,
            onDismiss = { showShieldInventory = false }
        )
    }
}

/**
 * Popup for selecting a shield from inventory.
 */
@Composable
fun ShieldInventoryPopup(
    game: ScorchedEarthGame,
    onDismiss: () -> Unit
) {
    val currentPlayer = game.players[game.currentPlayerIndex]

    // Simple popup with shield options
    androidx.compose.ui.window.Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
    ) {
        androidx.compose.material3.Card(
            modifier = Modifier.width(400.dp).padding(16.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = Color.DarkGray.copy(alpha = 0.9f)
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Shield",
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Display player's money
                Text(
                    text = "Money: $${currentPlayer.money}",
                    color = Color.Yellow,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Basic shield option
                val basicShield = ShieldType.BASIC_SHIELD
                val basicShieldQuantity = currentPlayer.inventory.getItemQuantity(basicShield)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Button(
                        onClick = {
                            game.selectShield(basicShield)
                            onDismiss()
                        },
                        enabled = basicShieldQuantity > 0
                    ) {
                        Text("${basicShield.displayName} (${basicShieldQuantity})")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Purchase button
                    Button(
                        onClick = {
                            val success = game.purchaseShield(basicShield)
                            if (success) {
                                // Auto-select after purchase
                                game.selectShield(basicShield)
                            }
                        },
                        enabled = currentPlayer.money >= basicShield.cost
                    ) {
                        Text("Buy (${basicShield.cost})")
                    }
                }

                // More shield types can be added here in the future

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End).padding(top = 16.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
