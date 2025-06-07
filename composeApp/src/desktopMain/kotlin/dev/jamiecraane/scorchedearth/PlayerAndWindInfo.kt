package dev.jamiecraane.scorchedearth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.jamiecraane.scorchedearth.engine.ScorchedEarthGame

/**
 * Displays the current player name and wind information.
 */
@Composable
fun PlayerAndWindInfo(game: ScorchedEarthGame) {
    Row(
        modifier = Modifier.Companion.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val currentPlayer = game.players[game.currentPlayerIndex]
        val playerName =
            if (currentPlayer.name.isNotEmpty()) currentPlayer.name else "Player ${game.currentPlayerIndex + 1}"
        Text("Player: $playerName", color = Color.Companion.White)

        Text("Wind: ${game.wind.toInt()} mph", color = Color.Companion.White)
    }
}
