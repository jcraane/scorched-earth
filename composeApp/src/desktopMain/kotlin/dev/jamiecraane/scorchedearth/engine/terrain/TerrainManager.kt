package dev.jamiecraane.scorchedearth.engine.terrain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.*
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
     * Generates procedural terrain using an algorithm selected based on variance level.
     * @param width Width of the game area
     * @param height Height of the game area
     * @return A Path object representing the terrain
     */
    fun generateTerrain(width: Float, height: Float): Path {
        // Update the stored game height for ground level calculations
        this.gameHeight = height

        // Select terrain generation method based on variance level
        return when {
            terrainVarianceState >= 75 -> generateTerrainWithFeatures(width, height)
            terrainVarianceState >= 40 -> generateTerrainWithFeatures(width, height)
            terrainVarianceState > 0 -> generateMultiOctaveTerrain(width, height)
            else -> generateFlatTerrain(width, height)  // Completely flat terrain
        }
    }

    /**
     * Generates completely flat terrain.
     * @param width Width of the game area
     * @param height Height of the game area
     * @return A Path object representing flat terrain
     */
    private fun generateFlatTerrain(width: Float, height: Float): Path {
        val path = Path()
        val baseHeight = height * 0.7f
        val heights = mutableMapOf<Float, Float>()

        // Create flat terrain with just two points
        path.moveTo(0f, height)
        path.lineTo(0f, baseHeight)
        path.lineTo(width, baseHeight)
        path.lineTo(width, height)
        path.close()

        // Store only the endpoints for flat terrain
        heights[0f] = baseHeight
        heights[width] = baseHeight

        // Store the terrain heights for collision detection
        terrainHeights = heights
        terrain = path

        return path
    }

    /**
     * Generates terrain using multi-octave noise for more natural looking hills and valleys.
     * @param width Width of the game area
     * @param height Height of the game area
     * @return A Path object representing the terrain
     */
    private fun generateMultiOctaveTerrain(width: Float, height: Float): Path {
        val path = Path()
        val baseHeight = height * 0.7f
        val segments = 150  // Increased for smoother terrain
        val segmentWidth = width / segments
        val heights = mutableMapOf<Float, Float>()
        val normalizedVariance = terrainVarianceState / 100f

        // Start at the left edge
        path.moveTo(0f, height)

        for (i in 0..segments) {
            val x = i * segmentWidth
            val normalizedX = x / width

            val y = generateMultiOctaveHeight(normalizedX, baseHeight, normalizedVariance, height)

            if (i == 0) {
                path.lineTo(x, y)
            } else {
                path.lineTo(x, y)
            }
            heights[x] = y
        }

        // Close the path
        path.lineTo(width, height)
        path.close()

        terrainHeights = heights
        terrain = path
        return path
    }

    /**
     * Generates terrain height using multi-octave noise for realistic hills and valleys.
     * @param x Normalized x position (0-1)
     * @param baseHeight Base height for the terrain
     * @param variance Normalized variance (0-1)
     * @param gameHeight Total height of the game area
     * @return The height at the given x position
     */
    private fun generateMultiOctaveHeight(x: Float, baseHeight: Float, variance: Float, gameHeight: Float): Float {
        // Multiple octaves of noise for different detail levels
        val octave1 = sin(x * 2 * PI * 1.5) * 0.6   // Large hills
        val octave2 = sin(x * 2 * PI * 4.0) * 0.25  // Medium features
        val octave3 = sin(x * 2 * PI * 8.0) * 0.1   // Small details
        val octave4 = sin(x * 2 * PI * 16.0) * 0.05 // Fine details

        // Combine octaves
        val combinedNoise = octave1 + octave2 + octave3 + octave4

        // Add some randomness for variety
        val randomNoise = (Random.nextFloat() - 0.5f) * 0.3f

        // Scale by variance - use quadratic scaling for more dramatic effect at higher settings
        val varianceSquared = variance * variance * 3f
        val amplitude = gameHeight * 0.3f * (variance + varianceSquared)
        val terrainOffset = (combinedNoise + randomNoise) * amplitude

        // Ensure terrain stays within reasonable bounds
        val minHeight = gameHeight * 0.2f
        val maxHeight = gameHeight * 0.85f

        return (baseHeight + terrainOffset).coerceIn(minHeight.toDouble(), maxHeight.toDouble()).toFloat()
    }

    /**
     * Enhanced terrain generation with deliberate valley and peak creation.
     * @param width Width of the game area
     * @param height Height of the game area
     * @return A Path object representing terrain with strategic features
     */
    private fun generateTerrainWithFeatures(width: Float, height: Float): Path {
        val path = Path()
        val baseHeight = height * 0.7f
        val segments = 200  // More segments for smoother terrain
        val segmentWidth = width / segments
        val heights = mutableMapOf<Float, Float>()

        // Generate strategic terrain features (valleys and peaks)
        val features = generateTerrainFeatures(width, height)

        path.moveTo(0f, height)

        for (i in 0..segments) {
            val x = i * segmentWidth
            val y = calculateHeightWithFeatures(x, baseHeight, features, width, height)

            if (i == 0) path.lineTo(x, y) else path.lineTo(x, y)
            heights[x] = y
        }

        path.lineTo(width, height)
        path.close()

        terrainHeights = heights
        terrain = path
        return path
    }

    /**
     * Generates strategic terrain features (valleys and peaks).
     * @param width Width of the game area
     * @param height Height of the game area
     * @return List of terrain features
     */
    private fun generateTerrainFeatures(width: Float, height: Float): List<TerrainFeature> {
        val features = mutableListOf<TerrainFeature>()
        val numFeatures = (terrainVarianceState / 15).coerceIn(2, 8) // 2-8 major features based on variance

        // Create alternating valleys and peaks spread across the terrain
        for (i in 0 until numFeatures) {
            // Distribute features evenly but with some randomness
            val x = (i + 1) * width / (numFeatures + 1) + (Random.nextFloat() - 0.5f) * width * 0.1f

            // Alternate between valleys and peaks, or randomize if preferred
            val isValley = i % 2 == 0 // Alternating pattern

            // Random intensity between 0.5 and 1.0
            val intensity = 0.5f + Random.nextFloat() * 0.5f

            features.add(TerrainFeature(x, isValley, intensity))
        }

        return features
    }

    /**
     * Calculates height based on proximity to terrain features.
     * @param x X-coordinate
     * @param baseHeight Base height for the terrain
     * @param features List of terrain features
     * @param width Width of the game area
     * @param height Height of the game area
     * @return The height at the given x position
     */
    private fun calculateHeightWithFeatures(
        x: Float,
        baseHeight: Float,
        features: List<TerrainFeature>,
        width: Float,
        height: Float
    ): Float {
        var totalHeight = baseHeight
        val variance = terrainVarianceState / 100f

        // Apply influence from each feature
        for (feature in features) {
            val distance = abs(x - feature.x)
            val influence = exp(-distance / (width * 0.15f)) // Gaussian falloff

            val featureHeight = if (feature.isValley) {
                // Valleys go down
                -variance * height * 0.35f * feature.intensity * influence
            } else {
                // Peaks go up
                variance * height * 0.4f * feature.intensity * influence
            }

            totalHeight += featureHeight
        }

        // Add some noise for natural variation
        val noise = (Random.nextFloat() - 0.5f) * variance * height * 0.05f
        totalHeight += noise

        // Add some underlying rolling hills using sine waves
        val hillEffect = sin(x / width * 10f * PI) * variance * height * 0.05f
        totalHeight += hillEffect.toFloat()

        // Keep within bounds
        return totalHeight.coerceIn(height * 0.15f, height * 0.85f)
    }

    /**
     * Generates terrain using fractal midpoint displacement for realistic, dramatic terrain.
     * This creates the most detailed and varied terrain, suitable for high variance settings.
     * @param width Width of the game area
     * @param height Height of the game area
     * @return A Path object representing the terrain
     */
    private fun generateFractalTerrain(width: Float, height: Float): Path {
        val path = Path()
        val baseHeight = height * 0.7f

        // For midpoint displacement, we need a power of 2 plus 1 points
        // Using 257 points (2^8 + 1) for high detail terrain
        val segments = 256
        val heightMap = FloatArray(segments + 1)
        val segmentWidth = width / segments

        // Initialize endpoints
        heightMap[0] = baseHeight
        heightMap[segments] = baseHeight

        // Calculate roughness based on terrain variance
        // Higher variance = rougher terrain
        val normalizedVariance = terrainVarianceState / 100f
        var roughness = normalizedVariance * height * 0.003f

        // Midpoint displacement algorithm
        var step = segments
        while (step > 1) {
            val halfStep = step / 2

            // For each segment, calculate midpoints
            for (i in halfStep until segments step step) {
                // Calculate midpoint by averaging endpoints
                val left = heightMap[i - halfStep]
                val right = heightMap[i + halfStep]
                val midpoint = (left + right) / 2f

                // Add random displacement proportional to segment length
                val displacement = (Random.nextFloat() - 0.5f) * roughness * 2
                heightMap[i] = midpoint + displacement
            }

            // Reduce roughness for the next iteration (produces more natural terrain)
            roughness *= 0.6f
            step = halfStep
        }

        // Apply post-processing to add interesting features
        for (i in 0..segments) {
            // Add some higher frequency noise for small details
            val detailNoise = sin(i * 0.2f) * normalizedVariance * height * 0.05f
            heightMap[i] += detailNoise

            // Add some dramatic peaks at random locations
            if (Random.nextFloat() < 0.02f * normalizedVariance) {
                // Create a localized peak or valley
                val peakHeight = normalizedVariance * height * 0.2f * (Random.nextFloat() - 0.3f)

                // Apply peak with falloff to nearby points
                for (j in max(0, i - 5)..min(segments, i + 5)) {
                    val distance = abs(i - j) / 5f
                    val influence = 1.0f - distance
                    if (influence > 0) {
                        heightMap[j] += peakHeight * influence * influence
                    }
                }
            }

            // Ensure heights stay within reasonable bounds
            heightMap[i] = heightMap[i].coerceIn(height * 0.15f, height * 0.85f)
        }

        // Convert heightMap to path and height map
        val heights = mutableMapOf<Float, Float>()

        path.moveTo(0f, height)

        for (i in 0..segments) {
            val x = i * segmentWidth
            val y = heightMap[i]

            if (i == 0) path.lineTo(x, y) else path.lineTo(x, y)
            heights[x] = y
        }

        // Close the path
        path.lineTo(width, height)
        path.close()

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
        // Update the stored game height for ground level calculations
        this.gameHeight = height

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
        // Update the stored game height for ground level calculations
        this.gameHeight = height

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

        // If there's no terrain at all, return the bottom of the screen (y=gameHeight)
        if (terrainXCoords.isEmpty()) {
            return gameHeight
        }

        // If position is outside the terrain bounds, return a default value
        if (x < terrainXCoords.first()) {
            return terrainHeights[terrainXCoords.first()] ?: gameHeight
        }

        if (x > terrainXCoords.last()) {
            return terrainHeights[terrainXCoords.last()] ?: gameHeight
        }

        // Find the two closest x-coordinates
        val lowerX = terrainXCoords.filter { it <= x }.maxOrNull() ?: return gameHeight
        val upperX = terrainXCoords.filter { it >= x }.minOrNull() ?: return gameHeight

        // Get the heights at those coordinates
        val lowerY = terrainHeights[lowerX] ?: return gameHeight
        val upperY = terrainHeights[upperX] ?: return gameHeight

        // Interpolate to find the terrain height at the exact x-coordinate
        return if (upperX == lowerX) {
            lowerY
        } else {
            lowerY + (upperY - lowerY) * (x - lowerX) / (upperX - lowerX)
        }
    }

    // Store game height for ground level calculations
    private var gameHeight: Float = 1200f

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

    /**
     * Represents a terrain feature (peak or valley) used for enhanced terrain generation
     */
    private data class TerrainFeature(
        val x: Float,          // X position of the feature
        val isValley: Boolean, // True if valley, false if peak
        val intensity: Float   // 0.0-1.0 intensity factor
    )
}
