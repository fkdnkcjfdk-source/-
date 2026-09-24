package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.CharacterDef
import com.example.game.Coin
import com.example.game.GamePlayState
import com.example.game.Obstacle
import com.example.game.ObstacleType
import com.example.game.Particle
import com.example.game.PowerUpPickup
import com.example.game.PowerUpType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameCanvas(
    state: GamePlayState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Shake offset
        val shakeX = if (state.screenShake > 0f) (sin(System.currentTimeMillis() * 0.05f) * state.screenShake * 18f) else 0f
        val shakeY = if (state.screenShake > 0f) (cos(System.currentTimeMillis() * 0.06f) * state.screenShake * 14f) else 0f

        val vanishX = w * 0.5f + shakeX
        val vanishY = h * 0.36f + shakeY
        val groundY = h * 0.88f + shakeY

        // 1. Draw Sky & Subway Tunnel Backdrop
        drawSubwayBackground(w, h, vanishX, vanishY)

        // 2. Draw 3 Perspective Tracks & Ties
        drawTracks(w, groundY, vanishX, vanishY, state.distance)

        // 3. Depth Sorting: Gather all entities (obstacles, coins, powerups) with their Z
        // Draw from far (high Z) to near (low Z)
        drawTrackEntities(w, groundY, vanishX, vanishY, state)

        // 4. Draw Chaser (Inspector & Dog) if close
        if (state.chaserProximity > 0.15f) {
            drawInspectorAndDog(w, groundY, vanishX, vanishY, state)
        }

        // 5. Draw Runner Character at Z = 0
        drawRunnerCharacter(w, groundY, vanishX, vanishY, state)

        // 6. Draw Sparks and Particles
        drawParticles(w, groundY, vanishX, vanishY, state.particles)

        // 7. Draw Speed streaks if moving fast
        if (state.speed > 9.5f) {
            drawSpeedLines(w, h, state.speed)
        }
    }
}

private fun DrawScope.drawSubwayBackground(w: Float, h: Float, vanishX: Float, vanishY: Float) {
    // Sky / Tunnel ceiling
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A), // Dark slate midnight
                Color(0xFF1E293B), // Subway tunnel blue
                Color(0xFF334155)  // Horizon haze
            ),
            startY = 0f,
            endY = vanishY
        ),
        topLeft = Offset(0f, 0f),
        size = Size(w, vanishY)
    )

    // Tunnel Arches & Distant Skyline
    val archColor = Color(0xFF1E1E2E)
    drawRect(
        color = archColor,
        topLeft = Offset(0f, vanishY - 45f),
        size = Size(w, 45f)
    )

    // Tunnel lamps glowing yellow
    val lampColor = Color(0xFFFFD54F)
    drawCircle(
        color = lampColor.copy(alpha = 0.8f),
        radius = 5f,
        center = Offset(vanishX - 60f, vanishY - 20f)
    )
    drawCircle(
        color = lampColor.copy(alpha = 0.8f),
        radius = 5f,
        center = Offset(vanishX + 60f, vanishY - 20f)
    )

    // Ground platform bed
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF263238), // Ballast dark gravel
                Color(0xFF1A1A24), // Foreground track bed
                Color(0xFF121218)
            ),
            startY = vanishY,
            endY = h
        ),
        topLeft = Offset(0f, vanishY),
        size = Size(w, h - vanishY)
    )
}

