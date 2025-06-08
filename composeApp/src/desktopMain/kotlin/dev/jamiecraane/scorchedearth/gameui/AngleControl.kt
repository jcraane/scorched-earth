package dev.jamiecraane.scorchedearth.gameui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.jamiecraane.scorchedearth.engine.ScorchedEarthGame

/**
 * Control for adjusting the angle of the cannon.
 */
@Composable
fun AngleControl(game: ScorchedEarthGame) {
    Row(
        modifier = Modifier.Companion.fillMaxWidth(),
        verticalAlignment = Alignment.Companion.CenterVertically
    ) {
        Text(
            "Angle: ${game.players[game.currentPlayerIndex].angle.toInt()}° (0=right)",
            modifier = Modifier.Companion.width(150.dp),
            color = Color.Companion.White
        )
        Slider(
            value = game.players[game.currentPlayerIndex].angle,
            onValueChange = {
                val players = game.players.toMutableList()
                players[game.currentPlayerIndex] = players[game.currentPlayerIndex].copy(angle = it)
                game.players = players
            },
            valueRange = -90f..90f,
            modifier = Modifier.Companion.weight(1f)
        )
    }
}
