export const RARITY_COLORS: Record<string, string> = {
  common: "#aaaaaa",
  uncommon: "#39ff14",
  rare: "#00f5ff",
  epic: "#cc44ff",
  legendary: "#ffd700",
  mythic: "url(#mythic-gradient)"
};

export function getCosmeticClass(itemId: string | null | undefined): string {
  if (!itemId) return "";
  return `cosmetic-${itemId}`;
}

export function getRarityColor(rarity: string): string {
  return RARITY_COLORS[rarity] ?? "#aaaaaa";
}

export function getRarityLabel(rarity: string): string {
  return rarity.charAt(0).toUpperCase() + rarity.slice(1);
}