private fun DrawScope.drawTracks(
    w: Float,
    groundY: Float,
    vanishX: Float,
    vanishY: Float,
    distanceTravelled: Int
) {
    val bottomSpread = w * 0.44f // Half spread of the 3 tracks
    val leftRailBottom = vanishX - bottomSpread
    val rightRailBottom = vanishX + bottomSpread

    // Ballast track trapezoid
    val trackBed = Path().apply {
        moveTo(vanishX - 25f, vanishY)
        lineTo(vanishX + 25f, vanishY)
        lineTo(rightRailBottom + 50f, groundY + 120f)
        lineTo(leftRailBottom - 50f, groundY + 120f)
        close()
    }
    drawPath(trackBed, color = Color(0xFF1B1D28))

    // Moving Wooden Sleepers (Ties)
    val tieCount = 14
    val tieOffset = (distanceTravelled % 25) / 25f

    for (i in 0..tieCount) {
        val t = ((i + tieOffset) / tieCount).coerceIn(0.02f, 1.0f)
        // Non-linear depth distribution for perspective
        val perspectiveT = t * t
        val y = vanishY + (groundY + 80f - vanishY) * perspectiveT
        val halfW = (bottomSpread + 40f) * perspectiveT

        drawLine(
            color = Color(0xFF3E2723), // Dark timber wood
            start = Offset(vanishX - halfW, y),
            end = Offset(vanishX + halfW, y),
            strokeWidth = 3f + 8f * perspectiveT
        )
    }

    // 4 Steel Rails separating the 3 lanes
    // Lane -1 is between rail 0 and 1, Lane 0 is between 1 and 2, Lane 1 is between 2 and 3
    val railFractions = listOf(-1.0f, -0.33f, 0.33f, 1.0f)
    for (frac in railFractions) {
        val bottomX = vanishX + bottomSpread * frac
        val topX = vanishX + 18f * frac

        // Rail shadow
        drawLine(
            color = Color(0xFF000000),
            start = Offset(topX, vanishY),
            end = Offset(bottomX + 2f, groundY + 100f),
            strokeWidth = 4f
        )
        // Rail shiny metal
        drawLine(
            color = Color(0xFFB0BEC5),
            start = Offset(topX, vanishY),
            end = Offset(bottomX, groundY + 100f),
            strokeWidth = 3.5f
        )
    }
}

private fun DrawScope.drawTrackEntities(
    w: Float,
    groundY: Float,
    vanishX: Float,
    vanishY: Float,
    state: GamePlayState
) {
    // Collect all entities and sort by Z descending (farthest first)
    val obstacles = state.obstacles.filter { it.z in -10f..400f }
    val coins = state.coinsList.filter { !it.collected && it.z in -10f..400f }
    val powerups = state.powerUpsList.filter { !it.collected && it.z in -10f..400f }

    // Group all by Z
    val drawList = mutableListOf<TrackEntity>()
    obstacles.forEach { drawList.add(TrackEntity.Obs(it.z, it)) }
    coins.forEach { drawList.add(TrackEntity.Cn(it.z, it)) }
    powerups.forEach { drawList.add(TrackEntity.Pu(it.z, it)) }

    drawList.sortByDescending { it.z }

    for (item in drawList) {
        when (item) {
            is TrackEntity.Obs -> drawObstacle(item.obs, w, groundY, vanishX, vanishY)
            is TrackEntity.Cn -> drawCoin(item.coin, w, groundY, vanishX, vanishY)
            is TrackEntity.Pu -> drawPowerUpPickup(item.pu, w, groundY, vanishX, vanishY)
        }
    }
}

private sealed class TrackEntity(val z: Float) {
    class Obs(z: Float, val obs: Obstacle) : TrackEntity(z)
    class Cn(z: Float, val coin: Coin) : TrackEntity(z)
    class Pu(z: Float, val pu: PowerUpPickup) : TrackEntity(z)
}

// Convert 3D world (lane, z, y) to 2D Screen (x, y, scale)
private fun project(
    lane: Float,
    z: Float,
    y: Float,
    w: Float,
    groundY: Float,
    vanishX: Float,
    vanishY: Float
): Triple<Float, Float, Float> {
    val scale = 1.0f / (1.0f + z * 0.016f)
    val bottomSpread = w * 0.32f
    val laneX = vanishX + (lane * bottomSpread) * scale
    val screenY = vanishY + (groundY - vanishY) * scale - (y * 22f * scale)
    return Triple(laneX, screenY, scale)
}

