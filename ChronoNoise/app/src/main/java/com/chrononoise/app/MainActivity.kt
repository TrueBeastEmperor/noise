package com.chrononoise.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chrononoise.app.audio.AudioPlaybackService
import com.chrononoise.app.audio.ClockSoundType
import com.chrononoise.app.audio.NoiseType
import com.chrononoise.app.audio.ThemeType
import com.chrononoise.app.databinding.ActivityMainBinding
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val engine = AudioPlaybackService.soundEngine
    private var currentTheme = ThemeType.CYBERPUNK

    private val tapTimestamps = mutableListOf<Long>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isActivityResumed = false
    private var lastObservedBeatSequence = 0L

    // Haptic Metronome
    private var isHapticsEnabled = false
    private var vibrator: Vibrator? = null

    private val PREFS_NAME = "chrononoise_prefs"

    private val beatSyncRunnable = object : Runnable {
        override fun run() {
            if (!isActivityResumed) return

            val seq = engine.beatSequenceCounter.get()
            if (seq != lastObservedBeatSequence) {
                lastObservedBeatSequence = seq
                val beatNum = engine.currentBeatAtomic.get()
                val isAccent = engine.isAccentAtomic.get()
                animateBeat(beatNum, isAccent)

                if (isHapticsEnabled) {
                    triggerHapticBeat(isAccent)
                }
            }

            // Continuously update sleep timer badge and status if active
            updateTimerDisplay()
            updateUiState()

            mainHandler.postDelayed(this, 16) // ~60 FPS polling of atomic counters
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkNotificationPermission()
        loadPreferences()

        setupTabs()
        setupSwitches()
        setupHapticsSwitch()
        setupSleepTimerControls()
        setupBackgroundAtmosphereControls()
        setupThemeChips()
        setupMorphSpeedChips()
        setupBpmControls()
        setupSignatureControls()
        setupClockTypeChips()
        setupNatureChips()
        setupKeyboardChips()
        setupIndoorChips()
        setupColorChips()
        setupVolumeAndAcousticSliders()
        setupRandomizerButton()
        setupPlayPauseButton()
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        mainHandler.post(beatSyncRunnable)
        updateUiState()
        updateTimerDisplay()
    }

    override fun onPause() {
        isActivityResumed = false
        mainHandler.removeCallbacks(beatSyncRunnable)
        savePreferences()
        super.onPause()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun setupTabs() {
        binding.tabLayoutCategories.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showTabContent(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showTabContent(position: Int) {
        binding.tabContentBeat.visibility = if (position == 0) View.VISIBLE else View.GONE
        binding.tabContentNature.visibility = if (position == 1) View.VISIBLE else View.GONE
        binding.tabContentKeyboards.visibility = if (position == 2) View.VISIBLE else View.GONE
        binding.tabContentColors.visibility = if (position == 3) View.VISIBLE else View.GONE
        binding.tabContentBiome.visibility = if (position == 4) View.VISIBLE else View.GONE
        binding.tabContentSynth.visibility = if (position == 5) View.VISIBLE else View.GONE
    }

    private fun setupSwitches() {
        // Metronome / Beat Switch
        binding.switchBeatEnabled.isChecked = !engine.muteClock
        binding.switchBeatEnabled.setOnCheckedChangeListener { _, isChecked ->
            engine.muteClock = !isChecked
            if (!isChecked) {
                binding.tvBeatSwitchSubtitle.text = "Beat muted (Ambiance only)"
            } else {
                binding.tvBeatSwitchSubtitle.text = "Turn off when you only want nature or keyboard sounds"
                if (engine.clockType == ClockSoundType.OFF) {
                    engine.clockType = ClockSoundType.ETERA_TICK_TOCK
                    binding.chipClockEtera.isChecked = true
                }
            }
            savePreferences()
        }

        // Nature Ambiance Switch
        binding.switchNatureEnabled.isChecked = !engine.muteAmbiance && engine.noiseType.category == "Nature"
        binding.switchNatureEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                engine.muteAmbiance = false
                if (engine.noiseType.category != "Nature") {
                    engine.noiseType = NoiseType.CRICKETS_NIGHT
                    binding.chipNatureCrickets.isChecked = true
                }
                binding.switchIndoorEnabled.isChecked = false
                binding.switchColorEnabled.isChecked = false
                binding.switchBiomeEnabled.isChecked = false
            } else if (engine.noiseType.category == "Nature") {
                engine.muteAmbiance = true
            }
            savePreferences()
        }

        // Keyboards & Indoor Switch
        binding.switchIndoorEnabled.isChecked = !engine.muteAmbiance && (engine.noiseType.category.startsWith("Keyboard") || engine.noiseType.category == "Indoor")
        binding.switchIndoorEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                engine.muteAmbiance = false
                if (!engine.noiseType.category.startsWith("Keyboard") && engine.noiseType.category != "Indoor") {
                    engine.noiseType = NoiseType.KEYBOARD_CREAMY_LUBED
                    binding.chipKeyCreamy.isChecked = true
                }
                binding.switchNatureEnabled.isChecked = false
                binding.switchColorEnabled.isChecked = false
                binding.switchBiomeEnabled.isChecked = false
            } else if (engine.noiseType.category.startsWith("Keyboard") || engine.noiseType.category == "Indoor") {
                engine.muteAmbiance = true
            }
            savePreferences()
        }

        // Spectral Colors Switch
        binding.switchColorEnabled.isChecked = !engine.muteAmbiance && engine.noiseType.category.startsWith("Color")
        binding.switchColorEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                engine.muteAmbiance = false
                if (!engine.noiseType.category.startsWith("Color")) {
                    engine.noiseType = NoiseType.BROWN
                    binding.chipColorBrown.isChecked = true
                }
                binding.switchNatureEnabled.isChecked = false
                binding.switchIndoorEnabled.isChecked = false
                binding.switchBiomeEnabled.isChecked = false
            } else if (engine.noiseType.category.startsWith("Color")) {
                engine.muteAmbiance = true
            }
            savePreferences()
        }

        // Living Biome Switch
        binding.switchBiomeEnabled.isChecked = !engine.muteAmbiance && engine.noiseType == NoiseType.LIVING_BIOME_DYNAMIC
        binding.switchBiomeEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                engine.muteAmbiance = false
                engine.noiseType = NoiseType.LIVING_BIOME_DYNAMIC
                binding.switchNatureEnabled.isChecked = false
                binding.switchIndoorEnabled.isChecked = false
                binding.switchColorEnabled.isChecked = false
            } else if (engine.noiseType == NoiseType.LIVING_BIOME_DYNAMIC) {
                engine.muteAmbiance = true
            }
            savePreferences()
        }
    }

    private fun setupHapticsSwitch() {
        binding.switchHaptics.isChecked = isHapticsEnabled
        binding.switchHaptics.setOnCheckedChangeListener { _, isChecked ->
            isHapticsEnabled = isChecked
            savePreferences()
            if (isChecked) {
                triggerHapticBeat(isAccent = true)
            }
        }
    }

    private fun triggerHapticBeat(isAccent: Boolean) {
        if (vibrator == null) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }

        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = if (isAccent) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            } else {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            }
            vib.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(if (isAccent) 30L else 12L)
        }
    }

    private fun setupSleepTimerControls() {
        binding.chipTimerOff.setOnClickListener { setSleepTimer(0) }
        binding.chipTimer15.setOnClickListener { setSleepTimer(15) }
        binding.chipTimer30.setOnClickListener { setSleepTimer(30) }
        binding.chipTimer45.setOnClickListener { setSleepTimer(45) }
        binding.chipTimer60.setOnClickListener { setSleepTimer(60) }
        binding.chipTimerPomodoro.setOnClickListener { setSleepTimer(25) }
    }

    private fun setSleepTimer(minutes: Int) {
        engine.startSleepTimer(minutes)
        updateTimerDisplay()
    }

    private fun updateTimerDisplay() {
        val remaining = engine.sleepTimerRemainingSeconds.get()
        if (remaining > 0L) {
            val mins = remaining / 60
            val secs = remaining % 60
            val formatted = String.format("%02d:%02d", mins, secs)
            binding.tvTimerBadge.text = "⏱️ $formatted"
            binding.tvTimerBadge.setTextColor(currentTheme.primaryAccent)
            binding.tvTimerCountdown.text = "$formatted remaining"
        } else {
            binding.tvTimerBadge.text = "⏱️ OFF"
            binding.tvTimerBadge.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
            binding.tvTimerCountdown.text = "Off"
        }
    }

    private fun setupBackgroundAtmosphereControls() {
        binding.sliderBgVolume.value = engine.backgroundNoiseVolume.coerceIn(0f, 1f)
        binding.tvBgBedVolume.text = "${(binding.sliderBgVolume.value * 100).toInt()}%"

        binding.sliderBgVolume.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                engine.backgroundNoiseVolume = value
                binding.tvBgBedVolume.text = "${(value * 100).toInt()}%"
                savePreferences()
            }
        }

        binding.chipBgNone.setOnClickListener { engine.backgroundNoiseType = NoiseType.OFF; savePreferences() }
        binding.chipBgRain.setOnClickListener { engine.backgroundNoiseType = NoiseType.RAIN_ON_LEAVES; savePreferences() }
        binding.chipBgMeadow.setOnClickListener { engine.backgroundNoiseType = NoiseType.CRICKETS_DISTANT_MEADOW; savePreferences() }
        binding.chipBgFan.setOnClickListener { engine.backgroundNoiseType = NoiseType.CEILING_FAN; savePreferences() }
        binding.chipBgVinyl.setOnClickListener { engine.backgroundNoiseType = NoiseType.VINYL_CRACKLE; savePreferences() }
        binding.chipBgFireplace.setOnClickListener { engine.backgroundNoiseType = NoiseType.COZY_FIREPLACE; savePreferences() }
    }

    private fun setupThemeChips() {
        binding.chipThemeCyber.setOnClickListener { applyTheme(ThemeType.CYBERPUNK) }
        binding.chipThemeOled.setOnClickListener { applyTheme(ThemeType.OLED_MIDNIGHT) }
        binding.chipThemeForest.setOnClickListener { applyTheme(ThemeType.FOREST_BIO) }
        binding.chipThemeSunset.setOnClickListener { applyTheme(ThemeType.SUNSET_HORIZON) }
        binding.chipThemeTokyo.setOnClickListener { applyTheme(ThemeType.TOKYO_VAPOR) }
        binding.chipThemeNordic.setOnClickListener { applyTheme(ThemeType.NORDIC_FROST) }
        binding.chipThemeCoffee.setOnClickListener { applyTheme(ThemeType.COFFEE_LIBRARY) }
        binding.chipThemeSolarized.setOnClickListener { applyTheme(ThemeType.SOLARIZED_DARK) }
        binding.chipThemeDracula.setOnClickListener { applyTheme(ThemeType.DRACULA_NEON) }
        binding.chipThemeAbyssal.setOnClickListener { applyTheme(ThemeType.ABYSSAL_OCEAN) }
    }

    private fun applyTheme(theme: ThemeType) {
        currentTheme = theme
        val accent = theme.primaryAccent

        binding.tvAppTitle.setTextColor(accent)
        binding.tabLayoutCategories.setSelectedTabIndicatorColor(accent)
        binding.tabLayoutCategories.tabSelectedTextColor = accent
        binding.btnBpmMinus.strokeColor = ColorStateList.valueOf(accent)
        binding.btnBpmPlus.strokeColor = ColorStateList.valueOf(accent)
        binding.sliderBpm.thumbTintList = ColorStateList.valueOf(accent)
        binding.sliderBpm.trackActiveTintList = ColorStateList.valueOf(accent)

        binding.viewBeatOrbPulse.backgroundTintList = ColorStateList.valueOf(accent)
        binding.btnMainPlayPause.backgroundTintList = ColorStateList.valueOf(
            if (engine.isPlaying) ContextCompat.getColor(this, R.color.accent_pink) else accent
        )

        binding.sliderClockVolume.thumbTintList = ColorStateList.valueOf(accent)
        binding.sliderClockVolume.trackActiveTintList = ColorStateList.valueOf(accent)

        savePreferences()
    }

    private fun setupMorphSpeedChips() {
        binding.chipMorph15s.setOnClickListener { engine.morphDurationSeconds = 15L; binding.tvMorphSpeedLabel.text = "Active: 15s Blitz"; savePreferences() }
        binding.chipMorph1m.setOnClickListener { engine.morphDurationSeconds = 60L; binding.tvMorphSpeedLabel.text = "Active: 1m Dynamic"; savePreferences() }
        binding.chipMorph5m.setOnClickListener { engine.morphDurationSeconds = 300L; binding.tvMorphSpeedLabel.text = "Active: 5m Study Cycle"; savePreferences() }
        binding.chipMorph15m.setOnClickListener { engine.morphDurationSeconds = 900L; binding.tvMorphSpeedLabel.text = "Active: 15m Pomodoro"; savePreferences() }
        binding.chipMorph1h.setOnClickListener { engine.morphDurationSeconds = 3600L; binding.tvMorphSpeedLabel.text = "Active: 1h Deep Work"; savePreferences() }
        binding.chipMorph4h.setOnClickListener { engine.morphDurationSeconds = 14400L; binding.tvMorphSpeedLabel.text = "Active: 4h Focus Block"; savePreferences() }
        binding.chipMorph24h.setOnClickListener { engine.morphDurationSeconds = 86400L; binding.tvMorphSpeedLabel.text = "Active: 24h Circadian"; savePreferences() }
    }

    private fun setupBpmControls() {
        binding.tvBpmValue.text = engine.bpm.toString()
        binding.sliderBpm.value = engine.bpm.toFloat().coerceIn(30f, 300f)

        binding.sliderBpm.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                engine.bpm = value.toInt()
                binding.tvBpmValue.text = engine.bpm.toString()
                savePreferences()
            }
        }

        binding.btnBpmMinus.setOnClickListener {
            if (engine.bpm > 30) {
                engine.bpm--
                binding.sliderBpm.value = engine.bpm.toFloat()
                binding.tvBpmValue.text = engine.bpm.toString()
                savePreferences()
            }
        }

        binding.btnBpmPlus.setOnClickListener {
            if (engine.bpm < 300) {
                engine.bpm++
                binding.sliderBpm.value = engine.bpm.toFloat()
                binding.tvBpmValue.text = engine.bpm.toString()
                savePreferences()
            }
        }

        binding.btnTapTempo.setOnClickListener {
            val now = System.currentTimeMillis()
            tapTimestamps.add(now)
            if (tapTimestamps.size > 5) tapTimestamps.removeAt(0)
            if (tapTimestamps.size >= 2) {
                var totalInterval = 0L
                for (i in 1 until tapTimestamps.size) {
                    totalInterval += (tapTimestamps[i] - tapTimestamps[i - 1])
                }
                val avgInterval = totalInterval / (tapTimestamps.size - 1)
                if (avgInterval in 200..2000) {
                    val computedBpm = (60000.0 / avgInterval).toInt().coerceIn(30, 300)
                    engine.bpm = computedBpm
                    binding.sliderBpm.value = computedBpm.toFloat()
                    binding.tvBpmValue.text = computedBpm.toString()
                    savePreferences()
                }
            }
        }
    }

    private fun setupSignatureControls() {
        binding.chipSig2.setOnClickListener { engine.beatsPerBar = 2 }
        binding.chipSig3.setOnClickListener { engine.beatsPerBar = 3 }
        binding.chipSig4.setOnClickListener { engine.beatsPerBar = 4 }
        binding.chipSig6.setOnClickListener { engine.beatsPerBar = 6 }
    }

    private fun setupClockTypeChips() {
        binding.chipClockOff.setOnClickListener {
            engine.clockType = ClockSoundType.OFF
            binding.switchBeatEnabled.isChecked = false
            savePreferences()
        }
        binding.chipClockEtera.setOnClickListener { selectClock(ClockSoundType.ETERA_TICK_TOCK) }
        binding.chipClockGrandfather.setOnClickListener { selectClock(ClockSoundType.GRANDFATHER_CLOCK) }
        binding.chipClockWatch.setOnClickListener { selectClock(ClockSoundType.MECHANICAL_WATCH) }
        binding.chipClockWoodblock.setOnClickListener { selectClock(ClockSoundType.WOODBLOCK) }
        binding.chipClockCowbell.setOnClickListener { selectClock(ClockSoundType.COWBELL) }
        binding.chipClockWaterDrop.setOnClickListener { selectClock(ClockSoundType.WATER_DROP) }
        binding.chipClockHeartbeat.setOnClickListener { selectClock(ClockSoundType.HEARTBEAT) }
        binding.chipClockVintage.setOnClickListener { selectClock(ClockSoundType.VINTAGE_ALARM) }
        binding.chipClockMantle.setOnClickListener { selectClock(ClockSoundType.MANTLE_CLOCK) }
        binding.chipClockClepsydra.setOnClickListener { selectClock(ClockSoundType.WATER_CLOCK_CLEPSYDRA) }
        binding.chipClockFingerSnap.setOnClickListener { selectClock(ClockSoundType.FINGERSNAP) }
        binding.chipClockSonar.setOnClickListener { selectClock(ClockSoundType.SONAR_PING) }
        binding.chipClockTelegraph.setOnClickListener { selectClock(ClockSoundType.TELEGRAPH_KEY) }
        binding.chipClockSingingBowl.setOnClickListener { selectClock(ClockSoundType.SINGING_BOWL) }
        binding.chipClockTowerBell.setOnClickListener { selectClock(ClockSoundType.TOWER_BELL) }
        binding.chipClockDigital.setOnClickListener { selectClock(ClockSoundType.DIGITAL_BEEP) }
        binding.chipClockOrganicTypewriter.setOnClickListener { selectClock(ClockSoundType.ORGANIC_TYPEWRITER) }
        binding.chipClockBookPage.setOnClickListener { selectClock(ClockSoundType.BOOK_PAGE_METRONOME) }
    }

    private fun selectClock(type: ClockSoundType) {
        engine.clockType = type
        binding.switchBeatEnabled.isChecked = true
        engine.muteClock = false
        savePreferences()
    }

    private fun setupNatureChips() {
        binding.chipNatureCrickets.setOnClickListener { selectNature(NoiseType.CRICKETS_NIGHT) }
        binding.chipNatureMeadow.setOnClickListener { selectNature(NoiseType.CRICKETS_DISTANT_MEADOW) }
        binding.chipNatureRain.setOnClickListener { selectNature(NoiseType.RAIN_ON_LEAVES) }
        binding.chipNatureThunder.setOnClickListener { selectNature(NoiseType.THUNDERSTORM) }
        binding.chipNatureBrook.setOnClickListener { selectNature(NoiseType.BROOK_STREAM) }
        binding.chipNatureCampfire.setOnClickListener { selectNature(NoiseType.CAMPFIRE_CRACKLE) }
        binding.chipNatureOcean.setOnClickListener { selectNature(NoiseType.OCEAN_SURF) }
        binding.chipNatureWind.setOnClickListener { selectNature(NoiseType.WIND_IN_TREES) }
        binding.chipNatureCicadas.setOnClickListener { selectNature(NoiseType.SUMMER_CICADAS) }
        binding.chipNatureCave.setOnClickListener { selectNature(NoiseType.DEEP_CAVE_DRIPS) }
        binding.chipNatureFrogs.setOnClickListener { selectNature(NoiseType.NIGHT_FROGS) }
        binding.chipNatureBlizzard.setOnClickListener { selectNature(NoiseType.BLIZZARD_HOWL) }
    }

    private fun selectNature(type: NoiseType) {
        engine.noiseType = type
        binding.switchNatureEnabled.isChecked = true
        engine.muteAmbiance = false
        savePreferences()
    }

    private fun setupKeyboardChips() {
        binding.chipKeyCreamy.setOnClickListener { selectKeyboard(NoiseType.KEYBOARD_CREAMY_LUBED) }
        binding.chipKeyMarbly.setOnClickListener { selectKeyboard(NoiseType.KEYBOARD_MARBLY_JELLY) }
        binding.chipKeyTopre.setOnClickListener { selectKeyboard(NoiseType.KEYBOARD_TOPRE_ELECTRO) }
        binding.chipKeyRed.setOnClickListener { selectKeyboard(NoiseType.KEYBOARD_RED_SWITCH) }
        binding.chipKeyBrown.setOnClickListener { selectKeyboard(NoiseType.KEYBOARD_BROWN_SWITCH) }
        binding.chipKeyBlue.setOnClickListener { selectKeyboard(NoiseType.KEYBOARD_BLUE_SWITCH) }
        binding.chipKeyChiclet.setOnClickListener { selectKeyboard(NoiseType.KEYBOARD_SILENT_CHICLET) }
        binding.chipKeyBuckling.setOnClickListener { selectKeyboard(NoiseType.KEYBOARD_BUCKLING_SPRING) }
    }

    private fun setupIndoorChips() {
        binding.chipIndoorBook.setOnClickListener { selectKeyboard(NoiseType.BOOK_PAGE_FLIPS) }
        binding.chipIndoorCoffee.setOnClickListener { selectKeyboard(NoiseType.COFFEE_SHOP) }
        binding.chipIndoorVinyl.setOnClickListener { selectKeyboard(NoiseType.VINYL_CRACKLE) }
        binding.chipIndoorClockRoom.setOnClickListener { selectKeyboard(NoiseType.CLOCK_ROOM_ANTIQUE) }
        binding.chipIndoorFan.setOnClickListener { selectKeyboard(NoiseType.CEILING_FAN) }
        binding.chipIndoorFireplace.setOnClickListener { selectKeyboard(NoiseType.COZY_FIREPLACE) }
        binding.chipIndoorPencil.setOnClickListener { selectKeyboard(NoiseType.PENCIL_SKETCH) }
    }

    private fun selectKeyboard(type: NoiseType) {
        engine.noiseType = type
        binding.switchIndoorEnabled.isChecked = true
        engine.muteAmbiance = false
        savePreferences()
    }

    private fun setupColorChips() {
        binding.chipColorBrown.setOnClickListener { selectColor(NoiseType.BROWN) }
        binding.chipColorPink.setOnClickListener { selectColor(NoiseType.PINK) }
        binding.chipColorWhite.setOnClickListener { selectColor(NoiseType.WHITE) }
        binding.chipColorGrey.setOnClickListener { selectColor(NoiseType.GREY) }
        binding.chipColorGreen.setOnClickListener { selectColor(NoiseType.GREEN) }
        binding.chipColorBlue.setOnClickListener { selectColor(NoiseType.BLUE) }
        binding.chipColorViolet.setOnClickListener { selectColor(NoiseType.VIOLET) }
        binding.chipColorBlack.setOnClickListener { selectColor(NoiseType.BLACK) }
    }

    private fun selectColor(type: NoiseType) {
        engine.noiseType = type
        binding.switchColorEnabled.isChecked = true
        engine.muteAmbiance = false
        savePreferences()
    }

    private fun setupVolumeAndAcousticSliders() {
        // Clock volume
        binding.sliderClockVolume.value = engine.clockVolume.coerceIn(0f, 1f)
        binding.sliderClockVolume.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                engine.clockVolume = value
                savePreferences()
            }
        }

        // Shared noise volume sliders across tabs
        val syncNoiseVol = { value: Float ->
            engine.noiseVolume = value
            binding.sliderNatureVolume.setSafeValue(value)
            binding.sliderIndoorVolume.setSafeValue(value)
            binding.sliderColorVolume.setSafeValue(value)
            savePreferences()
        }

        binding.sliderNatureVolume.addOnChangeListener { _, value, fromUser -> if (fromUser) syncNoiseVol(value) }
        binding.sliderIndoorVolume.addOnChangeListener { _, value, fromUser -> if (fromUser) syncNoiseVol(value) }
        binding.sliderColorVolume.addOnChangeListener { _, value, fromUser -> if (fromUser) syncNoiseVol(value) }

        // Acoustics
        binding.sliderJitter.value = engine.humanizeJitter.coerceIn(0f, 1f)
        binding.tvJitterValue.text = if (engine.humanizeJitter <= 0.01f) "0% (Robotic)" else "${(engine.humanizeJitter * 100).toInt()}% Humanized"
        binding.sliderJitter.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                engine.humanizeJitter = value
                binding.tvJitterValue.text = if (value <= 0.01f) "0% (Robotic)" else "${(value * 100).toInt()}% Humanized"
                savePreferences()
            }
        }

        binding.sliderPitch.value = engine.pitchMultiplier.coerceIn(0.5f, 2.5f)
        binding.tvPitchValue.text = String.format("%.2fx", engine.pitchMultiplier)
        binding.sliderPitch.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                engine.pitchMultiplier = value
                binding.tvPitchValue.text = String.format("%.2fx", value)
                savePreferences()
            }
        }

        binding.sliderDecay.value = engine.decayMultiplier.coerceIn(0.2f, 3.0f)
        binding.tvDecayValue.text = String.format("%.2fx", engine.decayMultiplier)
        binding.sliderDecay.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                engine.decayMultiplier = value
                binding.tvDecayValue.text = String.format("%.2fx", value)
                savePreferences()
            }
        }

        binding.sliderFilter.value = engine.filterCutoffHz.coerceIn(200f, 20000f)
        binding.tvFilterValue.text = "${engine.filterCutoffHz.toInt()} Hz"
        binding.sliderFilter.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                engine.filterCutoffHz = value
                binding.tvFilterValue.text = "${value.toInt()} Hz"
                savePreferences()
            }
        }
    }

    private fun Slider.setSafeValue(v: Float) {
        val clamped = v.coerceIn(valueFrom, valueTo)
        val step = stepSize
        val target = if (step > 0) {
            val steps = Math.round((clamped - valueFrom) / step)
            (valueFrom + steps * step).coerceIn(valueFrom, valueTo)
        } else {
            clamped
        }
        if (Math.abs(value - target) > 0.0001f) {
            value = target
        }
    }

    private fun setupRandomizerButton() {
        binding.btnRandomize.setOnClickListener {
            try {
                val clockTypes = ClockSoundType.values().filter { it != ClockSoundType.OFF }
                val randomClock = clockTypes[Random.nextInt(clockTypes.size)]
                engine.clockType = randomClock
                binding.switchBeatEnabled.isChecked = true
                engine.muteClock = false

                val noiseTypes = NoiseType.values().filter { it != NoiseType.OFF }
                val randomNoise = noiseTypes[Random.nextInt(noiseTypes.size)]
                engine.noiseType = randomNoise
                engine.muteAmbiance = false

                when (randomNoise.category) {
                    "Nature" -> binding.switchNatureEnabled.isChecked = true
                    "Keyboards", "Indoor" -> binding.switchIndoorEnabled.isChecked = true
                    "Colors" -> binding.switchColorEnabled.isChecked = true
                    else -> binding.switchBiomeEnabled.isChecked = true
                }

                engine.bpm = 40 + Random.nextInt(120)
                binding.sliderBpm.value = engine.bpm.toFloat()
                binding.tvBpmValue.text = engine.bpm.toString()

                val randomFilter = (4000 + Random.nextInt(120) * 100).toFloat()
                engine.filterCutoffHz = randomFilter
                binding.sliderFilter.setSafeValue(randomFilter)
                binding.tvFilterValue.text = "${randomFilter.toInt()} Hz"

                val randomJitterSteps = Random.nextInt(10)
                val randomJitter = (randomJitterSteps * 0.05f).coerceIn(0f, 0.45f)
                engine.humanizeJitter = randomJitter
                binding.sliderJitter.setSafeValue(randomJitter)
                binding.tvJitterValue.text = if (randomJitter <= 0.01f) "0% (Robotic)" else "${(randomJitter * 100).toInt()}% Humanized"

                savePreferences()
            } catch (_: Exception) {}
        }
    }

    private fun setupPlayPauseButton() {
        binding.btnMainPlayPause.setOnClickListener {
            val intent = Intent(this, AudioPlaybackService::class.java).apply {
                action = if (engine.isPlaying) {
                    AudioPlaybackService.ACTION_STOP
                } else {
                    AudioPlaybackService.ACTION_START
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            binding.rootCoordinator.postDelayed({
                updateUiState()
            }, 100)
        }
    }

    private fun animateBeat(beatNumber: Int, isAccented: Boolean) {
        val interpolator = AccelerateDecelerateInterpolator()
        binding.tvBeatCounter.text = beatNumber.toString()
        binding.tvBeatCounter.setTextColor(
            if (isAccented) currentTheme.primaryAccent else ContextCompat.getColor(this, R.color.text_main)
        )

        binding.viewBeatOrbPulse.apply {
            scaleX = if (isAccented) 1.4f else 1.2f
            scaleY = if (isAccented) 1.4f else 1.2f
            alpha = if (isAccented) 0.95f else 0.65f
            animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(0.25f)
                .setDuration(180)
                .setInterpolator(interpolator)
                .start()
        }
    }

    private fun updateUiState() {
        val playing = engine.isPlaying
        if (playing) {
            binding.btnMainPlayPause.text = "⏹ STOP CHRONONOISE"
            binding.btnMainPlayPause.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.accent_pink)
            )
            binding.tvStatusBadge.text = "ACTIVE"
            binding.tvStatusBadge.setTextColor(currentTheme.primaryAccent)
        } else {
            binding.btnMainPlayPause.text = "▶ START CHRONONOISE"
            binding.btnMainPlayPause.backgroundTintList = ColorStateList.valueOf(currentTheme.primaryAccent)
            binding.tvStatusBadge.text = "STANDBY"
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
        }
    }

    private fun savePreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("bpm", engine.bpm)
            putString("clockType", engine.clockType.name)
            putString("noiseType", engine.noiseType.name)
            putBoolean("muteClock", engine.muteClock)
            putBoolean("muteAmbiance", engine.muteAmbiance)
            putFloat("clockVolume", engine.clockVolume)
            putFloat("noiseVolume", engine.noiseVolume)
            putFloat("pitch", engine.pitchMultiplier)
            putFloat("decay", engine.decayMultiplier)
            putFloat("filter", engine.filterCutoffHz)
            putFloat("jitter", engine.humanizeJitter)
            putString("theme", currentTheme.name)
            putBoolean("haptics", isHapticsEnabled)
            putString("bgNoiseType", engine.backgroundNoiseType.name)
            putFloat("bgNoiseVolume", engine.backgroundNoiseVolume)
            apply()
        }
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains("bpm")) return

        engine.bpm = prefs.getInt("bpm", 60)
        val savedClock = prefs.getString("clockType", null)
        if (savedClock != null) {
            try { engine.clockType = ClockSoundType.valueOf(savedClock) } catch (_: Exception) {}
        }
        val savedNoise = prefs.getString("noiseType", null)
        if (savedNoise != null) {
            try { engine.noiseType = NoiseType.valueOf(savedNoise) } catch (_: Exception) {}
        }
        engine.muteClock = prefs.getBoolean("muteClock", false)
        engine.muteAmbiance = prefs.getBoolean("muteAmbiance", false)
        engine.clockVolume = prefs.getFloat("clockVolume", 0.75f)
        engine.noiseVolume = prefs.getFloat("noiseVolume", 0.30f)
        engine.pitchMultiplier = prefs.getFloat("pitch", 1.0f)
        engine.decayMultiplier = prefs.getFloat("decay", 1.0f)
        engine.filterCutoffHz = prefs.getFloat("filter", 12000f)
        engine.humanizeJitter = prefs.getFloat("jitter", 0.0f)

        isHapticsEnabled = prefs.getBoolean("haptics", false)

        val savedTheme = prefs.getString("theme", null)
        if (savedTheme != null) {
            try {
                currentTheme = ThemeType.valueOf(savedTheme)
            } catch (_: Exception) {}
        }

        val savedBg = prefs.getString("bgNoiseType", null)
        if (savedBg != null) {
            try { engine.backgroundNoiseType = NoiseType.valueOf(savedBg) } catch (_: Exception) {}
        }
        engine.backgroundNoiseVolume = prefs.getFloat("bgNoiseVolume", 0.25f)
    }
}
