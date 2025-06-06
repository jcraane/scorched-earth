package dev.jamiecraane.scorchedearth.sky

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

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
    },

    NIGHT("Night") {
        override fun createGradientBrush(): Brush {
            return Brush.verticalGradient(
                // Dark shaded background for night sky
                0f to Color(0xFF000011), // Almost black at the top
                0.3f to Color(0xFF000033), // Very dark blue
                0.7f to Color(0xFF000055), // Dark blue
                1f to Color(0xFF000088)  // Slightly lighter dark blue at the horizon
            )
        }
    };

    /**
     * Creates a gradient brush for this sky style.
     * @return A vertical gradient brush with colors appropriate for this sky style.
     */
    abstract fun createGradientBrush(): Brush

    companion object {
        /**
         * Generates random star positions for the night sky.
         * @param width Width of the canvas
         * @param height Height of the canvas
         * @param count Number of stars to generate
         * @return List of star positions (x, y) and sizes
         */
        fun generateStars(width: Float, height: Float, count: Int = 100): List<Star> {
            val stars = mutableListOf<Star>()
            val random = Random(System.currentTimeMillis())

            for (i in 0 until count) {
                val x = random.nextFloat() * width
                val y = random.nextFloat() * height * 0.7f // Stars only in the top 70% of the sky
                val size = 1f + random.nextFloat() * 2f // Random size between 1 and 3
                val brightness = 0.5f + random.nextFloat() * 0.5f // Random brightness between 0.5 and 1.0

                stars.add(Star(x, y, size, brightness))
            }

            return stars
        }

        /**
         * Updates the brightness of stars to create a flickering effect.
         * @param stars The list of stars to update
         */
        fun updateStarBrightness(stars: List<Star>) {
            val random = Random(System.currentTimeMillis())

            stars.forEach { star ->
                // Randomly adjust brightness up or down by a small amount
                val adjustment = (random.nextFloat() - 0.5f) * 0.2f // -0.1 to +0.1
                star.brightness = (star.brightness + adjustment).coerceIn(0.3f, 1.0f)
            }
        }
    }
}

/**
 * Represents a star in the night sky.
 * @property x X-coordinate of the star
 * @property y Y-coordinate of the star
 * @property size Size of the star
 * @property brightness Brightness of the star (0.0-1.0)
 */
data class Star(val x: Float, val y: Float, val size: Float, var brightness: Float)
