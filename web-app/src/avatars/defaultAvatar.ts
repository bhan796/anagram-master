import type { AvatarDefinition, PixelFrame } from "./avatarTypes";

const C = {
  armor: "#00f5ff",
  shadow: "#0088aa",
  visor: "#0a0a18",
  skin: "#ffb38a",
  suit: "#1c1c3a",
  trim: "#ffd700"
} as const;

const emptyFrame = (): PixelFrame =>
  Array.from({ length: 48 }, () => Array.from({ length: 32 }, () => ""));

const paintRect = (frame: PixelFrame, x1: number, y1: number, x2: number, y2: number, color: string) => {
  for (let y = Math.max(0, y1); y <= Math.min(47, y2); y += 1) {
    for (let x = Math.max(0, x1); x <= Math.min(31, x2); x += 1) {
      frame[y][x] = color;
    }
  }
};

const cloneFrame = (frame: PixelFrame): PixelFrame => frame.map((row) => [...row]);

const shiftFrame = (frame: PixelFrame, dx: number, dy: number): PixelFrame => {
  const shifted = emptyFrame();
  for (let y = 0; y < 48; y += 1) {
    for (let x = 0; x < 32; x += 1) {
      const color = frame[y][x];
      if (!color) continue;
      const nx = x + dx;
      const ny = y + dy;
      if (nx >= 0 && nx < 32 && ny >= 0 && ny < 48) {
        shifted[ny][nx] = color;
      }
    }
  }
  return shifted;
};

const buildBody = ({
  crouch = 0,
  lean = 0,
  leftArmLift = 0,
  rightArmLift = 0,
  rightPunch = 0,
  rightArmBack = 0,
  headDown = 0,
  slump = 0,
  rightArmUp = false,
  wideFeet = false,
  handsLow = 0
}: {
  crouch?: number;
  lean?: number;
  leftArmLift?: number;
  rightArmLift?: number;
  rightPunch?: number;
  rightArmBack?: number;
  headDown?: number;
  slump?: number;
  rightArmUp?: boolean;
  wideFeet?: boolean;
  handsLow?: number;
} = {}): PixelFrame => {
  const f = emptyFrame();
  const ox = lean;
  const oy = crouch;

  paintRect(f, 10 + ox, 0 + oy + headDown, 21 + ox, 7 + oy + headDown, C.armor);
  paintRect(f, 11 + ox, 1 + oy + headDown, 20 + ox, 6 + oy + headDown, C.shadow);
  paintRect(f, 13 + ox, 3 + oy + headDown, 18 + ox, 4 + oy + headDown, C.visor);
  paintRect(f, 14 + ox, 3 + oy + headDown, 14 + ox, 3 + oy + headDown, C.armor);
  paintRect(f, 17 + ox, 3 + oy + headDown, 17 + ox, 3 + oy + headDown, C.armor);

  paintRect(f, 13 + ox, 8 + oy, 18 + ox, 9 + oy, C.skin);

  paintRect(f, 10 + ox, 10 + oy, 21 + ox, 27 + oy, C.armor);
  paintRect(f, 11 + ox, 12 + oy, 20 + ox, 25 + oy, C.shadow);
  paintRect(f, 10 + ox, 12 + oy, 10 + ox, 25 + oy, C.suit);
  paintRect(f, 21 + ox, 12 + oy, 21 + ox, 25 + oy, C.suit);

  paintRect(f, 2 + ox, 13 + oy - leftArmLift + slump, 9 + ox, 22 + oy - leftArmLift + slump + handsLow, C.armor);
  paintRect(f, 3 + ox, 14 + oy - leftArmLift + slump, 8 + ox, 21 + oy - leftArmLift + slump + handsLow, C.shadow);
  paintRect(f, 2 + ox, 22 + oy - leftArmLift + slump + handsLow, 4 + ox, 23 + oy - leftArmLift + slump + handsLow, C.trim);

  if (rightArmUp) {
    paintRect(f, 19 + ox, 2 + oy, 22 + ox, 18 + oy, C.armor);
    paintRect(f, 20 + ox, 3 + oy, 21 + ox, 16 + oy, C.shadow);
    paintRect(f, 19 + ox, 1 + oy, 22 + ox, 2 + oy, C.trim);
  } else {
    paintRect(
      f,
      22 + ox + rightArmBack,
      13 + oy - rightArmLift + slump,
      29 + ox + rightPunch,
      22 + oy - rightArmLift + slump + handsLow,
      C.armor
    );
    paintRect(
      f,
      23 + ox + rightArmBack,
      14 + oy - rightArmLift + slump,
      28 + ox + rightPunch,
      21 + oy - rightArmLift + slump + handsLow,
      C.shadow
    );
    paintRect(f, 27 + ox + rightPunch, 22 + oy - rightArmLift + slump + handsLow, 29 + ox + rightPunch, 23 + oy - rightArmLift + slump + handsLow, C.trim);
  }

  paintRect(f, 11 + ox, 28 + oy, 20 + ox, 34 + oy, C.armor);
  paintRect(f, 12 + ox, 29 + oy, 19 + ox, 33 + oy, C.shadow);

  paintRect(f, 11 + ox, 35 + oy, 15 + ox, 42 + oy, C.suit);
  paintRect(f, 16 + ox, 35 + oy, 20 + ox, 42 + oy, C.suit);
  paintRect(f, 12 + ox, 38 + oy, 14 + ox, 39 + oy, C.armor);
  paintRect(f, 17 + ox, 38 + oy, 19 + ox, 39 + oy, C.armor);

  const leftBootStart = wideFeet ? 9 : 10;
  const rightBootEnd = wideFeet ? 22 : 21;
  paintRect(f, leftBootStart + ox, 43 + oy, 15 + ox, 47 + oy, C.shadow);
  paintRect(f, 16 + ox, 43 + oy, rightBootEnd + ox, 47 + oy, C.shadow);
  paintRect(f, leftBootStart + ox, 46 + oy, 15 + ox, 47 + oy, C.trim);
  paintRect(f, 16 + ox, 46 + oy, rightBootEnd + ox, 47 + oy, C.trim);

  return f;
};