private fun DrawScope.drawObstacle(
    obs: Obstacle,
    w: Float,
    groundY: Float,
    vanishX: Float,
    vanishY: Float
) {
    val (screenX, screenY, scale) = project(obs.lane.toFloat(), obs.z, 0f, w, groundY, vanishX, vanishY)
    if (scale <= 0.05f) return

    when (obs.type) {
        ObstacleType.LOW_HURDLE -> {
            val hurdleW = 90f * scale
            val hurdleH = 40f * scale
            val left = screenX - hurdleW / 2
            val top = screenY - hurdleH

            // Barrier legs
            drawLine(
                color = Color(0xFF424242),
                start = Offset(left + 10f * scale, screenY),
                end = Offset(left + 10f * scale, top),
                strokeWidth = 4f * scale
            )
            drawLine(
                color = Color(0xFF424242),
                start = Offset(left + hurdleW - 10f * scale, screenY),
                end = Offset(left + hurdleW - 10f * scale, top),
                strokeWidth = 4f * scale
            )

            // Red & White warning plank
            drawRect(
                color = Color(0xFFFF1744),
                topLeft = Offset(left, top),
                size = Size(hurdleW, hurdleH * 0.6f)
            )
            // Diagonal white stripes
            val stripeW = 14f * scale
            var sx = left
            var isWhite = false
            while (sx < left + hurdleW) {
                if (isWhite) {
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(sx, top),
                        size = Size(stripeW.coerceAtMost(left + hurdleW - sx), hurdleH * 0.6f)
                    )
                }
                sx += stripeW
                isWhite = !isWhite
            }
        }
        ObstacleType.HIGH_BARRIER -> {
            // Overhead tunnel gantry/sign (must slide under)
            val gantryW = 100f * scale
            val gantryH = 95f * scale
            val left = screenX - gantryW / 2
            val top = screenY - gantryH

            // Tall iron side poles
            drawLine(
                color = Color(0xFF37474F),
                start = Offset(left + 6f * scale, screenY),
                end = Offset(left + 6f * scale, top),
                strokeWidth = 5f * scale
            )
            drawLine(
                color = Color(0xFF37474F),
                start = Offset(left + gantryW - 6f * scale, screenY),
                end = Offset(left + gantryW - 6f * scale, top),
                strokeWidth = 5f * scale
            )

            // Overhead hanging signboard
            val signH = 34f * scale
            drawRect(
                color = Color(0xFFFF8F00), // Caution amber
                topLeft = Offset(left, top),
                size = Size(gantryW, signH)
            )
            // Yellow hazard stripes
            drawLine(
                color = Color(0xFF212121),
                start = Offset(left, top + signH / 2),
                end = Offset(left + gantryW, top + signH / 2),
                strokeWidth = 4f * scale
            )
        }
        ObstacleType.TRAIN_STATIONARY,
        ObstacleType.TRAIN_MOVING -> {
            val trainW = 105f * scale
            val trainH = 140f * scale
            val left = screenX - trainW / 2
            val top = screenY - trainH

            val trainColor = Color(obs.trainColorHex)
            val trainDark = Color(0xFF0D47A1)

            // Train Front Face
            drawRoundRect(
                color = trainColor,
                topLeft = Offset(left, top),
                size = Size(trainW, trainH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * scale)
            )

            // Windshield / Front Cab Window
            val windowW = trainW * 0.75f
            val windowH = trainH * 0.28f
            drawRoundRect(
                color = Color(0xFF81D4FA),
                topLeft = Offset(screenX - windowW / 2, top + 15f * scale),
                size = Size(windowW, windowH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale)
            )

            // Dual Glowing Headlights
            val lightRadius = 7f * scale
            drawCircle(
                color = Color(0xFFFFF59D),
                radius = lightRadius,
                center = Offset(left + 18f * scale, screenY - 22f * scale)
            )
            drawCircle(
                color = Color(0xFFFFF59D),
                radius = lightRadius,
                center = Offset(left + trainW - 18f * scale, screenY - 22f * scale)
            )

            // Subway Grille & Bumper
            drawRect(
                color = Color(0xFF212121),
                topLeft = Offset(left + 8f * scale, screenY - 14f * scale),
                size = Size(trainW - 16f * scale, 12f * scale)
            )
        }
        ObstacleType.TALL_BLOCKER -> {
            val blockW = 85f * scale
            val blockH = 120f * scale
            val left = screenX - blockW / 2
            val top = screenY - blockH

            drawRect(
                color = Color(0xFF455A64),
                topLeft = Offset(left, top),
                size = Size(blockW, blockH)
            )
            // Warning cross
            drawLine(
                color = Color(0xFFFFD600),
                start = Offset(left, top),
                end = Offset(left + blockW, screenY),
                strokeWidth = 5f * scale
            )
            drawLine(
                color = Color(0xFFFFD600),
                start = Offset(left + blockW, top),
                end = Offset(left, screenY),
                strokeWidth = 5f * scale
            )
        }
    }
}

