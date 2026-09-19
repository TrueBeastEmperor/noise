# ⚡ ChronoNoise: Metronome, Clocks, Nature & Indoor Soundscape Synthesizer

A 100% offline, native Android application combining precision mechanical/synthesized metronome clocks, organic typing simulations, procedural nature atmospheres, indoor ambient environments (book flipping, vinyl, coffee shop), and spectral colored noise. All audio is synthesized in real-time in pure Kotlin using Android's native `AudioTrack` API—requiring zero external audio files, zero latency, and allowing infinite acoustic customization.

---

## 🚀 GitHub Ready: 1-Click Cloud APK Build

The repository is pre-configured and 100% ready to push to GitHub:
```bash
cd "g:\My Drive\Antigravity desktop\ChronoNoise"
git init
git add .
git commit -m "Initial release of ChronoNoise"
git remote add origin https://github.com/YOUR_USERNAME/ChronoNoise.git
git branch -M main
git push -u origin main
```
Upon push, `.github/workflows/build-apk.yml` automatically starts building on GitHub's cloud runners and publishes the downloadable **`ChronoNoise-APK`** in the repository's **Actions** tab in under 2 minutes.

---

## 🎲 Randomizer & Dynamic Evolution

### 🎲 Ambiance-Only Randomizer (`🎲 RANDOMIZE AMBIANCE`)
* Specifically focused on ambient background generation without disrupting your metronome/clock pace:
  * Selects a random outdoor nature soundscape or indoor environment.
  * Preserves your selected Clock / Metronome model, tempo (BPM), and time signature.
  * If the **Organic Typewriter** is active, it randomizes keystroke jitter and typing speed.
  * Automatically balances the tone filter cutoff ($4\text{ kHz} - 16\text{ kHz}$) and ambient volume.

### 🌀 Living Biome (Morphing Sequencer: 15s to 24 Hours)
* A procedural ecosystem cross-fader that smoothly shifts between soundscapes:
  * Seamlessly transitions between rainfall, forest winds, nocturnal crickets, campfire crackle, and cozy book studies.
  * **Configurable Morph Speed**:
    * **15s**: Ultra-fast demo mode
    * **1m / 5m**: Short pomodoro focus session
    * **15m / 1h**: Work & reading sessions
    * **4h / 24h**: Day-long gradual circadian evolution that mirrors natural environmental shifts!

### 🫰 Humanize / Timing Jitter Slider
* Dial in anywhere from **0% (machine-rigid robotic beat)** to **100% (natural human groove with micro-timing fluctuations)** for metronome clicks, organic typing, and finger snaps.

---

## 🎨 10 Modern Aesthetic Themes
* **⚡ Cyber Cyan**: Slate (`#0F172A`) + Electric Cyan (`#00E5FF`) + Magenta
* **🌑 OLED Midnight**: True Pitch Black (`#000000`) + Ice Blue (`#38BDF8`) (maximum battery savings)
* **🌲 Forest Emerald**: Deep Moss (`#09140E`) + Bioluminescent Emerald (`#10B981`) + Mint
* **🌅 Sunset Ember**: Night Burgundy (`#170B16`) + Amber Gold (`#F59E0B`) + Coral Rose
* **🔮 Tokyo Neon**: Deep Indigo (`#0D061A`) + Electric Violet (`#D946EF`) + Neon Lavender
* **❄️ Nordic Frost**: Icy Slate (`#0B132B`) + Frosted Silver (`#E0E1DD`) + Arctic Cyan (`#48CAE4`)
* **☕ Coffee & Parchment**: Roasted Espresso (`#1A120B`) + Aged Paper (`#D5CEA3`) + Warm Amber (`#E5BA73`)
* **☀️ Solarized Teal**: Deep Navy Teal (`#002B36`) + Solar Yellow (`#B58900`) + Cyan (`#2AA198`)
* **🧛 Dracula Neon**: Vampire Purple (`#191A21`) + Radioactive Green (`#50FA7B`) + Orchid Pink (`#FF79C6`)
* **🌊 Abyssal Trench**: Deep Ocean Trench (`#03071E`) + Bioluminescent Teal (`#00F5D4`) + Aqua

---

## 🎧 Complete Procedural Sound Catalog (46 Presets)

