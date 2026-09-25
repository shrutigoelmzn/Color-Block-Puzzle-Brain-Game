package com.example.domain.model

import androidx.compose.ui.graphics.Color

enum class BlockStyle(val id: String, val displayName: String) {
    JEWEL("jewel", "Jewel"),
    GLOSSY("glossy", "Glossy"),
    WOODEN("wooden", "Wooden Box"),
    FROSTED_GLASS("frosted_glass", "Frosted Glass"),
    CYBER_NEON("cyber_neon", "Cyber Neon"),
    STONE("stone", "Stone Block"),
    GOLDEN("golden", "Golden Ingot")
}

enum class AppThemeMode(val id: String, val displayName: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromId(id: String): AppThemeMode = entries.find { it.id == id } ?: SYSTEM
    }
}

data class BlockThemeColors(
    val main: Color,
    val light: Color,
    val dark: Color,
    val glow: Color
)

data class GameTheme(
    val id: String,
    val name: String,
    val price: Int,
    val isDark: Boolean = true,
    val blockStyle: BlockStyle = BlockStyle.GLOSSY,
    val bgGradientStart: Color,
    val bgGradientEnd: Color,
    val boardBg: Color,
    val gridBorder: Color,
    val cellEmptyBg: Color,
    val cardBg: Color = boardBg,
    val cardBorder: Color = gridBorder,
    val textColorPrimary: Color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
    val textColorSecondary: Color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
    val accentColor: Color = Color(0xFF4F46E5),
    val onAccentColor: Color = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF),
    val blockPalettes: List<BlockThemeColors>
) {
    fun getBlockColor(colorId: Int): BlockThemeColors {
        val safeIndex = (colorId % blockPalettes.size + blockPalettes.size) % blockPalettes.size
        return blockPalettes[safeIndex]
    }

    fun forMode(isDarkMode: Boolean): GameTheme {
        return when (id) {
            "classic" -> if (isDarkMode) CLASSIC_DARK else CLASSIC_LIGHT
            "neon" -> if (isDarkMode) NEON_DARK else NEON_LIGHT
            "candy" -> if (isDarkMode) CANDY_DARK else CANDY_LIGHT
            "wood" -> if (isDarkMode) WOOD_DARK else WOOD_LIGHT
            "glass" -> if (isDarkMode) GLASS_DARK else GLASS_LIGHT
            "pastel" -> if (isDarkMode) PASTEL_DARK else PASTEL_LIGHT
            "dark_space" -> if (isDarkMode) DARK_SPACE_DARK else DARK_SPACE_LIGHT
            "stone" -> if (isDarkMode) STONE_DARK else STONE_LIGHT
            "golden" -> if (isDarkMode) GOLDEN_DARK else GOLDEN_LIGHT
            else -> if (isDarkMode) CLASSIC_DARK else CLASSIC_LIGHT
        }
    }

    companion object {
        // ----------------------------------------------------
        // 1. CLASSIC JEWEL
        // ----------------------------------------------------
        val CLASSIC_LIGHT = GameTheme(
            id = "classic",
            name = "Classic Jewel",
            price = 0,
            isDark = false,
            bgGradientStart = Color(0xFFF8FAFC),
            bgGradientEnd = Color(0xFFE2E8F0),
            boardBg = Color(0xFFFFFFFF),
            gridBorder = Color(0xFFCBD5E1),
            cellEmptyBg = Color(0xFFF1F5F9),
            cardBg = Color(0xFFFFFFFF),
            cardBorder = Color(0xFFE2E8F0),
            textColorPrimary = Color(0xFF0F172A),
            textColorSecondary = Color(0xFF64748B),
            accentColor = Color(0xFF4F46E5),
            blockPalettes = listOf(
                BlockThemeColors(Color(0xFFF59E0B), Color(0xFFFDE68A), Color(0xFFD97706), Color(0x66F59E0B)),
                BlockThemeColors(Color(0xFF06B6D4), Color(0xFF67E8F9), Color(0xFF0891B2), Color(0x6606B6D4)),
                BlockThemeColors(Color(0xFFEF4444), Color(0xFFFCA5A5), Color(0xFFDC2626), Color(0x66EF4444)),
                BlockThemeColors(Color(0xFF10B981), Color(0xFF6EE7B7), Color(0xFF059669), Color(0x6610B981)),
                BlockThemeColors(Color(0xFF8B5CF6), Color(0xFFC4B5FD), Color(0xFF7C3AED), Color(0x668B5CF6)),
                BlockThemeColors(Color(0xFFF97316), Color(0xFFFDBA74), Color(0xFFEA580C), Color(0x66F97316)),
                BlockThemeColors(Color(0xFF3B82F6), Color(0xFF93C5FD), Color(0xFF2563EB), Color(0x663B82F6))
            )
        )

        val CLASSIC_DARK = GameTheme(
            id = "classic",
            name = "Classic Jewel",
            price = 0,
            isDark = true,
            bgGradientStart = Color(0xFF0F172A),
            bgGradientEnd = Color(0xFF020617),
            boardBg = Color(0xFF1E293B),
            gridBorder = Color(0xFF334155),
            cellEmptyBg = Color(0xFF0F172A),
            cardBg = Color(0xFF1E293B),
            cardBorder = Color(0xFF334155),
            textColorPrimary = Color(0xFFF8FAFC),
            textColorSecondary = Color(0xFF94A3B8),
            accentColor = Color(0xFF38BDF8),
            onAccentColor = Color(0xFF082F49),
            blockPalettes = listOf(
                BlockThemeColors(Color(0xFFFFB300), Color(0xFFFFE082), Color(0xFFFF8F00), Color(0x66FFB300)),
                BlockThemeColors(Color(0xFF00BCD4), Color(0xFF80DEEA), Color(0xFF0097A7), Color(0x6600BCD4)),
                BlockThemeColors(Color(0xFFFF5252), Color(0xFFFF8A80), Color(0xFFD32F2F), Color(0x66FF5252)),
                BlockThemeColors(Color(0xFF4CAF50), Color(0xFFA5D6A7), Color(0xFF2E7D32), Color(0x664CAF50)),
                BlockThemeColors(Color(0xFFAB47BC), Color(0xFFE1BEE7), Color(0xFF7B1FA2), Color(0x66AB47BC)),
                BlockThemeColors(Color(0xFFFF7043), Color(0xFFFFAB91), Color(0xFFE64A19), Color(0x66FF7043)),
                BlockThemeColors(Color(0xFF29B6F6), Color(0xFF81D4FA), Color(0xFF0288D1), Color(0x6629B6F6))
            )
        )

        // ----------------------------------------------------
        // 2. CYBER NEON
        // ----------------------------------------------------
        val NEON_LIGHT = GameTheme(
            id = "neon",
            name = "Cyber Neon",
            price = 200,
            isDark = false,
            blockStyle = BlockStyle.CYBER_NEON,
            bgGradientStart = Color(0xFFF5F3FF),
            bgGradientEnd = Color(0xFFEDE9FE),
            boardBg = Color(0xFFFFFFFF),
            gridBorder = Color(0xFFDDD6FE),
            cellEmptyBg = Color(0xFFF5F3FF),
            cardBg = Color(0xFFFFFFFF),
            cardBorder = Color(0xFFDDD6FE),
            textColorPrimary = Color(0xFF1E1B4B),
            textColorSecondary = Color(0xFF6B21A8),
            accentColor = Color(0xFF7C3AED),
            blockPalettes = listOf(
                BlockThemeColors(Color(0xFF00B4D8), Color(0xFF90E0EF), Color(0xFF0077B6), Color(0x8000B4D8)),
                BlockThemeColors(Color(0xFFF72585), Color(0xFFFF70A6), Color(0xFFB5179E), Color(0x80F72585)),
                BlockThemeColors(Color(0xFF10B981), Color(0xFF6EE7B7), Color(0xFF047857), Color(0x8010B981)),
                BlockThemeColors(Color(0xFFF59E0B), Color(0xFFFDE68A), Color(0xFFD97706), Color(0x80F59E0B)),
                BlockThemeColors(Color(0xFF7209B7), Color(0xFFB5179E), Color(0xFF3F37C9), Color(0x807209B7)),
                BlockThemeColors(Color(0xFFFF6B35), Color(0xFFFFA07A), Color(0xFFE85D04), Color(0x80FF6B35)),
                BlockThemeColors(Color(0xFF06D6A0), Color(0xFF8CEFD5), Color(0xFF049B74), Color(0x8006D6A0))
            )
        )

        val NEON_DARK = GameTheme(
            id = "neon",
            name = "Cyber Neon",
            price = 200,
            isDark = true,
            blockStyle = BlockStyle.CYBER_NEON,
            bgGradientStart = Color(0xFF0B001A),
            bgGradientEnd = Color(0xFF190033),
            boardBg = Color(0xFF150826),
            gridBorder = Color(0xFF3D1B69),
            cellEmptyBg = Color(0xFF0D021C),
            cardBg = Color(0xFF150826),
            cardBorder = Color(0xFF3D1B69),
            textColorPrimary = Color(0xFFF3E8FF),
            textColorSecondary = Color(0xFFC084FC),
            accentColor = Color(0xFF00F0FF),
            onAccentColor = Color(0xFF002930),
            blockPalettes = listOf(
                BlockThemeColors(Color(0xFF00F0FF), Color(0xFFB3FBFF), Color(0xFF009BB3), Color(0x9900F0FF)),
                BlockThemeColors(Color(0xFFFF0055), Color(0xFFFF99BB), Color(0xFFB3003B), Color(0x99FF0055)),
                BlockThemeColors(Color(0xFF39FF14), Color(0xFFB0FFA0), Color(0xFF24A80D), Color(0x9939FF14)),
                BlockThemeColors(Color(0xFFFFEE00), Color(0xFFFFF9B3), Color(0xFFB3A700), Color(0x99FFEE00)),
                BlockThemeColors(Color(0xFFBD00FF), Color(0xFFE699FF), Color(0xFF7A00B3), Color(0x99BD00FF)),
                BlockThemeColors(Color(0xFFFF7700), Color(0xFFFFC28C), Color(0xFFB35300), Color(0x99FF7700)),
                BlockThemeColors(Color(0xFF00FFCC), Color(0xFFB3FFF0), Color(0xFF00B38F), Color(0x9900FFCC))
            )
        )

        // ----------------------------------------------------
        // 3. SWEET CANDY
        // ----------------------------------------------------
        val CANDY_LIGHT = GameTheme(
            id = "candy",
            name = "Sweet Candy",
            price = 300,
            isDark = false,
            bgGradientStart = Color(0xFFFFF1F2),
            bgGradientEnd = Color(0xFFFFE4E6),
            boardBg = Color(0xFFFFFFFF),
            gridBorder = Color(0xFFFECDD3),
            cellEmptyBg = Color(0xFFFFF5F7),
            cardBg = Color(0xFFFFFFFF),
            cardBorder = Color(0xFFFECDD3),
            textColorPrimary = Color(0xFF4C0519),
            textColorSecondary = Color(0xFF9F1239),
            accentColor = Color(0xFFF43F5E),
            onAccentColor = Color.White,
            blockPalettes = listOf(
                BlockThemeColors(Color(0xFFFB7185), Color(0xFFFECDD3), Color(0xFFE11D48), Color(0x66FB7185)),
                BlockThemeColors(Color(0xFFFBBF24), Color(0xFFFDE68A), Color(0xFFD97706), Color(0x66FBBF24)),
                BlockThemeColors(Color(0xFF34D399), Color(0xFFA7F3D0), Color(0xFF059669), Color(0x6634D399)),
                BlockThemeColors(Color(0xFF38BDF8), Color(0xFFBAE6FD), Color(0xFF0284C7), Color(0x6638BDF8)),
                BlockThemeColors(Color(0xFFA78BFA), Color(0xFFDDD6FE), Color(0xFF7C3AED), Color(0x66A78BFA)),
                BlockThemeColors(Color(0xFFF472B6), Color(0xFFFBCFE8), Color(0xFFDB2777), Color(0x66F472B6)),
                BlockThemeColors(Color(0xFF67E8F9), Color(0xFFCFFAFE), Color(0xFF0891B2), Color(0x6667E8F9))
            )
        )

        val CANDY_DARK = GameTheme(
            id = "candy",
            name = "Sweet Candy",
            price = 300,
            isDark = true,
            bgGradientStart = Color(0xFF3A1C3C),
            bgGradientEnd = Color(0xFF240E26),
            boardBg = Color(0xFF4A254D),
            gridBorder = Color(0xFF6B366F),
            cellEmptyBg = Color(0xFF2D1230),
            cardBg = Color(0xFF4A254D),
            cardBorder = Color(0xFF6B366F),
            textColorPrimary = Color(0xFFFFE4EC),
            textColorSecondary = Color(0xFFF472B6),
            accentColor = Color(0xFFFF6584),
            onAccentColor = Color(0xFF380010),
            blockPalettes = listOf(
                BlockThemeColors(Color(0xFFFF6584), Color(0xFFFFBAC7), Color(0xFFC73D5A), Color(0x66FF6584)),
                BlockThemeColors(Color(0xFFFFD166), Color(0xFFFFF0C2), Color(0xFFD4A02A), Color(0x66FFD166)),
                BlockThemeColors(Color(0xFF06D6A0), Color(0xFF8CEFD5), Color(0xFF049B74), Color(0x6606D6A0)),
                BlockThemeColors(Color(0xFF118AB2), Color(0xFF7AC9E2), Color(0xFF0B5E7A), Color(0x66118AB2)),
                BlockThemeColors(Color(0xFF9D4EDD), Color(0xFFD6A9F7), Color(0xFF6B25A6), Color(0x669D4EDD)),
                BlockThemeColors(Color(0xFFF72585), Color(0xFFFA90C2), Color(0xFFB30B58), Color(0x66F72585)),
                BlockThemeColors(Color(0xFF4CC9F0), Color(0xFFA5E6F8), Color(0xFF1D9CC3), Color(0x664CC9F0))
            )
        )

        // ----------------------------------------------------
        // 4. WARM TIMBER
        // ----------------------------------------------------
        val WOOD_LIGHT = GameTheme(
            id = "wood",
            name = "Warm Timber",
            price = 400,
            isDark = false,
            blockStyle = BlockStyle.WOODEN,
            bgGradientStart = Color(0xFFFDFBF7),
            bgGradientEnd = Color(0xFFF3EDE2),
            boardBg = Color(0xFFFFFFFF),
            gridBorder = Color(0xFFE5D5C5),
            cellEmptyBg = Color(0xFFF6EFE6),
            cardBg = Color(0xFFFFFFFF),
            cardBorder = Color(0xFFE5D5C5),
            textColorPrimary = Color(0xFF3E2723),
            textColorSecondary = Color(0xFF795548),
            accentColor = Color(0xFFB45309),
            blockPalettes = listOf(
                BlockThemeColors(Color(0xFFD97706), Color(0xFFFDE68A), Color(0xFFB45309), Color(0x66D97706)),
                BlockThemeColors(Color(0xFFCD853F), Color(0xFFE8C8A3), Color(0xFF9E5B1E), Color(0x66CD853F)),
                BlockThemeColors(Color(0xFF8B5A2B), Color(0xFFBA8A5E), Color(0xFF5E3A18), Color(0x668B5A2B)),
                BlockThemeColors(Color(0xFF65A30D), Color(0xFFBEF264), Color(0xFF4D7C0F), Color(0x6665A30D)),
                BlockThemeColors(Color(0xFFCA8A04), Color(0xFFFEF08A), Color(0xFFA16207), Color(0x66CA8A04)),
                BlockThemeColors(Color(0xFFC2410C), Color(0xFFFFEDD5), Color(0xFF9A3412), Color(0x66C2410C)),
                BlockThemeColors(Color(0xFF996B5B), Color(0xFFC7A89D), Color(0xFF6B4538), Color(0x66996B5B))
            )
        )

        val WOOD_DARK = GameTheme(
            id = "wood",
            name = "Warm Timber",
            price = 400,
            isDark = true,
            blockStyle = BlockStyle.WOODEN,
            bgGradientStart = Color(0xFF2C1810),
            bgGradientEnd = Color(0xFF180D08),
            boardBg = Color(0xFF3D2314),
            gridBorder = Color(0xFF5E3620),
            cellEmptyBg = Color(0xFF21110A),
            cardBg = Color(0xFF3D2314),
            cardBorder = Color(0xFF5E3620),
            textColorPrimary = Color(0xFFFDE8D7),
            textColorSecondary = Color(0xFFD7A888),
            accentColor = Color(0xFFCD853F),
            onAccentColor = Color(0xFF241005),
            blockPalettes = listOf(
                BlockThemeColors(Color(0xFFCD853F), Color(0xFFE3B888), Color(0xFF8B5A2B), Color(0x66CD853F)),
                BlockThemeColors(Color(0xFFDEB887), Color(0xFFEED7BA), Color(0xFFB8860B), Color(0x66DEB887)),
                BlockThemeColors(Color(0xFFD2691E), Color(0xFFE59C66), Color(0xFF8B4513), Color(0x66D2691E)),
                BlockThemeColors(Color(0xFF8FBC8F), Color(0xFFC2DCC2), Color(0xFF556B2F), Color(0x668FBC8F)),
                BlockThemeColors(Color(0xFFBDB76B), Color(0xFFDCD8A7), Color(0xFF6E6B34), Color(0x66BDB76B)),
                BlockThemeColors(Color(0xFFA0522D), Color(0xFFCA8766), Color(0xFF5A2C17), Color(0x66A0522D)),
                BlockThemeColors(Color(0xFFBC8F8F), Color(0xFFD8BEBE), Color(0xFF7A5757), Color(0x66BC8F8F))
            )
        )

        // ----------------------------------------------------
        // 5. FROSTED GLASS
        // ----------------------------------------------------
        val GLASS_LIGHT = GameTheme(
            id = "glass",
            name = "Frosted Glass",
            price = 500,
            isDark = false,
            blockStyle = BlockStyle.FROSTED_GLASS,
            bgGradientStart = Color(0xFFF0F9FF),
            bgGradientEnd = Color(0xFFE0F2FE),
            boardBg = Color(0xFFFFFFFF),
            gridBorder = Color(0xFFBAE6FD),
            cellEmptyBg = Color(0xFFF0F9FF),
            cardBg = Color(0xFFFFFFFF),
            cardBorder = Color(0xFFBAE6FD),
            textColorPrimary = Color(0xFF0C4A6E),
            textColorSecondary = Color(0xFF0369A1),
            accentColor = Color(0xFF0284C7),
            blockPalettes = listOf(
                BlockThemeColors(Color(0xFF0284C7), Color(0xFF7DD3FC), Color(0xFF0369A1), Color(0x660284C7)),
                BlockThemeColors(Color(0xFF06B6D4), Color(0xFF67E8F9), Color(0xFF0891B2), Color(0x6606B6D4)),
                BlockThemeColors(Color(0xFF10B981), Color(0xFF6EE7B7), Color(0xFF059669), Color(0x6610B981)),
                BlockThemeColors(Color(0xFFF59E0B), Color(0xFFFDE68A), Color(0xFFD97706), Color(0x66F59E0B)),
                BlockThemeColors(Color(0xFF8B5CF6), Color(0xFFC4B5FD), Color(0xFF7C3AED), Color(0x668B5CF6)),
                BlockThemeColors(Color(0xFFF97316), Color(0xFFFDBA74), Color(0xFFEA580C), Color(0x66F97316)),
                BlockThemeColors(Color(0xFF14B8A6), Color(0xFF5EEAD4), Color(0xFF0F766E), Color(0x6614B8A6))
            )
        )

        val GLASS_DARK = GameTheme(
            id = "glass",
            name = "Frosted Glass",
            price = 500,
            isDark = true,
            blockStyle = BlockStyle.FROSTED_GLASS,
            bgGradientStart = Color(0xFF102A43),
            bgGradientEnd = Color(0xFF0B1B2B),
            boardBg = Color(0xFF1A365D),
            gridBorder = Color(0xFF2B4C7E),
            cellEmptyBg = Color(0xFF0F223D),
            cardBg = Color(0xFF1A365D),
            cardBorder = Color(0xFF2B4C7E),
            textColorPrimary = Color(0xFFE0F2FE),
            textColorSecondary = Color(0xFF7DD3FC),
            accentColor = Color(0xFF38BDF8),
            onAccentColor = Color(0xFF082F49),
            blockPalettes = listOf(
                BlockThemeColors(Color(0xFF64B5F6), Color(0xFFBBDEFB), Color(0xFF1976D2), Color(0x6664B5F6)),
                BlockThemeColors(Color(0xFF4DD0E1), Color(0xFFB2EBF2), Color(0xFF0097A7), Color(0x664DD0E1)),
                BlockThemeColors(Color(0xFF81C784), Color(0xFFC8E6C9), Color(0xFF388E3C), Color(0x6681C784)),
                BlockThemeColors(Color(0xFFFFD54F), Color(0xFFFFF9C4), Color(0xFFFFA000), Color(0x66FFD54F)),
                BlockThemeColors(Color(0xFFBA68C8), Color(0xFFE1BEE7), Color(0xFF7B1FA2), Color(0x66BA68C8)),
                BlockThemeColors(Color(0xFFFF8A65), Color(0xFFFFCCBC), Color(0xFFD84315), Color(0x66FF8A65)),
                BlockThemeColors(Color(0xFF4DB6AC), Color(0xFFB2DFDB), Color(0xFF00796B), Color(0x664DB6AC))
            )
        )

        // ----------------------------------------------------
        // 6. ZEN PASTEL
        // ----------------------------------------------------
        val PASTEL_LIGHT = GameTheme(
            id = "pastel",
            name = "Zen Pastel",
            price = 600,
            isDark = false,
            bgGradientStart = Color(0xFFF8FAFC),
            bgGradientEnd = Color(0xFFF1F5F9),
            boardBg = Color(0xFFFFFFFF),
            gridBorder = Color(0xFFE2E8F0),
            cellEmptyBg = Color(0xFFF8FAFC),
            cardBg = Color(0xFFFFFFFF),
            cardBorder = Color(0xFFE2E8F0),
            textColorPrimary = Color(0xFF1E293B),
            textColorSecondary = Color(0xFF64748B),
            accentColor = Color(0xFF6366F1),
            blockPalettes = listOf(
                BlockThemeColors(Color(0xFFFDA4AF), Color(0xFFFEE2E2), Color(0xFFF43F5E), Color(0x66FDA4AF)),
                BlockThemeColors(Color(0xFFFED7AA), Color(0xFFFFF7ED), Color(0xFFF97316), Color(0x66FED7AA)),
                BlockThemeColors(Color(0xFFBBF7D0), Color(0xFFDCFCE7), Color(0xFF22C55E), Color(0x66BBF7D0)),
                BlockThemeColors(Color(0xFF99F6E4), Color(0xFFCCFBF1), Color(0xFF14B8A6), Color(0x6699F6E4)),
                BlockThemeColors(Color(0xFFC7D2FE), Color(0xFFE0E7FF), Color(0xFF6366F1), Color(0x66C7D2FE)),
                BlockThemeColors(Color(0xFFE9D5FF), Color(0xFFF3E8FF), Color(0xFFA855F7), Color(0x66E9D5FF)),
                BlockThemeColors(Color(0xFFFEF08A), Color(0xFFFEF9C3), Color(0xFFEAB308), Color(0x66FEF08A))
            )
        )

        val PASTEL_DARK = GameTheme(
            id = "pastel",
            name = "Zen Pastel",
            price = 600,
            isDark = true,
            bgGradientStart = Color(0xFF232931),
            bgGradientEnd = Color(0xFF181D23),
            boardBg = Color(0xFF323B47),
            gridBorder = Color(0xFF4E5D6E),
            cellEmptyBg = Color(0xFF20262E),
            cardBg = Color(0xFF323B47),
            cardBorder = Color(0xFF4E5D6E),
            textColorPrimary = Color(0xFFF1F5F9),
            textColorSecondary = Color(0xFF94A3B8),
            accentColor = Color(0xFFA5B4FC),
            onAccentColor = Color(0xFF1E1B4B),
            blockPalettes = listOf(
                BlockThemeColors(Color(0xFFFFB7B2), Color(0xFFFFD9D6), Color(0xFFD97E78), Color(0x66FFB7B2)),
                BlockThemeColors(Color(0xFFFFDAC1), Color(0xFFFFEDE0), Color(0xFFD9A685), Color(0x66FFDAC1)),
                BlockThemeColors(Color(0xFFE2F0CB), Color(0xFFF1F8E6), Color(0xFFABC08A), Color(0x66E2F0CB)),
                BlockThemeColors(Color(0xFFB5EAD7), Color(0xFFDCF6EE), Color(0xFF7CBDAB), Color(0x66B5EAD7)),
                BlockThemeColors(Color(0xFFC7CEEA), Color(0xFFE4E8F5), Color(0xFF919EC5), Color(0x66C7CEEA)),
                BlockThemeColors(Color(0xFFE8D7F1), Color(0xFFF4ECF8), Color(0xFFBFA7CB), Color(0x66E8D7F1)),
                BlockThemeColors(Color(0xFFFFF1C5), Color(0xFFFFF9E4), Color(0xFFD7BE7D), Color(0x66FFF1C5))
            )
        )

        // ----------------------------------------------------
        // 7. DARK SPACE
        // ----------------------------------------------------
        val DARK_SPACE_LIGHT = GameTheme(
            id = "dark_space",
            name = "Cosmic Nebula",
            price = 800,
            isDark = false,
            bgGradientStart = Color(0xFFF3F4F6),
            bgGradientEnd = Color(0xFFE5E7EB),
            boardBg = Color(0xFFFFFFFF),
            gridBorder = Color(0xFFD1D5DB),
            cellEmptyBg = Color(0xFFF3F4F6),
            cardBg = Color(0xFFFFFFFF),
            cardBorder = Color(0xFFD1D5DB),
            textColorPrimary = Color(0xFF111827),
            textColorSecondary = Color(0xFF4B5563),
            accentColor = Color(0xFF7C3AED),
            blockPalettes = listOf(
                BlockThemeColors(Color(0xFF7C3AED), Color(0xFFA78BFA), Color(0xFF5B21B6), Color(0x807C3AED)),
                BlockThemeColors(Color(0xFF06B6D4), Color(0xFF67E8F9), Color(0xFF0891B2), Color(0x8006B6D4)),
                BlockThemeColors(Color(0xFFEC4899), Color(0xFFF472B6), Color(0xFFBE185D), Color(0x80EC4899)),
                BlockThemeColors(Color(0xFFF59E0B), Color(0xFFFBBF24), Color(0xFFB45309), Color(0x80F59E0B)),
                BlockThemeColors(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF047857), Color(0x8010B981)),
                BlockThemeColors(Color(0xFFEF4444), Color(0xFFF87171), Color(0xFFB91C1C), Color(0x80EF4444)),
                BlockThemeColors(Color(0xFF8B5CF6), Color(0xFFC4B5FD), Color(0xFF6D28D9), Color(0x808B5CF6))
            )
        )

        val DARK_SPACE_DARK = GameTheme(
            id = "dark_space",
            name = "Dark Space",
            price = 800,
            isDark = true,
            bgGradientStart = Color(0xFF05050A),
            bgGradientEnd = Color(0xFF0A0815),
            boardBg = Color(0xFF101020),
            gridBorder = Color(0xFF202040),
            cellEmptyBg = Color(0xFF080812),
            cardBg = Color(0xFF101020),
            cardBorder = Color(0xFF202040),
            textColorPrimary = Color(0xFFF8FAFC),
            textColorSecondary = Color(0xFFA78BFA),
            accentColor = Color(0xFF00E5FF),
            onAccentColor = Color(0xFF002229),
            blockPalettes = listOf(
                BlockThemeColors(Color(0xFF7000FF), Color(0xFFB066FF), Color(0xFF45009E), Color(0x997000FF)),
                BlockThemeColors(Color(0xFF00E5FF), Color(0xFF80F2FF), Color(0xFF008FA0), Color(0x9900E5FF)),
                BlockThemeColors(Color(0xFFFF007F), Color(0xFFFF66B3), Color(0xFF99004C), Color(0x99FF007F)),
                BlockThemeColors(Color(0xFFFFB300), Color(0xFFFFD54F), Color(0xFFC68400), Color(0x99FFB300)),
                BlockThemeColors(Color(0xFF00FF88), Color(0xFF80FFC4), Color(0xFF009952), Color(0x9900FF88)),
                BlockThemeColors(Color(0xFFFF3300), Color(0xFFFF8566), Color(0xFF991F00), Color(0x99FF3300)),
                BlockThemeColors(Color(0xFF8C00FF), Color(0xFFBD66FF), Color(0xFF550099), Color(0x998C00FF))
            )
        )

        // ----------------------------------------------------
        // 8. RUGGED STONE
        // ----------------------------------------------------
        val STONE_LIGHT = GameTheme(
            id = "stone",
            name = "Ancient Stone",
            price = 450,
            isDark = false,
            blockStyle = BlockStyle.STONE,
            bgGradientStart = Color(0xFFF1F5F9),
            bgGradientEnd = Color(0xFFE2E8F0),
            boardBg = Color(0xFFFFFFFF),
            gridBorder = Color(0xFF94A3B8),
            cellEmptyBg = Color(0xFFF1F5F9),
            cardBg = Color(0xFFFFFFFF),
            cardBorder = Color(0xFF94A3B8),
            textColorPrimary = Color(0xFF1E293B),
            textColorSecondary = Color(0xFF475569),
            accentColor = Color(0xFF64748B),
            onAccentColor = Color.White,
            blockPalettes = listOf(
                BlockThemeColors(Color(0xFF64748B), Color(0xFF94A3B8), Color(0xFF334155), Color(0x6664748B)),
                BlockThemeColors(Color(0xFF78716C), Color(0xFFA8A29E), Color(0xFF44403C), Color(0x6678716C)),
                BlockThemeColors(Color(0xFF475569), Color(0xFF64748B), Color(0xFF1E293B), Color(0x66475569)),
                BlockThemeColors(Color(0xFF0D9488), Color(0xFF2DD4BF), Color(0xFF115E59), Color(0x660D9488)),
                BlockThemeColors(Color(0xFFB45309), Color(0xFFF59E0B), Color(0xFF78350F), Color(0x66B45309)),
                BlockThemeColors(Color(0xFF9333EA), Color(0xFFC084FC), Color(0xFF6B21A8), Color(0x669333EA)),
                BlockThemeColors(Color(0xFFBE123C), Color(0xFFFB7185), Color(0xFF881337), Color(0x66BE123C))
            )
        )

        val STONE_DARK = GameTheme(
            id = "stone",
            name = "Ancient Stone",
            price = 450,
            isDark = true,
            blockStyle = BlockStyle.STONE,
            bgGradientStart = Color(0xFF1E242B),
            bgGradientEnd = Color(0xFF14181E),
            boardBg = Color(0xFF262E38),
            gridBorder = Color(0xFF434E5D),
            cellEmptyBg = Color(0xFF181D24),
            cardBg = Color(0xFF262E38),
            cardBorder = Color(0xFF434E5D),
            textColorPrimary = Color(0xFFE2E8F0),
            textColorSecondary = Color(0xFF94A3B8),
            accentColor = Color(0xFF94A3B8),
            onAccentColor = Color(0xFF0F172A),
            blockPalettes = listOf(
                BlockThemeColors(Color(0xFF94A3B8), Color(0xFFCBD5E1), Color(0xFF475569), Color(0x6694A3B8)),
                BlockThemeColors(Color(0xFFA8A29E), Color(0xFFD6D3D1), Color(0xFF57534E), Color(0x66A8A29E)),
                BlockThemeColors(Color(0xFF64748B), Color(0xFF94A3B8), Color(0xFF334155), Color(0x6664748B)),
                BlockThemeColors(Color(0xFF2DD4BF), Color(0xFF99F6E4), Color(0xFF0F766E), Color(0x662DD4BF)),
                BlockThemeColors(Color(0xFFFBBF24), Color(0xFFFDE68A), Color(0xFFB45309), Color(0x66FBBF24)),
                BlockThemeColors(Color(0xFFC084FC), Color(0xFFE9D5FF), Color(0xFF7E22CE), Color(0x66C084FC)),
                BlockThemeColors(Color(0xFFFB7185), Color(0xFFFECDD3), Color(0xFFBE123C), Color(0x66FB7185))
            )
        )

        // ----------------------------------------------------
        // 9. GOLDEN INGOT
        // ----------------------------------------------------
        val GOLDEN_LIGHT = GameTheme(
            id = "golden",
            name = "Golden Ingot",
            price = 700,
            isDark = false,
            blockStyle = BlockStyle.GOLDEN,
            bgGradientStart = Color(0xFFFFFBEB),
            bgGradientEnd = Color(0xFFFEF3C7),
            boardBg = Color(0xFFFFFFFF),
            gridBorder = Color(0xFFFDE68A),
            cellEmptyBg = Color(0xFFFFFDF5),
            cardBg = Color(0xFFFFFFFF),
            cardBorder = Color(0xFFFDE68A),
            textColorPrimary = Color(0xFF78350F),
            textColorSecondary = Color(0xFFB45309),
            accentColor = Color(0xFFD97706),
            onAccentColor = Color.White,
            blockPalettes = listOf(
                BlockThemeColors(Color(0xFFF59E0B), Color(0xFFFEF08A), Color(0xFFB45309), Color(0x80F59E0B)),
                BlockThemeColors(Color(0xFFD97706), Color(0xFFFDE68A), Color(0xFF92400E), Color(0x80D97706)),
                BlockThemeColors(Color(0xFFEAB308), Color(0xFFFEF9C3), Color(0xFFA16207), Color(0x80EAB308)),
                BlockThemeColors(Color(0xFF10B981), Color(0xFFA7F3D0), Color(0xFF047857), Color(0x8010B981)),
                BlockThemeColors(Color(0xFF06B6D4), Color(0xFFA5F3FC), Color(0xFF0E7490), Color(0x8006B6D4)),
                BlockThemeColors(Color(0xFFEF4444), Color(0xFFFECACA), Color(0xFFB91C1C), Color(0x80EF4444)),
                BlockThemeColors(Color(0xFF8B5CF6), Color(0xFFDDD6FE), Color(0xFF6D28D9), Color(0x808B5CF6))
            )
        )

        val GOLDEN_DARK = GameTheme(
            id = "golden",
            name = "Golden Ingot",
            price = 700,
            isDark = true,
            blockStyle = BlockStyle.GOLDEN,
            bgGradientStart = Color(0xFF1C1504),
            bgGradientEnd = Color(0xFF0E0B02),
            boardBg = Color(0xFF261D07),
            gridBorder = Color(0xFF4A3B12),
            cellEmptyBg = Color(0xFF161103),
            cardBg = Color(0xFF261D07),
            cardBorder = Color(0xFF4A3B12),
            textColorPrimary = Color(0xFFFEF3C7),
            textColorSecondary = Color(0xFFFDE68A),
            accentColor = Color(0xFFFFD54F),
            onAccentColor = Color(0xFF3E1F00),
            blockPalettes = listOf(
                BlockThemeColors(Color(0xFFFFC107), Color(0xFFFFF3B0), Color(0xFFB28704), Color(0x99FFC107)),
                BlockThemeColors(Color(0xFFFFB300), Color(0xFFFFE082), Color(0xFFC68400), Color(0x99FFB300)),
                BlockThemeColors(Color(0xFFFFD54F), Color(0xFFFFF9C4), Color(0xFFFFA000), Color(0x99FFD54F)),
                BlockThemeColors(Color(0xFF00E676), Color(0xFFB9F6CA), Color(0xFF00A152), Color(0x9900E676)),
                BlockThemeColors(Color(0xFF00E5FF), Color(0xFFB8F9FF), Color(0xFF009EB0), Color(0x9900E5FF)),
                BlockThemeColors(Color(0xFFFF5252), Color(0xFFFF8A80), Color(0xFFC51162), Color(0x99FF5252)),
                BlockThemeColors(Color(0xFFE040FB), Color(0xFFEA80FC), Color(0xFFAA00FF), Color(0x99E040FB))
            )
        )

        val CLASSIC = CLASSIC_DARK
        val NEON = NEON_DARK
        val CANDY = CANDY_DARK
        val WOOD = WOOD_DARK
        val GLASS = GLASS_DARK
        val PASTEL = PASTEL_DARK
        val DARK_SPACE = DARK_SPACE_DARK
        val STONE = STONE_DARK
        val GOLDEN = GOLDEN_DARK

        val ALL_THEMES = listOf(CLASSIC, NEON, CANDY, WOOD, GLASS, PASTEL, DARK_SPACE, STONE, GOLDEN)

        fun findById(id: String, isDarkMode: Boolean = true): GameTheme {
            val base = ALL_THEMES.find { it.id == id } ?: CLASSIC
            return base.forMode(isDarkMode)
        }
    }
}
