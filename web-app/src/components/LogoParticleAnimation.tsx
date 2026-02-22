import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { TileLogo } from "./ArcadeComponents";
import * as SoundManager from "../sound/SoundManager";

const FONT: Record<string, number[][]> = {
  A: [
    [0, 1, 1, 1, 0],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 1, 1, 1, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1]
  ],
  N: [
    [1, 0, 0, 0, 1],
    [1, 1, 0, 0, 1],
    [1, 0, 1, 0, 1],
    [1, 0, 0, 1, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1]
  ],
  G: [
    [0, 1, 1, 1, 0],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 0],
    [1, 0, 1, 1, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [0, 1, 1, 1, 0]
  ],
  R: [
    [1, 1, 1, 1, 0],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 1, 1, 1, 0],
    [1, 0, 1, 0, 0],
    [1, 0, 0, 1, 0],
    [1, 0, 0, 0, 1]
  ],
  M: [
    [1, 0, 0, 0, 1],
    [1, 1, 0, 1, 1],
    [1, 0, 1, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1]
  ],
  E: [
    [1, 1, 1, 1, 1],
    [1, 0, 0, 0, 0],
    [1, 0, 0, 0, 0],
    [1, 1, 1, 1, 0],
    [1, 0, 0, 0, 0],
    [1, 0, 0, 0, 0],
    [1, 1, 1, 1, 1]
  ]
};

const PALETTE = ["#00f5ff", "#ffd700", "#ff00cc", "#39ff14"];
const TOP_WORD = "ANAGRAM";
const BOTTOM_WORD = "ARENA";
const BLOOM_ALPHAS = [0.12, 0.2, 0.35, 1.0];
const BLOOM_SCALES = [2.5, 2.0, 1.5, 1.0];

interface ParticleBuildResult {
  count: number;
  pxArr: Float32Array;
  pyArr: Float32Array;
  txArr: Float32Array;
  tyArr: Float32Array;
  sizeArr: Float32Array;
  delayArr: Float32Array;
  colors: string[];
  driftXArr: Float32Array;
  driftYArr: Float32Array;
  driftPhaseXArr: Float32Array;
  driftPhaseYArr: Float32Array;
  logoLeft: number;
  logoTop: number;
  logoWidth: number;
  logoHeight: number;
}

function hexToRgba(hex: string, alpha: number): string {
  const r = Number.parseInt(hex.slice(1, 3), 16);
  const g = Number.parseInt(hex.slice(3, 5), 16);
  const b = Number.parseInt(hex.slice(5, 7), 16);
  return `rgba(${r},${g},${b},${alpha})`;
}

function buildParticles(canvasWidth: number, canvasHeight: number): ParticleBuildResult {
  const pixelSize = Math.max(4, Math.min(8, Math.floor(canvasWidth / 50)));
  const letterGap = 2;
  const rowGap = 4;
  const subDiv = 3;
  const subSize = pixelSize / subDiv;
  const rowHeight = 7 * pixelSize;
  const topWidth = TOP_WORD.length * 5 * pixelSize + (TOP_WORD.length - 1) * letterGap;
  const bottomWidth = BOTTOM_WORD.length * 5 * pixelSize + (BOTTOM_WORD.length - 1) * letterGap;
  const logoWidth = Math.max(topWidth, bottomWidth);
  const logoHeight = rowHeight * 2 + rowGap;
  const logoLeft = (canvasWidth - logoWidth) * 0.5;
  const logoTop = (canvasHeight - logoHeight) * 0.5;
  const topLeft = logoLeft + (logoWidth - topWidth) * 0.5;
  const bottomLeft = logoLeft + (logoWidth - bottomWidth) * 0.5;
  const bottomTop = logoTop + rowHeight + rowGap;

  const tx: number[] = [];
  const ty: number[] = [];
  const colors: string[] = [];

  const addWord = (word: string, originX: number, originY: number, colorOffset: number) => {
    for (let letterIndex = 0; letterIndex < word.length; letterIndex += 1) {
      const char = word[letterIndex];
      const glyph = FONT[char];
      if (!glyph) continue;
      const letterColor = PALETTE[(letterIndex + colorOffset) % PALETTE.length];
      const letterX = originX + letterIndex * (5 * pixelSize + letterGap);
      for (let row = 0; row < 7; row += 1) {
        for (let col = 0; col < 5; col += 1) {
          if (glyph[row][col] !== 1) continue;
          const cellX = letterX + col * pixelSize;
          const cellY = originY + row * pixelSize;
          for (let subY = 0; subY < subDiv; subY += 1) {
            for (let subX = 0; subX < subDiv; subX += 1) {
              tx.push(cellX + subX * subSize + subSize * 0.5);
              ty.push(cellY + subY * subSize + subSize * 0.5);
              colors.push(letterColor);
            }
          }
        }
      }
    }
  };

  addWord(TOP_WORD, topLeft, logoTop, 0);
  addWord(BOTTOM_WORD, bottomLeft, bottomTop, 2);

  const count = tx.length;
  const pxArr = new Float32Array(count);
  const pyArr = new Float32Array(count);
  const txArr = new Float32Array(count);
  const tyArr = new Float32Array(count);
  const sizeArr = new Float32Array(count);
  const delayArr = new Float32Array(count);
  const driftXArr = new Float32Array(count);
  const driftYArr = new Float32Array(count);
  const driftPhaseXArr = new Float32Array(count);
  const driftPhaseYArr = new Float32Array(count);

  let maxCoord = 1;
  for (let i = 0; i < count; i += 1) {
    maxCoord = Math.max(maxCoord, tx[i] + ty[i]);
  }

  for (let i = 0; i < count; i += 1) {
    txArr[i] = tx[i];
    tyArr[i] = ty[i];
    pxArr[i] = -canvasWidth + Math.random() * canvasWidth * 3;
    pyArr[i] = -canvasHeight + Math.random() * canvasHeight * 3;
    sizeArr[i] = 2 + Math.random() * 2;
    delayArr[i] = ((tx[i] + ty[i]) / maxCoord) * 800;
    driftXArr[i] = 0.08 + Math.random() * 0.24;
    driftYArr[i] = 0.08 + Math.random() * 0.24;
    driftPhaseXArr[i] = Math.random() * Math.PI * 2;
    driftPhaseYArr[i] = Math.random() * Math.PI * 2;
  }

  return {
    count,
    pxArr,
    pyArr,
    txArr,
    tyArr,
    sizeArr,
    delayArr,
    colors,
    driftXArr,
    driftYArr,
    driftPhaseXArr,
    driftPhaseYArr,
    logoLeft,
    logoTop,
    logoWidth,
    logoHeight
  };
}

