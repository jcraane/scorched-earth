package dev.jamiecraane.scorchedearth.engine.terrain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Manages the terrain generation, deformation, and collision detection.
 */
class TerrainManager {
    // Terrain height data for collision detection
    var terrainHeights by mutableStateOf<Map<Float, Float>>(mapOf())

    // Terrain variance (0-100)
    var terrainVarianceState by mutableStateOf(25)

    // Terrain data
    var terrain by mutableStateOf<Path>(Path())

    /**
     * Generates procedural terrain using a simple algorithm.
     * @param width Width of the game area
     * @param height Height of the game area
     * @return A Path object representing the terrain
     */
    fun generateTerrain(width: Float, height: Float): Path {
        val path = Path()
        val baseHeight = height * 0.7f
        val segments = 100
        val segmentWidth = width / segments

        // Calculate variance factors based on terrainVariance (0-100)
        // At terrainVariance = 0, we want flat terrain (sine amplitude 0, random 0)
        // At terrainVariance = 25, we want moderate terrain
        // At terrainVariance = 100, we want extremely dramatic terrain

        // Use quadratic scaling to make higher variance values create more dramatic terrain
        // This makes the effect more pronounced at higher settings
        val normalizedVariance = terrainVarianceState / 100f
        val quadraticFactor = normalizedVariance * normalizedVariance * 3f // Amplify the quadratic effect

        // Combine linear and quadratic effects for a balanced but dramatic curve
        val sineAmplitude = terrainVarianceState * 2f * (1f + quadraticFactor)  // More dramatic sine wave amplitude
        val randomFactor = terrainVarianceState * 1.2f * (1f + quadraticFactor)  // More dramatic random variations

        // Create a map to store terrain heights
        val heights = mutableMapOf<Float, Float>()

        // Start at the left edge
        val startY = if (terrainVarianceState > 0) {
            baseHeight + Random.nextFloat() * randomFactor
        } else {
            baseHeight  // Completely flat at terrainVariance = 0
        }
        path.moveTo(0f, height)
        path.lineTo(0f, startY)
        heights[0f] = startY

        // Generate terrain points
        for (i in 1..segments) {
            val x = i * segmentWidth
            val y = if (terrainVarianceState > 0) {
                baseHeight + sin(i * 0.1).toFloat() * sineAmplitude + Random.nextFloat() * randomFactor
            } else {
                baseHeight  // Completely flat at terrainVariance = 0
            }
            path.lineTo(x, y)
            heights[x] = y
        }

        // Close the path at the bottom
        path.lineTo(width, height)
        path.close()

        // Store the terrain heights for collision detection
        terrainHeights = heights
        terrain = path

        return path
    }

    /**
     * Sets the terrain variance and regenerates the terrain.
     * @param variance The new terrain variance value (0-100)
     * @param width Width of the game area
     * @param height Height of the game area
     */
    fun setTerrainVariance(variance: Int, width: Float, height: Float) {
        terrainVarianceState = variance
        terrain = generateTerrain(width, height)
    }

    /**
     * Deforms the terrain at the explosion point based on the blast radius.
     * @param position The position of the explosion
     * @param blastRadius The radius of the explosion
     * @param width Width of the game area
     * @param height Height of the game area
     */
    fun deformTerrain(position: Offset, blastRadius: Float, width: Float, height: Float) {
        // Create a copy of the terrain heights map
        val newTerrainHeights = terrainHeights.toMutableMap()

        // Get all x-coordinates in the terrain
        val terrainXCoords = terrainHeights.keys.toList().sorted()

        // Calculate the deformation for each x-coordinate within the blast radius
        for (x in terrainXCoords) {
            // Calculate horizontal distance from explosion center
            val horizontalDistance = abs(x - position.x)

            // Only deform terrain within the blast radius
            if (horizontalDistance <= blastRadius) {
                // Get current height at this x-coordinate
                val currentHeight = terrainHeights[x] ?: continue

                // Get vertical distance from explosion to terrain
                val verticalDistance = max(0f, currentHeight - position.y)

                // Calculate actual distance from explosion center to terrain point
                val actualDistance = sqrt(horizontalDistance * horizontalDistance + verticalDistance * verticalDistance)

                // Only deform if the actual distance is within the blast radius
                if (actualDistance <= blastRadius) {
                    // Calculate deformation amount based on distance from explosion center
                    // Using a parabolic falloff for more realistic crater shape: (1-(d/r)²)
                    val distanceRatio = actualDistance / blastRadius
                    val deformationFactor = 1.0f - (distanceRatio * distanceRatio)

                    // Scale the deformation by the blast radius
                    val deformationAmount = blastRadius * deformationFactor * 0.8f

                    // Apply deformation (raise the terrain value, which means digging a crater)
                    // The y-coordinate increases downward in the canvas
                    val newHeight = currentHeight + deformationAmount

                    // Update the height map
                    newTerrainHeights[x] = newHeight
                }
            }
        }

        // Update the terrain heights map
        terrainHeights = newTerrainHeights

        // Regenerate the terrain path with the new heights
        terrain = regenerateTerrainPath(width, height)
    }

