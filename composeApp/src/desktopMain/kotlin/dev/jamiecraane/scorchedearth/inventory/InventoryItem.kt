package dev.jamiecraane.scorchedearth.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Composable function to display an inventory item in the grid.
 */
@Composable
fun InventoryItem(
    itemType: ItemType,
    isSelected: Boolean,
    quantity: Int = 0,
    canAfford: Boolean = true,
    onClick: () -> Unit,
    onBuy: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
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
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top section with icon and name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Item icon (simple colored circle for now)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            getItemColor(itemType),
                            RoundedCornerShape(50)
                        )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Item name
                Text(
                    text = itemType.displayName,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            // Middle section with details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Item details based on type
                when (itemType) {
                    is ProjectileType -> {
                        // Damage info for projectiles
                        Text(
                            text = if (itemType == ProjectileType.TRACER) "No Damage" else "DMG: ${itemType.minDamage}-${itemType.maxDamage}",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }
                    is ShieldType -> {
                        // Health info for shields
                        Text(
                            text = "Health: ${itemType.maxHealth}",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }
                }

                // Cost info (common for all items)
                Text(
                    text = "Cost: $${itemType.cost}",
                    color = Color.LightGray,
                    fontSize = 10.sp
                )

                // Quantity (common for all items)
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
 * Returns a color for the item based on its type.
 */
fun getItemColor(itemType: ItemType): Color {
    return when (itemType) {
        // Projectile colors
        ProjectileType.BABY_MISSILE -> Color(0xFF8BC34A)  // Light Green
        ProjectileType.SMALL_MISSILE -> Color(0xFFFFEB3B)  // Yellow
        ProjectileType.BIG_MISSILE -> Color(0xFFFF9800)  // Orange
        ProjectileType.DEATHS_HEAD -> Color(0xFFE91E63)  // Pink
        ProjectileType.NUCLEAR_BOMB -> Color(0xFFF44336)  // Red
        ProjectileType.FUNKY_BOMB -> Color(0xFF9C27B0)  // Purple
        ProjectileType.MIRV -> Color(0xFF9D50B0)
        ProjectileType.LEAPFROG -> Color(0xFF00BCD4)  // Cyan
        ProjectileType.TRACER -> Color(0xFFAAAAAA)  // Light Gray
        ProjectileType.BABY_NUKE -> Color(0xFFF0756E)
        ProjectileType.ROLLER -> Color(0xFF4CAF50)  // Green

        // Shield colors
        ShieldType.BASIC_SHIELD -> Color(0xFF2196F3)  // Blue

        // Default color for unknown types
        else -> Color.Gray
    }
}
