package dev.jamiecraane.scorchedearth

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
 * Control for adjusting the power of the cannon shot.
 */
@Composable
fun PowerControl(game: ScorchedEarthGame) {
    Row(
        modifier = Modifier.Companion.fillMaxWidth(),
        verticalAlignment = Alignment.Companion.CenterVertically
    ) {
        Text(
            "Power: ${game.players[game.currentPlayerIndex].power.toInt()}",
            modifier = Modifier.Companion.width(100.dp),
            color = Color.Companion.White
        )
        Slider(
            value = game.players[game.currentPlayerIndex].power,
            onValueChange = {
                val players = game.players.toMutableList()
                players[game.currentPlayerIndex] = players[game.currentPlayerIndex].copy(power = it)
                game.players = players
            },
            valueRange = 10f..100f,
            modifier = Modifier.Companion.weight(1f)
        )
    }
}
