package dev.jamiecraane.scorchedearth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jamiecraane.scorchedearth.sky.SkyStyleSelector
import kotlin.math.sin

/**
 * Intro screen shown before the game starts.
 *
 * @param onStartGame Callback when the start button is clicked, with the selected number of players and sky style
 */
@Composable
fun IntroScreen(onStartGame: (Int, SkyStyleSelector) -> Unit) {
    // State for the number of players slider
    var numberOfPlayers by remember { mutableStateOf(2f) }

    // State for the selected sky style
    var selectedSkyStyle by remember { mutableStateOf(SkyStyleSelector.getDefault()) }

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
        // Title at the top center
        Text(
            text = "Scorched Earth KMP",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 64.dp)
                .align(Alignment.TopCenter)
        )

        // Game illustration on the right side
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 64.dp)
                .size(300.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x33FFFFFF))
                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        ) {
            // Draw a simple game illustration
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw a simple terrain
                val path = Path()
                val width = size.width
                val height = size.height

                // Start at bottom left
                path.moveTo(0f, height)

                // Create a wavy terrain line
                for (x in 0..width.toInt() step 5) {
                    val xFloat = x.toFloat()
                    val y = height * 0.7f + sin(xFloat * 0.05f) * 30f
                    path.lineTo(xFloat, y)
                }

                // Close the path
                path.lineTo(width, height)
                path.close()

                // Draw the terrain
                drawPath(
                    path = path,
                    color = Color(0xFF8B4513) // Brown color
                )

                // Draw two tanks
                val tank1X = width * 0.2f
                val tank2X = width * 0.8f
                val tankY = height * 0.7f - 15f

                // Tank 1 (red)
                drawCircle(
                    color = Color.Red,
                    radius = 15f,
                    center = Offset(tank1X, tankY)
                )

                // Tank 2 (blue)
                drawCircle(
                    color = Color.Blue,
                    radius = 15f,
                    center = Offset(tank2X, tankY)
                )
            }
        }

        // Controls on the left side
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 64.dp)
                .width(300.dp)
        ) {
            // Number of players slider
            Text(
                text = "Number of Players: ${numberOfPlayers.toInt()}",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Slider(
                value = numberOfPlayers,
                onValueChange = { numberOfPlayers = it },
                valueRange = 2f..10f,
                steps = 7, // 8 steps for 2-10 players
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sky style selection
            Text(
                text = "Sky Style:",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Radio buttons for sky style selection
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                SkyStyleSelector.values().forEach { skyStyle ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (skyStyle == selectedSkyStyle),
                                onClick = { selectedSkyStyle = skyStyle }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (skyStyle == selectedSkyStyle),
                            onClick = { selectedSkyStyle = skyStyle }
                        )
                        Text(
                            text = skyStyle.displayName,
                            color = Color.White,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start button
            Button(
                onClick = { onStartGame(numberOfPlayers.toInt(), selectedSkyStyle) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Start Game",
                    fontSize = 20.sp
                )
            }
        }
    }
}
