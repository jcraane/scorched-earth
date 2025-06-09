package dev.jamiecraane.scorchedearth.terrain

import kotlin.random.Random

/**
 * Represents the available terrain style options for selection in the intro screen.
 * Includes all the specific terrain styles plus a RANDOM option.
 */
enum class TerrainStyleSelector(val displayName: String) {
    SAND("Sand"),
    GREEN("Green"),
    GREY("Grey"),
    RANDOM("Random (Default)");

    /**
     * Converts this selector to an actual TerrainStyle.
     * If RANDOM is selected, returns a random terrain style.
     * @return The corresponding TerrainStyle
     */
    fun toTerrainStyle(): TerrainStyle {
        return when (this) {
            SAND -> TerrainStyle.SAND
            GREEN -> TerrainStyle.GREEN
            GREY -> TerrainStyle.GREY
            RANDOM -> {
                // Get a random terrain style
                val styles = TerrainStyle.values()
                styles[Random.nextInt(styles.size)]
            }
        }
    }

    companion object {
        /**
         * Returns the default terrain style selector (RANDOM).
         */
        fun getDefault(): TerrainStyleSelector = RANDOM
    }
}
