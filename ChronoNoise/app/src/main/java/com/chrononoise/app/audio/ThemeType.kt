package com.chrononoise.app.audio

import android.graphics.Color

enum class ThemeType(
    val displayName: String,
    val bgDark: Int,
    val cardDark: Int,
    val cardStroke: Int,
    val primaryAccent: Int,
    val secondaryAccent: Int,
    val textMain: Int,
    val textMuted: Int
) {
    CYBERPUNK(
        "⚡ Cyber Cyan",
        Color.parseColor("#0F172A"),
        Color.parseColor("#1E293B"),
        Color.parseColor("#334155"),
        Color.parseColor("#00E5FF"),
        Color.parseColor("#A855F7"),
        Color.parseColor("#F8FAFC"),
        Color.parseColor("#94A3B8")
    ),
    OLED_MIDNIGHT(
        "🌑 OLED Midnight",
        Color.parseColor("#000000"),
        Color.parseColor("#111111"),
        Color.parseColor("#222222"),
        Color.parseColor("#38BDF8"),
        Color.parseColor("#E2E8F0"),
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#71717A")
    ),
    FOREST_BIO(
        "🌲 Forest Emerald",
        Color.parseColor("#09140E"),
        Color.parseColor("#132319"),
        Color.parseColor("#1E3827"),
        Color.parseColor("#10B981"),
        Color.parseColor("#34D399"),
        Color.parseColor("#ECFDF5"),
        Color.parseColor("#6EE7B7")
    ),
    SUNSET_HORIZON(
        "🌅 Sunset Ember",
        Color.parseColor("#170B16"),
        Color.parseColor("#291325"),
        Color.parseColor("#3F1F39"),
        Color.parseColor("#F59E0B"),
        Color.parseColor("#EC4899"),
        Color.parseColor("#FFF1F2"),
        Color.parseColor("#FDA4AF")
    ),
    TOKYO_VAPOR(
        "🔮 Tokyo Neon",
        Color.parseColor("#0D061A"),
        Color.parseColor("#1C0C38"),
        Color.parseColor("#2D1459"),
        Color.parseColor("#D946EF"),
        Color.parseColor("#818CF8"),
        Color.parseColor("#FAF5FF"),
        Color.parseColor("#C084FC")
    ),
    NORDIC_FROST(
        "❄️ Nordic Frost",
        Color.parseColor("#0B132B"),
        Color.parseColor("#1C2541"),
        Color.parseColor("#3A506B"),
        Color.parseColor("#48CAE4"),
        Color.parseColor("#E0E1DD"),
        Color.parseColor("#F0F8FF"),
        Color.parseColor("#90E0EF")
    ),
    COFFEE_LIBRARY(
        "☕ Coffee & Parchment",
        Color.parseColor("#1A120B"),
        Color.parseColor("#2C1D11"),
        Color.parseColor("#3C2A21"),
        Color.parseColor("#E5BA73"),
        Color.parseColor("#D5CEA3"),
        Color.parseColor("#F5EBE0"),
        Color.parseColor("#C5A880")
    ),
    SOLARIZED_DARK(
        "☀️ Solarized Teal",
        Color.parseColor("#002B36"),
        Color.parseColor("#073642"),
        Color.parseColor("#1B4D58"),
        Color.parseColor("#2AA198"),
        Color.parseColor("#B58900"),
        Color.parseColor("#EEE8D5"),
        Color.parseColor("#93A1A1")
    ),
    DRACULA_NEON(
        "🧛 Dracula Neon",
        Color.parseColor("#191A21"),
        Color.parseColor("#282A36"),
        Color.parseColor("#44475A"),
        Color.parseColor("#50FA7B"),
        Color.parseColor("#FF79C6"),
        Color.parseColor("#F8F8F2"),
        Color.parseColor("#BD93F9")
    ),
    ABYSSAL_OCEAN(
        "🌊 Abyssal Trench",
        Color.parseColor("#03071E"),
        Color.parseColor("#0A1128"),
        Color.parseColor("#1C2A4A"),
        Color.parseColor("#00F5D4"),
        Color.parseColor("#7B2CBF"),
        Color.parseColor("#E0FBFC"),
        Color.parseColor("#5BC0BE")
    )
}
