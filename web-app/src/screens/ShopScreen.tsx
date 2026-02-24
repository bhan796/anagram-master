import { useEffect, useMemo, useState } from "react";
import { ArcadeBackButton, ArcadeButton, ArcadeScaffold, NeonTitle } from "../components/ArcadeComponents";
import { ChestOpenModal } from "../components/ChestOpenModal";
import { getCosmeticClass, getRarityColor, getRarityLabel } from "../lib/cosmetics";

type CosmeticItem = {
  id: string;
  name: string;
  rarity: string;
  cssClass: string;
  description: string;
  obtainedAt?: string;
};

type InventoryResponse = {
  items: CosmeticItem[];
  equippedCosmetic: string | null;
  pendingChests: number;
  runes: number;
};

interface ShopScreenProps {
  accessToken: string;
  onBack: () => void;
}

const normalizeBackendUrl = (raw: string | undefined): string => {
  const candidate = (raw ?? "").trim();
  if (!candidate) return "http://localhost:4000";
  if (/^https?:\/\//i.test(candidate)) return candidate.replace(/\/+$/, "");
  return `https://${candidate.replace(/\/+$/, "")}`;
};
const apiBaseUrl = normalizeBackendUrl(import.meta.env.VITE_SERVER_URL as string | undefined);

export const ShopScreen = ({ accessToken, onBack }: ShopScreenProps) => {
  const [inventory, setInventory] = useState<InventoryResponse>({
    items: [],
    equippedCosmetic: null,
    pendingChests: 0,
    runes: 0
  });
  const [showChestModal, setShowChestModal] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isPurchasing, setIsPurchasing] = useState(false);

  const loadInventory = async () => {
    const response = await fetch(`${apiBaseUrl}/api/inventory`, {
      headers: { Authorization: `Bearer ${accessToken}` }
    });
    if (!response.ok) return;
    const payload = (await response.json()) as InventoryResponse;
    setInventory(payload);
  };

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        await loadInventory();
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    };
    void load();
    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  const purchaseDisabled = inventory.runes < 200 || isPurchasing;

  const handlePurchase = async () => {
    setIsPurchasing(true);
    try {
      const response = await fetch(`${apiBaseUrl}/api/shop/purchase`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`
        }
      });
      if (!response.ok) return;
      const payload = (await response.json()) as { remainingRunes: number; pendingChests: number };
      setInventory((previous) => ({ ...previous, runes: payload.remainingRunes, pendingChests: payload.pendingChests }));
    } finally {
      setIsPurchasing(false);
    }
  };

  const handleEquip = async (itemId: string | null) => {
    await fetch(`${apiBaseUrl}/api/inventory/equip`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`
      },
      body: JSON.stringify({ itemId })
    });
    setInventory((previous) => ({ ...previous, equippedCosmetic: itemId }));
  };

  const inventoryCards = useMemo(
    () =>
      inventory.items.map((item) => (
        <div key={item.id} style={{ border: `1px solid ${getRarityColor(item.rarity)}88`, background: "var(--surface-variant)", borderRadius: 4, padding: 8, display: "grid", gap: 6 }}>
          <div className={getCosmeticClass(item.id)} style={{ fontFamily: "var(--font-pixel)", fontSize: "var(--text-label)" }}>
            {item.name}
          </div>
          <div className={`text-dim ${item.rarity === "mythic" ? "rarity-mythic" : ""}`} style={{ color: getRarityColor(item.rarity) }}>{getRarityLabel(item.rarity)}</div>
          <ArcadeButton
            text={inventory.equippedCosmetic === item.id ? "Equipped" : "Equip"}
            onClick={() => {
              void handleEquip(inventory.equippedCosmetic === item.id ? null : item.id);
            }}
            accent={inventory.equippedCosmetic === item.id ? "gold" : "cyan"}
          />
        </div>
      )),
    [inventory.equippedCosmetic, inventory.items]
  );

  return (
    <ArcadeScaffold>
      <ArcadeBackButton onClick={onBack} />
      <NeonTitle text="SHOP" />
      <div className="headline" style={{ color: "var(--gold)" }}>? {inventory.runes.toLocaleString()} RUNES</div>
      {isLoading ? <div className="text-dim">Loading...</div> : null}
      <div className="card" style={{ display: "grid", gap: 8 }}>
        <div className="label">TREASURE CHEST</div>
        <div className="text-dim">Contains a random cosmetic for your display name.</div>
        <div className="text-dim" style={{ color: "var(--gold)" }}>200 ? RUNES</div>
        <ArcadeButton text="Purchase" onClick={() => void handlePurchase()} disabled={purchaseDisabled} />
        <ArcadeButton text={`Open Chest (${inventory.pendingChests})`} onClick={() => setShowChestModal(true)} disabled={inventory.pendingChests < 1} accent="gold" />
      </div>

      <div className="headline">MY COSMETICS</div>
      <div style={{ display: "grid", gap: 8, gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))" }}>{inventoryCards}</div>

      {showChestModal ? (
        <ChestOpenModal
          accessToken={accessToken}
          onClose={() => {
            setShowChestModal(false);
            void loadInventory();
          }}
          onEquip={(itemId) => {
            void handleEquip(itemId);
          }}
        />
      ) : null}
    </ArcadeScaffold>
  );
};