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

        override fun createSun(width: Float, height: Float): Sun? {
            // Position the sun near the horizon, slightly to the right
            return Sun(
                x = width * 0.7f,
                y = height * 0.75f,
                radius = width * 0.1f, // Large sun, 10% of screen width
                color = Color(0xFFFF8C00) // Dark orange color
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

        override fun createSun(width: Float, height: Float): Sun? {
            // No sun in afternoon sky
            return null
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

        override fun createSun(width: Float, height: Float): Sun? {
            // Position the sun near the horizon, slightly to the left
            return Sun(
                x = width * 0.3f,
                y = height * 0.8f,
                radius = width * 0.12f, // Large sun, 12% of screen width
                color = Color(0xFFFF4500) // Orange-red color
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

        override fun createSun(width: Float, height: Float): Sun? {
            // No sun in night sky
            return null
        }
    };

    /**
     * Creates a gradient brush for this sky style.
     * @return A vertical gradient brush with colors appropriate for this sky style.
     */
    abstract fun createGradientBrush(): Brush

    /**
     * Creates a sun for this sky style if applicable.
     * @param width Width of the canvas
     * @param height Height of the canvas
     * @return A Sun object or null if this sky style doesn't have a sun
     */
    abstract fun createSun(width: Float, height: Float): Sun?

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
                val baseSize = 1f + random.nextFloat() * 2f // Random size between 1 and 3
                val brightness = 0.5f + random.nextFloat() * 0.5f // Random brightness between 0.5 and 1.0
                val flickerSpeed = 0.02f + random.nextFloat() * 0.08f // Random flicker speed between 0.02 and 0.1
                val flickerRange = 0.2f + random.nextFloat() * 0.3f // Random flicker range between 0.2 and 0.5
                val flickerDirection = if (random.nextBoolean()) 1 else -1 // Random initial flicker direction

                stars.add(Star(
                    x = x,
                    y = y,
                    baseSize = baseSize,
                    size = baseSize,
                    brightness = brightness,
                    flickerSpeed = flickerSpeed,
                    flickerRange = flickerRange,
                    flickerDirection = flickerDirection
                ))
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
                // Determine if we should change flicker direction
                // Higher flickerSpeed means more frequent direction changes
                if (random.nextFloat() < star.flickerSpeed * 0.5f) {
                    star.flickerDirection *= -1 // Reverse direction
                }

                // Calculate brightness adjustment based on direction and speed
                val adjustment = star.flickerSpeed * star.flickerDirection

                // Update brightness
                star.brightness = (star.brightness + adjustment).coerceIn(0.3f, 1.0f)

                // If we hit the brightness limits, reverse direction
                if (star.brightness >= 1.0f || star.brightness <= 0.3f) {
                    star.flickerDirection *= -1
                }

                // Also slightly adjust star size for a twinkling effect
                val sizeAdjustment = adjustment * 0.3f // Smaller size adjustment
                star.size = (star.baseSize + sizeAdjustment).coerceIn(star.baseSize * 0.7f, star.baseSize * 1.3f)

                // Occasionally add a random "twinkle" effect
                if (random.nextFloat() < 0.02f) { // 2% chance per update
                    star.brightness = 1.0f // Brief flash to full brightness
                    star.size = star.baseSize * 1.5f // Brief increase in size
                }
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
 * @property flickerSpeed How quickly this star flickers (0.0-1.0)
 * @property flickerRange The range of brightness variation for this star
 * @property flickerDirection Current direction of flickering (1 = brightening, -1 = dimming)
 * @property baseSize Base size of the star before any animation
 */
data class Star(
    val x: Float,
    val y: Float,
    val baseSize: Float,
    var size: Float,
    var brightness: Float,
    val flickerSpeed: Float = 0.05f,
    val flickerRange: Float = 0.3f,
    var flickerDirection: Int = 1
)

/**
 * Represents a sun in the sky.
 * @property x X-coordinate of the sun's center
 * @property y Y-coordinate of the sun's center
 * @property radius Radius of the sun
 * @property color Color of the sun
 */
data class Sun(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color
)