export const LogoParticleAnimation = ({ onComplete }: { onComplete?: () => void }) => {
  const wrapperRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const rafRef = useRef<number>(0);
  const completeTimeoutRef = useRef<number>(0);
  const startTimeRef = useRef<number>(0);
  const px = useRef(new Float32Array(0));
  const py = useRef(new Float32Array(0));
  const tx = useRef(new Float32Array(0));
  const ty = useRef(new Float32Array(0));
  const vx = useRef(new Float32Array(0));
  const vy = useRef(new Float32Array(0));
  const psize = useRef(new Float32Array(0));
  const delay = useRef(new Float32Array(0));
  const driftX = useRef(new Float32Array(0));
  const driftY = useRef(new Float32Array(0));
  const driftPhaseX = useRef(new Float32Array(0));
  const driftPhaseY = useRef(new Float32Array(0));
  const color = useRef<string[]>([]);
  const count = useRef(0);
  const logoBounds = useRef({ left: 0, top: 0, width: 0, height: 0 });
  const onCompleteRef = useRef(onComplete);

  const [canvasOpacity, setCanvasOpacity] = useState(1);
  const [tileLogoOpacity, setTileLogoOpacity] = useState(0);
  const [showTileLogo, setShowTileLogo] = useState(false);

  useEffect(() => {
    onCompleteRef.current = onComplete;
  }, [onComplete]);

  useLayoutEffect(() => {
    const canvas = canvasRef.current;
    const wrapper = wrapperRef.current;
    if (!canvas || !wrapper) {
      onCompleteRef.current?.();
      return;
    }

    const cssW = Math.max(wrapper.clientWidth || 320, 320);
    const cssH = 120;
    const dpr = Math.max(1, window.devicePixelRatio || 1);
    canvas.width = Math.floor(cssW * dpr);
    canvas.height = Math.floor(cssH * dpr);
    canvas.style.width = `${cssW}px`;
    canvas.style.height = `${cssH}px`;

    const ctx = canvas.getContext("2d");
    if (!ctx) {
      onCompleteRef.current?.();
      return;
    }
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.scale(dpr, dpr);

    const p = buildParticles(cssW, cssH);
    count.current = p.count;
    px.current = p.pxArr as Float32Array<ArrayBuffer>;
    py.current = p.pyArr as Float32Array<ArrayBuffer>;
    tx.current = p.txArr as Float32Array<ArrayBuffer>;
    ty.current = p.tyArr as Float32Array<ArrayBuffer>;
    vx.current = new Float32Array(p.count);
    vy.current = new Float32Array(p.count);
    psize.current = p.sizeArr as Float32Array<ArrayBuffer>;
    delay.current = p.delayArr as Float32Array<ArrayBuffer>;
    driftX.current = p.driftXArr as Float32Array<ArrayBuffer>;
    driftY.current = p.driftYArr as Float32Array<ArrayBuffer>;
    driftPhaseX.current = p.driftPhaseXArr as Float32Array<ArrayBuffer>;
    driftPhaseY.current = p.driftPhaseYArr as Float32Array<ArrayBuffer>;
    color.current = p.colors;
    logoBounds.current = {
      left: p.logoLeft,
      top: p.logoTop,
      width: p.logoWidth,
      height: p.logoHeight
    };

    void SoundManager.playLogoAssemble();

    const colorCache = new Map<string, string[]>();
    for (const c of PALETTE) {
      colorCache.set(c, BLOOM_ALPHAS.map((a) => hexToRgba(c, a)));
    }

    startTimeRef.current = performance.now();
    let snapFlashAlpha = 0;
    let snapTriggered = false;
    let handoffTriggered = false;

    const frame = (now: number) => {
      const elapsed = now - startTimeRef.current;
      const n = count.current;
      const spring = 0.08;
      const damp = 0.78;
      const snap = 1.5;
      const scatterAlpha = Math.max(0, Math.min(1, elapsed / 80));
      const bloomActive = elapsed >= 1400 && elapsed <= 1900;

      for (let i = 0; i < n; i += 1) {
        if (elapsed <= 400) {
          const t = elapsed * 0.004;
          px.current[i] += Math.sin(t + driftPhaseX.current[i]) * driftX.current[i];
          py.current[i] += Math.sin(t + driftPhaseY.current[i]) * driftY.current[i];
        }

        if (elapsed < 200 + delay.current[i]) continue;
        const dx = tx.current[i] - px.current[i];
        const dy = ty.current[i] - py.current[i];
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < snap) {
          px.current[i] = tx.current[i];
          py.current[i] = ty.current[i];
          vx.current[i] = 0;
          vy.current[i] = 0;
        } else {
          vx.current[i] = (vx.current[i] + dx * spring) * damp;
          vy.current[i] = (vy.current[i] + dy * spring) * damp;
          px.current[i] += vx.current[i];
          py.current[i] += vy.current[i];
        }
      }

      if (!snapTriggered && elapsed >= 1400) {
        snapTriggered = true;
        for (let i = 0; i < n; i += 1) {
          px.current[i] = tx.current[i];
          py.current[i] = ty.current[i];
          vx.current[i] = 0;
          vy.current[i] = 0;
        }
        snapFlashAlpha = 0.95;
        void SoundManager.playMatchFound();
      }

      ctx.clearRect(0, 0, cssW, cssH);
      if (bloomActive) {
        for (let b = 0; b < 4; b += 1) {
          const scale = BLOOM_SCALES[b];
          for (let i = 0; i < n; i += 1) {
            const col = color.current[i];
            const colorVariants = colorCache.get(col);
            ctx.fillStyle = colorVariants ? colorVariants[b] : hexToRgba(col, BLOOM_ALPHAS[b]);
            const s = psize.current[i] * scale;
            ctx.fillRect(px.current[i] - s * 0.5, py.current[i] - s * 0.5, s, s);
          }
        }
      } else {
        ctx.globalAlpha = scatterAlpha;
        for (let i = 0; i < n; i += 1) {
          const s = psize.current[i];
          ctx.fillStyle = color.current[i];
          ctx.fillRect(px.current[i] - s * 0.5, py.current[i] - s * 0.5, s, s);
        }
        ctx.globalAlpha = 1;
      }

      if (snapFlashAlpha > 0) {
        ctx.fillStyle = `rgba(255,255,255,${snapFlashAlpha})`;
        const bounds = logoBounds.current;
        ctx.fillRect(bounds.left, bounds.top, bounds.width, bounds.height);
        snapFlashAlpha = Math.max(0, snapFlashAlpha - 1 / 9);
      }

      if (!handoffTriggered && elapsed >= 1900) {
        handoffTriggered = true;
        setShowTileLogo(true);
        requestAnimationFrame(() => {
          setCanvasOpacity(0);
          setTileLogoOpacity(1);
        });
        completeTimeoutRef.current = window.setTimeout(() => onCompleteRef.current?.(), 320);
      }

      if (elapsed < 2300) {
        rafRef.current = requestAnimationFrame(frame);
      }
    };

    rafRef.current = requestAnimationFrame(frame);
    return () => {
      cancelAnimationFrame(rafRef.current);
      window.clearTimeout(completeTimeoutRef.current);
    };
  }, []);

  return (
    <div ref={wrapperRef} style={{ position: "relative", width: "100%", height: 120 }}>
      <canvas
        ref={canvasRef}
        style={{
          display: "block",
          width: "100%",
          height: 120,
          opacity: canvasOpacity,
          transition: "opacity 300ms ease"
        }}
      />
      <div
        style={{
          position: "absolute",
          inset: 0,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          pointerEvents: "none",
          opacity: showTileLogo ? tileLogoOpacity : 0,
          transition: "opacity 300ms ease"
        }}
      >
        <TileLogo />
      </div>
    </div>
  );
};
