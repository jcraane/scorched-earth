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
import dev.jamiecraane.scorchedearth.inventory.ProjectileType

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
        modifier = Modifier.Companion
            .fillMaxWidth()
            .height(40.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.Companion.DarkGray.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.Companion.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${currentMissile.displayName} (Damage: ${currentMissile.minDamage}-${currentMissile.maxDamage})",
                color = Color.Companion.White
            )

            // Display quantity
            Text(
                text = "Qty: $currentMissileQuantity",
                color = if (currentMissileQuantity > 0) Color.Companion.White else Color.Companion.Red
            )
        }
    }
}
