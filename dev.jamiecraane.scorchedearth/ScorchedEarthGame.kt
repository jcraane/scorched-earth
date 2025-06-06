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

            // Create a new projectile instance to trigger recomposition
            projectile = Projectile(
                position = newPosition,
                velocity = newVelocity
            )

            // Check for collision with boundaries
            if (newPosition.x < 0 || newPosition.x > gameWidth || newPosition.y > gameHeight) {
                createExplosion(newPosition)
                endProjectileFlight()
                return@let
            }

            // Check for collision with terrain
            if (isCollidingWithTerrain(newPosition)) {
                createExplosion(newPosition)
                endProjectileFlight()
                return@let
            }

            // Check for collision with players
            for ((index, player) in players.withIndex()) {
                if (isCollidingWithPlayer(newPosition, player)) {
                    // Create explosion at player's position instead of projectile position
                    createExplosion(player.position)

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
                    return@let
                }
            }
