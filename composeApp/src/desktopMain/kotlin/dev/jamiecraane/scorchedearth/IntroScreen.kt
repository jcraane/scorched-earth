package dev.jamiecraane.scorchedearth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import dev.jamiecraane.scorchedearth.terrain.TerrainStyleSelector
import dev.jamiecraane.scorchedearth.weather.WeatherType
import kotlin.math.sin

/**
 * Intro screen shown before the game starts.
 *
 * @param onStartGame Callback when the start button is clicked, with the selected number of players, sky style, terrain style, terrain variance, number of rounds, and weather type
 */
@Composable
fun IntroScreen(onStartGame: (Int, SkyStyleSelector, TerrainStyleSelector, Int, Int, WeatherType) -> Unit) {
    // State for the number of players slider
    var numberOfPlayers by remember { mutableStateOf(2f) }

    // State for the terrain variance slider (0-100)
    var terrainVariance by remember { mutableStateOf(25f) }

    // State for the number of rounds slider (1-25)
    var numberOfRounds by remember { mutableStateOf(1f) }

    // State for the selected sky style
    var selectedSkyStyle by remember { mutableStateOf(SkyStyleSelector.getDefault()) }

    // State for the selected terrain style
    var selectedTerrainStyle by remember { mutableStateOf(TerrainStyleSelector.getDefault()) }

    // State for rain enabled/disabled
    var rainEnabled by remember { mutableStateOf(false) }

    // State for lightning enabled/disabled
    var lightningEnabled by remember { mutableStateOf(false) }

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
                    color = selectedTerrainStyle.toTerrainStyle().color // Use selected terrain style color
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

            // Terrain variance slider
            Text(
                text = "Terrain Variance: ${terrainVariance.toInt()}",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "0 = Flat, 100 = Extreme",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Slider(
                value = terrainVariance,
                onValueChange = { terrainVariance = it },
                valueRange = 0f..100f,
                steps = 99, // 100 steps for 0-100 variance
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Number of rounds slider
            Text(
                text = "Number of Rounds: ${numberOfRounds.toInt()}",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Slider(
                value = numberOfRounds,
                onValueChange = { numberOfRounds = it },
                valueRange = 1f..25f,
                steps = 23, // 24 steps for 1-25 rounds
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

            // Terrain style selection
            Text(
                text = "Terrain Style:",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Radio buttons for terrain style selection
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                TerrainStyleSelector.values().forEach { terrainStyle ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (terrainStyle == selectedTerrainStyle),
                                onClick = { selectedTerrainStyle = terrainStyle }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (terrainStyle == selectedTerrainStyle),
                            onClick = { selectedTerrainStyle = terrainStyle }
                        )
                        Text(
                            text = terrainStyle.displayName,
                            color = Color.White,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rain checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rainEnabled,
                    onCheckedChange = {
                        rainEnabled = it
                        // Disable lightning if rain is enabled
                        if (it) {
                            lightningEnabled = false
                        }
                    }
                )
                Text(
                    text = "Enable Rain",
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Lightning checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = lightningEnabled,
                    onCheckedChange = {
                        lightningEnabled = it
                        // Disable rain if lightning is enabled
                        if (it) {
                            rainEnabled = false
                        }
                    }
                )
                Text(
                    text = "Enable Lightning",
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start button
            Button(
                onClick = {
                    // Determine weather type based on checkboxes
                    val weatherType = when {
                        rainEnabled -> WeatherType.RAIN
                        lightningEnabled -> WeatherType.LIGHTNING
                        else -> WeatherType.NONE
                    }
                    onStartGame(numberOfPlayers.toInt(), selectedSkyStyle, selectedTerrainStyle, terrainVariance.toInt(), numberOfRounds.toInt(), weatherType)
                },
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