private fun DrawScope.drawCoin(
    coin: Coin,
    w: Float,
    groundY: Float,
    vanishX: Float,
    vanishY: Float
) {
    val (screenX, screenY, scale) = project(coin.lane.toFloat(), coin.z, coin.y, w, groundY, vanishX, vanishY)
    if (scale <= 0.05f) return

    val radius = 14f * scale
    // Coin spin oscillation
    val spin = cos(coin.z * 0.15f + System.currentTimeMillis() * 0.008f)
    val coinW = radius * (0.35f + 0.65f * abs(spin))

    // Outer gold edge
    drawOval(
        color = Color(0xFFFFB300),
        topLeft = Offset(screenX - coinW, screenY - radius),
        size = Size(coinW * 2, radius * 2)
    )
    // Inner shiny face
    drawOval(
        color = Color(0xFFFFD700),
        topLeft = Offset(screenX - coinW * 0.75f, screenY - radius * 0.75f),
        size = Size(coinW * 1.5f, radius * 1.5f)
    )
    // Center star glint
    drawCircle(
        color = Color(0xFFFFF9C4),
        radius = radius * 0.25f,
        center = Offset(screenX, screenY)
    )
}

private fun DrawScope.drawPowerUpPickup(
    pu: PowerUpPickup,
    w: Float,
    groundY: Float,
    vanishX: Float,
    vanishY: Float
) {
    val (screenX, screenY, scale) = project(pu.lane.toFloat(), pu.z, 1.8f, w, groundY, vanishX, vanishY)
    if (scale <= 0.05f) return

    val size = 32f * scale
    val left = screenX - size / 2
    val top = screenY - size / 2

    val (bgCol, iconCol) = when (pu.type) {
        PowerUpType.MAGNET -> Pair(Color(0xFFE53935), Color.White)
        PowerUpType.JETPACK -> Pair(Color(0xFFFF9800), Color(0xFFFFEB3B))
        PowerUpType.SUPER_SNEAKERS -> Pair(Color(0xFF7C4DFF), Color.White)
        PowerUpType.MULTIPLIER_2X -> Pair(Color(0xFF00E676), Color(0xFF1B5E20))
    }

    // Glowing halo
    drawCircle(
        color = bgCol.copy(alpha = 0.4f),
        radius = size * 0.85f,
        center = Offset(screenX, screenY)
    )

    // Rounded Mystery Box / Power-up badge
    drawRoundRect(
        color = bgCol,
        topLeft = Offset(left, top),
        size = Size(size, size),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * scale)
    )

    // Power-up Symbol
    when (pu.type) {
        PowerUpType.MAGNET -> {
            // U-shape magnet
            drawArc(
                color = iconCol,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(screenX - size * 0.25f, screenY - size * 0.25f),
                size = Size(size * 0.5f, size * 0.5f),
                style = Stroke(width = 4f * scale)
            )
        }
        PowerUpType.JETPACK -> {
            // Rocket cone
            drawCircle(
                color = iconCol,
                radius = size * 0.25f,
                center = Offset(screenX, screenY)
            )
        }
        PowerUpType.SUPER_SNEAKERS -> {
            // Sneaker sole
            drawRoundRect(
                color = iconCol,
                topLeft = Offset(screenX - size * 0.3f, screenY - size * 0.15f),
                size = Size(size * 0.6f, size * 0.3f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * scale)
            )
        }
        PowerUpType.MULTIPLIER_2X -> {
            // Diamond with 2X
            drawCircle(
                color = iconCol,
                radius = size * 0.28f,
                center = Offset(screenX, screenY)
            )
        }
    }
}

