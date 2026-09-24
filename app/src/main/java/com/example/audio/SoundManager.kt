package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val sampleRate = 22050

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var soundEnabled: Boolean = true
    var hapticEnabled: Boolean = true

    fun playCoin() {
        vibrate(15)
        if (!soundEnabled) return
        scope.launch {
            val durationMs = 80
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            val freq1 = 988.0
            val freq2 = 1318.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val freq = if (i < numSamples / 2) freq1 else freq2
                val envelope = (1.0 - (i.toDouble() / numSamples))
                val sample = (sin(2.0 * Math.PI * freq * t) * envelope * 14000).toInt().toShort()
                buffer[i] = sample
            }
            playPcm(buffer)
        }
    }

    fun playJump() {
        vibrate(25)
        if (!soundEnabled) return
        scope.launch {
            val durationMs = 120
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                val freq = 260.0 + progress * 400.0
                val envelope = sin(Math.PI * progress)
                buffer[i] = (sin(2.0 * Math.PI * freq * t) * envelope * 12000).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playRoll() {
        vibrate(25)
        if (!soundEnabled) return
        scope.launch {
            val durationMs = 130
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                val freq = 420.0 - progress * 240.0
                val envelope = (1.0 - progress) * 0.8
                buffer[i] = (sin(2.0 * Math.PI * freq * t) * envelope * 11000).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playHoverboard() {
        vibrate(40)
        if (!soundEnabled) return
        scope.launch {
            val durationMs = 250
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val freq = 440.0 + sin(t * 30.0) * 80.0
                val envelope = (1.0 - (i.toDouble() / numSamples)) * 0.7
                buffer[i] = (sin(2.0 * Math.PI * freq * t) * envelope * 13000).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playCrash() {
        vibrate(70)
        if (!soundEnabled) return
        scope.launch {
            val durationMs = 260
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val envelope = (1.0 - progress) * (1.0 - progress)
                val noise = (Math.random() * 2.0 - 1.0)
                val lowFreq = sin(2.0 * Math.PI * 90.0 * (i.toDouble() / sampleRate))
                buffer[i] = ((noise * 0.7 + lowFreq * 0.5) * envelope * 18000).toInt().coerceIn(-32768, 32767).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playPowerUp() {
        vibrate(35)
        if (!soundEnabled) return
        scope.launch {
            val notes = listOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
            val noteDurationMs = 70
            val numSamples = (sampleRate * (notes.size * noteDurationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            val samplesPerNote = numSamples / notes.size

            for (n in notes.indices) {
                val freq = notes[n]
                for (i in 0 until samplesPerNote) {
                    val idx = n * samplesPerNote + i
                    val t = idx.toDouble() / sampleRate
                    val env = (1.0 - (i.toDouble() / samplesPerNote)) * 0.9
                    buffer[idx] = (sin(2.0 * Math.PI * freq * t) * env * 14000).toInt().toShort()
                }
            }
            playPcm(buffer)
        }
    }

    fun playUnlockSuccess() {
        vibrate(60)
        if (!soundEnabled) return
        scope.launch {
            val notes = listOf(440.0, 554.37, 659.25, 880.0, 1108.73) // A4, C#5, E5, A5, C#6
            val noteDurationMs = 90
            val numSamples = (sampleRate * (notes.size * noteDurationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            val samplesPerNote = numSamples / notes.size

            for (n in notes.indices) {
                val freq = notes[n]
                for (i in 0 until samplesPerNote) {
                    val idx = n * samplesPerNote + i
                    val t = idx.toDouble() / sampleRate
                    val env = (1.0 - (i.toDouble() / samplesPerNote * 0.7)) * 0.95
                    buffer[idx] = (sin(2.0 * Math.PI * freq * t) * env * 15000).toInt().toShort()
                }
            }
            playPcm(buffer)
        }
    }

    private fun playPcm(buffer: ShortArray) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            // Release after playback finished
            scope.launch {
                val playTimeMs = (buffer.size * 1000L) / sampleRate + 50
                kotlinx.coroutines.delay(playTimeMs)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            // Ignore fallback
        }
    }

    private fun vibrate(durationMs: Long) {
        if (!hapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }
}
