package dev.jamiecraane.scorchedearth.sky

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Represents different sky styles with their corresponding gradient colors.
 * Each style provides a gradient brush that can be used for rendering.
 */
enum class SkyStyle(val displayName: String) {
    SUNRISE("Sunrise") {
        override fun createGradientBrush(): Brush {
            return Brush.verticalGradient(
                // Using color-stop pairs for a more natural look
                0f to Color(0xFF1E3C72), // Deep blue at the top
                0.3f to Color(0xFF2A5298), // Mid blue
                0.6f to Color(0xFFF9D423), // Yellow
                1f to Color(0xFFFF7F50)  // Coral/orange at the horizon
            )
        }
    },

    AFTERNOON("Afternoon") {
        override fun createGradientBrush(): Brush {
            return Brush.verticalGradient(
                // Using color-stop pairs for a more natural look
                0f to Color(0xFF0078D7), // Sky blue at the top
                0.4f to Color(0xFF48B0F7), // Lighter blue
                0.7f to Color(0xFF87CEEB), // Light sky blue
                1f to Color(0xFFADD8E6)  // Very light blue at the horizon
            )
        }
    },

    SUNSET("Sunset") {
        override fun createGradientBrush(): Brush {
            return Brush.verticalGradient(
                // Using color-stop pairs for a more dramatic sunset effect
                0f to Color(0xFF0D1B2A), // Dark blue at the top
                0.25f to Color(0xFF2C3E50), // Navy blue
                0.5f to Color(0xFF7F5A83), // Purple
                0.75f to Color(0xFFE96443), // Orange
                1f to Color(0xFFFF9966)  // Light orange at the horizon
            )
        }
    };

    /**
     * Creates a gradient brush for this sky style.
     * @return A vertical gradient brush with colors appropriate for this sky style.
     */
    abstract fun createGradientBrush(): Brush
}
