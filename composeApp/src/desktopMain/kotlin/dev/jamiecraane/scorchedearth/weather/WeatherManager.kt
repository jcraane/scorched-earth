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
                timeRemaining = lightning!!.timeRemaining - deltaTime,
                hitPlayers = lightning!!.hitPlayers // Preserve the set of hit players
            )

            // Remove lightning if its time is up
            if (updatedLightning.timeRemaining <= 0) {
                println("[DEBUG_LOG] Lightning expired, removing it. Hit ${updatedLightning.hitPlayers.size} players.")
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
        val spread = (30f + Random.nextFloat() * 50f) * 2f // Random spread between 60-160px (doubled from original 30-80px)

        val newLightning = Lightning(
            strikePosition = Offset(strikeX, 0f), // Lightning strikes from the top
            spread = spread
        )

        println("[DEBUG_LOG] Created lightning at x=$strikeX with spread=$spread")
        println("[DEBUG_LOG] New lightning has empty hit players set: ${newLightning.hitPlayers.isEmpty()}")

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
        println("[DEBUG_LOG] Lightning strike triggered, lightning=${result != null}, hitPlayers=${result.hitPlayers.size}")
        return result
    }

    /**
     * Checks if a player is hit by the current lightning strike.
     * @param playerPosition The position of the player
     * @param playerIndex The index of the player in the players list
     * @return True if the player is hit by lightning and hasn't been hit before
     */
    fun isPlayerHitByLightning(playerPosition: Offset, playerIndex: Int): Boolean {
        val currentLightning = lightning ?: return false

        // Check if this player has already been hit by this lightning strike
        if (currentLightning.hitPlayers.contains(playerIndex)) {
            println("[DEBUG_LOG] isPlayerHitByLightning: Player $playerIndex already hit by this lightning, skipping")
            return false
        }

        // Calculate the ground position where lightning hits
        val groundY = gameHeight * 0.7f // Approximate ground level, same as in WeatherRenderer
        val strikeGroundPosition = Offset(currentLightning.strikePosition.x, groundY)

        // Calculate distance from player to lightning strike point on ground
        val distanceToStrike = kotlin.math.sqrt(
            (playerPosition.x - strikeGroundPosition.x) * (playerPosition.x - strikeGroundPosition.x) +
            (playerPosition.y - strikeGroundPosition.y) * (playerPosition.y - strikeGroundPosition.y)
        )

        // The blast radius is half of the lightning's spread
        val blastRadius = currentLightning.spread / 2

        println("[DEBUG_LOG] isPlayerHitByLightning: playerPosition=$playerPosition, strikeGroundPosition=$strikeGroundPosition")
        println("[DEBUG_LOG] isPlayerHitByLightning: distanceToStrike=$distanceToStrike, blastRadius=$blastRadius")

        val isHit = distanceToStrike <= blastRadius
        println("[DEBUG_LOG] isPlayerHitByLightning result: $isHit")

        // If the player is hit, mark them as hit in the lightning's hitPlayers set
        if (isHit) {
            currentLightning.hitPlayers.add(playerIndex)
            println("[DEBUG_LOG] isPlayerHitByLightning: Marked player $playerIndex as hit by this lightning")
        }

        return isHit
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
    val damage: Int = 30, // Damage caused by lightning (reduced from 15)
    val hitPlayers: MutableSet<Int> = mutableSetOf() // Tracks which players have already been hit by this lightning
)
