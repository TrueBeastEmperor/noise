package com.chrononoise.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

class SoundEngine {

    companion object {
        const val SAMPLE_RATE = 44100
        private const val CHUNK_SIZE = 1024
    }

    @Volatile var isPlaying: Boolean = false
        private set

    @Volatile var bpm: Int = 60
    @Volatile var clockType: ClockSoundType = ClockSoundType.ETERA_TICK_TOCK
    @Volatile var noiseType: NoiseType = NoiseType.OFF

    @Volatile var clockVolume: Float = 0.75f
    @Volatile var noiseVolume: Float = 0.30f

    @Volatile var pitchMultiplier: Float = 1.0f
    @Volatile var decayMultiplier: Float = 1.0f
    @Volatile var filterCutoffHz: Float = 12000f

    @Volatile var beatsPerBar: Int = 4
    @Volatile var muteClock: Boolean = false
    @Volatile var muteAmbiance: Boolean = false

    // Multi-Stem Background Layering
    @Volatile var backgroundNoiseType: NoiseType = NoiseType.OFF
    @Volatile var backgroundNoiseVolume: Float = 0.25f

    // Audio Focus Ducking Multiplier (1.0 = normal, 0.2 = ducked for notifications/calls)
    @Volatile var duckingMultiplier: Float = 1.0f

    // Explicit convenience toggles
    var isClockEnabled: Boolean
        get() = !muteClock && clockType != ClockSoundType.OFF
        set(value) {
            muteClock = !value
        }

    var isAmbianceEnabled: Boolean
        get() = !muteAmbiance && noiseType != NoiseType.OFF
        set(value) {
            muteAmbiance = !value
        }

    // Humanize / timing jitter (0.0 to 1.0)
    @Volatile var humanizeJitter: Float = 0.0f

    // Dynamic Morphing duration: 15 seconds to 86,400 seconds (24 hours)
    @Volatile var morphDurationSeconds: Long = 60L // default 1 minute

    // Decoupled Atomic Beat Playhead (Read by UI without blocking Audio Thread)
    val currentBeatAtomic = AtomicInteger(1)
    val isAccentAtomic = AtomicBoolean(true)
    val beatSequenceCounter = AtomicLong(0L)

    // Legacy callback kept for backwards compatibility (invoked asynchronously if needed)
    var onBeatListener: ((beatNumber: Int, isAccented: Boolean) -> Unit)? = null

    // Sleep & Focus Timer with Smooth Exponential Fade-Out
    @Volatile var sleepTimerTotalSeconds: Long = 0L
    val sleepTimerRemainingSeconds = AtomicLong(0L)
    @Volatile var onTimerExpired: (() -> Unit)? = null

    private var audioTrack: AudioTrack? = null
    private var audioThread: Thread? = null
    private val shouldRun = AtomicBoolean(false)

    // Fast Register-Based 64-bit XorShift PRNG (Zero lock contention, zero allocations)
    private var rngState = 0x853c49e6748fea9bL

    private inline fun fastWhiteNoise(): Double {
        rngState = rngState xor (rngState shl 13)
        rngState = rngState xor (rngState ushr 7)
        rngState = rngState xor (rngState shl 17)
        return ((rngState and 0xFFFFFFFFL).toDouble() / 2147483648.0) - 1.0
    }

    private inline fun fastRandomCheck(threshold: Double): Boolean {
        rngState = rngState xor (rngState shl 13)
        rngState = rngState xor (rngState ushr 7)
        rngState = rngState xor (rngState shl 17)
        return ((rngState and 0x7FFFFFFFL).toDouble() / 2147483647.0) < threshold
    }

    // Fast Pade/Rational Soft-Clipping Saturation Curve (Replaces heavy per-sample tanh)
    private inline fun fastSoftClip(x: Double): Double {
        val absX = if (x < 0.0) -x else x
        return x / (1.0 + absX)
    }

    // Stereo Decorrelated Pink Noise States (L & R channels)
    private var pB0_L = 0.0; private var pB1_L = 0.0; private var pB2_L = 0.0
    private var pB3_L = 0.0; private var pB4_L = 0.0; private var pB5_L = 0.0; private var pB6_L = 0.0

    private var pB0_R = 0.0; private var pB1_R = 0.0; private var pB2_R = 0.0
    private var pB3_R = 0.0; private var pB4_R = 0.0; private var pB5_R = 0.0; private var pB6_R = 0.0

    // Stereo Brown Noise Integrator States
    private var lastBrownL = 0.0
    private var lastBrownR = 0.0

    // Master Stereo Low-Pass Filter States
    private var lpfY_L = 0.0
    private var lpfY_R = 0.0

    // Nature & Indoor State Variables
    private var globalSampleCounter: Long = 0L
    private var campfirePopCountdown = 0
    private var campfirePopSample = 0.0
    private var campfirePopPanL = 0.707
    private var campfirePopPanR = 0.707
    private var windLpf = 0.0
    private var thunderRumbleEnvelope = 0.0
    private var thunderCountdown = 0
    private var lastWhite1 = 0.0

    // Cricket Multi-Oscillator State (Trail-Sense inspired)
    private var cricketPhase1 = 0.0
    private var cricketPhase2 = 0.0
    private var cricketPhase3 = 0.0
    private var cricketPhase4 = 0.0
    private var meadowB0 = 0.0
    private var meadowB1 = 0.0

    // Keyboard Typist & Spatial Panning Simulation State
    private var keyTypistDelaySamples = 0
    private var keyActiveSamples = 0
    private var keyElapsedSamples = 0
    private var keyIsSpacebar = false
    private var keyRandomAmp = 1.0
    private var keyPanL = 0.707
    private var keyPanR = 0.707

    // Indoor Sound States
    private var bookFlipCountdown = 0
    private var bookFlipProgress = 0
    private var coffeeCupChime = 0.0
    private var coffeeCupCountdown = 0

    // Organic Typewriter State
    private var typewriterStrikeCount = 0
    private var typewriterMaxLineLength = 14
    private var typewriterBellOffset = -1L
    private var typewriterCarriageOffset = -1L
    private var typewriterCurrentKeyPitch = 1400.0

    // Dynamic Morphing Biome State Weights
    private var biomeTargetRain = 0.5
    private var biomeTargetCrickets = 0.5
    private var biomeTargetWind = 0.0
    private var biomeTargetCampfire = 0.0
    private var biomeTargetBook = 0.0
    private var biomeCurRain = 0.5
    private var biomeCurCrickets = 0.5
    private var biomeCurWind = 0.0
    private var biomeCurCampfire = 0.0
    private var biomeCurBook = 0.0
    private var biomeTransitionCounter = 0L

    // Temporary Stereo Output Holders for Zero-Allocation Sample Processing
    private var outAmbL = 0.0
    private var outAmbR = 0.0

    fun start() {
        if (isPlaying) return

        val minBufSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufSize, CHUNK_SIZE * 4 * 2)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        track.play()

