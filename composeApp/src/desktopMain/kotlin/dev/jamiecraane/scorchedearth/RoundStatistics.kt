package dev.jamiecraane.scorchedearth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jamiecraane.scorchedearth.model.Player

@Composable
fun RoundStatistics(
    currentRound: Int,
    players: List<Player>,
    onNextCLick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { /* Do nothing, user must click button */ },
        title = { Text("Round ${currentRound} Results") },
        text = {
            Column {
                // Find the winner (player with highest elimination order)
                val winner = players.maxByOrNull { it.eliminationOrder }
                if (winner != null) {
                    Text(
                        "Winner: ${winner.name.ifEmpty { "Player ${players.indexOf(winner) + 1}" }}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.Companion.padding(bottom = 8.dp)
                    )
                }

                Text(
                    "Player Rankings:",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.Companion.padding(top = 8.dp, bottom = 4.dp)
                )

                // Sort players by elimination order (highest to lowest)
                val sortedPlayers = players.sortedByDescending { it.eliminationOrder }
                sortedPlayers.forEachIndexed { index, player ->
                    val playerName = player.name.ifEmpty { "Player ${players.indexOf(player) + 1}" }
                    Text(
                        "${index + 1}. $playerName",
                        modifier = Modifier.Companion.padding(vertical = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onNextCLick
            ) {
                Text("Next Round")
            }
        }
    )
}
