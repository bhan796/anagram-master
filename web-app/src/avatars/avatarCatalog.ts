import { DEFAULT_ROOKIE_AVATAR } from "./defaultAvatar";
import type { AvatarDefinition } from "./avatarTypes";

export const AVATAR_CATALOG: AvatarDefinition[] = [DEFAULT_ROOKIE_AVATAR];

const fallbackAvatar = DEFAULT_ROOKIE_AVATAR;

export const getAvatarById = (id: string): AvatarDefinition => {
  return AVATAR_CATALOG.find((avatar) => avatar.id === id) ?? fallbackAvatar;
};
