// Each row is an array of 32 entries. "" = transparent. Any CSS hex string = solid color.
export type PixelRow = string[];
export type PixelFrame = PixelRow[];

export interface AvatarAnimation {
  frames: PixelFrame[];
  fps: number;
}

export type AvatarState = "idle" | "battle" | "attack" | "victory" | "defeat";

export interface AvatarDefinition {
  id: string;
  name: string;
  rarity: "default" | "common" | "uncommon" | "rare" | "epic" | "legendary" | "mythic";
  animations: Record<AvatarState, AvatarAnimation>;
}
