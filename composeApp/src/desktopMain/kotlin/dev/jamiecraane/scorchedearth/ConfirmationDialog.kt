package dev.jamiecraane.scorchedearth

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ConfirmationDialog(
    show: Boolean,
    onConfirmClick: () -> Unit,
    onDismissClick: () -> Unit,
) {
    if (show) {
        AlertDialog(
            onDismissRequest = { onDismissClick() },
            title = { Text("Return to Main Menu") },
            text = { Text("Are you sure you want to return to the main menu? Your current game progress will be lost.") },
            confirmButton = {
                Button(
                    onClick = onConfirmClick
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(
                    onClick = onDismissClick
                ) {
                    Text("No")
                }
            }
        )
    }

}
