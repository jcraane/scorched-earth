package dev.jamiecraane.scorchedearth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Screen for players to enter their names.
 *
 * @param numberOfPlayers The total number of players in the game
 * @param onComplete Callback when all players have entered their names, with a list of player names
 */
@Composable
fun PlayerNameInputScreen(
    numberOfPlayers: Int,
    onComplete: (List<String>) -> Unit
) {
    // State to track the current player index
    var currentPlayerIndex by remember { mutableStateOf(0) }

    // State to store all player names
    val playerNames = remember { mutableStateListOf<String>().apply {
        // Initialize with default names
        repeat(numberOfPlayers) { index ->
            add("Player ${index + 1}")
        }
    }}

    // State for the current player's name input
    var currentPlayerName by remember(currentPlayerIndex) {
        mutableStateOf(playerNames[currentPlayerIndex])
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
                text = "Enter Player Names",
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
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(8.dp)
            )

            // Next/Start button
            Button(
                onClick = {
                    // Save the current player's name
                    playerNames[currentPlayerIndex] = currentPlayerName

                    if (currentPlayerIndex < numberOfPlayers - 1) {
                        // Move to the next player
                        currentPlayerIndex++
                    } else {
                        // All players have entered their names, complete
                        onComplete(playerNames)
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
