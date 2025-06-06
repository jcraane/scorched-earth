/**
 * Updates the projectile position and checks for collisions.
 * @param deltaTime Time elapsed since the last update in seconds
 */
private fun updateProjectile(deltaTime: Float) {
    projectile?.let { proj ->
        // Update projectile position based on velocity and gravity
        val gravity = 9.8f * 30f // Scaled gravity

        val newVelocity = Offset(
            proj.velocity.x + wind * deltaTime,
            proj.velocity.y + gravity * deltaTime
        )

        val newPosition = Offset(
            proj.position.x + newVelocity.x * deltaTime,
            proj.position.y + newVelocity.y * deltaTime
        )

        // Special handling for MIRV - check if it should split at apex
        if (proj.type == ProjectileType.MIRV) {
            // Check if MIRV has reached its apex (velocity.y becomes positive, meaning it's falling)
            val wasRising = proj.velocity.y <= 0
            val nowFalling = newVelocity.y > 0

            if (wasRising && nowFalling) {
                // MIRV has reached its apex - split into sub-projectiles
                generateMIRVSubProjectiles(newPosition, proj, newVelocity)
                endProjectileFlight()
                return@let
            }
        }

        // Create a new projectile instance with trail to trigger recomposition
        val newTrail = (proj.trail + proj.position).takeLast(10) // Keep last 10 positions for trail
        projectile = Projectile(
            position = newPosition,
            velocity = newVelocity,
            type = proj.type,
            minDamage = proj.minDamage,
            maxDamage = proj.maxDamage,
            blastRadius = proj.blastRadius,
            trail = newTrail
        )

        // Check for collision with boundaries
        if (newPosition.x < 0 || newPosition.x > gameWidth || newPosition.y > gameHeight) {
            // For MIRV, don't explode on boundary collision - just remove it
            if (proj.type == ProjectileType.MIRV) {
                endProjectileFlight()
            } else {
                createExplosion(newPosition, proj)
                endProjectileFlight()
            }
            return@let
        }

        // Check for collision with terrain
        if (isCollidingWithTerrain(newPosition)) {
            // For MIRV, don't explode on terrain collision - just remove it
            if (proj.type == ProjectileType.MIRV) {
                endProjectileFlight()
            } else {
                createExplosion(newPosition, proj)
                endProjectileFlight()
            }
            return@let
        }

        // Check for collision with players
        for ((index, player) in players.withIndex()) {
            if (isCollidingWithPlayer(newPosition, player)) {
                // For MIRV, don't explode on player collision - just remove it
                if (proj.type == ProjectileType.MIRV) {
                    endProjectileFlight()
                } else {
                    // Create explosion at player's position instead of projectile position
                    createExplosion(player.position, proj)

                    // Create completely new list without the hit player to ensure recomposition
                    val newPlayersList = players.toMutableList()
                    newPlayersList.removeAt(index)
                    players = newPlayersList.toList() // Convert back to immutable list

                    // Adjust current player index if needed
                    if (players.isEmpty()) {
                        gameState = GameState.GAME_OVER
                        projectile = null
                        return@let
                    } else if (currentPlayerIndex >= players.size) {
                        currentPlayerIndex = 0
                    } else if (index <= currentPlayerIndex && currentPlayerIndex > 0) {
                        // If we removed a player that comes before the current player in the list,
                        // we need to adjust the current player index
                        currentPlayerIndex--
                    }

                    endProjectileFlight()
                }
                return@let
            }
        }
    }
}
