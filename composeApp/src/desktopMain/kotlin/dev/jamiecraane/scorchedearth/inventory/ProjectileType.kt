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
    SMALL_MISSILE("Small Missile", 20, 50, 90f, 1875, purchaseQuantity = 5),
    BIG_MISSILE("Big Missile", 30, 75, 200f, 2500),
    DEATHS_HEAD("Death's Head", 50, 100, 300f, 5000),
    BABY_NUKE("Baby Nuke", 60, 125, 650f, 10000, purchaseQuantity = 3),
    NUCLEAR_BOMB("Nuclear Bomb", 75, 150, 1000f, 12000),
    FUNKY_BOMB("Funky Bomb", 25, 60, 150f, 7000, purchaseQuantity = 2),
    MIRV("MIRV", 15, 40, 80f, 7500, purchaseQuantity = 3),
    LEAPFROG("Leapfrog", 15, 35, 70f, 7500, purchaseQuantity = 2),
    TRACER("Tracer", 0, 0, 0f, 500, purchaseQuantity = 10),
    ROLLER("Roller", 40, 80, 150f, 4000, purchaseQuantity = 4)
}
