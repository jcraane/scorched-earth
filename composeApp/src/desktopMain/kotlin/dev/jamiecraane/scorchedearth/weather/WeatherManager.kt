package dev.jamiecraane.scorchedearth.weather

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.random.Random

/**
 * Manages weather effects in the game.
 */
class WeatherManager {
    // Weather type
    var weatherTypeState by mutableStateOf(WeatherType.NONE)

    // Rain particles
    var rainDrops by mutableStateOf<List<RainDrop>>(listOf())

    // Game dimensions
    private var gameWidth = 0f
    private var gameHeight = 0f

    // Rain settings
    private val maxRainDrops = 200
    private val rainSpeed = 300f // Base speed of rain in pixels per second

    /**
     * Sets the game dimensions.
     * @param width Width of the game area
     * @param height Height of the game area
     */
    fun setGameDimensions(width: Float, height: Float) {
        gameWidth = width
        gameHeight = height

        // Initialize rain drops if weather type is RAIN
        if (weatherTypeState == WeatherType.RAIN) {
            generateRainDrops()
        }
    }

    /**
     * Sets the weather type.
     * @param type The weather type to set
     */
    fun setWeatherType(type: WeatherType) {
        weatherTypeState = type

        // Initialize rain drops if weather type is RAIN
        if (type == WeatherType.RAIN && gameWidth > 0 && gameHeight > 0) {
            generateRainDrops()
        } else {
            // Clear rain drops if weather type is not RAIN
            rainDrops = listOf()
        }
    }

    /**
     * Generates rain drops.
     */
    private fun generateRainDrops() {
        val newRainDrops = mutableListOf<RainDrop>()

        // Use a default wind value for initial angle calculation
        val defaultWind = 0f

        for (i in 0 until maxRainDrops) {
            // Calculate a default angle based on a vertical drop with slight wind influence
            val initialAngle = kotlin.math.atan2(1f, -defaultWind * 0.1f)

            newRainDrops.add(
                RainDrop(
                    position = Offset(
                        Random.nextFloat() * gameWidth,
                        Random.nextFloat() * gameHeight
                    ),
                    length = Random.nextFloat() * 20f + 10f, // Random length between 10 and 30
                    speed = rainSpeed + Random.nextFloat() * 100f, // Random speed variation
                    angle = initialAngle
                )
            )
        }

        rainDrops = newRainDrops
    }

    /**
     * Updates the weather effects.
     * @param deltaTime Time elapsed since the last update in seconds
     * @param wind Current wind value (negative = left, positive = right)
     */
    fun update(deltaTime: Float, wind: Float) {
        if (weatherTypeState == WeatherType.RAIN) {
            updateRain(deltaTime, wind)
        }
    }

    /**
     * Updates rain particles.
     * @param deltaTime Time elapsed since the last update in seconds
     * @param wind Current wind value (negative = left, positive = right)
     */
    private fun updateRain(deltaTime: Float, wind: Float) {
        val updatedRainDrops = mutableListOf<RainDrop>()

        for (drop in rainDrops) {
            // Calculate new position based on speed, wind, and gravity
            val horizontalMovement = wind * deltaTime * 2f // Wind affects horizontal movement
            val verticalMovement = drop.speed * deltaTime // Gravity affects vertical movement

            val newX = drop.position.x + horizontalMovement
            val newY = drop.position.y + verticalMovement

            // Calculate the angle based on the movement vector
            // atan2 returns the angle in radians between the positive x-axis and the point (x,y)
            // We use negative horizontalMovement because we want the tail to be behind the raindrop
            val angle = kotlin.math.atan2(verticalMovement, -horizontalMovement)

            // If rain drop is off-screen, reset it to the top with a random x position
            if (newY > gameHeight) {
                // For new raindrops at the top, calculate a default angle based on wind
                val newDropAngle = kotlin.math.atan2(1f, -wind * 0.1f) // Default angle based on wind
                updatedRainDrops.add(
                    RainDrop(
                        position = Offset(
                            Random.nextFloat() * gameWidth,
                            0f - Random.nextFloat() * 50f // Start slightly above the screen
                        ),
                        length = drop.length,
                        tailLength = drop.tailLength,
                        speed = drop.speed,
                        angle = newDropAngle
                    )
                )
            } else if (newX < 0 || newX > gameWidth) {
                // If rain drop goes off the sides, reset it to the top with a random x position
                // For new raindrops at the top, calculate a default angle based on wind
                val newDropAngle = kotlin.math.atan2(1f, -wind * 0.1f) // Default angle based on wind
                updatedRainDrops.add(
                    RainDrop(
                        position = Offset(
                            Random.nextFloat() * gameWidth,
                            0f - Random.nextFloat() * 50f // Start slightly above the screen
                        ),
                        length = drop.length,
                        tailLength = drop.tailLength,
                        speed = drop.speed,
                        angle = newDropAngle
                    )
                )
            } else {
                // Otherwise, update the position
                updatedRainDrops.add(
                    RainDrop(
                        position = Offset(newX, newY),
                        length = drop.length,
                        tailLength = drop.tailLength,
                        speed = drop.speed,
                        angle = angle
                    )
                )
            }
        }

        rainDrops = updatedRainDrops
    }
}

/**
 * Data class representing a rain drop.
 */
data class RainDrop(
    val position: Offset,
    val length: Float,
    val tailLength: Float = length * 0.5f, // Tail length as a proportion of the main drop length
    val speed: Float,
    val angle: Float = 0f // Angle in radians representing the direction of fall
)
