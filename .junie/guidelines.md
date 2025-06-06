You are a senior game developer proficient in Kotlin Multiplatform. Your task is to design and implement a modern desktop remake of the classic DOS game *Scorched Earth*, using Kotlin Multiplatform and Compose for Desktop.

### Project Goal

Recreate a simplified but functional version of *Scorched Earth* as a 2D turn-based artillery game with the following core features:

* Procedurally generated, destructible terrain
* Two-player mode: human vs human or human vs CPU
* Single weapon: missile (other weapons can be added later)
* Wind and gravity-based physics
* Turn-based cannon control (adjust angle and power)
* Projectile motion, collision detection, terrain deformation
* Basic AI with adjustable difficulty (accuracy, prediction error, learning speed)

### Platform & Tech Stack

* Kotlin Multiplatform Desktop (targeting Windows, macOS, Linux)
* Compose Multiplatform for UI/canvas rendering
* `kotlinx.coroutines` for turn management and delays
* Local input only (keyboard/mouse), no networking
* Sound/music and saving/loading features will be added later
* Prioritize correctness, simplicity, and modular code
