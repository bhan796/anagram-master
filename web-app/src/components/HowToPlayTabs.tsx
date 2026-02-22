import { useEffect, useRef, useState } from "react";
import { LetterTile, RankBadge, ScoreBadge } from "./ArcadeComponents";

const TABS = [
  { id: "match", label: "MATCH", icon: "?" },
  { id: "letters", label: "LETTERS", icon: "A" },
  { id: "conundrum", label: "COND'M", icon: "?" },
  { id: "winning", label: "WINNING", icon: "?" }
] as const;

type TabId = (typeof TABS)[number]["id"];

const EXAMPLE_LETTERS_ALL = ["C", "A", "T", "S", "R", "O", "N", "E", "L"];

export const HowToPlayTabs = () => {
  const [activeTab, setActiveTab] = useState<TabId>("match");
  const [indicatorStyle, setIndicatorStyle] = useState({ left: "0px", width: "0px" });
  const tabsRef = useRef<HTMLDivElement>(null);
  const [revealedCount, setRevealedCount] = useState(0);

  useEffect(() => {
    const container = tabsRef.current;
    if (!container) return;
    const activeEl = container.querySelector<HTMLButtonElement>(".htp-tab.active");
    if (!activeEl) return;
    setIndicatorStyle({
      left: `${activeEl.offsetLeft}px`,
      width: `${activeEl.offsetWidth}px`
    });
  }, [activeTab]);

  useEffect(() => {
    if (activeTab !== "letters") {
      setRevealedCount(0);
      return;
    }

    setRevealedCount(0);
    let count = 0;
    const interval = window.setInterval(() => {
      count += 1;
      setRevealedCount(count);
      if (count >= EXAMPLE_LETTERS_ALL.length) {
        window.clearInterval(interval);
      }
    }, 80);

    return () => window.clearInterval(interval);
  }, [activeTab]);

  return (
    <>
      <div className="htp-tabs" ref={tabsRef}>
        {TABS.map((tab) => (
          <button
            key={tab.id}
            className={`htp-tab${activeTab === tab.id ? " active" : ""}`}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.icon} {tab.label}
          </button>
        ))}
        <div className="htp-tab-indicator" style={indicatorStyle} />
      </div>

      <div className="htp-panel" key={activeTab}>
        {activeTab === "match" ? (
          <div className="htp-card" style={{ borderColor: "rgba(255,0,204,.35)" }}>
            <div className="htp-card-header">
              <span className="htp-icon" style={{ color: "var(--magenta)" }}>
                ?
              </span>
              <span className="headline" style={{ color: "var(--magenta)" }}>
                Match Format
              </span>
            </div>

            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <span
                style={{
                  fontFamily: "var(--font-orbitron)",
                  fontSize: "clamp(36px,6vw,52px)",
                  color: "var(--cyan)"
                }}
              >
                5
              </span>
              <span className="headline">Rounds Total</span>
            </div>

            <div className="htp-rounds-row">
              {[1, 2, 3, 4].map((number) => (
                <div
                  key={number}
                  className="htp-round-chip"
                  style={{ borderColor: "var(--cyan)", color: "var(--cyan)" }}
                >
                  <span>{number}</span>
                  <span style={{ fontSize: "5px" }}>LTR</span>
                </div>
              ))}
              <div className="htp-round-chip" style={{ borderColor: "var(--gold)", color: "var(--gold)" }}>
                <span>5</span>
                <span style={{ fontSize: "5px" }}>CON</span>
              </div>
            </div>

            <div className="text-dim">
              Rounds 1–4 are Letters rounds. Round 5 is the Conundrum. Highest total score wins.
            </div>
          </div>
        ) : null}

        {activeTab === "letters" ? (
          <div className="htp-card" style={{ borderColor: "rgba(0,245,255,.35)" }}>
            <div className="htp-card-header">
              <LetterTile letter="A" accent="var(--cyan)" style={{ width: 24, height: 24, fontSize: 11 }} />
              <span className="headline">Letters Round</span>
            </div>

            <div className="letter-row" style={{ justifyContent: "flex-start" }}>
              {EXAMPLE_LETTERS_ALL.map((letter, index) => (
                <LetterTile
                  key={`${letter}-${index}`}
                  letter={letter}
                  empty={index >= revealedCount}
                  style={{
                    transition: "border-color 200ms, color 200ms, opacity 200ms",
                    opacity: index < revealedCount ? 1 : 0.25
                  }}
                />
              ))}
            </div>

            <div style={{ display: "grid", gap: 4 }}>
              {[
                ["3 letters", "3 pts"],
                ["5 letters", "5 pts"],
                ["7 letters", "7 pts"],
                ["9 letters", "12 pts ?"]
              ].map(([key, value]) => (
                <div key={key} className="htp-stat-row">
                  <span className="htp-stat-key">{key}</span>
                  <span className="htp-stat-val">{value}</span>
                </div>
              ))}
            </div>

            <div className="text-dim">
              Picker chooses Vowel or Consonant to build 9 letters. Both players get 30s to submit their best word.
            </div>
          </div>
        ) : null}

        {activeTab === "conundrum" ? (
          <div className="htp-card" style={{ borderColor: "rgba(255,215,0,.35)" }}>
            <div className="htp-card-header">
              <span className="htp-icon" style={{ color: "var(--gold)" }}>
                ?
              </span>
              <span className="headline" style={{ color: "var(--gold)" }}>
                Conundrum
              </span>
            </div>

            <div className="htp-conundrum-scramble">RANALUGAM</div>
            <div className="htp-reveal-arrow">?</div>

            <div className="letter-row" style={{ justifyContent: "flex-start" }}>
              {"ANAGRAM".split("").map((letter, index) => (
                <LetterTile key={`${letter}-${index}`} letter={letter} accent="var(--green)" />
              ))}
            </div>

            <div className="text-dim">Unscramble the 9-letter word in 30 seconds. First correct answer scores 12 points.</div>
          </div>
        ) : null}

        {activeTab === "winning" ? (
          <div className="htp-card" style={{ borderColor: "rgba(255,215,0,.35)" }}>
            <div className="htp-card-header">
              <span className="htp-icon" style={{ color: "var(--gold)" }}>
                ?
              </span>
              <span className="headline" style={{ color: "var(--gold)" }}>
                Winning
              </span>
            </div>

            <div className="htp-mock-score">
              <ScoreBadge label="YOU" score={42} color="var(--cyan)" />
              <span className="headline" style={{ color: "var(--dim)" }}>
                VS
              </span>
              <ScoreBadge label="OPP" score={38} color="var(--gold)" />
            </div>

            <div style={{ display: "flex", justifyContent: "center" }}>
              <RankBadge tier="gold" />
            </div>

            <div className="text-dim">Highest total score after 5 rounds wins. Leaving an active match forfeits the game.</div>
          </div>
        ) : null}
      </div>
    </>
  );
};
