package dev.jamiecraane.scorchedearth.inventory

/**
 * Defines the different types of projectiles available in the game.
 */
enum class ProjectileType(
    val displayName: String,
    val minDamage: Int,
    val maxDamage: Int,
    val blastRadius: Float,
    val cost: Int,
    val purchaseQuantity: Int = 1,
) {
    BABY_MISSILE("Baby Missile", 10, 30, 60f, 250, purchaseQuantity = 10),
    SMALL_MISSILE("Small Missile", 20, 40, 90f, 1875, purchaseQuantity = 5),
    BIG_MISSILE("Big Missile", 30, 60, 200f, 2500),
    DEATHS_HEAD("Death's Head", 75, 125, 400f, 5000),
    BABY_NUKE("Baby Nuke", 100, 175, 650f, 10000, purchaseQuantity = 3),
    NUCLEAR_BOMB("Nuclear Bomb", 150, 250, 1000f, 12000),
    FUNKY_BOMB("Funky Bomb", 25, 75, 150f, 7000, purchaseQuantity = 2),
    MIRV("MIRV", 35, 60, 80f, 7500, purchaseQuantity = 3),
    LEAPFROG("Leapfrog", 25, 45, 70f, 7500, purchaseQuantity = 2),
    TRACER("Tracer", 0, 0, 0f, 500, purchaseQuantity = 10),
    ROLLER("Roller", 40, 80, 150f, 4000, purchaseQuantity = 4)
}
