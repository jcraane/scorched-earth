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

/**
 * Displays the final game statistics when all rounds are completed.
 * Shows the overall winner and player rankings based on their performance across all rounds.
 *
 * @param totalRounds The total number of rounds played
 * @param players The list of players
 * @param onBackToIntroClick Callback to navigate back to the intro screen
 */
@Composable
fun GameStatistics(
    totalRounds: Int,
    players: List<Player>,
    onBackToIntroClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { /* Do nothing, user must click button */ },
        title = { Text("Game Over - Final Results") },
        text = {
            Column {
                Text(
                    "Total Rounds Played: $totalRounds",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Find the overall winner (player with highest elimination order)
                val winner = players.maxByOrNull { it.eliminationOrder }
                if (winner != null) {
                    Text(
                        "Overall Winner: ${winner.name.ifEmpty { "Player ${players.indexOf(winner) + 1}" }}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Text(
                    "Final Rankings:",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                // Sort players by elimination order (highest to lowest)
                val sortedPlayers = players.sortedByDescending { it.eliminationOrder }
                sortedPlayers.forEachIndexed { index, player ->
                    val playerName = player.name.ifEmpty { "Player ${players.indexOf(player) + 1}" }
                    Text(
                        "${index + 1}. $playerName",
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onBackToIntroClick
            ) {
                Text("Back to Main Menu")
            }
        }
    )
}
