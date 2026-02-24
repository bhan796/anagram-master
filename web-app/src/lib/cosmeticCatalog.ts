export interface CosmeticItem {
  id: string;
  name: string;
  rarity: "common" | "uncommon" | "rare" | "epic" | "legendary" | "mythic";
  cssClass: string;
  description: string;
}

export const COSMETIC_CATALOG: CosmeticItem[] = [
  { id: "c_silver", name: "Sterling", rarity: "common", cssClass: "cosmetic-c_silver", description: "Silver name color" },
  { id: "c_mint", name: "Mint Leaf", rarity: "common", cssClass: "cosmetic-c_mint", description: "Cool mint green name" },
  { id: "c_crimson", name: "Crimson", rarity: "common", cssClass: "cosmetic-c_crimson", description: "Deep red name color" },
  { id: "c_amber", name: "Amber", rarity: "common", cssClass: "cosmetic-c_amber", description: "Warm amber name color" },
  { id: "c_pulse", name: "Neon Pulse", rarity: "common", cssClass: "cosmetic-c_pulse", description: "Slow cyan glow pulse" },
  { id: "u_royal", name: "Royal Blue", rarity: "uncommon", cssClass: "cosmetic-u_royal", description: "Vivid royal blue" },
  { id: "u_violet", name: "Violet", rarity: "uncommon", cssClass: "cosmetic-u_violet", description: "Deep violet" },
  { id: "u_forest", name: "Forest", rarity: "uncommon", cssClass: "cosmetic-u_forest", description: "Vivid neon green" },
  { id: "u_glow", name: "Soft Glow", rarity: "uncommon", cssClass: "cosmetic-u_glow", description: "White radial halo" },
  { id: "u_wave", name: "Color Wave", rarity: "uncommon", cssClass: "cosmetic-u_wave", description: "Slow cyan?blue wave" },
  { id: "r_electric", name: "Electric", rarity: "rare", cssClass: "cosmetic-r_electric", description: "Electric blue strong glow" },
  { id: "r_neon_green", name: "Reactor", rarity: "rare", cssClass: "cosmetic-r_neon_green", description: "Neon green reactor glow" },
  { id: "r_spectrum", name: "Spectrum", rarity: "rare", cssClass: "cosmetic-r_spectrum", description: "Slow full color cycle" },
  { id: "r_flicker", name: "Neon Light", rarity: "rare", cssClass: "cosmetic-r_flicker", description: "Neon tube flicker" },
  { id: "r_dual", name: "Dual Tone", rarity: "rare", cssClass: "cosmetic-r_dual", description: "Cyan/gold gradient text" },
  { id: "e_magenta", name: "Magenta Blaze", rarity: "epic", cssClass: "cosmetic-e_magenta", description: "Magenta with blazing glow" },
  { id: "e_gold_rush", name: "Gold Rush", rarity: "epic", cssClass: "cosmetic-e_gold_rush", description: "Gold shimmer" },
  { id: "e_glitch", name: "Glitch", rarity: "epic", cssClass: "cosmetic-e_glitch", description: "Digital glitch scanline" },
  { id: "e_plasma", name: "Plasma", rarity: "epic", cssClass: "cosmetic-e_plasma", description: "Electric plasma ripple" },
  { id: "l_solar", name: "Solar Flare", rarity: "legendary", cssClass: "cosmetic-l_solar", description: "Fire gradient sweep" },
  { id: "l_aurora", name: "Aurora", rarity: "legendary", cssClass: "cosmetic-l_aurora", description: "Aurora borealis sweep" },
  { id: "l_ice", name: "Crystal Ice", rarity: "legendary", cssClass: "cosmetic-l_ice", description: "Frost shimmer" },
  { id: "l_obsidian", name: "Obsidian", rarity: "legendary", cssClass: "cosmetic-l_obsidian", description: "Black with purple edge" },
  { id: "l_holo", name: "Holographic", rarity: "legendary", cssClass: "cosmetic-l_holo", description: "Iridescent hologram" },
  { id: "l_crown", name: "The Crown", rarity: "legendary", cssClass: "cosmetic-l_crown", description: "Gold + ? prefix" },
  { id: "m_prism", name: "Prismatic Storm", rarity: "mythic", cssClass: "cosmetic-m_prism", description: "Ultra-fast rainbow + pulse" },
  { id: "m_void", name: "Void Walker", rarity: "mythic", cssClass: "cosmetic-m_void", description: "Purple energy tendril glow" },
  { id: "m_matrix", name: "Matrix", rarity: "mythic", cssClass: "cosmetic-m_matrix", description: "Green code rain flicker" },
  { id: "m_phoenix", name: "Phoenix Fire", rarity: "mythic", cssClass: "cosmetic-m_phoenix", description: "Phoenix flame animation" },
  { id: "m_aurora_ex", name: "Aurora EX", rarity: "mythic", cssClass: "cosmetic-m_aurora_ex", description: "Extreme aurora storm" }
];