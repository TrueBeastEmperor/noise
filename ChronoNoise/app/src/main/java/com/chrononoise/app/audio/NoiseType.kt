package com.chrononoise.app.audio

enum class NoiseType(val displayName: String, val description: String, val category: String) {
    OFF(
        "🚫 Off",
        "Silence ambient background noise",
        "None"
    ),

    // 🌀 Dynamic Procedural Morphing Biome
    LIVING_BIOME_DYNAMIC(
        "🌀 Living Biome (Morphing)",
        "Endless evolving nature & indoor sequence: cross-fades seamlessly over seconds to hours",
        "Dynamic"
    ),

    // 🌿 Outdoor Nature & Organic Soundscapes (Trail-Sense Inspired Synthesis)
    CRICKETS_NIGHT(
        "🦗 Summer Night Crickets",
        "Multi-harmonic 4.2-4.8 kHz nocturnal meadow chirps with warm natural cadence",
        "Nature"
    ),
    CRICKETS_DISTANT_MEADOW(
        "🌾 Nocturnal Meadow (Trail-Sense)",
        "Calm atmospheric night field bed (1.8-2.5 kHz banded hum) without close chirps",
        "Nature"
    ),
    OCEAN_SURF(
        "🌊 Ocean Waves (Trail-Sense)",
        "Deep 20-second cubic rolling swell tides with long tranquil troughs and coastal surf",
        "Nature"
    ),
    RAIN_ON_LEAVES(
        "🌧️ Gentle Rain",
        "Shaped pink noise shower with sporadic droplet impacts on foliage",
        "Nature"
    ),
    THUNDERSTORM(
        "⛈️ Distant Thunder",
        "Sub-bass rolling thunderclaps with steady background deluge",
        "Nature"
    ),
    BROOK_STREAM(
        "🏞️ Mountain Brook",
        "Babbling water stream with bubbling resonant liquid ripples",
        "Nature"
    ),
    CAMPFIRE_CRACKLE(
        "🔥 Campfire Crackle",
        "Warm low-frequency hearth rumble punctuated by random wood embers",
        "Nature"
    ),
    WIND_IN_TREES(
        "🍃 Forest Wind",
        "Modulated band-pass breeze rustling through mountain canopies",
        "Nature"
    ),
    SUMMER_CICADAS(
        "🪲 Summer Cicadas",
        "Shimmering high-frequency canopy drone with undulating intensity",
        "Nature"
    ),
    DEEP_CAVE_DRIPS(
        "🕳️ Deep Cave Echoes",
        "Subterranean acoustic chamber with sporadic echoing mineral droplets",
        "Nature"
    ),
    NIGHT_FROGS(
        "🐸 Night Frogs",
        "Rhythmic marshland chorus of nocturnal peepers and reed frogs",
        "Nature"
    ),
    BLIZZARD_HOWL(
        "❄️ Blizzard Howl",
        "Sub-zero whistling gale with resonant wind turbulence",
        "Nature"
    ),

