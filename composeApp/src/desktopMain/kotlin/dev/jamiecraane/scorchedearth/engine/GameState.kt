package dev.jamiecraane.scorchedearth.engine

/**
 * Represents the current state of the game.
 */
enum class GameState {
    WAITING_FOR_PLAYER,
    AIMING,
    PROJECTILE_IN_FLIGHT,
    GAME_OVER
}
