package dev.jamiecraane.scorchedearth

import dev.jamiecraane.scorchedearth.weather.Lightning
import dev.jamiecraane.scorchedearth.weather.WeatherManager
import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals

class LightningDamageTest {
    @Test
    fun testLightningDamage() {
        // Create a Lightning object and verify its damage value
        val lightning = Lightning(
            strikePosition = Offset(100f, 0f),
            spread = 100f
        )

        // Verify that the default damage is 10 (reduced from 15)
        assertEquals(10, lightning.damage, "Lightning damage should be 10")

        println("[DEBUG_LOG] Lightning damage test passed: damage=${lightning.damage}")
    }

    @Test
    fun testLightningDamageFromWeatherManager() {
        // Create a WeatherManager and trigger a lightning strike
        val weatherManager = WeatherManager()
        weatherManager.setGameDimensions(800f, 600f)
        weatherManager.setWeatherType(dev.jamiecraane.scorchedearth.weather.WeatherType.LIGHTNING)

        // Trigger a lightning strike
        val lightning = weatherManager.triggerLightningStrike()

        // Verify that the lightning damage is 10
        assertEquals(10, lightning.damage, "Lightning damage from WeatherManager should be 10")

        println("[DEBUG_LOG] Lightning damage from WeatherManager test passed: damage=${lightning.damage}")
    }
}
