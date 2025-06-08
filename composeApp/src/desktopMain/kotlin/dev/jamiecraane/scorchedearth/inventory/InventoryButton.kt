package dev.jamiecraane.scorchedearth.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Button to display the current inventory item and open the inventory popup.
 */
@Composable
fun InventoryButton(
    currentItem: ItemType?,
    currentItemQuantity: Int,
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
            if (currentItem != null) {
                // Display item details based on type
                val itemDetails = when (currentItem) {
                    is ProjectileType -> "Damage: ${currentItem.minDamage}-${currentItem.maxDamage}"
                    is ShieldType -> "Health: ${currentItem.maxHealth}"
                    else -> ""
                }

                Text(
                    text = "${currentItem.displayName} ($itemDetails)",
                    color = Color.White
                )

                // Display quantity
                Text(
                    text = "Qty: $currentItemQuantity",
                    color = if (currentItemQuantity > 0) Color.White else Color.Red
                )
            } else {
                Text(
                    text = "No item selected",
                    color = Color.Gray
                )
            }
        }
    }
}
