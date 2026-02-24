import { useEffect, useMemo, useRef, useState } from "react";
import { ArcadeButton } from "./ArcadeComponents";
import { COSMETIC_CATALOG, type CosmeticItem } from "../lib/cosmeticCatalog";
import { getCosmeticClass, getRarityColor, getRarityLabel } from "../lib/cosmetics";

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

export const ChestOpenModal = ({ accessToken, onClose, onEquip }: ChestOpenModalProps) => {
  const [loading, setLoading] = useState(true);
  const [wonItem, setWonItem] = useState<CosmeticItem | null>(null);
  const [alreadyOwned, setAlreadyOwned] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [revealed, setRevealed] = useState(false);
  const [x, setX] = useState(0);
  const stripRef = useRef<HTMLDivElement | null>(null);
  const hasOpenedRef = useRef(false);
  const viewportWidth = 640;

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
    if (!wonItem) return [] as CosmeticItem[];
    const items: CosmeticItem[] = [];
    for (let i = 0; i < 35; i += 1) {
      items.push(COSMETIC_CATALOG[i % COSMETIC_CATALOG.length]!);
    }
    items.push(wonItem);
    for (let i = 0; i < 6; i += 1) {
      items.push(COSMETIC_CATALOG[(i + 9) % COSMETIC_CATALOG.length]!);
    }
    return items;
  }, [wonItem]);

  useEffect(() => {
    if (!wonItem || carouselItems.length === 0) return;
    const duration = 3500;
    const cardWidth = 83;
    const target = -(35 * cardWidth - viewportWidth / 2 + 40);
    const started = performance.now();
    let raf = 0;

    const tick = (now: number) => {
      const t = Math.min(1, (now - started) / duration);
      setX(target * easeOut(t));
      if (t < 1) {
        raf = requestAnimationFrame(tick);
        return;
      }
      window.setTimeout(() => setRevealed(true), 300);
    };

    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [carouselItems, wonItem]);

  return (
    <div style={{ position: "fixed", inset: 0, background: "rgba(10,10,24,0.95)", zIndex: 100, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 14 }}>
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
          <div style={{ color: "var(--gold)", fontFamily: "var(--font-pixel)" }}>\u25BC</div>
          <div style={{ width: viewportWidth, maxWidth: "92vw", overflow: "hidden", border: "1px solid rgba(0,245,255,.3)", borderRadius: 8, padding: "10px 0", background: "var(--surface)" }}>
            <div ref={stripRef} style={{ display: "flex", gap: 3, transform: `translateX(${x}px)` }}>
              {carouselItems.map((item, index) => {
                const won = index === 35;
                return (
                  <div
                    key={`${item.id}-${index}`}
                    style={{
                      width: 80,
                      height: 120,
                      borderLeft: `3px solid ${getRarityColor(item.rarity)}`,
                      border: `1px solid ${won && revealed ? getRarityColor(item.rarity) : "rgba(255,255,255,.12)"}`,
                      background: "var(--surface-variant)",
                      display: "grid",
                      alignContent: "center",
                      justifyItems: "center",
                      padding: 6,
                      transform: won && revealed ? "scale(1.2)" : "scale(1)",
                      boxShadow: won && revealed ? `0 0 22px ${getRarityColor(item.rarity)}` : "none",
                      transition: "all 300ms ease"
                    }}
                  >
                    <div className={`${getCosmeticClass(item.id)} label`} style={{ fontSize: 9, textAlign: "center" }}>
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
              <div className="text-dim">{alreadyOwned ? "Already owned (rerolled)." : "NEW ITEM!"}</div>
              <div style={{ display: "grid", gap: 8, width: 320, maxWidth: "90vw" }}>
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