const idle0 = buildBody();
const idle1 = shiftFrame(buildBody({ handsLow: 1 }), 0, 1);
const idle2 = shiftFrame(buildBody({ handsLow: 1 }), 0, 0);

const battle0 = buildBody({ crouch: 2, leftArmLift: 5, rightArmLift: 3 });
const battle1 = shiftFrame(buildBody({ crouch: 2, leftArmLift: 5, rightArmLift: 3, lean: 1 }), 1, 0);
const battle2 = buildBody({ crouch: 2, leftArmLift: 3, rightPunch: 1, rightArmLift: 2, lean: 1 });
const battle3 = cloneFrame(battle0);

const attack0 = buildBody({ crouch: 2, rightArmBack: -2, lean: -1, leftArmLift: 3 });
const attack1 = buildBody({ crouch: 1, rightPunch: 4, lean: 2, leftArmLift: 3 });
const attack2 = cloneFrame(battle0);

const victory0 = buildBody({ crouch: 3, wideFeet: true, leftArmLift: 2 });
const victory1 = shiftFrame(buildBody({ leftArmLift: 4, rightArmLift: 2 }), 0, -6);
const victory2 = shiftFrame(buildBody({ leftArmLift: 4, rightArmUp: true }), 0, -4);
const victory3 = buildBody({ crouch: 1, leftArmLift: 2, rightArmLift: 1, wideFeet: true });

const defeat0 = buildBody({ headDown: 2, slump: 2, lean: 1, leftArmLift: -1, rightArmLift: -1 });
const defeat1 = shiftFrame(buildBody({ headDown: 2, slump: 3, lean: 1, leftArmLift: -1, rightArmLift: -1, handsLow: 1 }), 0, 1);

export const DEFAULT_ROOKIE_AVATAR: AvatarDefinition = {
  id: "default_rookie",
  name: "The Rookie",
  rarity: "default",
  animations: {
    idle: { frames: [idle0, idle1, idle2], fps: 6 },
    battle: { frames: [battle0, battle1, battle2, battle3], fps: 8 },
    attack: { frames: [attack0, attack1, attack2], fps: 12 },
    victory: { frames: [victory0, victory1, victory2, victory3], fps: 8 },
    defeat: { frames: [defeat0, defeat1], fps: 4 }
  }
};
