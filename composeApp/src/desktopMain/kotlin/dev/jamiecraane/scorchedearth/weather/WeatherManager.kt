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

        for (i in 0 until maxRainDrops) {
            newRainDrops.add(
                RainDrop(
                    position = Offset(
                        Random.nextFloat() * gameWidth,
                        Random.nextFloat() * gameHeight
                    ),
                    length = Random.nextFloat() * 10f + 5f, // Random length between 5 and 15
                    speed = rainSpeed + Random.nextFloat() * 100f // Random speed variation
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
            val newX = drop.position.x + wind * deltaTime * 2f // Wind affects horizontal movement
            val newY = drop.position.y + drop.speed * deltaTime // Gravity affects vertical movement

            // If rain drop is off-screen, reset it to the top with a random x position
            if (newY > gameHeight) {
                updatedRainDrops.add(
                    RainDrop(
                        position = Offset(
                            Random.nextFloat() * gameWidth,
                            0f - Random.nextFloat() * 50f // Start slightly above the screen
                        ),
                        length = drop.length,
                        speed = drop.speed
                    )
                )
            } else if (newX < 0 || newX > gameWidth) {
                // If rain drop goes off the sides, reset it to the top with a random x position
                updatedRainDrops.add(
                    RainDrop(
                        position = Offset(
                            Random.nextFloat() * gameWidth,
                            0f - Random.nextFloat() * 50f // Start slightly above the screen
                        ),
                        length = drop.length,
                        speed = drop.speed
                    )
                )
            } else {
                // Otherwise, update the position
                updatedRainDrops.add(
                    RainDrop(
                        position = Offset(newX, newY),
                        length = drop.length,
                        speed = drop.speed
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
    val speed: Float
)
