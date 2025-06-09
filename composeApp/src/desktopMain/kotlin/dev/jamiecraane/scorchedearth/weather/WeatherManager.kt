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

    // Lightning
    var lightning by mutableStateOf<Lightning?>(null)
    var lightningTimer = 0f
    private val lightningFrequency = 10f // Average seconds between lightning strikes

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
        } else if (type != WeatherType.LIGHTNING) {
            // Clear rain drops if weather type is not RAIN or LIGHTNING
            rainDrops = listOf()
        }

        // Reset lightning timer when changing weather type
        if (type == WeatherType.LIGHTNING) {
            lightningTimer = Random.nextFloat() * lightningFrequency
        } else {
            lightning = null
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

        if (weatherTypeState == WeatherType.LIGHTNING) {
            updateLightning(deltaTime)
        }
    }

    /**
     * Updates lightning effects.
     * @param deltaTime Time elapsed since the last update in seconds
     */
    private fun updateLightning(deltaTime: Float) {
        // Update existing lightning
        if (lightning != null) {
            val updatedLightning = lightning!!.copy(
                timeRemaining = lightning!!.timeRemaining - deltaTime
            )

            // Remove lightning if its time is up
            if (updatedLightning.timeRemaining <= 0) {
                lightning = null
            } else {
                lightning = updatedLightning
            }
        } else {
            // No active lightning, update timer for next strike
            lightningTimer -= deltaTime

            // Generate new lightning strike when timer expires
            if (lightningTimer <= 0) {
                generateLightningStrike()
                // Reset timer with some randomness
                lightningTimer = lightningFrequency * (0.8f + Random.nextFloat() * 0.4f)
            }
        }
    }

    /**
     * Generates a lightning strike at a random position.
     * @return The generated Lightning object
     */
    private fun generateLightningStrike(): Lightning {
        println("[DEBUG_LOG] generateLightningStrike called, gameWidth=$gameWidth, gameHeight=$gameHeight")

        val strikeX = Random.nextFloat() * gameWidth
        val spread = 30f + Random.nextFloat() * 50f // Random spread between 30-80px

        val newLightning = Lightning(
            strikePosition = Offset(strikeX, 0f), // Lightning strikes from the top
            spread = spread
        )

        println("[DEBUG_LOG] Created lightning at x=$strikeX with spread=$spread")

        lightning = newLightning
        println("[DEBUG_LOG] Lightning property set, current weatherType=$weatherTypeState")

        return newLightning
    }

    /**
     * Manually triggers a lightning strike.
     * @return The generated Lightning object
     */
    fun triggerLightningStrike(): Lightning {
        println("[DEBUG_LOG] triggerLightningStrike called, current weatherType=$weatherTypeState")

        // Ensure weather type is set to LIGHTNING
        if (weatherTypeState != WeatherType.LIGHTNING) {
            println("[DEBUG_LOG] Setting weather type to LIGHTNING")
            weatherTypeState = WeatherType.LIGHTNING
        }

        val result = generateLightningStrike()
        println("[DEBUG_LOG] Lightning strike triggered, lightning=${result != null}")
        return result
    }

    /**
     * Checks if a player is hit by the current lightning strike.
     * @param playerPosition The position of the player
     * @return True if the player is hit by lightning
     */
    fun isPlayerHitByLightning(playerPosition: Offset): Boolean {
        val currentLightning = lightning ?: return false

        // Check if player's x position is within the lightning strike area
        val lightningLeft = currentLightning.strikePosition.x - currentLightning.spread / 2
        val lightningRight = currentLightning.strikePosition.x + currentLightning.spread / 2

        return playerPosition.x in lightningLeft..lightningRight
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

/**
 * Data class representing a lightning strike.
 */
data class Lightning(
    val strikePosition: Offset, // Position where lightning hits the ground
    val spread: Float, // Spread of the lightning on the ground (30-80px)
    val timeRemaining: Float = 0.5f, // Time in seconds the lightning will be visible
    val damage: Int = 30 // Damage caused by lightning
)