private fun DrawScope.drawRunnerCharacter(
    w: Float,
    groundY: Float,
    vanishX: Float,
    vanishY: Float,
    state: GamePlayState
) {
    val bottomSpread = w * 0.32f
    val runnerX = vanishX + state.playerLaneX * bottomSpread
    val baseRunnerY = groundY - (state.playerY * 18f)

    val char = state.character
    val primaryColor = Color(char.primaryColorHex)
    val secondaryColor = Color(char.secondaryColorHex)

    // 1. Character Shadow on the Track
    val shadowRadiusW = 32f * (1.0f - (state.playerY / 25f).coerceIn(0f, 0.7f))
    val shadowRadiusH = 10f * (1.0f - (state.playerY / 25f).coerceIn(0f, 0.7f))
    drawOval(
        color = Color(0x77000000),
        topLeft = Offset(runnerX - shadowRadiusW, groundY - shadowRadiusH / 2),
        size = Size(shadowRadiusW * 2, shadowRadiusH * 2)
    )

    // 2. Hoverboard (if active)
    if (state.isHoverboardActive) {
        val boardW = 75f
        val boardH = 18f
        val boardY = baseRunnerY - 6f

        // Neon Glow around board
        drawOval(
            color = Color(0x6600E5FF),
            topLeft = Offset(runnerX - boardW * 0.6f, boardY - 6f),
            size = Size(boardW * 1.2f, boardH + 12f)
        )

        // Deck
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(Color(0xFFFF0055), Color(0xFF00E5FF), Color(0xFFFFD700))
            ),
            topLeft = Offset(runnerX - boardW / 2, boardY),
            size = Size(boardW, boardH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(9f)
        )

        // Thruster sparks
        drawCircle(
            color = Color(0xFF00E5FF),
            radius = 6f,
            center = Offset(runnerX - 22f, boardY + boardH)
        )
        drawCircle(
            color = Color(0xFF00E5FF),
            radius = 6f,
            center = Offset(runnerX + 22f, boardY + boardH)
        )
    }

    // 3. Shield Bubble Aura (if shield active)
    if (state.isShieldActive) {
        val pulse = (sin(System.currentTimeMillis() * 0.01f) * 4f)
        drawCircle(
            color = Color(0x4400E5FF),
            radius = 55f + pulse,
            center = Offset(runnerX, baseRunnerY - 45f)
        )
        drawCircle(
            color = Color(0xAA00E5FF),
            radius = 55f + pulse,
            center = Offset(runnerX, baseRunnerY - 45f),
            style = Stroke(width = 3.5f)
        )
    }

    // 4. Character Body & Limbs
    val runCycle = sin(System.currentTimeMillis() * 0.02f * state.speed)

    if (state.isRolling) {
        // Rolling tucked ball
        val rollRadius = 26f
        drawCircle(
            color = primaryColor,
            radius = rollRadius,
            center = Offset(runnerX, baseRunnerY - rollRadius)
        )
        // Cap/Head detail inside roll
        drawCircle(
            color = secondaryColor,
            radius = rollRadius * 0.5f,
            center = Offset(runnerX + runCycle * 6f, baseRunnerY - rollRadius)
        )
    } else {
        // Upright or Jumping posture
        val hipY = baseRunnerY - 32f
        val torsoY = baseRunnerY - 60f
        val headY = baseRunnerY - 80f

        // Legs (animated sprint swing or tucked jump)
        val legLeftSwing = if (state.isJumping) -8f else runCycle * 18f
        val legRightSwing = if (state.isJumping) -8f else -runCycle * 18f

        // Left Leg
        drawLine(
            color = secondaryColor, // Pants/denim
            start = Offset(runnerX - 9f, hipY),
            end = Offset(runnerX - 12f + legLeftSwing, baseRunnerY),
            strokeWidth = 9f
        )
        // Right Leg
        drawLine(
            color = secondaryColor,
            start = Offset(runnerX + 9f, hipY),
            end = Offset(runnerX + 12f + legRightSwing, baseRunnerY),
            strokeWidth = 9f
        )

        // Torso / Hoodie
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(runnerX - 16f, torsoY),
            size = Size(32f, 32f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
        )

        // Arms pumping
        val armLeftSwing = -legLeftSwing
        val armRightSwing = -legRightSwing
        drawLine(
            color = primaryColor,
            start = Offset(runnerX - 16f, torsoY + 6f),
            end = Offset(runnerX - 22f, torsoY + 22f + armLeftSwing),
            strokeWidth = 7f
        )
        drawLine(
            color = primaryColor,
            start = Offset(runnerX + 16f, torsoY + 6f),
            end = Offset(runnerX + 22f, torsoY + 22f + armRightSwing),
            strokeWidth = 7f
        )

        // Head (Skin tone)
        drawCircle(
            color = Color(0xFFFFCC80),
            radius = 14f,
            center = Offset(runnerX, headY)
        )

        // Character Signature Hat / Crown / Hair
        when (char.id) {
            "JAKE" -> {
                // Backwards baseball cap
                drawRoundRect(
                    color = Color(0xFFFF1744),
                    topLeft = Offset(runnerX - 15f, headY - 15f),
                    size = Size(30f, 15f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
                )
                // Cap visor facing backwards
                drawRect(
                    color = Color(0xFFD50000),
                    topLeft = Offset(runnerX - 19f, headY - 6f),
                    size = Size(8f, 5f)
                )
            }
            "TRICKY" -> {
                // Skater beanie & blonde hair
                drawCircle(
                    color = Color(0xFFE91E63),
                    radius = 15f,
                    center = Offset(runnerX, headY - 6f)
                )
                // Sunglasses
                drawRect(
                    color = Color(0xFF212121),
                    topLeft = Offset(runnerX - 11f, headY - 2f),
                    size = Size(22f, 7f)
                )
            }
            "FRESH" -> {
                // High-top afro & retro DJ headphones
                drawCircle(
                    color = Color(0xFF3E2723),
                    radius = 16f,
                    center = Offset(runnerX, headY - 8f)
                )
                // Headphones
                drawCircle(
                    color = Color(0xFF00E676),
                    radius = 8f,
                    center = Offset(runnerX - 15f, headY)
                )
                drawCircle(
                    color = Color(0xFF00E676),
                    radius = 8f,
                    center = Offset(runnerX + 15f, headY)
                )
            }
            "NINJA" -> {
                // Ninja cowl with yellow headband
                drawCircle(
                    color = Color(0xFF1A237E),
                    radius = 16f,
                    center = Offset(runnerX, headY)
                )
                // Headband trailing cloth
                drawRect(
                    color = Color(0xFFFFD600),
                    topLeft = Offset(runnerX - 15f, headY - 3f),
                    size = Size(30f, 6f)
                )
            }
            "CYBORG" -> {
                // Cybernetic chrome helmet with neon laser visor
                drawCircle(
                    color = Color(0xFF78909C),
                    radius = 15f,
                    center = Offset(runnerX, headY)
                )
                drawRect(
                    color = Color(0xFF00E5FF),
                    topLeft = Offset(runnerX - 12f, headY - 2f),
                    size = Size(24f, 6f)
                )
            }
            "GOLDEN_KING" -> {
                // Royal Golden Crown with jewels
                val crownPath = Path().apply {
                    moveTo(runnerX - 14f, headY - 10f)
                    lineTo(runnerX - 14f, headY - 24f)
                    lineTo(runnerX - 7f, headY - 16f)
                    lineTo(runnerX, headY - 27f)
                    lineTo(runnerX + 7f, headY - 16f)
                    lineTo(runnerX + 14f, headY - 24f)
                    lineTo(runnerX + 14f, headY - 10f)
                    close()
                }
                drawPath(crownPath, color = Color(0xFFFFD700))
                drawCircle(color = Color(0xFFFF1744), radius = 2.5f, center = Offset(runnerX, headY - 22f))
            }
        }
    }
}

