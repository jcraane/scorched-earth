package dev.jamiecraane.scorchedearth

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "scorched-earth",
    ) {
        App()
    }
}