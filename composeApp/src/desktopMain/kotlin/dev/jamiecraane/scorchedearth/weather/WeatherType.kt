package dev.jamiecraane.scorchedearth.weather

/**
 * Enum representing different weather types in the game.
 */
enum class WeatherType(val displayName: String) {
    NONE("None"),
    RAIN("Rain"),
    LIGHTNING("Lightning");

    companion object {
        /**
         * Returns the default weather type.
         */
        fun getDefault(): WeatherType = NONE
    }
}
