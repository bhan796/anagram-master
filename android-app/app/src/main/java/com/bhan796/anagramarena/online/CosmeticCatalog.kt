package com.bhan796.anagramarena.online

import androidx.compose.ui.graphics.Color

object CosmeticCatalog {
    val items: List<CosmeticItem> = listOf(
        CosmeticItem("c_silver", "Sterling", "common", "cosmetic-c_silver", "Silver name color"),
        CosmeticItem("c_mint", "Mint Leaf", "common", "cosmetic-c_mint", "Cool mint green name"),
        CosmeticItem("c_crimson", "Crimson", "common", "cosmetic-c_crimson", "Deep red name color"),
        CosmeticItem("c_amber", "Amber", "common", "cosmetic-c_amber", "Warm amber name color"),
        CosmeticItem("c_pulse", "Neon Pulse", "common", "cosmetic-c_pulse", "Slow cyan glow pulse"),
        CosmeticItem("u_royal", "Royal Blue", "uncommon", "cosmetic-u_royal", "Vivid royal blue"),
        CosmeticItem("u_violet", "Violet", "uncommon", "cosmetic-u_violet", "Deep violet"),
        CosmeticItem("u_forest", "Forest", "uncommon", "cosmetic-u_forest", "Vivid neon green"),
        CosmeticItem("u_glow", "Soft Glow", "uncommon", "cosmetic-u_glow", "White radial halo"),
        CosmeticItem("u_wave", "Color Wave", "uncommon", "cosmetic-u_wave", "Slow cyan?blue wave"),
        CosmeticItem("r_electric", "Electric", "rare", "cosmetic-r_electric", "Electric blue strong glow"),
        CosmeticItem("r_neon_green", "Reactor", "rare", "cosmetic-r_neon_green", "Neon green reactor glow"),
        CosmeticItem("r_spectrum", "Spectrum", "rare", "cosmetic-r_spectrum", "Slow full color cycle"),
        CosmeticItem("r_flicker", "Neon Light", "rare", "cosmetic-r_flicker", "Neon tube flicker"),
        CosmeticItem("r_dual", "Dual Tone", "rare", "cosmetic-r_dual", "Cyan/gold gradient text"),
        CosmeticItem("e_magenta", "Magenta Blaze", "epic", "cosmetic-e_magenta", "Magenta with blazing glow"),
        CosmeticItem("e_gold_rush", "Gold Rush", "epic", "cosmetic-e_gold_rush", "Gold shimmer"),
        CosmeticItem("e_glitch", "Glitch", "epic", "cosmetic-e_glitch", "Digital glitch scanline"),
        CosmeticItem("e_plasma", "Plasma", "epic", "cosmetic-e_plasma", "Electric plasma ripple"),
        CosmeticItem("l_solar", "Solar Flare", "legendary", "cosmetic-l_solar", "Fire gradient sweep"),
        CosmeticItem("l_aurora", "Aurora", "legendary", "cosmetic-l_aurora", "Aurora borealis sweep"),
        CosmeticItem("l_ice", "Crystal Ice", "legendary", "cosmetic-l_ice", "Frost shimmer"),
        CosmeticItem("l_obsidian", "Obsidian", "legendary", "cosmetic-l_obsidian", "Black with purple edge"),
        CosmeticItem("l_holo", "Holographic", "legendary", "cosmetic-l_holo", "Iridescent hologram"),
        CosmeticItem("l_crown", "The Crown", "legendary", "cosmetic-l_crown", "Gold + ? prefix"),
        CosmeticItem("m_prism", "Prismatic Storm", "mythic", "cosmetic-m_prism", "Ultra-fast rainbow + pulse"),
        CosmeticItem("m_void", "Void Walker", "mythic", "cosmetic-m_void", "Purple energy tendril glow"),
        CosmeticItem("m_matrix", "Matrix", "mythic", "cosmetic-m_matrix", "Green code rain flicker"),
        CosmeticItem("m_phoenix", "Phoenix Fire", "mythic", "cosmetic-m_phoenix", "Phoenix flame animation"),
        CosmeticItem("m_aurora_ex", "Aurora EX", "mythic", "cosmetic-m_aurora_ex", "Extreme aurora storm")
    )

    fun getRarityColor(rarity: String): Color = when (rarity) {
        "common" -> Color(0xFFAAAAAA)
        "uncommon" -> Color(0xFF39FF14)
        "rare" -> Color(0xFF00F5FF)
        "epic" -> Color(0xFFCC44FF)
        "legendary" -> Color(0xFFFFD700)
        "mythic" -> Color(0xFFFF88FF)
        else -> Color.White
    }
}