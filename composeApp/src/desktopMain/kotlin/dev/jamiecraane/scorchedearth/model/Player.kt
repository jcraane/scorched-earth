package dev.jamiecraane.scorchedearth.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import dev.jamiecraane.scorchedearth.engine.ProjectileType
import dev.jamiecraane.scorchedearth.inventory.Inventory

/**
 * Defines the type of player (human or CPU).
 */
enum class PlayerType {
    HUMAN,
    CPU
}

/**
 * Represents a player in the game.
 */
data class Player(
    val position: Offset,
    val color: Color,
    var name: String = "",
    var health: Int = 100,
    var angle: Float = 0f,
    var power: Float = 50f,
    var selectedProjectileType: ProjectileType = ProjectileType.BABY_MISSILE,
    var money: Int = 1000,
    val inventory: Inventory = Inventory(),
    var type: PlayerType = PlayerType.HUMAN
)
