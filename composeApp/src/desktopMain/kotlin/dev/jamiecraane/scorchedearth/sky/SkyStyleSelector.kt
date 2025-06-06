package dev.jamiecraane.scorchedearth.sky

import kotlin.random.Random

/**
 * Represents the available sky style options for selection in the intro screen.
 * Includes all the specific sky styles plus a RANDOM option.
 */
enum class SkyStyleSelector(val displayName: String) {
    SUNRISE("Sunrise"),
    AFTERNOON("Afternoon"),
    SUNSET("Sunset"),
    RANDOM("Random (Default)");

    /**
     * Converts this selector to an actual SkyStyle.
     * If RANDOM is selected, returns a random sky style.
     * @return The corresponding SkyStyle
     */
    fun toSkyStyle(): SkyStyle {
        return when (this) {
            SUNRISE -> SkyStyle.SUNRISE
            AFTERNOON -> SkyStyle.AFTERNOON
            SUNSET -> SkyStyle.SUNSET
            RANDOM -> {
                // Get a random sky style
                val styles = SkyStyle.values()
                styles[Random.nextInt(styles.size)]
            }
        }
    }

    companion object {
        /**
         * Returns the default sky style selector (RANDOM).
         */
        fun getDefault(): SkyStyleSelector = RANDOM
    }
}
