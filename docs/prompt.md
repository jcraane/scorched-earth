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

### Development Workflow

Build the game iteratively in these phases:

1. **Project Setup**

    * Initialize Kotlin Multiplatform Desktop project
    * Add Compose Multiplatform and relevant dependencies
    * Create a game window and a simple drawable canvas

2. **Game Engine Core**

    * Main game loop with update and render steps
    * Procedural terrain generation and rendering
    * Input handling for controlling angle/power
    * Wind and gravity simulation

3. **Gameplay Systems**

    * Turn-based logic and firing
    * Missile physics, collision detection, and explosion effects
    * Terrain deformation on hit
    * AI opponent with difficulty levels
    * Game win/loss logic

### Output Instructions

For each step, provide:

* Complete, runnable Kotlin code (including `main()`, imports, etc.)
* Gradle configuration for dependencies
* Comments explaining logic and design choices
* Modular and idiomatic Kotlin structure

Start by creating the Gradle project with Compose Multiplatform and showing a basic game window.
