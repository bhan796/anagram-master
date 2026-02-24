export interface AchievementDef {
  id: string;
  name: string;
  description: string;
  tier: "easy" | "medium" | "hard" | "legendary";
  runesReward: number;
}

export interface CosmeticItem {
  id: string;
  name: string;
  rarity: "common" | "uncommon" | "rare" | "epic" | "legendary" | "mythic";
  cssClass: string;
  description: string;
}

export const ACHIEVEMENTS: AchievementDef[] = [
  { id: "first_win", name: "First Blood", description: "Win your first ranked match", tier: "easy", runesReward: 50 },
  { id: "first_word", name: "Word Warden", description: "Submit a valid word in an online match", tier: "easy", runesReward: 50 },
  { id: "long_word_6", name: "Six Letters", description: "Submit a valid 6-letter word", tier: "easy", runesReward: 50 },
  { id: "vowel_baron", name: "Vowel Baron", description: "Pick 5 or more vowels in a single letters round", tier: "easy", runesReward: 50 },
  { id: "fast_conundrum_5s", name: "Speed Reader", description: "Solve a conundrum in under 5 seconds", tier: "easy", runesReward: 50 },
  { id: "hat_trick", name: "Hat Trick", description: "Win 3 ranked games in a row", tier: "medium", runesReward: 150 },
  { id: "silver_tier", name: "Silver Tongue", description: "Reach 1200 ELO", tier: "medium", runesReward: 150 },
  { id: "century", name: "Century Club", description: "Play 100 ranked games", tier: "medium", runesReward: 150 },
  { id: "conundrum_25", name: "Cracker", description: "Solve 25 conundrums correctly", tier: "medium", runesReward: 150 },
  { id: "nine_letters", name: "Long Game", description: "Submit a valid 9-letter word", tier: "medium", runesReward: 150 },
  { id: "gold_tier", name: "Golden Gate", description: "Reach 1400 ELO", tier: "hard", runesReward: 400 },
  { id: "streak_5", name: "Iron Will", description: "Win 5 ranked games in a row", tier: "hard", runesReward: 400 },
  { id: "games_250", name: "Scholar", description: "Play 250 ranked games", tier: "hard", runesReward: 400 },
  { id: "comeback", name: "Comeback Kid", description: "Win a ranked match after trailing by 20+ points", tier: "hard", runesReward: 400 },
  { id: "max_round_30", name: "Triple Kingpin", description: "Score 30 points in a single letters round", tier: "hard", runesReward: 400 },
  { id: "platinum_tier", name: "Platinum Legend", description: "Reach 1600 ELO", tier: "legendary", runesReward: 1000 },
  { id: "streak_10", name: "Unstoppable", description: "Win 10 ranked games in a row", tier: "legendary", runesReward: 1000 },
  { id: "diamond_tier", name: "Diamond Mind", description: "Reach 1800 ELO", tier: "legendary", runesReward: 1000 }
];

export const COSMETIC_ITEMS: CosmeticItem[] = [
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

export const CHEST_WEIGHTS: Record<string, number> = {
  c_silver: 1000,
  c_mint: 1000,
  c_crimson: 1000,
  c_amber: 1000,
  c_pulse: 1000,
  u_royal: 500,
  u_violet: 500,
  u_forest: 500,
  u_glow: 500,
  u_wave: 500,
  r_electric: 300,
  r_neon_green: 300,
  r_spectrum: 300,
  r_flicker: 300,
  r_dual: 300,
  e_magenta: 175,
  e_gold_rush: 175,
  e_glitch: 175,
  e_plasma: 175,
  l_solar: 40,
  l_aurora: 40,
  l_ice: 40,
  l_obsidian: 40,
  l_holo: 40,
  l_crown: 50,
  m_prism: 10,
  m_void: 10,
  m_matrix: 10,
  m_phoenix: 10,
  m_aurora_ex: 10
};

export function rollChest(): CosmeticItem {
  const roll = Math.floor(Math.random() * 10000) + 1;
  let acc = 0;
  for (const item of COSMETIC_ITEMS) {
    const weight = CHEST_WEIGHTS[item.id] ?? 0;
    acc += weight;
    if (roll <= acc) return item;
  }
  return COSMETIC_ITEMS[COSMETIC_ITEMS.length - 1]!;
}

export function computeMatchRunes(mode: "casual" | "ranked", won: boolean, winStreakLength: number): number {
  let runes = mode === "ranked" ? 20 : 10;
  if (mode === "ranked" && won) {
    runes += 15;
    if (winStreakLength >= 10) runes += 20;
    else if (winStreakLength >= 5) runes += 10;
    else if (winStreakLength >= 3) runes += 5;
  }
  return runes;
}
