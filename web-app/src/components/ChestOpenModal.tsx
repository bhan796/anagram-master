import { type CSSProperties, useEffect, useMemo, useRef, useState } from "react";
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
const RARITY_INTENSITY: Record<string, number> = {
  common: 1,
  uncommon: 2,
  rare: 3,
  epic: 4,
  legendary: 5,
  mythic: 6
};

const getRarityBadge = (rarity: string): string => {
  switch (rarity) {
    case "common":
      return "C";
    case "uncommon":
      return "U";
    case "rare":
      return "R";
    case "epic":
      return "E";
    case "legendary":
      return "L";
    case "mythic":
      return "M";
    default:
      return "?";
  }
};

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
  const [flashActive, setFlashActive] = useState(false);
  const [shakeActive, setShakeActive] = useState(false);
  const [ringActive, setRingActive] = useState(false);
  const [blackoutActive, setBlackoutActive] = useState(false);
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

  const revealIntensity = wonItem ? RARITY_INTENSITY[wonItem.rarity] ?? 1 : 1;
  const rarityColor = wonItem ? getRarityColor(wonItem.rarity) : "#aaaaaa";

  const getWinnerRevealShadow = (rarity: string): string => {
    switch (rarity) {
      case "common":
        return "0 0 8px #aaaaaa44";
      case "uncommon":
        return "0 0 14px #39ff14, 0 0 28px #39ff1444";
      case "rare":
        return "0 0 20px #00f5ff, 0 0 40px #00f5ff66";
      case "epic":
        return "0 0 28px #cc44ff, 0 0 60px #cc44ff88, 0 0 100px #cc44ff22";
      case "legendary":
      case "mythic":
        return "0 0 40px #ffd700, 0 0 80px #ffd70066, 0 0 120px #ffd70033";
      default:
        return "0 0 8px #aaaaaa44";
    }
  };

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
        const rarity = wonItem.rarity;
        const doReveal = () => {
          setRevealed(true);
          if ((RARITY_INTENSITY[rarity] ?? 1) >= 3) {
            setFlashActive(true);
            window.setTimeout(() => setFlashActive(false), 400);
          }
          if ((RARITY_INTENSITY[rarity] ?? 1) >= 4) {
            setShakeActive(true);
            window.setTimeout(() => setShakeActive(false), 300);
          }
          if ((RARITY_INTENSITY[rarity] ?? 1) >= 5) {
            if (rarity === "mythic") {
              setRingActive(true);
              window.setTimeout(() => setRingActive(false), 800);
            } else {
              window.setTimeout(() => {
                setRingActive(true);
                window.setTimeout(() => setRingActive(false), 800);
              }, 400);
            }
          }
          void SoundManager.playChestReveal(rarity);
        };

        if (rarity === "mythic") {
          setBlackoutActive(true);
          window.setTimeout(() => {
            setBlackoutActive(false);
            doReveal();
          }, 600);
          return;
        }

        doReveal();
      }, 300);
    };

    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [carouselItems, wonItem]);

  return (
    <div
      className={`chest-open-modal-root${shakeActive ? " chest-open-modal-root--shake" : ""}${blackoutActive ? " chest-open-modal-root--blackout" : ""}`}
      style={{ position: "fixed", inset: 0, background: "rgba(10,10,24,0.95)", zIndex: 100, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 20, padding: "18px 12px", "--reveal-intensity": revealIntensity } as CSSProperties}
    >
      {flashActive ? <div className="reveal-flash" style={{ "--reveal-color": rarityColor } as CSSProperties} /> : null}
      {ringActive ? <div className="reveal-ring" style={{ "--reveal-color": rarityColor } as CSSProperties} /> : null}
      {blackoutActive && wonItem?.rarity === "mythic" ? <div className="mythic-scanline" /> : null}
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
                    className={`reel-card reel-card--${item.rarity}${won && revealed ? " reel-card--winner-revealed" : ""}${won && revealed && item.rarity === "legendary" ? " reel-card--winner-legendary" : ""}${won && revealed && item.rarity === "mythic" ? " reel-card--winner-mythic" : ""}`}
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
                      transform: won && revealed ? "scale(1.25)" : "scale(1)",
                      boxShadow: won && revealed ? getWinnerRevealShadow(item.rarity) : "none",
                      transition: "all 300ms ease"
                    }}
                  >
                    <div
                      style={{
                        minWidth: 30,
                        padding: "5px 8px",
                        borderRadius: 999,
                        border: `1px solid ${getRarityColor(item.rarity)}`,
                        color: getRarityColor(item.rarity),
                        background: "rgba(10, 10, 24, 0.5)",
                        fontFamily: "var(--font-pixel)",
                        fontSize: 11,
                        letterSpacing: "0.04em",
                        lineHeight: 1,
                        textAlign: "center"
                      }}
                    >
                      {getRarityBadge(item.rarity)}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
          {revealed ? (
            <>
              {wonItem.rarity === "mythic" ? (
                <div className="mythic-particles" aria-hidden>
                  <div className="mythic-particle mythic-particle--1" />
                  <div className="mythic-particle mythic-particle--2" />
                  <div className="mythic-particle mythic-particle--3" />
                  <div className="mythic-particle mythic-particle--4" />
                  <div className="mythic-particle mythic-particle--5" />
                  <div className="mythic-particle mythic-particle--6" />
                  <div className="mythic-particle mythic-particle--7" />
                  <div className="mythic-particle mythic-particle--8" />
                </div>
              ) : null}
              <div className={getCosmeticClass(wonItem.id)} style={{ color: getRarityColor(wonItem.rarity), fontFamily: "var(--font-pixel)" }}>
                {wonItem.name}
              </div>
              <div className={`rarity-reveal rarity-reveal--${wonItem.rarity}${wonItem.rarity === "mythic" ? " mythic-glitch-once" : ""}`}>
                {getRarityLabel(wonItem.rarity).toUpperCase()}
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
