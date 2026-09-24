package com.example.game

import com.example.audio.SoundManager
import com.example.data.CharacterCatalog
import com.example.data.CharacterDef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class GamePlayState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val score: Int = 0,
    val coins: Int = 0,
    val distance: Int = 0,
    val speed: Float = 7.0f,
    val playerLaneX: Float = 0f,
    val targetLane: Int = 0,
    val playerY: Float = 0f,
    val isJumping: Boolean = false,
    val isRolling: Boolean = false,
    val isHoverboardActive: Boolean = false,
    val hoverboardTimeLeft: Float = 0f,
    val hoverboardTotalTime: Float = 15f,
    val isShieldActive: Boolean = false,
    val shieldTimeLeft: Float = 0f,
    val activePowerUps: List<ActivePowerUp> = emptyList(),
    val obstacles: List<Obstacle> = emptyList(),
    val coinsList: List<Coin> = emptyList(),
    val powerUpsList: List<PowerUpPickup> = emptyList(),
    val particles: List<Particle> = emptyList(),
    val chaserProximity: Float = 0.15f, // 0 = far, 1 = touching
    val character: CharacterDef = CharacterCatalog.JAKE,
    val runSeconds: Float = 0f,
    val jumpsCount: Int = 0,
    val usedHoverboardThisRun: Boolean = false,
    val screenShake: Float = 0f
)

