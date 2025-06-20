package dev.jamiecraane.scorchedearth

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Scorched Earth",
        state = WindowState(size = DpSize(1600.dp, 1200.dp)),
        resizable = false
    ) {
        App()
    }
}