    // ⌨️ Keyboards & Smooth Creamy Switches (Mechanical & Acoustic Modeling)
    KEYBOARD_CREAMY_LUBED(
        "🍦 Creamy Lubed (Buttery Smooth)",
        "Ultra-smooth factory-lubed POM linear switches with rounded creamy pop",
        "Keyboard"
    ),
    KEYBOARD_MARBLY_JELLY(
        "🪨 Marbly Jelly (Poppy Foam)",
        "Custom keyboard with PE foam mod: rounded, poppy raindrop acoustic profile",
        "Keyboard"
    ),
    KEYBOARD_TOPRE_ELECTRO(
        "☁️ Muted Topre (Capacitive)",
        "Buttery smooth rubber dome over capacitive spring: deep cushioned thock-clop",
        "Keyboard"
    ),
    KEYBOARD_RED_SWITCH(
        "⌨️ Red Switch (Linear Thock)",
        "Smooth linear keystroke landing directly with a deep satisfying bass thock",
        "Keyboard"
    ),
    KEYBOARD_BROWN_SWITCH(
        "⌨️ Brown Switch (Tactile)",
        "Gentle tactile bump with muted housing clack for smooth typing flow",
        "Keyboard"
    ),
    KEYBOARD_BLUE_SWITCH(
        "⌨️ Blue Switch (Clicky)",
        "Crisp tactile click leaf snap with resonant bottom-out clack",
        "Keyboard"
    ),
    KEYBOARD_SILENT_CHICLET(
        "💻 Laptop / Chiclet (Silent)",
        "Ultra-quiet low-travel scissor switches with dampened rubber dome impact",
        "Keyboard"
    ),
    KEYBOARD_BUCKLING_SPRING(
        "📟 IBM Model M (Buckling Spring)",
        "Heavy vintage mechanical strike with metallic spring snap and frame ping",
        "Keyboard"
    ),
    KEYBOARD_MECHANICAL(
        "⌨️ Mechanical Study",
        "Rhythmic typing cadence in a quiet study",
        "Keyboard"
    ),

    // 🏠 Indoor & Focus Ambient Soundscapes
    CEILING_FAN(
        "💨 Room Fan (Trail-Sense)",
        "Calibrated 100Hz dual-harmonic electric motor hum with soothing brown noise draft",
        "Indoor"
    ),
    BOOK_PAGE_FLIPS(
        "📖 Book Page Flipping",
        "Crisp paper friction rustle and soft page-turning flutter every few seconds",
        "Indoor"
    ),
    COFFEE_SHOP(
        "☕ Coffee Shop Ambience",
        "Gentle distant café room murmur, soft ceramic cup clinks and espresso steam",
        "Indoor"
    ),
    VINYL_CRACKLE(
        "📻 Vintage Vinyl Record",
        "Warm 33 RPM needle hum, subtle dust crackle and hypnotic groove pops",
        "Indoor"
    ),
    CLOCK_ROOM_ANTIQUE(
        "🕰️ Antique Horology Study",
        "Gentle chorus of multiple distant grandfather, mantle and pendulum clocks",
        "Indoor"
    ),
    COZY_FIREPLACE(
        "🪵 Indoor Hearth Fireplace",
        "Intimate indoor stone fireplace with warm sub-bass rumble and burning ember pops",
        "Indoor"
    ),
    PENCIL_SKETCH(
        "✏️ Pencil on Paper",
        "Textured graphite sketching strokes on heavy cotton parchment",
        "Indoor"
    ),

    // 🌈 Spectral Noise Colors
    BROWN(
        "🟤 Brown (Deep)",
        "1/f² power density, deep soothing low-frequency rumble",
        "Color"
    ),
    PINK(
        "🌸 Pink (Rain)",
        "1/f power density, balanced natural spectrum across all octaves",
        "Color"
    ),
    WHITE(
        "⚪ White (Static)",
        "Equal energy per frequency across the entire audio spectrum",
        "Color"
    ),
    GREY(
        "🔘 Grey (Balanced)",
        "Psychoacoustic inverted A-weighting for perceived equal loudness",
        "Color"
    ),
    GREEN(
        "🟢 Green (Forest Mid)",
        "Mid-frequency energy concentrated around 500Hz, mimicking environmental foliage hum",
        "Color"
    ),
    BLUE(
        "🔵 Blue (Crisp)",
        "+3 dB/octave, airy and crisp high-frequency masking sheen",
        "Color"
    ),
    VIOLET(
        "🟣 Violet (High)",
        "+6 dB/octave, differentiated ultra-crisp high hiss",
        "Color"
    ),
    BLACK(
        "⚫ Black (Sub-Bass)",
        "Deep infrasound rumble below 90Hz for profound somatic relaxation",
        "Color"
    )
}