    /**
     * Regenerates the terrain path using the current terrain heights.
     * @param width Width of the game area
     * @param height Height of the game area
     * @return A Path object representing the terrain
     */
    private fun regenerateTerrainPath(width: Float, height: Float): Path {
        val path = Path()

        // Get all x-coordinates in the terrain sorted
        val terrainXCoords = terrainHeights.keys.toList().sorted()

        if (terrainXCoords.isEmpty()) {
            return generateTerrain(width, height)
        }

        // Start at the left edge
        path.moveTo(0f, height)
        path.lineTo(terrainXCoords.first(), terrainHeights[terrainXCoords.first()] ?: 0f)

        // Connect all terrain points
        for (x in terrainXCoords.drop(1)) {
            path.lineTo(x, terrainHeights[x] ?: 0f)
        }

        // Close the path at the bottom
        path.lineTo(width, height)
        path.close()

        return path
    }

    /**
     * Gets the terrain height at a specific x-coordinate using interpolation.
     * @param x The x-coordinate to get the height for
     * @return The interpolated height at the given x-coordinate
     */
    fun getTerrainHeightAtX(x: Float): Float {
        // Find the closest x-coordinates in our terrain height map
        val terrainXCoords = terrainHeights.keys.toList().sorted()

        // If position is outside the terrain bounds, return a default value
        if (x < terrainXCoords.first()) {
            return terrainHeights[terrainXCoords.first()] ?: 0f
        }

        if (x > terrainXCoords.last()) {
            return terrainHeights[terrainXCoords.last()] ?: 0f
        }

        // Find the two closest x-coordinates
        val lowerX = terrainXCoords.filter { it <= x }.maxOrNull() ?: return 0f
        val upperX = terrainXCoords.filter { it >= x }.minOrNull() ?: return 0f

        // Get the heights at those coordinates
        val lowerY = terrainHeights[lowerX] ?: return 0f
        val upperY = terrainHeights[upperX] ?: return 0f

        // Interpolate to find the terrain height at the exact x-coordinate
        return if (upperX == lowerX) {
            lowerY
        } else {
            lowerY + (upperY - lowerY) * (x - lowerX) / (upperX - lowerX)
        }
    }

    /**
     * Gets the terrain height at a specific x-coordinate.
     * @param x The x-coordinate to check
     * @return The terrain height at the specified x-coordinate, or null if outside terrain bounds
     */
    fun getTerrainHeightAt(x: Float): Float? {
        // Find the closest x-coordinates in our terrain height map
        val terrainXCoords = terrainHeights.keys.toList().sorted()

        // If position is outside the terrain bounds, return null
        if (x < terrainXCoords.first() || x > terrainXCoords.last()) {
            return null
        }

        // Find the two closest x-coordinates
        val lowerX = terrainXCoords.filter { it <= x }.maxOrNull() ?: return null
        val upperX = terrainXCoords.filter { it >= x }.minOrNull() ?: return null

        // Get the heights at those coordinates
        val lowerY = terrainHeights[lowerX] ?: return null
        val upperY = terrainHeights[upperX] ?: return null

        // Interpolate to find the terrain height at the exact x-coordinate
        return if (upperX == lowerX) {
            lowerY
        } else {
            lowerY + (upperY - lowerY) * (x - lowerX) / (upperX - lowerX)
        }
    }

    /**
     * Gets the slope of the terrain at a specific position.
     * @param position The position to check
     * @return The slope value (positive for uphill, negative for downhill)
     */
    fun getTerrainSlopeAt(position: Offset): Float {
        // Find the closest x-coordinates in our terrain height map
        val terrainXCoords = terrainHeights.keys.toList().sorted()

        // If position is outside the terrain bounds, return flat slope
        if (position.x < terrainXCoords.first() || position.x > terrainXCoords.last()) {
            return 0f
        }

        // Find the two closest x-coordinates
        val lowerX = terrainXCoords.filter { it <= position.x }.maxOrNull() ?: return 0f
        val upperX = terrainXCoords.filter { it >= position.x }.minOrNull() ?: return 0f

        // If they're the same point, we can't calculate slope
        if (upperX == lowerX) {
            return 0f
        }

        // Get the heights at those coordinates
        val lowerY = terrainHeights[lowerX] ?: return 0f
        val upperY = terrainHeights[upperX] ?: return 0f

        // Calculate slope (rise over run)
        // Note: In screen coordinates, y increases downward, so we negate the result
        return (upperY - lowerY) / (upperX - lowerX)
    }

    /**
     * Determines if a position is in a valley (local minimum) based on terrain slope.
     * @param position The position to check
     * @param currentSlope The current slope at this position
     * @return True if the position is in a valley
     */
    fun isInValley(position: Offset, currentSlope: Float): Boolean {
        // A valley is where the slope changes from negative to positive
        // We can detect this by checking slopes slightly to the left and right

        // Check slope slightly to the left
        val leftPosition = Offset(position.x - 5f, position.y)
        val leftSlope = getTerrainSlopeAt(leftPosition)

        // Check slope slightly to the right
        val rightPosition = Offset(position.x + 5f, position.y)
        val rightSlope = getTerrainSlopeAt(rightPosition)

        // In a valley: left slope is negative (downhill from left), right slope is positive (uphill to right)
        return leftSlope < -0.1f && rightSlope > 0.1f
    }

    /**
     * Checks if a point is colliding with the terrain.
     * @param position The position to check
     * @return True if the position is below the terrain surface
     */
    fun isCollidingWithTerrain(position: Offset): Boolean {
        // Get the terrain height at the position's x-coordinate
        val terrainHeight = getTerrainHeightAt(position.x) ?: return false

        // Check if the position is below the terrain surface
        return position.y >= terrainHeight
    }
}
