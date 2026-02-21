import { ArcadeBackButton, ArcadeScaffold, NeonTitle } from "../components/ArcadeComponents";

interface HowToPlayScreenProps {
  onBack: () => void;
}

export const HowToPlayScreen = ({ onBack }: HowToPlayScreenProps) => (
  <ArcadeScaffold>
    <ArcadeBackButton onClick={onBack} />
    <NeonTitle text="How To Play" />

    <div className="card" style={{ display: "grid", gap: 10 }}>
      <div className="headline">Match Format</div>
      <div className="text-dim">5 rounds total.</div>
      <div className="text-dim">Rounds 1-4: Letters. Round 5: Conundrum.</div>
    </div>

    <div className="card" style={{ display: "grid", gap: 10 }}>
      <div className="headline">Letters Rounds</div>
      <div className="text-dim">Picker chooses Vowel or Consonant to build 9 letters.</div>
      <div className="text-dim">At least one vowel and one consonant are required.</div>
      <div className="text-dim">20 seconds to pick. Remaining letters auto-fill on timeout.</div>
      <div className="text-dim">Both players then have 30 seconds to submit their best word.</div>
      <div className="text-dim">Score = word length, except valid 9-letter word = 12 points.</div>
    </div>

    <div className="card" style={{ display: "grid", gap: 10 }}>
      <div className="headline">Conundrum Round</div>
      <div className="text-dim">Unscramble the 9-letter conundrum in 30 seconds.</div>
      <div className="text-dim">First correct answer gets 12 points.</div>
    </div>

    <div className="card" style={{ display: "grid", gap: 10 }}>
      <div className="headline">Winning</div>
      <div className="text-dim">Highest total score after 5 rounds wins.</div>
      <div className="text-dim">Leaving early forfeits the match.</div>
    </div>
  </ArcadeScaffold>
);
