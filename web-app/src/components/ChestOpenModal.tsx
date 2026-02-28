import { useEffect, useMemo, useRef, useState } from "react";
import { ArcadeButton } from "./ArcadeComponents";
import { COSMETIC_CATALOG, type CosmeticItem } from "../lib/cosmeticCatalog";
import { getCosmeticClass, getRarityColor, getRarityLabel } from "../lib/cosmetics";
import * as SoundManager from "../sound/SoundManager";

type OpenChestResponse = {
  item: CosmeticItem;
  alreadyOwned: boolean;
};

const normalizeBackendUrl = (raw: string | undefined): string => {
  const candidate = (raw ?? "").trim();
  if (!candidate) return "http://localhost:4000";
  if (/^https?:\/\//i.test(candidate)) return candidate.replace(/\/+$/, "");
  return `https://${candidate.replace(/\/+$/, "")}`;
};

const apiBaseUrl = normalizeBackendUrl(import.meta.env.VITE_SERVER_URL as string | undefined);

interface ChestOpenModalProps {
  accessToken: string;
  onClose: () => void;
  onEquip: (itemId: string) => void;
}

const easeOut = (t: number) => 1 - Math.pow(1 - t, 3);
const CARD_WIDTH = 92;
const CARD_GAP = 8;
const CARD_STRIDE = CARD_WIDTH + CARD_GAP;
const TOTAL_ITEMS = 140;
const LANDING_MIN_INDEX = 85;
const LANDING_MAX_INDEX = 105;

type CarouselEntry = {
  item: CosmeticItem;
  isWinner: boolean;
  key: string;
};

export const ChestOpenModal = ({ accessToken, onClose, onEquip }: ChestOpenModalProps) => {
  const [loading, setLoading] = useState(true);
  const [wonItem, setWonItem] = useState<CosmeticItem | null>(null);
  const [alreadyOwned, setAlreadyOwned] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [revealed, setRevealed] = useState(false);
  const [x, setX] = useState(0);
  const viewportRef = useRef<HTMLDivElement | null>(null);
  const winnerCardRef = useRef<HTMLDivElement | null>(null);
  const hasOpenedRef = useRef(false);

  useEffect(() => {
    if (hasOpenedRef.current) return;
    hasOpenedRef.current = true;
    let cancelled = false;
    const run = async () => {
      try {
        const response = await fetch(`${apiBaseUrl}/api/shop/open-chest`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${accessToken}`
          }
        });
        if (!response.ok) throw new Error("Failed to open chest.");
        const payload = (await response.json()) as OpenChestResponse;
        if (!cancelled) {
          setWonItem(payload.item);
          setAlreadyOwned(payload.alreadyOwned);
          setErrorMessage(null);
        }
      } catch {
        if (!cancelled) setErrorMessage("Could not open chest. Please try again.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    void run();
    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  const carouselItems = useMemo(() => {
    if (!wonItem) return [] as CarouselEntry[];
    const items: CarouselEntry[] = [];
    const landingIndex =
      LANDING_MIN_INDEX + Math.floor(Math.random() * (LANDING_MAX_INDEX - LANDING_MIN_INDEX + 1));
    for (let i = 0; i < TOTAL_ITEMS; i += 1) {
      const filler = COSMETIC_CATALOG[Math.floor(Math.random() * COSMETIC_CATALOG.length)]!;
      items.push({ item: filler, isWinner: false, key: `filler-${filler.id}-${i}` });
    }
    items[landingIndex] = { item: wonItem, isWinner: true, key: `win-${wonItem.id}-${landingIndex}` };
    return items;
  }, [wonItem]);

  useEffect(() => {
    if (!wonItem || carouselItems.length === 0) return;
    const winIndex = carouselItems.findIndex((entry) => entry.isWinner);
    if (winIndex < 0) return;

    // Read actual rendered width from the DOM — avoids stale-closure issues
    // from storing viewportWidth in state (which creates a circular reference
    // because the div's own width: state drives the measurement).
    setX(0);
    const duration = 3500;
    const viewport = viewportRef.current;
    const winnerCard = winnerCardRef.current;
    const vw = viewport?.getBoundingClientRect().width ?? 640;
    const fallbackTarget = -(winIndex * CARD_STRIDE - (vw / 2 - CARD_WIDTH / 2));
    let target = fallbackTarget;
    if (viewport && winnerCard) {
      const viewportRect = viewport.getBoundingClientRect();
      const winnerRect = winnerCard.getBoundingClientRect();
      const viewportCenter = viewportRect.left + viewportRect.width / 2;
      const winnerCenter = winnerRect.left + winnerRect.width / 2;
      target = viewportCenter - winnerCenter;
    }
    const started = performance.now();
    let raf = 0;
    let lastTickTime = 0;

    const tick = (now: number) => {
      const t = Math.min(1, (now - started) / duration);
      setX(target * easeOut(t));

      // Play a tick sound that slows down as the carousel decelerates.
      // tickInterval ramps from ~60 ms at t=0 up to ~300 ms at t=1.
      const tickInterval = 60 + t * 240;
      if (lastTickTime === 0 || now - lastTickTime >= tickInterval) {
        lastTickTime = now;
        void SoundManager.playChestTick();
      }

      if (t < 1) {
        raf = requestAnimationFrame(tick);
        return;
      }
      window.setTimeout(() => {
        setRevealed(true);
        void SoundManager.playChestReveal(wonItem.rarity);
      }, 300);
    };

    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [carouselItems, wonItem]);

  return (
    <div style={{ position: "fixed", inset: 0, background: "rgba(10,10,24,0.95)", zIndex: 100, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 20, padding: "18px 12px" }}>
      {loading ? <div className="headline">Opening chest...</div> : null}
      {!loading && errorMessage ? (
        <div style={{ width: 360, maxWidth: "92vw", display: "grid", gap: 10, textAlign: "center" }}>
          <div className="headline" style={{ color: "var(--red)" }}>Chest Error</div>
          <div className="text-dim">{errorMessage}</div>
          <ArcadeButton text="Close" onClick={onClose} />
        </div>
      ) : null}
      {!loading && wonItem ? (
        <>
          <div
            style={{
              color: "var(--gold)",
              fontFamily: "\"Segoe UI Symbol\", \"Arial Unicode MS\", Arial, sans-serif",
              fontSize: "24px",
              lineHeight: 1
            }}
          >
            {"\u25bc"}
          </div>
          <div
            ref={viewportRef}
            style={{
              width: "min(760px, 96vw)",
              overflow: "hidden",
              border: "1px solid rgba(0,245,255,.3)",
              borderRadius: 10,
              padding: "14px 0",
              background: "var(--surface)",
              position: "relative"
            }}
          >
            <div
              aria-hidden
              style={{
                position: "absolute",
                top: 0,
                bottom: 0,
                left: "50%",
                width: 2,
                transform: "translateX(-1px)",
                background: "linear-gradient(180deg, rgba(255,215,0,0.1), rgba(255,215,0,0.95), rgba(255,215,0,0.1))",
                boxShadow: "0 0 12px rgba(255,215,0,.75)",
                pointerEvents: "none",
                zIndex: 2
              }}
            />
            <div
              aria-hidden
              style={{
                position: "absolute",
                inset: 0,
                pointerEvents: "none",
                zIndex: 2,
                background:
                  "linear-gradient(90deg, rgba(10,10,24,.95) 0%, rgba(10,10,24,0) 8%, rgba(10,10,24,0) 92%, rgba(10,10,24,.95) 100%)"
              }}
            />
            <div style={{ display: "flex", gap: CARD_GAP, transform: `translateX(${x}px)` }}>
              {carouselItems.map((entry) => {
                const item = entry.item;
                const won = entry.isWinner;
                return (
                  <div
                    key={entry.key}
                    ref={won ? winnerCardRef : undefined}
                    style={{
                      width: CARD_WIDTH,
                      height: 132,
                      borderLeft: `3px solid ${getRarityColor(item.rarity)}`,
                      border: `1px solid ${won && revealed ? getRarityColor(item.rarity) : "rgba(255,255,255,.12)"}`,
                      boxSizing: "border-box",
                      background: "var(--surface-variant)",
                      display: "grid",
                      alignContent: "center",
                      justifyItems: "center",
                      padding: 8,
                      transform: won && revealed ? "scale(1.2)" : "scale(1)",
                      boxShadow: won && revealed ? `0 0 22px ${getRarityColor(item.rarity)}` : "none",
                      transition: "all 300ms ease"
                    }}
                  >
                    <div className={`${getCosmeticClass(item.id)} label`} style={{ fontSize: 11, lineHeight: 1.15, textAlign: "center" }}>
                      {item.name}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
          {revealed ? (
            <>
              <div className={getCosmeticClass(wonItem.id)} style={{ color: getRarityColor(wonItem.rarity), fontFamily: "var(--font-pixel)" }}>
                {wonItem.name} ({getRarityLabel(wonItem.rarity)})
              </div>
              <div className="text-dim" style={{ marginTop: 2 }}>{alreadyOwned ? "Already owned (rerolled)." : "NEW ITEM!"}</div>
              <div style={{ display: "grid", gap: 10, width: 360, maxWidth: "92vw", marginTop: 6 }}>
                <ArcadeButton text="Equip Now" onClick={() => { onEquip(wonItem.id); onClose(); }} accent="gold" />
                <ArcadeButton text="Close" onClick={onClose} />
              </div>
            </>
          ) : null}
        </>
      ) : null}
    </div>
  );
};
