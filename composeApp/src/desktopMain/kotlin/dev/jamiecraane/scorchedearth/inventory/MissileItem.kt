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
import dev.jamiecraane.scorchedearth.inventory.ProjectileType

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
        modifier = Modifier.Companion
            .fillMaxWidth()
            .height(200.dp) // Fixed height instead of aspect ratio
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) Color.Companion.Yellow else Color.Companion.Transparent,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF444444) else Color(0xFF333333)
        )
    ) {
        Column(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween, // Changed to SpaceBetween
            horizontalAlignment = Alignment.Companion.CenterHorizontally
        ) {
            // Top section with icon and name
            Column(
                horizontalAlignment = Alignment.Companion.CenterHorizontally
            ) {
                // Missile icon (simple colored circle for now)
                Box(
                    modifier = Modifier.Companion
                        .size(24.dp)
                        .background(
                            getMissileColor(projectileType),
                            androidx.compose.foundation.shape.RoundedCornerShape(50)
                        )
                )

                Spacer(modifier = Modifier.Companion.height(4.dp))

                // Missile name
                Text(
                    text = projectileType.displayName,
                    color = Color.Companion.White,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            // Middle section with details
            Column(
                horizontalAlignment = Alignment.Companion.CenterHorizontally
            ) {
                // Damage info
                Text(
                    text = if (projectileType == ProjectileType.TRACER) "No Damage" else "DMG: ${projectileType.minDamage}-${projectileType.maxDamage}",
                    color = Color.Companion.LightGray,
                    fontSize = 10.sp
                )

                // Cost info
                Text(
                    text = "Cost: $${projectileType.cost}",
                    color = Color.Companion.LightGray,
                    fontSize = 10.sp
                )

                // Quantity
                Text(
                    text = "Qty: $quantity",
                    color = if (quantity > 0) Color.Companion.White else Color.Companion.Red,
                    fontSize = 10.sp
                )
            }

            // Bottom section with buy button
            if (onBuy != null) {
                Button(
                    onClick = onBuy,
                    modifier = Modifier.Companion
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
                        color = if (canAfford) Color.Companion.White else Color.Companion.DarkGray
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
        ProjectileType.LEAPFROG -> Color(0xFF00BCD4)  // Cyan
        ProjectileType.TRACER -> Color(0xFFAAAAAA)  // Light Gray
        ProjectileType.BABY_NUKE -> Color(0xFFF0756E)
        ProjectileType.ROLLER -> Color(0xFF4CAF50)  // Green
    }
}
