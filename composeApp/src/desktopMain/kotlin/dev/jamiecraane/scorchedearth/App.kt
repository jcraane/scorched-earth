package dev.jamiecraane.scorchedearth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Slider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.cos
import kotlin.math.sin

@Composable
@Preview
fun App() {
    // State to track whether the game has started
    var gameStarted by remember { mutableStateOf(false) }

    // State to track the number of players
    var numberOfPlayers by remember { mutableStateOf(2) }

    // Track canvas size to detect changes
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    // Show intro screen if game hasn't started
    if (!gameStarted) {
        IntroScreen(
            onStartGame = { players ->
                numberOfPlayers = players
                gameStarted = true
            }
        )
    } else {
        // Game is started, create the game instance
        val game = remember(numberOfPlayers) { ScorchedEarthGame(numberOfPlayers) }

        // Game UI
        GameUI(game, canvasSize)
    }
}

@Composable
private fun GameUI(game: ScorchedEarthGame, initialCanvasSize: Size) {
    // Track canvas size to detect changes
    var canvasSize by remember { mutableStateOf(initialCanvasSize) }

    // Game loop using LaunchedEffect
    LaunchedEffect(Unit) {
        var lastTime = System.currentTimeMillis()
        while (true) {
            val currentTime = System.currentTimeMillis()
            val deltaTime = (currentTime - lastTime) / 1000f
            lastTime = currentTime

            game.update(deltaTime)
            delay(16) // ~60 FPS
        }
    }

    // Main game container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF87CEEB)) // Sky blue background
    ) {

        // Game canvas where all rendering happens
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Only update dimensions when canvas size actually changes
            if (canvasSize != size) {
                canvasSize = size
                game.updateDimensions(size.width, size.height)
            }

            // Draw the game elements
            drawGame(game)
        }

        // UI controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            // Display current player and wind
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Player: ${game.currentPlayerIndex + 1}", color = Color.White)
                Text("Wind: ${game.wind.toInt()} mph", color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Angle control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Angle: ${game.players[game.currentPlayerIndex].angle.toInt()}°",
                    modifier = Modifier.width(100.dp),
                    color = Color.White)
                Slider(
                    value = game.players[game.currentPlayerIndex].angle,
                    onValueChange = {
                        val players = game.players.toMutableList()
                        players[game.currentPlayerIndex] = players[game.currentPlayerIndex].copy(angle = it)
                        game.players = players
                    },
                    valueRange = 0f..90f,
                    modifier = Modifier.weight(1f)
                )
            }

            // Power control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Power: ${game.players[game.currentPlayerIndex].power.toInt()}",
                    modifier = Modifier.width(100.dp),
                    color = Color.White)
                Slider(
                    value = game.players[game.currentPlayerIndex].power,
                    onValueChange = {
                        val players = game.players.toMutableList()
                        players[game.currentPlayerIndex] = players[game.currentPlayerIndex].copy(power = it)
                        game.players = players
                    },
                    valueRange = 10f..100f,
                    modifier = Modifier.weight(1f)
                )
            }

            // Missile type selection
            var showInventoryPopup by remember { mutableStateOf(false) }
            val currentPlayer = game.players[game.currentPlayerIndex]
            val currentMissile = currentPlayer.selectedProjectileType
            val currentMissileQuantity = currentPlayer.inventory.getItemQuantity(currentMissile)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Inventory: ",
                    modifier = Modifier.width(100.dp),
                    color = Color.White)

                // Button to show current missile and open popup
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable { showInventoryPopup = true },
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

                // Missile selection popup
                if (showInventoryPopup) {
                    Popup(
                        alignment = Alignment.Center,
                        onDismissRequest = { showInventoryPopup = false },
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
                                    items(ProjectileType.values()) { projectileType ->
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
                                                    showInventoryPopup = false
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fire button
            var showNoMissilesMessage by remember { mutableStateOf(false) }

            Button(
                onClick = {
                    val player = game.players[game.currentPlayerIndex]
                    val success = game.fireProjectile(player.angle, player.power)
                    if (!success) {
                        showNoMissilesMessage = true
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = game.gameState == GameState.WAITING_FOR_PLAYER
            ) {
                Text("FIRE!")
            }

            // Show message if player doesn't have the selected missile
            if (showNoMissilesMessage) {
                Text(
                    text = "No ${game.players[game.currentPlayerIndex].selectedProjectileType.displayName} missiles left!",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
                )

                // Hide the message after a delay
                LaunchedEffect(showNoMissilesMessage) {
                    delay(2000)
                    showNoMissilesMessage = false
                }
            }
        }
    }
}

/**
 * Composable function to display a missile item in the grid.
 */
@Composable
private fun MissileItem(
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
private fun getMissileColor(projectileType: ProjectileType): Color {
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

// Function to draw all game elements
private fun DrawScope.drawGame(game: ScorchedEarthGame) {
    // Scale all game elements to match the canvas size
    scale(scaleX = 1f, scaleY = 1f) {
        // Draw terrain
        drawPath(
            path = game.terrain,
            color = Color(0xFF8B4513), // Brown color for terrain
            style = Stroke(width = 2f)
        )
        drawPath(
            path = game.terrain,
            color = Color(0xFF8B4513).copy(alpha = 0.5f) // Semi-transparent fill
        )

        // Draw players (tanks)
        game.players.forEachIndexed { index, player ->
            // Draw tank body
            drawCircle(
                color = player.color,
                radius = 15f,
                center = player.position
            )

            // Draw tank cannon
            val angleRadians = player.angle * Math.PI.toFloat() / 180f
            val cannonLength = 30f

            // For the blue player (index 1), we need to flip the angle to face left
            val adjustedAngleRadians = if (index == 1) {
                // Convert angle to face left: 180° - angle
                (180f - player.angle) * Math.PI.toFloat() / 180f
            } else {
                angleRadians
            }

            drawLine(
                color = player.color,
                start = player.position,
                end = Offset(
                    player.position.x + cos(adjustedAngleRadians) * cannonLength,
                    player.position.y - sin(adjustedAngleRadians) * cannonLength
                ),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )

            // Highlight current player
            if (index == game.currentPlayerIndex) {
                drawCircle(
                    color = Color.Yellow,
                    radius = 20f,
                    center = player.position,
                    style = Stroke(width = 2f)
                )
            }

            // Draw player health
            // Calculate health bar width based on health percentage
            val maxHealthBarWidth = 80f
            val healthBarHeight = 14f
            val healthPercentage = player.health / 100f
            val healthBarWidth = maxHealthBarWidth * healthPercentage

            // Draw health background (red)
            drawRect(
                color = Color.Red,
                topLeft = Offset(player.position.x - maxHealthBarWidth / 2, player.position.y - 36f),
                size = androidx.compose.ui.geometry.Size(maxHealthBarWidth, healthBarHeight)
            )

            // Draw health foreground (green)
            drawRect(
                color = Color.Green,
                topLeft = Offset(player.position.x - maxHealthBarWidth / 2, player.position.y - 36f),
                size = androidx.compose.ui.geometry.Size(healthBarWidth, healthBarHeight)
            )
        }

        // Draw projectile if in flight
        game.projectile?.let { projectile ->
            // Draw the trail with fade-out effect
            projectile.trail.forEachIndexed { index, position ->
                // Calculate alpha based on position in trail (older positions are more transparent)
                val alpha = (index + 1).toFloat() / projectile.trail.size.toFloat()

                drawCircle(
                    color = Color.Gray.copy(alpha = alpha * 0.7f),
                    radius = 3f + (2f * alpha), // Smaller circles for older positions
                    center = position
                )
            }

            // Draw the actual projectile
            drawCircle(
                color = Color.Black,
                radius = 5f,
                center = projectile.position
            )
        }

        game.miniBombs.forEach { miniBomb ->
            // Draw the trail with fade-out effect
            miniBomb.trail.forEachIndexed { index, position ->
                // Calculate alpha based on position in trail (older positions are more transparent)
                val alpha = (index + 1).toFloat() / miniBomb.trail.size.toFloat()

                drawCircle(
                    color = Color.Gray.copy(alpha = alpha * 0.7f),
                    radius = 2f + (1f * alpha), // Smaller circles for older positions
                    center = position
                )
            }

            // Draw the actual mini-bomb projectile
            drawCircle(
                color = Color.Red, // Different color to distinguish from main projectile
                radius = 3f,
                center = miniBomb.position
            )
        }

        // Draw explosion if it exists
        game.explosion?.let { explosion ->
            // Create radial gradient from light (hot) center to dark red outer
            val gradientBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFF99), // Light yellow-white (hottest center)
                    Color(0xFFFFCC00), // Bright yellow-orange
                    Color(0xFFFF6600), // Orange
                    Color(0xFFFF3300), // Red-orange
                    Color(0xFFCC0000), // Dark red
                    Color(0xFF990000)  // Very dark red (outer edge)
                ),
                center = explosion.position,
                radius = explosion.currentRadius
            )

            drawCircle(
                brush = gradientBrush,
                radius = explosion.currentRadius,
                center = explosion.position
            )
        }

        // Draw wind indicator
        val windIndicatorX = size.width / 2
        val windIndicatorY = 50f
        val windStrength = game.wind.coerceIn(-10f, 10f)
        val windLineLength = windStrength * 5f

        drawLine(
            color = Color.White,
            start = Offset(windIndicatorX - windLineLength, windIndicatorY),
            end = Offset(windIndicatorX + windLineLength, windIndicatorY),
            strokeWidth = 2f
        )

        // Wind direction arrow
        if (windStrength != 0f) {
            val arrowX = if (windStrength > 0) windIndicatorX + windLineLength else windIndicatorX - windLineLength
            val arrowSize = 10f
            val arrowDirection = if (windStrength > 0) 0f else 180f
            val arrowRadians = arrowDirection * Math.PI.toFloat() / 180f

            drawLine(
                color = Color.White,
                start = Offset(arrowX, windIndicatorY),
                end = Offset(
                    arrowX - cos(arrowRadians + 0.5f) * arrowSize,
                    windIndicatorY - sin(arrowRadians + 0.5f) * arrowSize
                ),
                strokeWidth = 2f
            )

            drawLine(
                color = Color.White,
                start = Offset(arrowX, windIndicatorY),
                end = Offset(
                    arrowX - cos(arrowRadians - 0.5f) * arrowSize,
                    windIndicatorY - sin(arrowRadians - 0.5f) * arrowSize
                ),
                strokeWidth = 2f
            )
        }
    }
}
