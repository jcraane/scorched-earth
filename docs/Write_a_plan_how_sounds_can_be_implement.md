
# Sound Implementation Plan for Scorched Earth Game

## 1. Audio Format Selection

For a Kotlin Multiplatform desktop game, WAV files are the recommended format for sound effects because:

- **Universal compatibility**: WAV files work across all desktop platforms (Windows, macOS, Linux)
- **Lossless quality**: Provides high-quality sound effects without compression artifacts
- **Low latency**: WAV files can be loaded and played with minimal delay, essential for responsive game sound effects
- **Simplicity**: No decompression overhead compared to formats like MP3 or OGG

For background music, you might consider MP3 or OGG formats to save space, but for sound effects, WAV is ideal.

## 2. Audio Library Selection

For Kotlin Multiplatform desktop applications, I recommend using one of these libraries:

1. **JavaFX Media** - If you want to stay within the JVM ecosystem
2. **LWJGL with OpenAL** - For more advanced audio capabilities
3. **Korlibs Korau** - A Kotlin Multiplatform audio library that works well with Compose

The simplest approach would be to use JavaFX Media since it's already part of the JVM.

## 3. Sound Manager Implementation

Create a dedicated `SoundManager` class:

```kotlin
package dev.jamiecraane.scorchedearth.sound

import javafx.scene.media.AudioClip
import java.io.File
import java.net.URL

class SoundManager {
    private val soundCache = mutableMapOf<SoundEffect, AudioClip>()
    private var soundEnabled = true
    
    init {
        // Preload all sound effects
        SoundEffect.values().forEach { effect ->
            try {
                val resourceUrl = javaClass.getResource(effect.path)
                if (resourceUrl != null) {
                    soundCache[effect] = AudioClip(resourceUrl.toString())
                } else {
                    println("Warning: Could not load sound effect: ${effect.path}")
                }
            } catch (e: Exception) {
                println("Error loading sound effect ${effect.path}: ${e.message}")
            }
        }
    }
    
    fun playSound(effect: SoundEffect, volume: Double = 1.0) {
        if (!soundEnabled) return
        
        soundCache[effect]?.let { clip ->
            clip.volume = volume
            clip.play()
        }
    }
    
    fun toggleSound(enabled: Boolean) {
        soundEnabled = enabled
    }
    
    fun dispose() {
        soundCache.clear()
    }
}

enum class SoundEffect(val path: String) {
    FIRE("/sounds/fire.wav"),
    EXPLOSION("/sounds/explosion.wav"),
    IMPACT("/sounds/impact.wav"),
    WIND("/sounds/wind.wav"),
    PLAYER_HIT("/sounds/player_hit.wav"),
    PLAYER_DEATH("/sounds/player_death.wav"),
    MENU_SELECT("/sounds/menu_select.wav"),
    ROUND_START("/sounds/round_start.wav"),
    ROUND_END("/sounds/round_end.wav"),
    SHIELD_ACTIVATE("/sounds/shield_activate.wav"),
    SPECIAL_WEAPON("/sounds/special_weapon.wav")
}
```

## 4. Resource Management

1. Create a resources directory structure:

```
composeApp/src/desktopMain/resources/
└── sounds/
    ├── explosion.wav
    ├── fire.wav
    ├── impact.wav
    ├── menu_select.wav
    ├── player_death.wav
    ├── player_hit.wav
    ├── round_end.wav
    ├── round_start.wav
    ├── shield_activate.wav
    ├── special_weapon.wav
    └── wind.wav
```

2. Update the build.gradle.kts file to include resources:

```kotlin
kotlin {
    sourceSets {
        val desktopMain by getting {
            resources.srcDirs("src/desktopMain/resources")
        }
    }
}
```

## 5. Integration with Game Events

Integrate the SoundManager with key game events:

### In ScorchedEarthGame.kt:

```kotlin
class ScorchedEarthGame(private val numberOfPlayers: Int = 2, val totalRounds: Int = 1) {
    // Add sound manager
    private val soundManager = SoundManager()
    
    // Existing code...
    
    fun fireProjectile(angle: Float, power: Float): Boolean {
        val result = projectileManager.fireProjectile(angle, power)
        if (result) {
            soundManager.playSound(SoundEffect.FIRE)
        }
        return result
    }
    
    fun prepareNextRound() {
        // Existing code...
        soundManager.playSound(SoundEffect.ROUND_START)
    }
    
    fun transitionToNextRound() {
        // Existing code...
        soundManager.playSound(SoundEffect.ROUND_END)
    }
}
```

### In ProjectileManager.kt:

```kotlin
fun updateProjectile(deltaTime: Float): Boolean {
    // Existing code...
    
    // When projectile hits terrain or boundary
    if (collision) {
        soundManager.playSound(SoundEffect.IMPACT)
    }
    
    return projectileActive
}
```

### In ExplosionManager.kt:

```kotlin
fun createExplosion(position: Offset, proj: Projectile? = null, gameWidth: Float, gameHeight: Float) {
    // Existing code...
    
    // Play explosion sound
    soundManager.playSound(SoundEffect.EXPLOSION)
    
    // Existing code...
}

private fun applyBlastDamageToPlayers(explosionPosition: Offset, blastRadius: Float, projectile: Projectile? = null): Boolean {
    // Existing code...
    
    // When player takes damage
    if (damage > 0) {
        val result = playerManager.applyDamageToPlayer(index, damage)
        if (result) {
            soundManager.playSound(SoundEffect.PLAYER_DEATH)
        } else {
            soundManager.playSound(SoundEffect.PLAYER_HIT)
        }
    }
    
    return gameOver
}
```

## 6. Sound Settings UI

Add a sound toggle in the game settings:

```kotlin
@Composable
fun SoundSettings(soundManager: SoundManager) {
    var soundEnabled by remember { mutableStateOf(true) }
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Sound Effects")
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = soundEnabled,
            onCheckedChange = { enabled ->
                soundEnabled = enabled
                soundManager.toggleSound(enabled)
            }
        )
    }
}
```

## 7. Sound Acquisition

For sound effects, you can:
1. Create custom sounds using tools like Audacity
2. Purchase sound packs from asset stores
3. Use royalty-free sound effects from sites like Freesound.org or OpenGameArt.org

Ensure all sounds are properly licensed for your game.

## 8. Performance Considerations

- Preload all sound effects at startup to avoid delays during gameplay
- Use a sound cache to avoid reloading the same sounds
- Implement volume control for both master volume and individual sound categories
- Add distance-based volume attenuation for explosion sounds

This implementation plan provides a solid foundation for adding sound effects to your Scorched Earth game while maintaining compatibility across desktop platforms.