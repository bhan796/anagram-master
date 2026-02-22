import { createCanvas, GlobalFonts, Path2D } from '@napi-rs/canvas';
import sharp from 'sharp';
import toIco from 'to-ico';
import fs from 'node:fs/promises';
import path from 'node:path';

const root = process.cwd();
const fontPath = path.join(root, 'android-app', 'app', 'src', 'main', 'res', 'font', 'press_start_2p_regular.ttf');
GlobalFonts.registerFromPath(fontPath, 'PressStart2P');

const BG = '#0a0a18';
const TILE_FILL = '#1c1c3a';
const TILE_BORDER = '#4a4a84';
const TILE_INSET = '#2f2f59';
const CYAN = '#00f5ff';
const GOLD = '#ffd700';
const MAGENTA_FAINT = 'rgba(255,0,204,0.15)';

const size = 1024;
const canvas = createCanvas(size, size);
const ctx = canvas.getContext('2d');

ctx.fillStyle = BG;
ctx.fillRect(0, 0, size, size);

ctx.strokeStyle = MAGENTA_FAINT;
ctx.lineWidth = 20;
ctx.lineCap = 'round';
ctx.beginPath();
ctx.moveTo(180, 280);
ctx.lineTo(844, 760);
ctx.moveTo(844, 280);
ctx.lineTo(180, 760);
ctx.stroke();

function drawBlockA(ctx, x, y, w, h, color) {
  const t = Math.max(4, Math.floor(w * 0.18));
  const legW = Math.floor(w * 0.24);
  const gap = Math.floor(w * 0.1);
  ctx.fillStyle = color;
  ctx.fillRect(x, y + t, legW, h - t);
  ctx.fillRect(x + w - legW, y + t, legW, h - t);
  ctx.fillRect(x + legW - gap, y, w - (legW - gap) * 2, t);
  ctx.fillRect(x + legW - gap, y + Math.floor(h * 0.46), w - (legW - gap) * 2, t);
}

function drawTile(cx, cy, tileSize, angleDeg, color, glowColor) {
  ctx.save();
  ctx.translate(cx, cy);
  ctx.rotate((angleDeg * Math.PI) / 180);

  const half = tileSize / 2;
  const bevel = Math.round(tileSize * 0.07);

  ctx.shadowColor = glowColor;
  ctx.shadowBlur = 90;
  ctx.fillStyle = 'rgba(0,0,0,0)';
  ctx.fillRect(-half, -half, tileSize, tileSize);

  ctx.shadowBlur = 0;
  ctx.fillStyle = TILE_FILL;
  ctx.fillRect(-half, -half, tileSize, tileSize);

  ctx.lineWidth = Math.max(10, Math.round(tileSize * 0.07));
  ctx.strokeStyle = TILE_BORDER;
  ctx.strokeRect(-half + 2, -half + 2, tileSize - 4, tileSize - 4);

  ctx.lineWidth = Math.max(6, Math.round(tileSize * 0.04));
  ctx.strokeStyle = TILE_INSET;
  ctx.strokeRect(-half + bevel, -half + bevel, tileSize - bevel * 2, tileSize - bevel * 2);

  const letterW = Math.round(tileSize * 0.47);
  const letterH = Math.round(tileSize * 0.52);
  const letterX = -Math.round(letterW / 2);
  const letterY = -Math.round(letterH / 2) + Math.round(tileSize * 0.02);

  ctx.shadowColor = glowColor;
  ctx.shadowBlur = 26;
  drawBlockA(ctx, letterX, letterY, letterW, letterH, color);
  ctx.shadowBlur = 0;

  ctx.restore();
}

drawTile(430, 430, 280, -6, CYAN, CYAN);
drawTile(594, 432, 280, 6, GOLD, GOLD);

ctx.font = '82px PressStart2P';
ctx.textAlign = 'center';
ctx.textBaseline = 'middle';
ctx.fillStyle = CYAN;
ctx.fillText('ARENA', 513, 768 + 1);
ctx.fillStyle = '#ffffff';
ctx.fillText('ARENA', 512, 768);

const sourceBuffer = canvas.toBuffer('image/png');
await fs.writeFile(path.join(root, 'logo-source-1024.png'), sourceBuffer);

const webPublic = path.join(root, 'web-app', 'public');
await fs.mkdir(webPublic, { recursive: true });

async function writePng(target, w, h) {
  await sharp(sourceBuffer).resize(w, h, { kernel: 'lanczos3' }).png().toFile(target);
}

await writePng(path.join(webPublic, 'favicon-16x16.png'), 16, 16);
await writePng(path.join(webPublic, 'favicon-32x32.png'), 32, 32);
await writePng(path.join(webPublic, 'apple-touch-icon.png'), 180, 180);
await writePng(path.join(webPublic, 'android-chrome-192x192.png'), 192, 192);
await writePng(path.join(webPublic, 'android-chrome-512x512.png'), 512, 512);

const icoBuffers = await Promise.all([
  sharp(sourceBuffer).resize(16, 16).png().toBuffer(),
  sharp(sourceBuffer).resize(32, 32).png().toBuffer(),
  sharp(sourceBuffer).resize(48, 48).png().toBuffer()
]);
const icoBuffer = await toIco(icoBuffers);
await fs.writeFile(path.join(webPublic, 'favicon.ico'), icoBuffer);

const androidRes = path.join(root, 'android-app', 'app', 'src', 'main', 'res');
const densities = [
  ['mipmap-mdpi', 48],
  ['mipmap-hdpi', 72],
  ['mipmap-xhdpi', 96],
  ['mipmap-xxhdpi', 144],
  ['mipmap-xxxhdpi', 192]
];

for (const [folder, px] of densities) {
  const outDir = path.join(androidRes, folder);
  await fs.mkdir(outDir, { recursive: true });
  await writePng(path.join(outDir, 'ic_launcher.png'), px, px);
  await writePng(path.join(outDir, 'ic_launcher_round.png'), px, px);
}