### 🕰️ 18 Clock & Metronome Presets
1. **⚡ Etera Synthesized**: Dual-tone $1050\text{ Hz}$ tick / $750\text{ Hz}$ tock with exponential decay.
2. **⌨️ Organic Typewriter**: Dynamic keystroke pitch randomization ($1200 - 1650\text{ Hz}$), spring-rebound click, automatic line-end carriage bell ding + slide.
3. **📖 Book Page Metronome**: Crisp paper sheet friction turn with gentle air flutter metronome.
4. **🕰️ Grandfather Clock**: Deep wooden body tick-tock with sub-harmonics ($240 - 320\text{ Hz}$).
5. **⏱️ Watch Escapement**: High-frequency balance wheel click ($3200 - 3800\text{ Hz}$).
6. **⏰ Vintage Twin-Bell**: Spring-driven clockwork with subtle delayed brass rattle.
7. **🕰️ Soft Mantle Clock**: Cushioned wooden escapement click with warm acoustic dampening.
8. **🪵 Woodblock Metronome**: Orchestral woodblock with pitch sweep.
9. **🔔 Latin Cowbell**: Dry metallic percussive cowbell metronome.
10. **💧 Water Droplet**: Liquid droplet with upward frequency sweep ($650\text{ Hz} \to 1800\text{ Hz}$).
11. **🏺 Ancient Clepsydra**: Hellenic water clock clay amphora drip and hollow splash.
12. **💓 Human Heartbeat**: Dual "lub-dub" cardiac metronome ($55\text{ Hz} + 75\text{ Hz}$).
13. **🫰 Finger Snap**: Acoustic finger snap transient (flesh impact + air burst).
14. **📡 Submarine Sonar**: Nautical sonar pulse with oceanic cavitation echo.
15. **📻 Morse Telegraph**: Mechanical copper contact strike with spring return.
16. **🥣 Tibetan Singing Bowl**: Meditative bronze bell strike with shimmering sustain.
17. **🔔 Tower Bell Chime**: Cathedral tower bell chime transient with metallic harmonics.
18. **📟 Digital Beep**: Studio electronic metronome pulse in a Hann window envelope.

### 🌊 28 Indoor, Nature & Spectral Soundscapes
#### 🏠 Indoor Environments
1. **📖 Book Page Flipping**: Crisp paper friction rustle and soft page-turning flutter every few seconds.
2. **☕ Coffee Shop Ambience**: Muffled café murmur, soft ceramic cup clinks, espresso steam hiss.
3. **📻 Vintage Vinyl Record**: 33 RPM needle warmth, subtle dust crackle, hypnotic groove pops.
4. **🕰️ Antique Horology Study**: Layered chorus of multiple distant soft-ticking clocks in an old library.
5. **💨 Ceiling Fan & Room Drone**: Calming low-frequency motor hum with cyclical blade air displacement.
6. **🪵 Indoor Hearth Fireplace**: Enclosed stone fireplace with warm sub-bass rumble and burning ember pops.
7. **✏️ Pencil on Paper**: Textured graphite sketching strokes on heavy cotton parchment.
8. **⌨️ Mechanical Keyboard Room**: Gentle rhythmic thocky mechanical keystroke clicks.

#### 🌀 Dynamic Morphing
9. **🌀 Living Biome**: Endless evolving nature & indoor sequence cross-fading from 15s to 24h.

#### 🌿 Outdoor Nature Soundscapes
10. **🦗 Night Crickets**: Procedural $4.5\text{ kHz}$ nocturnal field chirps with natural pulsing cadence.
11. **🌧️ Gentle Rain**: Shaped pink noise shower with sporadic droplet impacts on foliage.
12. **⛈️ Distant Thunder**: Sub-bass rolling thunderclaps with steady background deluge.
13. **🏞️ Mountain Brook**: Babbling water stream with bubbling resonant liquid ripples.
14. **🔥 Campfire Crackle**: Warm low rumble punctuated by random wood ember sap pops.
15. **🌊 Ocean Waves**: Rolling tidal Brownian swells with rhythmic $10\text{s}$ coastal surf.
16. **🍃 Forest Wind**: Modulated band-pass breeze rustling through mountain tree canopies.
17. **🪲 Summer Cicadas**: Shimmering high-frequency canopy drone with undulating intensity.
18. **🕳️ Deep Cave Echoes**: Subterranean acoustic chamber with sporadic echoing mineral droplets.
19. **🐸 Night Frogs**: Rhythmic marshland chorus of nocturnal peepers and reed frogs.
20. **❄️ Blizzard Howl**: Sub-zero whistling gale with resonant wind turbulence.

#### 🌈 Spectral Noise Colors
21. **🟤 Brown (Brownian)**: $1/f^2$ deep soothing low rumble via leaky integration.
22. **🌸 Pink**: $1/f$ balanced natural spectrum across all octaves.
23. **⚪ White**: Equal energy across the entire audio spectrum ($20\text{ Hz} - 20\text{ kHz}$).
24. **🔘 Grey**: Psychoacoustic inverted A-weighting for perceived equal loudness across all human hearing.
25. **🟢 Green**: Mid-frequency energy concentrated around $500\text{ Hz}$ (foliage/forest hum).
26. **🔵 Blue**: $+3\text{ dB/octave}$ airy and crisp high-frequency masking sheen.
27. **🟣 Violet**: $+6\text{ dB/octave}$ differentiated ultra-crisp high hiss.
28. **⚫ Black**: Deep infrasound rumble below $90\text{ Hz}$ for profound somatic relaxation.
29. **🚫 Off**: Clock / metronome only.

---

## 🌐 Instant Web Preview
Open [`web_preview/index.html`](file:///g:/My%20Drive/Antigravity%20desktop/ChronoNoise/web_preview/index.html) in any browser to test all 18 clocks, 28 soundscapes, the ambiance-only randomizer, morph speeds, and 10 modern themes right now.
