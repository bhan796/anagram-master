import { useEffect, useRef, type CSSProperties } from "react";
import { getAvatarById } from "../avatars/avatarCatalog";
import type { AvatarState } from "../avatars/avatarTypes";

interface AvatarSpriteProps {
  avatarId: string;
  state: AvatarState;
  scale: number;
  facing: "right" | "left";
  style?: CSSProperties;
}

export const AvatarSprite = ({ avatarId, state, scale, facing, style }: AvatarSpriteProps) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const avatar = getAvatarById(avatarId);
    const animation = avatar.animations[state] ?? avatar.animations.idle;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    canvas.width = 32 * scale;
    canvas.height = 48 * scale;

    let frameIndex = 0;
    const drawFrame = () => {
      const frame = animation.frames[frameIndex % animation.frames.length];
      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      ctx.imageSmoothingEnabled = false;
      if (facing === "left") {
        ctx.setTransform(-1, 0, 0, 1, 32 * scale, 0);
      }

      for (let y = 0; y < frame.length; y += 1) {
        for (let x = 0; x < frame[y].length; x += 1) {
          const color = frame[y][x];
          if (!color) continue;
          ctx.fillStyle = color;
          ctx.fillRect(x * scale, y * scale, scale, scale);
        }
      }
      ctx.setTransform(1, 0, 0, 1, 0, 0);
      frameIndex += 1;
    };

    drawFrame();
    const interval = window.setInterval(drawFrame, Math.max(16, Math.round(1000 / Math.max(1, animation.fps))));
    return () => window.clearInterval(interval);
  }, [avatarId, state, scale, facing]);

  return <canvas ref={canvasRef} className="pixel-art" style={style} />;
};
