package com.example.game

enum class ObstacleType {
    LOW_HURDLE,      // Jump over
    HIGH_BARRIER,    // Roll/slide under
    TRAIN_STATIONARY,// Solid train car
    TRAIN_MOVING,    // Approaching train car
    TALL_BLOCKER     // Tall barrier, must change lane
}

enum class PowerUpType {
    MAGNET,
    JETPACK,
    SUPER_SNEAKERS,
    MULTIPLIER_2X
}

data class Obstacle(
    val id: Long,
    val type: ObstacleType,
    val lane: Int, // -1, 0, 1
    var z: Float,  // Distance in front of camera
    val lengthZ: Float = 14f,
    val height: Float = 6f,
    val trainColorHex: Long = 0xFF1E88E5
)

data class Coin(
    val id: Long,
    val lane: Int,
    var z: Float,
    var y: Float = 1.2f,
    var collected: Boolean = false
)

data class PowerUpPickup(
    val id: Long,
    val type: PowerUpType,
    val lane: Int,
    var z: Float,
    var collected: Boolean = false
)

data class Particle(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var colorHex: Long,
    var life: Float = 1.0f,
    val decay: Float = 0.05f,
    val size: Float = 6f
)

data class ActivePowerUp(
    val type: PowerUpType,
    var remainingSeconds: Float,
    val totalSeconds: Float
)
