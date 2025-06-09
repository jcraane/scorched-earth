package dev.jamiecraane.scorchedearth.terrain

import androidx.compose.ui.graphics.Color

/**
 * Represents different terrain styles with their corresponding colors.
 * Each style provides a color that can be used for rendering the terrain.
 */
enum class TerrainStyle(val displayName: String, val color: Color) {
    SAND("Sand", Color(0xFF8B4513)), // Brown color (original)
    GREEN("Green", Color(0xFF2E8B57)), // Sea green color
    GREY("Grey", Color(0xFF708090)); // Slate grey color
}