class GameEngine(
    private val soundManager: SoundManager
) {
    private val _state = MutableStateFlow(GamePlayState())
    val state: StateFlow<GamePlayState> = _state.asStateFlow()

    private var nextEntityId = 1L
    private var spawnZCounter = 80f
    private var doubleJumpAvailable = false
    private var verticalVelocity = 0f
    private val gravity = 0.72f
    private val jumpForce = 13.5f

    fun startRun(character: CharacterDef) {
        val shieldInitial = if (character.startsWithShield) 5.0f else 0f
        spawnZCounter = 80f
        nextEntityId = 1L
        verticalVelocity = 0f
        doubleJumpAvailable = character.hasDoubleJump

        _state.value = GamePlayState(
            isRunning = true,
            isPaused = false,
            isGameOver = false,
            score = 0,
            coins = 0,
            distance = 0,
            speed = 6.8f,
            playerLaneX = 0f,
            targetLane = 0,
            playerY = 0f,
            isJumping = false,
            isRolling = false,
            isHoverboardActive = false,
            hoverboardTimeLeft = 0f,
            hoverboardTotalTime = 15f * character.boardDurationBonus,
            isShieldActive = shieldInitial > 0f,
            shieldTimeLeft = shieldInitial,
            activePowerUps = emptyList(),
            obstacles = emptyList(),
            coinsList = emptyList(),
            powerUpsList = emptyList(),
            particles = emptyList(),
            chaserProximity = 0.12f,
            character = character,
            runSeconds = 0f,
            jumpsCount = 0,
            usedHoverboardThisRun = false,
            screenShake = 0f
        )

        // Seed initial track elements
        prePopulateTrack()
    }

    fun pause() {
        if (_state.value.isRunning && !_state.value.isGameOver) {
            _state.value = _state.value.copy(isPaused = true)
        }
    }

    fun resume() {
        if (_state.value.isRunning && !_state.value.isGameOver) {
            _state.value = _state.value.copy(isPaused = false)
        }
    }

    fun moveLeft() {
        val current = _state.value
        if (!current.isRunning || current.isPaused || current.isGameOver) return
        if (current.targetLane > -1) {
            val newTarget = current.targetLane - 1
            _state.value = current.copy(targetLane = newTarget)
            soundManager.playRoll() // Swift swish
        }
    }

    fun moveRight() {
        val current = _state.value
        if (!current.isRunning || current.isPaused || current.isGameOver) return
        if (current.targetLane < 1) {
            val newTarget = current.targetLane + 1
            _state.value = current.copy(targetLane = newTarget)
            soundManager.playRoll()
        }
    }

    fun jump() {
        val current = _state.value
        if (!current.isRunning || current.isPaused || current.isGameOver) return

        val hasSneakers = current.activePowerUps.any { it.type == PowerUpType.SUPER_SNEAKERS }
        val boost = if (hasSneakers) 1.35f else 1.0f

        if (!current.isJumping) {
            verticalVelocity = jumpForce * boost
            doubleJumpAvailable = current.character.hasDoubleJump
            _state.value = current.copy(
                isJumping = true,
                isRolling = false,
                jumpsCount = current.jumpsCount + 1
            )
            soundManager.playJump()
        } else if (doubleJumpAvailable) {
            // Ninja double jump!
            verticalVelocity = jumpForce * 1.1f
            doubleJumpAvailable = false
            spawnDustParticles(current.playerLaneX, current.playerY, 0xFF00E5FF)
            _state.value = current.copy(
                jumpsCount = current.jumpsCount + 1
            )
            soundManager.playJump()
        }
    }

    fun roll() {
        val current = _state.value
        if (!current.isRunning || current.isPaused || current.isGameOver) return

        if (current.isJumping) {
            // Fast-fall downward
            verticalVelocity = -18f
        }
        _state.value = current.copy(isRolling = true)
        soundManager.playRoll()
    }

    fun activateHoverboard(): Boolean {
        val current = _state.value
        if (!current.isRunning || current.isPaused || current.isGameOver) return false
        if (current.isHoverboardActive) return false

        val duration = 16f * current.character.boardDurationBonus
        _state.value = current.copy(
            isHoverboardActive = true,
            hoverboardTimeLeft = duration,
            hoverboardTotalTime = duration,
            usedHoverboardThisRun = true
        )
        soundManager.playHoverboard()
        spawnSparkles(current.playerLaneX, 0f, 0xFF00E5FF, 15)
        return true
    }

    fun update(deltaSeconds: Float) {
        val current = _state.value
        if (!current.isRunning || current.isPaused || current.isGameOver) return

        val clampedDelta = deltaSeconds.coerceIn(0.005f, 0.05f)

        // 1. Calculate speed progression
        val targetSpeed = (6.8f + (current.distance * 0.0035f)).coerceAtMost(14.5f)
        val currentSpeed = current.speed + (targetSpeed - current.speed) * 0.02f
        val distanceAdvanced = currentSpeed * clampedDelta * 10f

        // 2. Multipliers
        val has2X = current.activePowerUps.any { it.type == PowerUpType.MULTIPLIER_2X }
        val activeMultiplier = (if (has2X) 2.0f else 1.0f) * current.character.scoreMultiplierBonus
        val scoreIncrement = (distanceAdvanced * 0.5f * activeMultiplier).toInt()

        // 3. Player X Lane Interpolation
        val targetX = current.targetLane.toFloat()
        val newLaneX = current.playerLaneX + (targetX - current.playerLaneX) * 0.35f

        // 4. Player Jump & Gravity
        var newPlayerY = current.playerY
        var newIsJumping = current.isJumping
        var newIsRolling = current.isRolling

        if (current.isJumping || verticalVelocity != 0f) {
            verticalVelocity -= gravity
            newPlayerY += verticalVelocity * clampedDelta * 30f
            if (newPlayerY <= 0f) {
                newPlayerY = 0f
                newIsJumping = false
                verticalVelocity = 0f
            }
        }

        // Rolling timer decay
        if (newIsRolling) {
            if (verticalVelocity <= 0f && newPlayerY <= 0f) {
                // Keep rolling briefly
            }
        }

        // 5. Active Power-ups and Hoverboard Timers
        var hoverboardActive = current.isHoverboardActive
        var hoverboardTime = current.hoverboardTimeLeft
        if (hoverboardActive) {
            hoverboardTime -= clampedDelta
            if (hoverboardTime <= 0f) {
                hoverboardActive = false
                hoverboardTime = 0f
            }
        }

        var shieldActive = current.isShieldActive
        var shieldTime = current.shieldTimeLeft
        if (shieldActive) {
            shieldTime -= clampedDelta
            if (shieldTime <= 0f) {
                shieldActive = false
                shieldTime = 0f
            }
        }

        val updatedPowerUps = current.activePowerUps.mapNotNull { p ->
            val remaining = p.remainingSeconds - clampedDelta
            if (remaining > 0f) p.copy(remainingSeconds = remaining) else null
        }

        // 6. Move Obstacles & Check Collisions
        val hasMagnet = updatedPowerUps.any { it.type == PowerUpType.MAGNET }
        val magnetRange = 45f * current.character.magnetRadiusBonus

        val updatedObstacles = mutableListOf<Obstacle>()
        var collisionOccurred = false

        for (obs in current.obstacles) {
            val movedZ = obs.z - (currentSpeed * clampedDelta * 10f)
            if (movedZ > -20f) {
                obs.z = movedZ
                updatedObstacles.add(obs)

                // Collision box test
                // Player is at Z = 0
                if (movedZ in -2.5f..4.0f) {
                    val laneDiff = abs(newLaneX - obs.lane.toFloat())
                    if (laneDiff < 0.65f) {
                        // Check if avoided
                        val safe = when (obs.type) {
                            ObstacleType.LOW_HURDLE -> newPlayerY > 3.0f // jumped over
                            ObstacleType.HIGH_BARRIER -> newIsRolling && newPlayerY < 1.0f // rolled under
                            ObstacleType.TRAIN_STATIONARY,
                            ObstacleType.TRAIN_MOVING -> false // solid train crash
                            ObstacleType.TALL_BLOCKER -> false
                        }

                        if (!safe) {
                            collisionOccurred = true
                        }
                    }
                }
            }
        }

        // Handle collision
        var isGameOver = false
        var screenShake = max(0f, current.screenShake - clampedDelta * 3f)

        if (collisionOccurred) {
            if (hoverboardActive) {
                // Hoverboard saves player!
                hoverboardActive = false
                hoverboardTime = 0f
                shieldActive = true
                shieldTime = 2.0f // 2 seconds safety
                screenShake = 1.0f
                soundManager.playCrash()
                spawnSparkles(newLaneX, newPlayerY, 0xFFFF0055, 30)
                // Clear immediate obstacle ahead in lane so player doesn't instantly double-crash
                updatedObstacles.removeAll { abs(it.z) < 15f && abs(it.lane - current.targetLane) == 0 }
            } else if (shieldActive) {
                // Shield saves player
                shieldActive = false
                shieldTime = 0f
                screenShake = 0.8f
                soundManager.playCrash()
                spawnSparkles(newLaneX, newPlayerY, 0xFF00E5FF, 25)
                updatedObstacles.removeAll { abs(it.z) < 15f && abs(it.lane - current.targetLane) == 0 }
            } else {
                // Fatal crash!
                soundManager.playCrash()
                isGameOver = true
                screenShake = 1.5f
            }
        }

        // 7. Move & Collect Coins
        var newCoinsCount = current.coins
        val updatedCoins = mutableListOf<Coin>()

        for (coin in current.coinsList) {
            var coinZ = coin.z - (currentSpeed * clampedDelta * 10f)
            var coinLane = coin.lane.toFloat()

            // Magnet attraction
            if (hasMagnet && !coin.collected && coinZ in -5f..magnetRange) {
                // Pull coin towards player lane and z
                val pullSpeed = 22f * clampedDelta
                coinLane += (newLaneX - coinLane) * pullSpeed * 0.15f
                coinZ += (0f - coinZ) * pullSpeed * 0.15f
            }

            // Collection test
            val laneDiff = abs(newLaneX - coinLane)
            val zDiff = abs(coinZ)
            val yDiff = abs(newPlayerY - coin.y)

            if (!coin.collected && laneDiff < 0.7f && zDiff < 3.2f && yDiff < 4.0f) {
                coin.collected = true
                newCoinsCount++
                soundManager.playCoin()
                spawnSparkles(newLaneX, coin.y, 0xFFFFD700, 5)
            } else if (coinZ > -10f && !coin.collected) {
                coin.z = coinZ
                updatedCoins.add(coin)
            }
        }

        // 8. Move & Collect Power-ups
        val activePowerUpsList = updatedPowerUps.toMutableList()
        val updatedPowerUpPickups = mutableListOf<PowerUpPickup>()

        for (pu in current.powerUpsList) {
            val puZ = pu.z - (currentSpeed * clampedDelta * 10f)
            val laneDiff = abs(newLaneX - pu.lane.toFloat())
            if (!pu.collected && laneDiff < 0.7f && abs(puZ) < 3.0f) {
                pu.collected = true
                soundManager.playPowerUp()
                spawnSparkles(newLaneX, 1.5f, 0xFF00FFCC, 20)
                // Add powerup
                val duration = when (pu.type) {
                    PowerUpType.MAGNET -> 12f
                    PowerUpType.JETPACK -> 10f
                    PowerUpType.SUPER_SNEAKERS -> 14f
                    PowerUpType.MULTIPLIER_2X -> 15f
                }
                activePowerUpsList.removeAll { it.type == pu.type }
                activePowerUpsList.add(ActivePowerUp(pu.type, duration, duration))
            } else if (puZ > -10f && !pu.collected) {
                pu.z = puZ
                updatedPowerUpPickups.add(pu)
            }
        }

        // 9. Particles simulation
        val updatedParticles = current.particles.mapNotNull { p ->
            p.x += p.vx * clampedDelta * 30f
            p.y += p.vy * clampedDelta * 30f
            p.life -= p.decay
            if (p.life > 0f) p else null
        }

        // 10. Chaser inspector logic
        var chaser = current.chaserProximity
        if (collisionOccurred && !isGameOver) {
            chaser = min(0.95f, chaser + 0.35f) // Inspector lunges close on stumble!
        } else {
            chaser = max(0.12f, chaser - clampedDelta * 0.04f)
        }

        // 11. Procedural track generation ahead
        spawnZCounter -= (currentSpeed * clampedDelta * 10f)
        if (spawnZCounter < 250f) {
            spawnTrackSegment(updatedObstacles, updatedCoins, updatedPowerUpPickups)
        }

        // Rolling auto-standup after short duration
        if (newIsRolling && Random.nextFloat() < clampedDelta * 3f) {
            newIsRolling = false
        }

        _state.value = current.copy(
            isGameOver = isGameOver,
            score = current.score + scoreIncrement,
            coins = newCoinsCount,
            distance = current.distance + distanceAdvanced.toInt(),
            speed = currentSpeed,
            playerLaneX = newLaneX,
            playerY = newPlayerY,
            isJumping = newIsJumping,
            isRolling = newIsRolling,
            isHoverboardActive = hoverboardActive,
            hoverboardTimeLeft = hoverboardTime,
            isShieldActive = shieldActive,
            shieldTimeLeft = shieldTime,
            activePowerUps = activePowerUpsList,
            obstacles = updatedObstacles,
            coinsList = updatedCoins,
            powerUpsList = updatedPowerUpPickups,
            particles = updatedParticles,
            chaserProximity = chaser,
            runSeconds = current.runSeconds + clampedDelta,
            screenShake = screenShake
        )
    }

    private fun prePopulateTrack() {
        val obstacles = mutableListOf<Obstacle>()
        val coins = mutableListOf<Coin>()
        val powerups = mutableListOf<PowerUpPickup>()

        // Generate peaceful first 60 units with golden coins
        for (i in 0..12) {
            val z = 30f + i * 8f
            coins.add(Coin(nextEntityId++, lane = 0, z = z, y = 1.2f))
        }

        // Spawn a couple introductory hurdles and coin arcs
        obstacles.add(Obstacle(nextEntityId++, ObstacleType.LOW_HURDLE, lane = -1, z = 70f))
        obstacles.add(Obstacle(nextEntityId++, ObstacleType.HIGH_BARRIER, lane = 1, z = 100f))

        _state.value = _state.value.copy(
            obstacles = obstacles,
            coinsList = coins,
            powerUpsList = powerups
        )
        spawnZCounter = 120f
    }

    private fun spawnTrackSegment(
        obstacles: MutableList<Obstacle>,
        coins: MutableList<Coin>,
        powerups: MutableList<PowerUpPickup>
    ) {
        val baseZ = spawnZCounter + 60f
        spawnZCounter = baseZ + 80f

        val pattern = Random.nextInt(5)
        when (pattern) {
            0 -> {
                // Train in center lane, coins on sides
                obstacles.add(
                    Obstacle(
                        id = nextEntityId++,
                        type = ObstacleType.TRAIN_STATIONARY,
                        lane = 0,
                        z = baseZ,
                        lengthZ = 45f,
                        height = 7.5f,
                        trainColorHex = 0xFFD32F2F
                    )
                )
                for (i in 0..6) {
                    coins.add(Coin(nextEntityId++, lane = -1, z = baseZ + i * 6f))
                    coins.add(Coin(nextEntityId++, lane = 1, z = baseZ + i * 6f))
                }
            }
            1 -> {
                // Hurdle sequence with jumping coins
                val lane = listOf(-1, 0, 1).random()
                obstacles.add(Obstacle(nextEntityId++, ObstacleType.LOW_HURDLE, lane = lane, z = baseZ))
                // Arch of coins over hurdle
                coins.add(Coin(nextEntityId++, lane = lane, z = baseZ - 6f, y = 1.5f))
                coins.add(Coin(nextEntityId++, lane = lane, z = baseZ, y = 4.2f))
                coins.add(Coin(nextEntityId++, lane = lane, z = baseZ + 6f, y = 1.5f))

                // Other lane has high barrier
                val otherLane = (lane + 1) % 3 - 1
                obstacles.add(Obstacle(nextEntityId++, ObstacleType.HIGH_BARRIER, lane = otherLane, z = baseZ + 25f))
            }
            2 -> {
                // Two trains leaving one open lane
                val openLane = listOf(-1, 0, 1).random()
                for (l in listOf(-1, 0, 1)) {
                    if (l != openLane) {
                        obstacles.add(
                            Obstacle(
                                id = nextEntityId++,
                                type = ObstacleType.TRAIN_STATIONARY,
                                lane = l,
                                z = baseZ,
                                lengthZ = 40f,
                                height = 7.5f,
                                trainColorHex = 0xFF1976D2
                            )
                        )
                    } else {
                        // Open lane with golden coin string
                        for (i in 0..7) {
                            coins.add(Coin(nextEntityId++, lane = l, z = baseZ + i * 5f, y = 1.2f))
                        }
                    }
                }
            }
            3 -> {
                // Power-up pickup opportunity!
                val puLane = listOf(-1, 0, 1).random()
                val puType = PowerUpType.values().random()
                powerups.add(PowerUpPickup(nextEntityId++, puType, lane = puLane, z = baseZ + 20f))

                // High and low hurdles in alternate lanes
                for (l in listOf(-1, 0, 1)) {
                    if (l != puLane) {
                        obstacles.add(Obstacle(nextEntityId++, ObstacleType.LOW_HURDLE, lane = l, z = baseZ + 15f))
                    }
                }
            }
            4 -> {
                // Dual high barriers and coin lines
                val blockerLane = listOf(-1, 1).random()
                obstacles.add(Obstacle(nextEntityId++, ObstacleType.TALL_BLOCKER, lane = blockerLane, z = baseZ))
                for (i in 0..5) {
                    coins.add(Coin(nextEntityId++, lane = 0, z = baseZ + i * 6f, y = 1.2f))
                }
            }
        }
    }

    private fun spawnSparkles(x: Float, y: Float, colorHex: Long, count: Int) {
        val currentParticles = _state.value.particles.toMutableList()
        for (i in 0 until count) {
            currentParticles.add(
                Particle(
                    x = x + (Random.nextFloat() - 0.5f) * 0.4f,
                    y = y + (Random.nextFloat() - 0.5f) * 0.4f,
                    z = 0f,
                    vx = (Random.nextFloat() - 0.5f) * 2.5f,
                    vy = (Random.nextFloat() * 2.5f),
                    colorHex = colorHex,
                    life = 1.0f,
                    decay = 0.04f + Random.nextFloat() * 0.04f,
                    size = 4f + Random.nextFloat() * 4f
                )
            )
        }
        _state.value = _state.value.copy(particles = currentParticles)
    }

    private fun spawnDustParticles(x: Float, y: Float, colorHex: Long) {
        spawnSparkles(x, y, colorHex, 8)
    }
}
