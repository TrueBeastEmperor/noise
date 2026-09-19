package com.chrononoise.app.audio

enum class ClockSoundType(val displayName: String, val description: String) {
    OFF(
        "🔇 Mute / Off",
        "Silence metronome and clock beats (Ambiance only)"
    ),
    ETERA_TICK_TOCK(
        "⚡ Etera Synthesized",
        "Original 1050Hz / 750Hz dual-frequency decayed sine wave"
    ),
    GRANDFATHER_CLOCK(
        "🕰️ Grandfather Clock",
        "Deep resonant acoustic wooden body tick-tock with sub-harmonics"
    ),
    MECHANICAL_WATCH(
        "⏱️ Watch Escapement",
        "Crisp, rapid pocket-watch balance wheel click"
    ),
    VINTAGE_ALARM(
        "⏰ Vintage Twin-Bell",
        "Spring-driven mechanical clockwork with subtle brass rattle"
    ),
    MANTLE_CLOCK(
        "🕰️ Soft Mantle Clock",
        "Cushioned wooden escapement click with warm acoustic dampening"
    ),
    WOODBLOCK(
        "🪵 Woodblock Metronome",
        "Classic hollow percussive orchestral woodblock with pitch sweep"
    ),
    COWBELL(
        "🔔 Latin Cowbell",
        "Dry metallic percussive cowbell accent metronome"
    ),
    BOOK_PAGE_METRONOME(
        "📖 Book Page Flip Rhythm",
        "Paper sheet friction turn with gentle air flutter metronome"
    ),
    WATER_DROP(
        "💧 Water Droplet",
        "Resonant liquid droplet drip with rapid upward frequency sweep"
    ),
    WATER_CLOCK_CLEPSYDRA(
        "🏺 Ancient Clepsydra",
        "Ancient Hellenic water clock hollow vessel drip and splash"
    ),
    HEARTBEAT(
        "💓 Human Heartbeat",
        "Low-frequency visceral dual 'lub-dub' cardiac metronome"
    ),
    ORGANIC_TYPEWRITER(
        "⌨️ Organic Typewriter",
        "Real-life typing with varied strike pitches, mechanical bounce & carriage bell"
    ),
    FINGERSNAP(
        "🫰 Finger Snap",
        "Crisp acoustic finger snap percussion"
    ),
    SONAR_PING(
        "📡 Submarine Sonar",
        "Nautical sonar acoustic ping with echoing oceanic cavitation"
    ),
    TELEGRAPH_KEY(
        "📻 Morse Telegraph",
        "Mechanical copper contact strike with spring-back rebound"
    ),
    SINGING_BOWL(
        "🥣 Tibetan Singing Bowl",
        "Meditative bronze bell strike transient with shimmering acoustic drone"
    ),
    TOWER_BELL(
        "🔔 Tower Bell Chime",
        "Cathedral tower bell transient chime with metallic sustain"
    ),
    DIGITAL_BEEP(
        "📟 Digital Beep",
        "Crystal clear studio electronic metronome pulse"
    )
}
