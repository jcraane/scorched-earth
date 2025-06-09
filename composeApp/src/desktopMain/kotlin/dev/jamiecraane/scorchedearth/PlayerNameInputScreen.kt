package dev.jamiecraane.scorchedearth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jamiecraane.scorchedearth.model.PlayerType

/**
 * Data class to hold player setup information
 */
data class PlayerSetup(
    val name: String,
    val type: PlayerType,
)

/**
 * Screen for players to enter their names and select their types (human or CPU).
 *
 * @param numberOfPlayers The total number of players in the game
 * @param onComplete Callback when all players have entered their information, with a list of player setups
 */
@Composable
fun PlayerNameInputScreen(
    numberOfPlayers: Int,
    onComplete: (List<PlayerSetup>) -> Unit,
) {
    // State to track the current player index
    var currentPlayerIndex by remember { mutableStateOf(0) }

    // State to store all player setups
    val playerSetups = remember {
        mutableStateListOf<PlayerSetup>().apply {
            // Initialize with default values
            repeat(numberOfPlayers) { index ->
                add(PlayerSetup("Player ${index + 1}", PlayerType.HUMAN))
            }
        }
    }

    // State for the current player's name input
    var currentPlayerName by remember(currentPlayerIndex) {
        mutableStateOf(playerSetups[currentPlayerIndex].name)
    }

    // State for the current player's type selection
    var currentPlayerType by remember(currentPlayerIndex) {
        mutableStateOf(playerSetups[currentPlayerIndex].type)
    }

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
        Column(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Enter Player Information",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Current player label
            Text(
                text = "Player ${currentPlayerIndex + 1}",
                color = Color.White,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Name input field
            OutlinedTextField(
                value = currentPlayerName,
                onValueChange = { currentPlayerName = it },
                label = { Text("Name") },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(8.dp)
            )

            // Player type selection
            Text(
                text = "Player Type",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Radio buttons for player type
            Column(
                modifier = Modifier
                    .selectableGroup()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .selectable(
                            selected = currentPlayerType == PlayerType.HUMAN,
                            onClick = { currentPlayerType = PlayerType.HUMAN },
                            role = Role.RadioButton
                        )
                        .padding(8.dp)
                ) {
                    RadioButton(
                        selected = currentPlayerType == PlayerType.HUMAN,
                        onClick = null // null because we're handling it in the Row
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Human",
                        color = Color.White
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .selectable(
                            selected = currentPlayerType == PlayerType.CPU,
                            onClick = { currentPlayerType = PlayerType.CPU },
                            role = Role.RadioButton
                        )
                        .padding(8.dp)
                ) {
                    RadioButton(
                        selected = currentPlayerType == PlayerType.CPU,
                        onClick = null // null because we're handling it in the Row
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CPU",
                        color = Color.White
                    )
                }
            }

            // Next/Start button
            Button(
                onClick = {
                    // Save the current player's information
                    playerSetups[currentPlayerIndex] = PlayerSetup(currentPlayerName, currentPlayerType)

                    if (currentPlayerIndex < numberOfPlayers - 1) {
                        // Move to the next player
                        currentPlayerIndex++
                    } else {
                        // All players have entered their information, complete
                        onComplete(playerSetups.toList())
                    }
                },
                modifier = Modifier
                    .width(200.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = if (currentPlayerIndex < numberOfPlayers - 1) "Next" else "Start Game",
                    fontSize = 20.sp
                )
            }
        }
    }
}
