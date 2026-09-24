package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.ActivePowerUp
import com.example.game.GamePlayState
import com.example.game.PowerUpType
import kotlin.math.abs

@Composable
fun GameHud(
    state: GamePlayState,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onJump: () -> Unit,
    onRoll: () -> Unit,
    onHoverboard: () -> Unit,
    onPauseToggle: () -> Unit,
    showOnScreenButtons: Boolean = true,
    modifier: Modifier = Modifier
) {
    var dragAccumX by remember { mutableFloatStateOf(0f) }
    var dragAccumY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Combined Double-tap & Swipe gesture detector
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        onHoverboard()
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragAccumX = 0f
                        dragAccumY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumX += dragAmount.x
                        dragAccumY += dragAmount.y

                        val threshold = 35f
                        if (abs(dragAccumX) > threshold || abs(dragAccumY) > threshold) {
                            if (abs(dragAccumX) > abs(dragAccumY)) {
                                if (dragAccumX > threshold) {
                                    onMoveRight()
                                    dragAccumX = 0f
                                    dragAccumY = 0f
                                } else if (dragAccumX < -threshold) {
                                    onMoveLeft()
                                    dragAccumX = 0f
                                    dragAccumY = 0f
                                }
                            } else {
                                if (dragAccumY > threshold) {
                                    onRoll()
                                    dragAccumX = 0f
                                    dragAccumY = 0f
                                } else if (dragAccumY < -threshold) {
                                    onJump()
                                    dragAccumX = 0f
                                    dragAccumY = 0f
                                }
                            }
                        }
                    }
                )
            }
    ) {
        // TOP HUD: Score, Multiplier, Coins, Pause
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score + Multiplier Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xCC0F172A),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${state.score}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Multiplier Pill
                            val has2X = state.activePowerUps.any { it.type == PowerUpType.MULTIPLIER_2X }
                            val totalMult = (if (has2X) 2 else 1) * state.character.scoreMultiplierBonus
                            Surface(
                                color = if (totalMult > 1f) Color(0xFFFF0055) else Color(0xFF2563EB),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "x${if (totalMult % 1f == 0f) totalMult.toInt().toString() else "%.1f".format(totalMult)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Coins & Pause Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xCC0F172A),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🪙", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${state.coins}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD700)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onPauseToggle,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC0F172A))
                            .testTag("pause_btn")
                    ) {
                        Icon(
                            imageVector = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active Power-Ups & Hoverboard Countdown Bars
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                // Hoverboard bar
                if (state.isHoverboardActive) {
                    PowerUpProgressBar(
                        title = "الزلاجة (Hoverboard)",
                        remaining = state.hoverboardTimeLeft,
                        total = state.hoverboardTotalTime,
                        color = Color(0xFF00E5FF)
                    )
                }

                // Shield bar
                if (state.isShieldActive) {
                    PowerUpProgressBar(
                        title = "درع الحماية",
                        remaining = state.shieldTimeLeft,
                        total = 5.0f,
                        color = Color(0xFF10B981)
                    )
                }

                // Other powerups
                for (pu in state.activePowerUps) {
                    val (title, color) = when (pu.type) {
                        PowerUpType.MAGNET -> Pair("مغناطيس 🧲", Color(0xFFEF4444))
                        PowerUpType.JETPACK -> Pair("نفاث صاروخي 🚀", Color(0xFFF59E0B))
                        PowerUpType.SUPER_SNEAKERS -> Pair("حذاء القفز 👟", Color(0xFF8B5CF6))
                        PowerUpType.MULTIPLIER_2X -> Pair("مضاعف 2X 💎", Color(0xFF10B981))
                    }
                    PowerUpProgressBar(
                        title = title,
                        remaining = pu.remainingSeconds,
                        total = pu.totalSeconds,
                        color = color
                    )
                }
            }
        }

        // Double-Tap Hoverboard Hint & Summon Button (Bottom-Right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 100.dp)
        ) {
            Surface(
                onClick = onHoverboard,
                color = if (state.isHoverboardActive) Color(0xFF00E5FF) else Color(0xCC0F172A),
                shape = CircleShape,
                modifier = Modifier
                    .size(60.dp)
                    .border(2.dp, Color(0xFF00E5FF), CircleShape)
                    .testTag("hoverboard_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🛹", fontSize = 22.sp)
                        Text(
                            text = if (state.isHoverboardActive) "نشط" else "نقرتين",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isHoverboardActive) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        // On-Screen D-Pad / Buttons (optional & friendly for mouse/emulator testing!)
        if (showOnScreenButtons) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 20.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left & Right Lane Switch Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TouchControlButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        tag = "lane_left_btn",
                        onClick = onMoveLeft
                    )
                    TouchControlButton(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        tag = "lane_right_btn",
                        onClick = onMoveRight
                    )
                }

                // Jump & Roll Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TouchControlButton(
                        icon = Icons.Default.ArrowDownward,
                        label = "تدحرج",
                        tag = "roll_btn",
                        onClick = onRoll
                    )
                    TouchControlButton(
                        icon = Icons.Default.ArrowUpward,
                        label = "قفز",
                        tag = "jump_btn",
                        onClick = onJump
                    )
                }
            }
        }

        // Pause Menu Overlay
        AnimatedVisibility(
            visible = state.isPaused,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .border(1.dp, Color(0xFF475569), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⏸️ اللعبة متوقفة مؤقتاً",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "النقاط الحالية: ${state.score}",
                            fontSize = 15.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "العملات: ${state.coins}",
                            fontSize = 15.sp,
                            color = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Surface(
                            onClick = onPauseToggle,
                            color = Color(0xFF10B981),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("resume_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "متابعة الركض",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PowerUpProgressBar(
    title: String,
    remaining: Float,
    total: Float,
    color: Color
) {
    val progress = (remaining / total).coerceIn(0f, 1f)
    Surface(
        color = Color(0xCC0F172A),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
                Text(text = "${remaining.toInt()}s", fontSize = 10.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = Color(0x33FFFFFF)
            )
        }
    }
}

@Composable
private fun TouchControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String? = null,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color(0x880F172A),
        shape = CircleShape,
        modifier = Modifier
            .size(54.dp)
            .border(1.5.dp, Color(0x66FFFFFF), CircleShape)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                if (label != null) {
                    Text(
                        text = label,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }
    }
}
