package dev.jamiecraane.scorchedearth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    // Create a game instance to manage state
    val game = remember { ScorchedEarthGame() }

    // Main game container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF87CEEB)) // Sky blue background
    ) {
        // Game canvas where all rendering happens
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw the game elements
            drawGame(game)
        }
    }
}

// Function to draw all game elements
private fun DrawScope.drawGame(game: ScorchedEarthGame) {
    // Draw a simple placeholder terrain for now
    drawRect(
        color = Color(0xFF8B4513), // Brown color for terrain
        topLeft = Offset(0f, size.height * 0.7f),
        size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.3f)
    )

    // Draw a placeholder tank
    drawCircle(
        color = Color.Green,
        radius = 20f,
        center = Offset(100f, size.height * 0.7f - 20f)
    )
}