        shouldRun.set(true)
        isPlaying = true

        audioThread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            runAudioLoop()
        }.apply {
            name = "ChronoNoiseStereoAudioThread"
            start()
        }
    }

    fun stop() {
        if (!isPlaying) return
        shouldRun.set(false)
        try {
            audioThread?.join(500)
        } catch (_: Exception) {}
        audioThread = null

        try {
            audioTrack?.apply {
                pause()
                flush()
                stop()
                release()
            }
        } catch (_: Exception) {}
        audioTrack = null
        isPlaying = false
    }

    fun startSleepTimer(minutes: Int) {
        if (minutes <= 0) {
            cancelSleepTimer()
            return
        }
        val totalSec = minutes * 60L
        sleepTimerTotalSeconds = totalSec
        sleepTimerRemainingSeconds.set(totalSec)
    }

    fun cancelSleepTimer() {
        sleepTimerTotalSeconds = 0L
        sleepTimerRemainingSeconds.set(0L)
    }

    private fun runAudioLoop() {
        // Interleaved Stereo Buffer: [L0, R0, L1, R1, ..., L(CHUNK-1), R(CHUNK-1)]
        val shortBuffer = ShortArray(CHUNK_SIZE * 2)
        var sampleIndexInBeat = 0L
        var currentBeat = 0
        var currentBeatDurationSamples = (SAMPLE_RATE * 60.0 / bpm.coerceIn(30, 300)).toLong()
        var timerSampleAccumulator = 0

        while (shouldRun.get()) {
            val baseBpm = bpm.coerceIn(30, 300)
            val nominalSamplesPerBeat = (SAMPLE_RATE * 60.0 / baseBpm).toLong()

            val currentClockType = clockType
            val currentNoiseType = noiseType
            val currentBgType = backgroundNoiseType

            val cVol = if (muteClock || currentClockType == ClockSoundType.OFF) 0f else clockVolume.coerceIn(0f, 1f)
            val nVol = if (muteAmbiance || currentNoiseType == NoiseType.OFF) 0f else noiseVolume.coerceIn(0f, 1f)
            val bgVol = if (muteAmbiance || currentBgType == NoiseType.OFF) 0f else backgroundNoiseVolume.coerceIn(0f, 1f)

            val pitch = pitchMultiplier.coerceIn(0.5f, 2.5f)
            val decay = decayMultiplier.coerceIn(0.2f, 3.0f)
            val cutoff = filterCutoffHz.coerceIn(200f, 20000f)
            val jitter = humanizeJitter.coerceIn(0f, 1f)

            // Low-pass filter smoothing coefficient
            val rc = 1.0 / (2.0 * PI * cutoff)
            val dt = 1.0 / SAMPLE_RATE
            val alpha = dt / (rc + dt)

            // Handle Sleep Timer countdown and exponential fade-out
            if (sleepTimerTotalSeconds > 0L) {
                timerSampleAccumulator += CHUNK_SIZE
                if (timerSampleAccumulator >= SAMPLE_RATE) {
                    timerSampleAccumulator -= SAMPLE_RATE
                    val remaining = sleepTimerRemainingSeconds.decrementAndGet()
                    if (remaining <= 0L) {
                        sleepTimerTotalSeconds = 0L
                        sleepTimerRemainingSeconds.set(0L)
                        shouldRun.set(false)
                        onTimerExpired?.invoke()
                    }
                }
            }

            // Smooth exponential volume fade-out over final 60 seconds
            val fadeMultiplier = if (sleepTimerTotalSeconds > 0L) {
                val rem = sleepTimerRemainingSeconds.get()
                if (rem in 1..60) {
                    (rem.toDouble() / 60.0).pow(1.5).coerceIn(0.0, 1.0)
                } else if (rem <= 0L) 0.0 else 1.0
            } else 1.0

            val masterGain = fadeMultiplier * duckingMultiplier.coerceIn(0f, 1f).toDouble()

            for (i in 0 until CHUNK_SIZE) {
                globalSampleCounter++

                // Check beat trigger
                if (sampleIndexInBeat >= currentBeatDurationSamples) {
                    sampleIndexInBeat = 0
                    currentBeat = (currentBeat + 1) % beatsPerBar
                    val beatNum = currentBeat + 1
                    val isAccent = currentBeat == 0

                    // Apply humanized micro-timing jitter
                    if (jitter > 0.01f) {
                        val jitterFactor = 1.0 + fastWhiteNoise() * (0.12 * jitter)
                        currentBeatDurationSamples = (nominalSamplesPerBeat * jitterFactor).toLong().coerceAtLeast(SAMPLE_RATE / 20L)
                    } else {
                        currentBeatDurationSamples = nominalSamplesPerBeat
                    }

                    // Prepare organic typewriter randomized pitch & carriage return logic
                    if (currentClockType == ClockSoundType.ORGANIC_TYPEWRITER) {
                        typewriterStrikeCount++
                        typewriterCurrentKeyPitch = 1200.0 + ((fastWhiteNoise() + 1.0) * 0.5 * 450.0)
                        if (typewriterStrikeCount >= typewriterMaxLineLength) {
                            typewriterStrikeCount = 0
                            typewriterMaxLineLength = 12 + ((fastWhiteNoise() + 1.0) * 0.5 * 7).toInt()
                            typewriterBellOffset = sampleIndexInBeat
                            typewriterCarriageOffset = sampleIndexInBeat + (SAMPLE_RATE * 0.08).toLong()
                        }
                    }

                    // Decoupled atomic update: zero allocations, zero cross-thread blocking
                    currentBeatAtomic.set(beatNum)
                    isAccentAtomic.set(isAccent)
                    beatSequenceCounter.incrementAndGet()
                }

                // Update morphing biome weights smoothly
                if (currentNoiseType == NoiseType.LIVING_BIOME_DYNAMIC || currentBgType == NoiseType.LIVING_BIOME_DYNAMIC) {
                    updateDynamicBiomeWeights()
                }

                // 1. Clock / Metronome Sample (Centered acoustic placement)
                var cL = 0.0
                var cR = 0.0
                if (cVol > 0.001f) {
                    val c = generateClockSample(
                        currentClockType,
                        sampleIndexInBeat,
                        currentBeat,
                        pitch,
                        decay
                    ) * cVol
                    cL = c
                    cR = c
                }

                // 2. Primary Ambient / Nature / Keyboard Sample (Stereo Spatialized)
                var aL = 0.0
                var aR = 0.0
                if (nVol > 0.001f) {
                    generateAmbientStereo(currentNoiseType, pitch)
                    aL = outAmbL * nVol
                    aR = outAmbR * nVol
                }

                // 3. Layered Background Bed (Multi-Stem Stereo)
                var bL = 0.0
                var bR = 0.0
                if (bgVol > 0.001f && currentBgType != NoiseType.OFF) {
                    generateAmbientStereo(currentBgType, pitch)
                    bL = outAmbL * bgVol
                    bR = outAmbR * bgVol
                }

                // 4. Mix Master Stereo with Timer Fade and Focus Ducking
                val mixedL = (cL + aL + bL) * masterGain
                val mixedR = (cR + aR + bR) * masterGain

                // 5. Apply Stereo Low-Pass Filter
                lpfY_L += alpha * (mixedL - lpfY_L)
                lpfY_R += alpha * (mixedR - lpfY_R)

                // 6. Fast Algebraic Rational Soft-Clipping (Zero transcendentals)
                shortBuffer[2 * i] = (fastSoftClip(lpfY_L) * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
                shortBuffer[2 * i + 1] = (fastSoftClip(lpfY_R) * 32767.0).toInt().coerceIn(-32768, 32767).toShort()

                sampleIndexInBeat++
            }

            audioTrack?.write(shortBuffer, 0, CHUNK_SIZE * 2)
        }
    }

    private fun updateDynamicBiomeWeights() {
        biomeTransitionCounter++
        val transitionIntervalSamples = (SAMPLE_RATE * morphDurationSeconds).coerceAtLeast(SAMPLE_RATE * 15L)

        if (biomeTransitionCounter >= transitionIntervalSamples) {
            biomeTransitionCounter = 0
            val scene = ((fastWhiteNoise() + 1.0) * 0.5 * 5).toInt()
            biomeTargetRain = if (scene == 0 || scene == 1) 0.6 else 0.05
            biomeTargetCrickets = if (scene == 1 || scene == 2) 0.7 else 0.05
            biomeTargetWind = if (scene == 2 || scene == 3) 0.6 else 0.0
            biomeTargetCampfire = if (scene == 3 || scene == 4) 0.65 else 0.0
            biomeTargetBook = if (scene == 4) 0.55 else 0.0
        }

        val step = 0.000004
        biomeCurRain += (biomeTargetRain - biomeCurRain) * step
        biomeCurCrickets += (biomeTargetCrickets - biomeCurCrickets) * step
        biomeCurWind += (biomeTargetWind - biomeCurWind) * step
        biomeCurCampfire += (biomeTargetCampfire - biomeCurCampfire) * step
        biomeCurBook += (biomeTargetBook - biomeCurBook) * step
    }

    private fun generateClockSample(
        type: ClockSoundType,
        sampleOffset: Long,
        beatNumber: Int,
        pitchMult: Float,
        decayMult: Float
    ): Double {
        val t = sampleOffset.toDouble() / SAMPLE_RATE

        return when (type) {
            ClockSoundType.OFF -> 0.0

            ClockSoundType.ETERA_TICK_TOCK -> {
                val isTick = (beatNumber % 2 == 0)
                val freq = (if (isTick) 1050.0 else 750.0) * pitchMult
                val decayConstant = (if (isTick) 0.015 else 0.020) * decayMult
                val maxLen = (SAMPLE_RATE * 0.05 * decayMult).toLong()

                if (sampleOffset < maxLen) {
                    val envelope = exp(-t / decayConstant)
                    0.40 * sin(2.0 * PI * freq * t) * envelope
                } else 0.0
            }

            ClockSoundType.ORGANIC_TYPEWRITER -> {
                val maxLen = (SAMPLE_RATE * 0.040 * decayMult).toLong()
                var sample = 0.0

                if (sampleOffset < maxLen) {
                    val white = fastWhiteNoise()
                    val strike = sin(2.0 * PI * (typewriterCurrentKeyPitch * pitchMult) * t) * exp(-t / (0.005 * decayMult))
                    val clatter = white * 0.25 * exp(-t / (0.009 * decayMult))
                    sample += 0.35 * (strike + clatter)
                }

                if (typewriterBellOffset in 0..sampleOffset) {
                    val tBell = (sampleOffset - typewriterBellOffset).toDouble() / SAMPLE_RATE
                    val bellLen = (SAMPLE_RATE * 0.12).toLong()
                    if (sampleOffset - typewriterBellOffset < bellLen) {
                        sample += 0.28 * sin(2.0 * PI * 2800.0 * tBell) * exp(-tBell / 0.035)
                    }
                }

                if (typewriterCarriageOffset in 0..sampleOffset) {
                    val tCarriage = (sampleOffset - typewriterCarriageOffset).toDouble() / SAMPLE_RATE
                    val carLen = (SAMPLE_RATE * 0.08).toLong()
                    if (sampleOffset - typewriterCarriageOffset < carLen) {
                        val white = fastWhiteNoise()
                        sample += 0.18 * white * sin(2.0 * PI * 900.0 * tCarriage) * exp(-tCarriage / 0.02)
                    }
                }
                sample
            }

            ClockSoundType.BOOK_PAGE_METRONOME -> {
                val maxLen = (SAMPLE_RATE * 0.045 * decayMult).toLong()
                if (sampleOffset < maxLen) {
                    val white = fastWhiteNoise()
                    val paperFriction = white * sin(2.0 * PI * (1200.0 * pitchMult) * t) * exp(-t / (0.012 * decayMult))
                    val flutter = 0.3 * sin(2.0 * PI * (420.0 * pitchMult) * t) * exp(-t / (0.025 * decayMult))
                    0.38 * (paperFriction + flutter)
                } else 0.0
            }

            ClockSoundType.GRANDFATHER_CLOCK -> {
                val isTick = (beatNumber % 2 == 0)
                val baseFreq = (if (isTick) 320.0 else 240.0) * pitchMult
                val maxLen = (SAMPLE_RATE * 0.055 * decayMult).toLong()
                if (sampleOffset < maxLen) {
                    val decayConstant = (if (isTick) 0.012 else 0.016) * decayMult
                    val fundamental = sin(2.0 * PI * baseFreq * t)
                    val secondHarmonic = 0.5 * sin(2.0 * PI * (baseFreq * 2.2) * t)
                    val woodClick = 0.3 * sin(2.0 * PI * (baseFreq * 4.8) * t)
                    0.28 * (fundamental + secondHarmonic + woodClick) * exp(-t / decayConstant)
                } else 0.0
            }

            ClockSoundType.MECHANICAL_WATCH -> {
                val isTick = (beatNumber % 2 == 0)
                val primaryFreq = (if (isTick) 3800.0 else 3200.0) * pitchMult
                val maxLen = (SAMPLE_RATE * 0.014 * decayMult).toLong()
                if (sampleOffset < maxLen) {
                    val palletClick = sin(2.0 * PI * primaryFreq * t)
                    val metallicOver = 0.35 * sin(2.0 * PI * (primaryFreq * 1.6) * t)
                    0.24 * (palletClick + metallicOver) * exp(-t / (0.0025 * decayMult))
                } else 0.0
            }

            ClockSoundType.VINTAGE_ALARM -> {
                val isTick = (beatNumber % 2 == 0)
                val freq = (if (isTick) 1650.0 else 1250.0) * pitchMult
                val maxLen = (SAMPLE_RATE * 0.035 * decayMult).toLong()
                if (sampleOffset < maxLen) {
                    val springRattle = sin(2.0 * PI * 3400.0 * t) * 0.25
                    val bellStrike = sin(2.0 * PI * freq * t)
                    0.30 * (bellStrike + springRattle) * exp(-t / (0.008 * decayMult))
                } else 0.0
            }

            ClockSoundType.MANTLE_CLOCK -> {
                val isTick = (beatNumber % 2 == 0)
                val freq = (if (isTick) 580.0 else 440.0) * pitchMult
                val maxLen = (SAMPLE_RATE * 0.038 * decayMult).toLong()
                if (sampleOffset < maxLen) {
                    0.28 * sin(2.0 * PI * freq * t) * exp(-t / (0.014 * decayMult))
                } else 0.0
            }

            ClockSoundType.WOODBLOCK -> {
                val isAccent = (beatNumber == 0)
                val freq = (if (isAccent) 880.0 else 660.0) * pitchMult
                val maxLen = (SAMPLE_RATE * 0.030 * decayMult).toLong()
                if (sampleOffset < maxLen) {
                    val fundamental = sin(2.0 * PI * freq * t)
                    val knock = sin(2.0 * PI * (freq * 2.76) * t) * 0.4
                    0.35 * (fundamental + knock) * exp(-t / (0.007 * decayMult))
                } else 0.0
            }

            ClockSoundType.COWBELL -> {
                val isAccent = (beatNumber == 0)
                val freq = (if (isAccent) 820.0 else 560.0) * pitchMult
                val maxLen = (SAMPLE_RATE * 0.055 * decayMult).toLong()
                if (sampleOffset < maxLen) {
                    val m1 = sin(2.0 * PI * freq * t)
                    val m2 = sin(2.0 * PI * (freq * 1.5) * t) * 0.4
                    0.35 * (m1 + m2) * exp(-t / (0.015 * decayMult))
                } else 0.0
            }

            ClockSoundType.WATER_DROP -> {
                val maxLen = (SAMPLE_RATE * 0.040 * decayMult).toLong()
                if (sampleOffset < maxLen) {
                    val freq = (1200.0 + (t * 4000.0)) * pitchMult
                    0.38 * sin(2.0 * PI * freq * t) * exp(-t / (0.012 * decayMult))
                } else 0.0
            }

            ClockSoundType.WATER_CLOCK_CLEPSYDRA -> {
                val maxLen = (SAMPLE_RATE * 0.065 * decayMult).toLong()
                if (sampleOffset < maxLen) {
                    val splash = fastWhiteNoise() * 0.25 * exp(-t / (0.02 * decayMult))
                    val hollow = sin(2.0 * PI * (480.0 * pitchMult) * t) * exp(-t / (0.03 * decayMult))
                    0.30 * (splash + hollow)
                } else 0.0
            }

            ClockSoundType.HEARTBEAT -> {
                val maxLen = (SAMPLE_RATE * 0.12 * decayMult).toLong()
                if (sampleOffset < maxLen) {
                    val lub = sin(2.0 * PI * 65.0 * t) * exp(-t / (0.035 * decayMult))
                    val tDub = t - 0.045
                    val dub = if (tDub > 0) sin(2.0 * PI * 85.0 * tDub) * exp(-tDub / (0.025 * decayMult)) * 0.7 else 0.0
                    0.45 * (lub + dub)
                } else 0.0
            }

            ClockSoundType.FINGERSNAP -> {
                val maxLen = (SAMPLE_RATE * 0.018 * decayMult).toLong()
                if (sampleOffset < maxLen) {
                    val white = fastWhiteNoise() * 0.35 * exp(-t / (0.004 * decayMult))
                    val pop = sin(2.0 * PI * (2200.0 * pitchMult) * t) * exp(-t / (0.006 * decayMult))
                    0.32 * (white + pop)
                } else 0.0
            }

            ClockSoundType.SONAR_PING -> {
                val maxLen = (SAMPLE_RATE * 0.25 * decayMult).toLong()
                if (sampleOffset < maxLen) {
                    val ping = sin(2.0 * PI * (2400.0 * pitchMult) * t) * exp(-t / (0.05 * decayMult))
                    0.32 * ping
                } else 0.0
            }

            ClockSoundType.TELEGRAPH_KEY -> {
                val maxLen = (SAMPLE_RATE * 0.016 * decayMult).toLong()
                if (sampleOffset < maxLen) {
                    val metallic = sin(2.0 * PI * (2200.0 * pitchMult) * t)
                    val rebound = sin(2.0 * PI * (780.0 * pitchMult) * t) * 0.5
                    0.35 * (metallic + rebound) * exp(-t / (0.004 * decayMult))
                } else 0.0
            }

            ClockSoundType.SINGING_BOWL -> {
                val maxLen = (SAMPLE_RATE * 0.50 * decayMult).toLong()
                if (sampleOffset < maxLen) {
                    val f1 = 432.0 * pitchMult
                    val f2 = 630.0 * pitchMult
                    val f3 = 952.0 * pitchMult
                    val h1 = sin(2.0 * PI * f1 * t) * exp(-t / (0.12 * decayMult))
                    val h2 = 0.45 * sin(2.0 * PI * f2 * t) * exp(-t / (0.08 * decayMult))
                    val h3 = 0.22 * sin(2.0 * PI * f3 * t) * exp(-t / (0.05 * decayMult))
                    0.32 * (h1 + h2 + h3)
                } else 0.0
            }

            ClockSoundType.TOWER_BELL -> {
                val maxLen = (SAMPLE_RATE * 0.25 * decayMult).toLong()
                if (sampleOffset < maxLen) {
                    val f1 = 520.0 * pitchMult
                    val f2 = 1045.0 * pitchMult
                    val f3 = 1580.0 * pitchMult
                    val h1 = sin(2.0 * PI * f1 * t) * exp(-t / (0.06 * decayMult))
                    val h2 = 0.5 * sin(2.0 * PI * f2 * t) * exp(-t / (0.04 * decayMult))
                    val h3 = 0.25 * sin(2.0 * PI * f3 * t) * exp(-t / (0.02 * decayMult))
                    0.30 * (h1 + h2 + h3)
                } else 0.0
            }

            ClockSoundType.DIGITAL_BEEP -> {
                val isAccent = (beatNumber == 0)
                val freq = (if (isAccent) 1760.0 else 880.0) * pitchMult
                val durationSec = 0.020 * decayMult
                val maxLen = (SAMPLE_RATE * durationSec).toLong()
                if (sampleOffset < maxLen) {
                    val window = 0.5 * (1.0 - cos(2.0 * PI * t / durationSec))
                    0.28 * sin(2.0 * PI * freq * t) * window
                } else 0.0
            }

            else -> 0.0
        }
    }

    // Zero-Allocation Stereo Ambient Synthesis
    private fun generateAmbientStereo(type: NoiseType, pitchMult: Float) {
        val whiteL = fastWhiteNoise()
        val whiteR = fastWhiteNoise()

        when (type) {
            NoiseType.OFF -> {
                outAmbL = 0.0
                outAmbR = 0.0
            }

            // 🌀 Living Biome Dynamic Morphing Engine (Stereo Spatialized)
            NoiseType.LIVING_BIOME_DYNAMIC -> {
                var mL = 0.0
                var mR = 0.0
                if (biomeCurRain > 0.01) {
                    generateRainStereo(whiteL, whiteR)
                    mL += outAmbL * biomeCurRain
                    mR += outAmbR * biomeCurRain
                }
                if (biomeCurCrickets > 0.01) {
                    generateCricketsStereo(pitchMult)
                    mL += outAmbL * biomeCurCrickets
                    mR += outAmbR * biomeCurCrickets
                }
                if (biomeCurWind > 0.01) {
                    generateWindStereo()
                    mL += outAmbL * biomeCurWind
                    mR += outAmbR * biomeCurWind
                }
                if (biomeCurCampfire > 0.01) {
                    generateCampfireStereo()
                    mL += outAmbL * biomeCurCampfire
                    mR += outAmbR * biomeCurCampfire
                }
                if (biomeCurBook > 0.01) {
                    generateBookPageStereo(whiteL, whiteR)
                    mL += outAmbL * biomeCurBook
                    mR += outAmbR * biomeCurBook
                }
                outAmbL = mL
                outAmbR = mR
            }

            // 🌿 Outdoor Nature & Organic Soundscapes
            NoiseType.CRICKETS_NIGHT -> generateCricketsStereo(pitchMult)

            NoiseType.CRICKETS_DISTANT_MEADOW -> {
                // Trail-Sense CricketsNoChirp: Calming meadow hum bed with stereo width
                meadowB0 += 0.08 * (whiteL - meadowB0)
                meadowB1 += 0.08 * (meadowB0 - meadowB1)
                val bed = (meadowB0 - meadowB1) * 0.18
                outAmbL = bed + generateBrownSampleL(whiteL) * 0.04
                outAmbR = bed + generateBrownSampleR(whiteR) * 0.04
            }

            NoiseType.OCEAN_SURF -> {
                // Trail-Sense OceanWavesAudioStream: 20-second wave with cubic power swell across stereo field
                val wavePhase = (globalSampleCounter % (SAMPLE_RATE * 20.0)).toDouble() / (SAMPLE_RATE * 20.0)
                val waveSine = 0.5 + 0.5 * sin(2.0 * PI * wavePhase)
                val cubicSwell = waveSine.pow(3.0)
                val brL = generateBrownSampleL(whiteL)
                val brR = generateBrownSampleR(whiteR)
                outAmbL = (0.95 * brL * 0.65 * cubicSwell) + (0.05 * generatePinkSampleL(whiteL) * 0.05)
                outAmbR = (0.95 * brR * 0.65 * cubicSwell) + (0.05 * generatePinkSampleR(whiteR) * 0.05)
            }

            NoiseType.RAIN_ON_LEAVES -> generateRainStereo(whiteL, whiteR)
            NoiseType.CAMPFIRE_CRACKLE -> generateCampfireStereo()
            NoiseType.WIND_IN_TREES -> generateWindStereo()

            NoiseType.THUNDERSTORM -> {
                generateRainStereo(whiteL, whiteR)
                val rL = outAmbL * 0.35
                val rR = outAmbR * 0.35
                if (thunderCountdown > 0) {
                    thunderCountdown--
                    thunderRumbleEnvelope *= 0.9996
                } else if (fastRandomCheck(0.00008)) {
                    thunderCountdown = (SAMPLE_RATE * 3.5).toInt()
                    thunderRumbleEnvelope = 0.55
                }
                val thL = generateBrownSampleL(whiteL) * thunderRumbleEnvelope * 0.5
                val thR = generateBrownSampleR(whiteR) * thunderRumbleEnvelope * 0.5
                outAmbL = rL + thL
                outAmbR = rR + thR
            }

            NoiseType.BROOK_STREAM -> {
                val pL = generatePinkSampleL(whiteL) * 0.18
                val pR = generatePinkSampleR(whiteR) * 0.18
                var bubbleL = 0.0
                var bubbleR = 0.0
                if (fastRandomCheck(0.008)) {
                    val fBubble = 750.0 + ((fastWhiteNoise() + 1.0) * 0.5 * 950.0)
                    val b = sin(2.0 * PI * fBubble * (globalSampleCounter.toDouble() / SAMPLE_RATE)) * 0.35
                    val pan = fastWhiteNoise() * 0.5
                    bubbleL = b * cos((pan + 1.0) * PI / 4.0)
                    bubbleR = b * sin((pan + 1.0) * PI / 4.0)
                }
                outAmbL = pL + bubbleL
                outAmbR = pR + bubbleR
            }

            NoiseType.SUMMER_CICADAS -> {
                val tSec = globalSampleCounter.toDouble() / SAMPLE_RATE
                val swellL = 0.5 + 0.5 * sin(2.0 * PI * 0.4 * tSec)
                val swellR = 0.5 + 0.5 * sin(2.0 * PI * 0.4 * tSec + 0.5)
                val carrierL = sin(2.0 * PI * (5200.0 * pitchMult) * tSec)
                val carrierR = sin(2.0 * PI * (5350.0 * pitchMult) * tSec)
                outAmbL = (carrierL * swellL * 0.28) + generatePinkSampleL(whiteL) * 0.06
                outAmbR = (carrierR * swellR * 0.28) + generatePinkSampleR(whiteR) * 0.06
            }

            NoiseType.DEEP_CAVE_DRIPS -> {
                val caveAirL = generateBrownSampleL(whiteL) * 0.15
                val caveAirR = generateBrownSampleR(whiteR) * 0.15
                var dripL = 0.0
                var dripR = 0.0
                if (fastRandomCheck(0.001)) {
                    val fDrip = 800.0 + ((fastWhiteNoise() + 1.0) * 0.5 * 1200.0)
                    val drip = sin(2.0 * PI * fDrip * (globalSampleCounter.toDouble() / SAMPLE_RATE)) * 0.45
                    val pan = fastWhiteNoise() * 0.6
                    dripL = drip * cos((pan + 1.0) * PI / 4.0)
                    dripR = drip * sin((pan + 1.0) * PI / 4.0)
                }
                outAmbL = caveAirL + dripL
                outAmbR = caveAirR + dripR
            }

            NoiseType.NIGHT_FROGS -> {
                val phase = (globalSampleCounter % (SAMPLE_RATE * 0.75)).toDouble() / SAMPLE_RATE
                var frogEnv = 0.0
                if (phase < 0.06) frogEnv = sin(PI * (phase / 0.06))
                val croak = sin(2.0 * PI * (420.0 * pitchMult) * phase)
                outAmbL = (croak * frogEnv * 0.32) + generateBrownSampleL(whiteL) * 0.12
                outAmbR = (croak * frogEnv * 0.28) + generateBrownSampleR(whiteR) * 0.12
            }

            NoiseType.BLIZZARD_HOWL -> {
                val tSec = globalSampleCounter.toDouble() / SAMPLE_RATE
                val gust = 0.5 + 0.5 * sin(2.0 * PI * 0.6 * tSec)
                val whistle = sin(2.0 * PI * (620.0 + 350.0 * gust) * tSec) * 0.25 * gust
                outAmbL = (generatePinkSampleL(whiteL) * 0.35) + whistle
                outAmbR = (generatePinkSampleR(whiteR) * 0.35) + whistle * 0.9
            }

            // ⌨️ Keyboards & Smooth Creamy Switches (Stereo Spatialized Panning)
            NoiseType.KEYBOARD_CREAMY_LUBED,
            NoiseType.KEYBOARD_MARBLY_JELLY,
            NoiseType.KEYBOARD_TOPRE_ELECTRO,
            NoiseType.KEYBOARD_BLUE_SWITCH,
            NoiseType.KEYBOARD_BROWN_SWITCH,
            NoiseType.KEYBOARD_RED_SWITCH,
            NoiseType.KEYBOARD_SILENT_CHICLET,
            NoiseType.KEYBOARD_BUCKLING_SPRING,
            NoiseType.KEYBOARD_MECHANICAL -> generateKeyboardStereo(whiteL, whiteR, type, pitchMult)

            // 🏠 Indoor & Focus Ambient Soundscapes
            NoiseType.CEILING_FAN -> {
                // Trail-Sense FanNoiseAudioStream: 0.85 brown noise + 0.15 dual-harmonic 100Hz motor hum
                val hum = (sin(2.0 * PI * 100.0 * (globalSampleCounter.toDouble() / SAMPLE_RATE)) + 0.5 * sin(2.0 * PI * 200.0 * (globalSampleCounter.toDouble() / SAMPLE_RATE))) * 0.15
                val bladeDraft = 0.85 + 0.15 * sin(2.0 * PI * 3.5 * (globalSampleCounter.toDouble() / SAMPLE_RATE))
                outAmbL = (generateBrownSampleL(whiteL) * 0.85 + hum) * bladeDraft * 0.40
                outAmbR = (generateBrownSampleR(whiteR) * 0.85 + hum) * bladeDraft * 0.40
            }

            NoiseType.BOOK_PAGE_FLIPS -> generateBookPageStereo(whiteL, whiteR)

            NoiseType.COFFEE_SHOP -> {
                val murmurL = generateBrownSampleL(whiteL) * 0.18
                val murmurR = generateBrownSampleR(whiteR) * 0.18
                val hissL = generatePinkSampleL(whiteL) * 0.08
                val hissR = generatePinkSampleR(whiteR) * 0.08
                if (coffeeCupCountdown > 0) {
                    coffeeCupCountdown--
                    coffeeCupChime *= 0.992
                } else if (fastRandomCheck(0.00015)) {
                    coffeeCupCountdown = (SAMPLE_RATE * 0.04).toInt()
                    val fCup = 2800.0 + (fastWhiteNoise() + 1.0) * 0.5 * 600.0
                    coffeeCupChime = sin(2.0 * PI * fCup * (globalSampleCounter.toDouble() / SAMPLE_RATE)) * 0.28
                }
                outAmbL = murmurL + hissL + coffeeCupChime
                outAmbR = murmurR + hissR + coffeeCupChime * 0.85
            }

            NoiseType.VINYL_CRACKLE -> {
                val rumbleL = generateBrownSampleL(whiteL) * 0.12
                val rumbleR = generateBrownSampleR(whiteR) * 0.12
                val dustHiss = ((whiteL - lastWhite1) * 0.22) * 0.15
                lastWhite1 = whiteL
                var pop = 0.0
                if (fastRandomCheck(0.0008)) pop = fastWhiteNoise() * 0.35
                outAmbL = rumbleL + dustHiss + pop
                outAmbR = rumbleR + dustHiss + pop * 0.92
            }

            NoiseType.CLOCK_ROOM_ANTIQUE -> {
                val tSec = globalSampleCounter.toDouble() / SAMPLE_RATE
                val t1 = (tSec % 1.0)
                val c1 = if (t1 < 0.015) 0.25 * sin(2.0 * PI * 850.0 * t1) else 0.0
                val t2 = (tSec % 1.25)
                val c2 = if (t2 < 0.015) 0.18 * sin(2.0 * PI * 620.0 * t2) else 0.0
                val t3 = (tSec % 0.85)
                val c3 = if (t3 < 0.015) 0.20 * sin(2.0 * PI * 1100.0 * t3) else 0.0
                outAmbL = generateBrownSampleL(whiteL) * 0.08 + c1 + c2 * 0.5
                outAmbR = generateBrownSampleR(whiteR) * 0.08 + c2 + c3 * 0.5
            }

            NoiseType.COZY_FIREPLACE -> {
                generateCampfireStereo()
                outAmbL *= 1.15
                outAmbR *= 1.15
            }

            NoiseType.PENCIL_SKETCH -> {
                val tSec = globalSampleCounter.toDouble() / SAMPLE_RATE
                val strokeSwell = 0.5 + 0.5 * sin(2.0 * PI * 1.8 * tSec)
                val graphite = ((whiteL - lastWhite1) * 0.22) * 0.35 * strokeSwell
                lastWhite1 = whiteL
                outAmbL = graphite
                outAmbR = graphite * 0.88
            }

            // 🌈 Calibrated Spectral Colors (Decorrelated Stereo Width)
            NoiseType.WHITE -> {
                outAmbL = whiteL * 0.25
                outAmbR = whiteR * 0.25
            }

            NoiseType.PINK -> {
                outAmbL = generatePinkSampleL(whiteL) * 0.40
                outAmbR = generatePinkSampleR(whiteR) * 0.40
            }

            NoiseType.BROWN -> {
                outAmbL = generateBrownSampleL(whiteL) * 0.45
                outAmbR = generateBrownSampleR(whiteR) * 0.45
            }

            NoiseType.GREEN -> {
                val gL = generatePinkSampleL(whiteL) * 0.35
                val gR = generatePinkSampleR(whiteR) * 0.35
                outAmbL = gL
                outAmbR = gR
            }

            NoiseType.BLUE -> {
                val b = ((whiteL - lastWhite1) * 0.25)
                lastWhite1 = whiteL
                outAmbL = b
                outAmbR = b
            }

            NoiseType.VIOLET -> {
                val v = ((whiteL - lastWhite1) * 0.30)
                lastWhite1 = whiteL
                outAmbL = v
                outAmbR = v
            }

            NoiseType.GREY -> {
                outAmbL = (generatePinkSampleL(whiteL) * 0.25) + (generateBrownSampleL(whiteL) * 0.20)
                outAmbR = (generatePinkSampleR(whiteR) * 0.25) + (generateBrownSampleR(whiteR) * 0.20)
            }

            NoiseType.BLACK -> {
                outAmbL = generateBrownSampleL(whiteL) * 0.55
                outAmbR = generateBrownSampleR(whiteR) * 0.55
            }

            else -> {
                outAmbL = 0.0
                outAmbR = 0.0
            }
        }
    }

    // Keyboard Typist Stereo Modeling with Dynamic Acoustic Panning
    private fun generateKeyboardStereo(whiteL: Double, whiteR: Double, type: NoiseType, pitchMult: Float) {
        val quietRoomL = generateBrownSampleL(whiteL) * 0.02
        val quietRoomR = generateBrownSampleR(whiteR) * 0.02

        if (keyActiveSamples > 0) {
            keyActiveSamples--
            keyElapsedSamples++
            val tKey = keyElapsedSamples.toDouble() / SAMPLE_RATE

            val s = when (type) {
                NoiseType.KEYBOARD_CREAMY_LUBED -> {
                    // Ultra-smooth lubed POM linear switch: Buttery, cushioned rounded pop (520Hz)
                    val baseFreq = if (keyIsSpacebar) 340.0 else (520.0 * pitchMult)
                    val pop = sin(2.0 * PI * baseFreq * tKey) * exp(-tKey / 0.018) * 0.42
                    val subLube = sin(2.0 * PI * (baseFreq * 0.6) * tKey) * exp(-tKey / 0.026) * 0.22
                    (pop + subLube) * keyRandomAmp
                }

                NoiseType.KEYBOARD_MARBLY_JELLY -> {
                    // PE foam mod: Poppy, smooth marble raindrop sound (680Hz fundamental + warm harmonic)
                    val baseFreq = if (keyIsSpacebar) 450.0 else (680.0 * pitchMult)
                    val marblePop = sin(2.0 * PI * baseFreq * tKey) * exp(-tKey / 0.015) * 0.40
                    val harmonic = sin(2.0 * PI * (baseFreq * 1.9) * tKey) * exp(-tKey / 0.008) * 0.18
                    (marblePop + harmonic) * keyRandomAmp
                }

                NoiseType.KEYBOARD_TOPRE_ELECTRO -> {
                    // Electro-capacitive rubber dome: Buttery smooth thock-clop
                    val domeCollapse = if (tKey < 0.010) sin(2.0 * PI * (340.0 * pitchMult) * tKey) * 0.35 else 0.0
                    val bottomClop = sin(2.0 * PI * (480.0 * pitchMult) * tKey) * exp(-tKey / 0.020) * 0.32
                    (domeCollapse + bottomClop) * keyRandomAmp
                }

                NoiseType.KEYBOARD_RED_SWITCH -> {
                    // Linear switch: bass thock
                    val baseFreq = if (keyIsSpacebar) 280.0 else (420.0 * pitchMult)
                    val thock = sin(2.0 * PI * baseFreq * tKey) * exp(-tKey / 0.024) * 0.40
                    val subResonance = sin(2.0 * PI * (baseFreq * 0.5) * tKey) * exp(-tKey / 0.035) * 0.20
                    (thock + subResonance) * keyRandomAmp
                }

                NoiseType.KEYBOARD_BROWN_SWITCH, NoiseType.KEYBOARD_MECHANICAL -> {
                    // Tactile switch: bump + dampened clack
                    val bump = if (tKey < 0.006) sin(2.0 * PI * (1500.0 * pitchMult) * tKey) * 0.22 else 0.0
                    val baseFreq = if (keyIsSpacebar) 420.0 else 680.0
                    val clack = sin(2.0 * PI * (baseFreq * pitchMult) * tKey) * exp(-tKey / 0.016) * 0.32
                    (bump + clack) * keyRandomAmp
                }

                NoiseType.KEYBOARD_BLUE_SWITCH -> {
                    // Clicky switch: click leaf (3100Hz) + clack (920Hz)
                    val clickLeaf = if (tKey < 0.0035) sin(2.0 * PI * (3100.0 * pitchMult) * tKey) * 0.45 else 0.0
                    val baseFreq = if (keyIsSpacebar) 550.0 else 920.0
                    val bottomClack = sin(2.0 * PI * (baseFreq * pitchMult) * tKey) * exp(-tKey / 0.012) * 0.35
                    (clickLeaf + bottomClack) * keyRandomAmp
                }

                NoiseType.KEYBOARD_SILENT_CHICLET -> {
                    val baseFreq = if (keyIsSpacebar) 220.0 else (310.0 * pitchMult)
                    val tap = sin(2.0 * PI * baseFreq * tKey) * exp(-tKey / 0.010) * 0.22
                    val mutedThud = generateBrownSampleL(whiteL) * exp(-tKey / 0.008) * 0.15
                    (tap + mutedThud) * keyRandomAmp
                }

                NoiseType.KEYBOARD_BUCKLING_SPRING -> {
                    val springBuckle = if (tKey < 0.018) sin(2.0 * PI * (2450.0 * pitchMult) * tKey) * exp(-tKey / 0.008) * 0.38 else 0.0
                    val steelFrame = sin(2.0 * PI * (195.0 * pitchMult) * tKey) * exp(-tKey / 0.040) * 0.45
                    (springBuckle + steelFrame) * keyRandomAmp
                }

                else -> 0.0
            }

            outAmbL = quietRoomL + s * keyPanL
            outAmbR = quietRoomR + s * keyPanR
        } else {
            if (keyTypistDelaySamples <= 0) {
                // Next keystroke in 110ms to 320ms (~50-80 WPM)
                keyTypistDelaySamples = (SAMPLE_RATE * (0.11 + ((fastWhiteNoise() + 1.0) * 0.5) * 0.22)).toInt()
                keyIsSpacebar = fastRandomCheck(0.16)
                keyRandomAmp = 0.8 + ((fastWhiteNoise() + 1.0) * 0.5) * 0.35
                keyActiveSamples = (SAMPLE_RATE * 0.045).toInt()
                keyElapsedSamples = 0

                // Keystroke stereo panning law (-0.55 left to +0.55 right)
                val pan = if (keyIsSpacebar) 0.0 else (fastWhiteNoise() * 0.55)
                keyPanL = cos((pan + 1.0) * PI / 4.0)
                keyPanR = sin((pan + 1.0) * PI / 4.0)
            } else {
                keyTypistDelaySamples--
            }
            outAmbL = quietRoomL
            outAmbR = quietRoomR
        }
    }

    private fun generateRainStereo(whiteL: Double, whiteR: Double) {
        val rainL = generatePinkSampleL(whiteL) * 0.22
        val rainR = generatePinkSampleR(whiteR) * 0.22
        var splatterL = 0.0
        var splatterR = 0.0
        if (fastRandomCheck(0.003)) {
            val sp = fastWhiteNoise() * 0.35
            val pan = fastWhiteNoise() * 0.6
            splatterL = sp * cos((pan + 1.0) * PI / 4.0)
            splatterR = sp * sin((pan + 1.0) * PI / 4.0)
        }
        outAmbL = rainL + splatterL
        outAmbR = rainR + splatterR
    }

    private fun generateCampfireStereo() {
        val wL = fastWhiteNoise()
        val wR = fastWhiteNoise()
        val rL = generateBrownSampleL(wL) * 0.20
        val rR = generateBrownSampleR(wR) * 0.20
        if (campfirePopCountdown > 0) {
            campfirePopCountdown--
            campfirePopSample *= 0.82
        } else if (fastRandomCheck(0.0006)) {
            campfirePopCountdown = (SAMPLE_RATE * 0.006).toInt()
            campfirePopSample = fastWhiteNoise() * 0.55
            val pan = fastWhiteNoise() * 0.5
            campfirePopPanL = cos((pan + 1.0) * PI / 4.0)
            campfirePopPanR = sin((pan + 1.0) * PI / 4.0)
        }
        outAmbL = rL + campfirePopSample * campfirePopPanL
        outAmbR = rR + campfirePopSample * campfirePopPanR
    }

    private fun generateWindStereo() {
        val pinkL = generatePinkSampleL(fastWhiteNoise())
        val pinkR = generatePinkSampleR(fastWhiteNoise())
        val windPhase = (globalSampleCounter % (SAMPLE_RATE * 6.0)).toDouble() / (SAMPLE_RATE * 6.0)
        val cutoffWind = 500.0 + 800.0 * (0.5 + 0.5 * sin(2.0 * PI * windPhase))
        val alphaWind = (1.0 / (2.0 * PI * cutoffWind))
        val a = (1.0 / SAMPLE_RATE) / (alphaWind + (1.0 / SAMPLE_RATE))
        windLpf += a * (pinkL - windLpf)
        outAmbL = windLpf * 0.55
        outAmbR = (windLpf + (pinkR * 0.05)) * 0.52
    }

    private fun generateBookPageStereo(whiteL: Double, whiteR: Double) {
        val libraryL = generateBrownSampleL(whiteL) * 0.05
        val libraryR = generateBrownSampleR(whiteR) * 0.05
        var pageL = 0.0
        var pageR = 0.0

        if (bookFlipCountdown > 0) {
            bookFlipCountdown--
            bookFlipProgress++
            val tFlip = bookFlipProgress.toDouble() / SAMPLE_RATE
            val friction = whiteL * sin(2.0 * PI * 1100.0 * tFlip) * exp(-tFlip / 0.04)
            val whoosh = generatePinkSampleL(whiteL) * 0.35 * exp(-tFlip / 0.08)
            val page = (friction * 0.6 + whoosh * 0.5) * 0.5
            pageL = page
            pageR = page * 0.85
        } else {
            if (fastRandomCheck(0.0001)) {
                bookFlipCountdown = (SAMPLE_RATE * 0.18).toInt()
                bookFlipProgress = 0
            }
        }
        outAmbL = libraryL + pageL
        outAmbR = libraryR + pageR
    }

    private fun generateCricketsStereo(pitchMult: Float) {
        val wL = fastWhiteNoise()
        val wR = fastWhiteNoise()

        meadowB0 += 0.08 * (wL - meadowB0)
        meadowB1 += 0.08 * (meadowB0 - meadowB1)
        val meadowBed = (meadowB0 - meadowB1) * 0.12 + generateBrownSampleL(wL) * 0.03

        val periodSamples = (SAMPLE_RATE * 1.4).toLong()
        val phaseSec = (globalSampleCounter % periodSamples).toDouble() / SAMPLE_RATE

        var pulseEnv = 0.0
        val impulseDuration = 0.040
        val gapDuration = 0.010
        val pulseCycle = impulseDuration + gapDuration

        for (p in 0..2) {
            val pStart = 0.06 + p * pulseCycle
            if (phaseSec >= pStart && phaseSec < pStart + impulseDuration) {
                val tPulse = phaseSec - pStart
                pulseEnv = sin(PI * (tPulse / impulseDuration))
                break
            }
        }

        if (pulseEnv <= 0.0001) {
            outAmbL = meadowBed
            outAmbR = meadowBed + generateBrownSampleR(wR) * 0.02
            return
        }

        val f1 = 4250.0 * pitchMult
        val f2 = 4500.0 * pitchMult
        val f3 = 4750.0 * pitchMult
        val f4 = 4900.0 * pitchMult

        val dt = 1.0 / SAMPLE_RATE
        cricketPhase1 += 2.0 * PI * f1 * dt
        cricketPhase2 += 2.0 * PI * f2 * dt
        cricketPhase3 += 2.0 * PI * f3 * dt
        cricketPhase4 += 2.0 * PI * f4 * dt

        val chirpSignal = (
            0.40 * sin(cricketPhase1) +
            0.35 * sin(cricketPhase2 + 1.2) +
            0.20 * sin(cricketPhase3 + 2.5) +
            0.15 * sin(cricketPhase4 + 0.7)
        ) * 0.32

        outAmbL = meadowBed + (chirpSignal * pulseEnv * 0.85)
        outAmbR = meadowBed + (chirpSignal * pulseEnv * 1.0)
    }

    private fun generatePinkSampleL(white: Double): Double {
        pB0_L = 0.99886 * pB0_L + white * 0.0555179
        pB1_L = 0.99332 * pB1_L + white * 0.0750759
        pB2_L = 0.96900 * pB2_L + white * 0.1538520
        pB3_L = 0.86650 * pB3_L + white * 0.3104856
        pB4_L = 0.55000 * pB4_L + white * 0.5329522
        pB5_L = -0.7616 * pB5_L - white * 0.0168980
        val pink = (pB0_L + pB1_L + pB2_L + pB3_L + pB4_L + pB5_L + pB6_L + white * 0.5362) * 0.065
        pB6_L = white * 0.115926
        return pink
    }

    private fun generatePinkSampleR(white: Double): Double {
        pB0_R = 0.99886 * pB0_R + white * 0.0555179
        pB1_R = 0.99332 * pB1_R + white * 0.0750759
        pB2_R = 0.96900 * pB2_R + white * 0.1538520
        pB3_R = 0.86650 * pB3_R + white * 0.3104856
        pB4_R = 0.55000 * pB4_R + white * 0.5329522
        pB5_R = -0.7616 * pB5_R - white * 0.0168980
        val pink = (pB0_R + pB1_R + pB2_R + pB3_R + pB4_R + pB5_R + pB6_R + white * 0.5362) * 0.065
        pB6_R = white * 0.115926
        return pink
    }

    private fun generateBrownSampleL(white: Double): Double {
        lastBrownL = (lastBrownL + (0.02 * white)) / 1.02
        return lastBrownL * 0.45
    }

    private fun generateBrownSampleR(white: Double): Double {
        lastBrownR = (lastBrownR + (0.02 * white)) / 1.02
        return lastBrownR * 0.45
    }
}
