package dev.jamiecraane.scorchedearth.gameui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Header(
    currentRound: Int,
    onBackButtonClick: () -> Unit,
    transitionToNextRoundClick: () -> Unit,
    modifier: Modifier = Modifier.Companion,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Companion.CenterVertically,
    ) {
        Button(
            onClick = onBackButtonClick,
            modifier = Modifier.Companion.padding(4.dp)
        ) {
            Text("Back")
        }

        Text(
            "Round: $currentRound",
            modifier = Modifier.Companion.padding(4.dp),
            color = Color.Companion.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )

//            for debug purposes
        Button(
            onClick = transitionToNextRoundClick,
            modifier = Modifier.Companion.padding(4.dp)
        ) {
            Text("Trigger next round")
        }
    }
}