private fun DrawScope.drawInspectorAndDog(
    w: Float,
    groundY: Float,
    vanishX: Float,
    vanishY: Float,
    state: GamePlayState
) {
    // Proximity 0 to 1 places inspector right behind the runner
    val bottomSpread = w * 0.32f
    val inspectorX = vanishX + (state.playerLaneX * bottomSpread) - 30f
    val inspectorY = groundY + 45f - (state.chaserProximity * 55f)

    if (inspectorY > groundY + 80f) return

    // Inspector Body (Blue Uniform)
    drawRoundRect(
        color = Color(0xFF1565C0),
        topLeft = Offset(inspectorX - 18f, inspectorY - 70f),
        size = Size(36f, 40f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
    )
    // Head with Police/Inspector Hat
    drawCircle(
        color = Color(0xFFFFCC80),
        radius = 14f,
        center = Offset(inspectorX, inspectorY - 84f)
    )
    drawRect(
        color = Color(0xFF0D47A1),
        topLeft = Offset(inspectorX - 16f, inspectorY - 96f),
        size = Size(32f, 12f)
    )
    // Gold badge on hat
    drawCircle(
        color = Color(0xFFFFD700),
        radius = 3f,
        center = Offset(inspectorX, inspectorY - 90f)
    )

    // Guard Dog (Pitbull/Bulldog) running on the right side
    val dogX = inspectorX + 45f
    val dogY = inspectorY - 10f
    drawRoundRect(
        color = Color(0xFF795548), // Brown dog body
        topLeft = Offset(dogX - 14f, dogY - 22f),
        size = Size(28f, 18f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f)
    )
    // Dog head & spiked red collar
    drawCircle(
        color = Color(0xFF5D4037),
        radius = 10f,
        center = Offset(dogX + 10f, dogY - 24f)
    )
    drawRect(
        color = Color(0xFFD50000),
        topLeft = Offset(dogX + 4f, dogY - 24f),
        size = Size(4f, 12f)
    )
}

private fun DrawScope.drawParticles(
    w: Float,
    groundY: Float,
    vanishX: Float,
    vanishY: Float,
    particles: List<Particle>
) {
    val bottomSpread = w * 0.32f
    for (p in particles) {
        val px = vanishX + p.x * bottomSpread
        val py = groundY - p.y * 18f
        drawCircle(
            color = Color(p.colorHex).copy(alpha = p.life.coerceIn(0f, 1f)),
            radius = p.size * p.life,
            center = Offset(px, py)
        )
    }
}

private fun DrawScope.drawSpeedLines(w: Float, h: Float, speed: Float) {
    val alpha = ((speed - 9.5f) / 5f).coerceIn(0.1f, 0.45f)
    val lineCol = Color.White.copy(alpha = alpha)

    for (i in 0..7) {
        val y = h * (0.2f + i * 0.1f)
        val len = 60f + (speed * 8f)
        // Left side streak
        drawLine(
            color = lineCol,
            start = Offset(10f, y),
            end = Offset(10f + len, y),
            strokeWidth = 2.5f
        )
        // Right side streak
        drawLine(
            color = lineCol,
            start = Offset(w - 10f, y),
            end = Offset(w - 10f - len, y),
            strokeWidth = 2.5f
        )
    }
}
