import { useEffect, useMemo, useRef, useState } from "react";
import type { MatchStatePayload, OnlineUiState } from "../types/online";
import {
  ArcadeButton,
  ArcadeScaffold,
  LetterTile,
  NeonTitle,
  RankBadge,
  ScoreBadge,
  TimerBar,
  WordTiles
} from "../components/ArcadeComponents";
import { TapLetterComposer } from "../components/TapLetterComposer";
import * as SoundManager from "../sound/SoundManager";

interface OnlineMatchScreenProps {
  state: OnlineUiState;
  onPickVowel: () => void;
  onPickConsonant: () => void;
  onWordChange: (value: string) => void;
  onSubmitWord: () => void;
  onConundrumGuessChange: (value: string) => void;
  onSubmitConundrumGuess: () => void;
  onDismissError: () => void;
  onLeaveGame: () => void;
  onPlayAgain: () => void;
  onBackToHome: () => void;
}

const phaseLabel = (phase: MatchStatePayload["phase"]) => phase.replaceAll("_", " ");

const phaseTotalSeconds = (phase: MatchStatePayload["phase"]): number => {
  if (phase === "awaiting_letters_pick") return 20;
  if (phase === "round_result") return 5;
  if (phase === "letters_solving" || phase === "conundrum_solving") return 30;
  return 0;
};

const LetterSlots = ({ letters }: { letters: string[] }) => (
  <div className="letter-row">
    {Array.from({ length: 9 }).map((_, index) => {
      const letter = letters[index] ?? "_";
      return <LetterTile key={`slot-${index}`} letter={letter} empty={letter === "_"} />;
    })}
  </div>
);

const PlayerResultRow = ({
  name,
  word,
  points,
  extra,
  extraColor
}: {
  name: string;
  word: string;
  points: number;
  extra: string;
  extraColor: string;
}) => (
  <div style={{ display: "grid", gap: 6 }}>
    <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
      <div className="text-dim">{name}</div>
      <div className="label" style={{ color: "var(--cyan)" }}>
        {points} pts
      </div>
    </div>
    <WordTiles word={word || "-"} accent="var(--cyan)" />
    <div className="text-dim" style={{ color: extraColor }}>
      {extra}
    </div>
  </div>
);

export const OnlineMatchScreen = ({
  state,
  onPickVowel,
  onPickConsonant,
  onWordChange,
  onSubmitWord,
  onConundrumGuessChange,
  onSubmitConundrumGuess,
  onDismissError,
  onLeaveGame,
  onPlayAgain,
  onBackToHome
}: OnlineMatchScreenProps) => {
  const match = state.matchState;
  const previousPhaseRef = useRef<MatchStatePayload["phase"] | undefined>(undefined);
  const isFinished = match?.phase === "finished";
  const [showLeaveConfirm, setShowLeaveConfirm] = useState(false);

  useEffect(() => {
    if (isFinished) {
      setShowLeaveConfirm(false);
    }
  }, [isFinished]);

  const winnerLabel = useMemo(() => {
    if (!match || match.phase !== "finished") return "";
    if (!match.winnerPlayerId) return "Draw";
    return match.winnerPlayerId === state.playerId ? "You Win" : "You Lose";
  }, [match, state.playerId]);

  const [animatedDelta, setAnimatedDelta] = useState(0);
  const myDelta = useMemo(() => {
    if (!match || match.phase !== "finished" || !state.playerId) return 0;
    return match.ratingChanges?.[state.playerId] ?? 0;
  }, [match, state.playerId]);

  useEffect(() => {
    if (!match || match.phase !== "finished") return;
    setAnimatedDelta(0);
    const step = myDelta > 0 ? 1 : myDelta < 0 ? -1 : 0;
    if (step === 0) return;
    const timer = window.setInterval(() => {
      setAnimatedDelta((prev) => {
        if (prev === myDelta) {
          window.clearInterval(timer);
          return prev;
        }
        const next = prev + step;
        if ((step > 0 && next > myDelta) || (step < 0 && next < myDelta)) {
          window.clearInterval(timer);
          return myDelta;
        }
        return next;
      });
    }, 20);
    return () => window.clearInterval(timer);
  }, [match, myDelta]);

  useEffect(() => {
    const phase = state.matchState?.phase;
    if (!phase || phase === previousPhaseRef.current) return;

    if (phase === "round_result") {
      void SoundManager.playRoundResult();
    }

    if (phase === "finished") {
      const didWin = state.matchState?.winnerPlayerId === state.playerId;
      if (didWin) {
        void SoundManager.playWin();
      } else {
        void SoundManager.playLose();
      }
    }

    previousPhaseRef.current = phase;
  }, [state.matchState?.phase, state.matchState?.winnerPlayerId, state.playerId]);

  return (
    <ArcadeScaffold>
      {isFinished ? (
        <>
          <ArcadeButton text="Play Again" onClick={onPlayAgain} />
          <ArcadeButton text="Back Home" onClick={onBackToHome} accent="gold" />
        </>
      ) : (
        <>
          <ArcadeButton text="Leave Game" onClick={() => setShowLeaveConfirm(true)} accent="gold" />
          <div className="text-dim" style={{ color: "var(--red)" }}>
            If you leave now, you will forfeit the match.
          </div>
        </>
      )}

      {!match ? (
        <>
          <NeonTitle text="Match" />
          <div className="text-dim">Waiting for match state...</div>
        </>
      ) : (
        <>
          <NeonTitle text={`Round ${match.roundNumber}`} />
          <NeonTitle text={phaseLabel(match.phase)} />
          <TimerBar secondsRemaining={state.secondsRemaining} totalSeconds={phaseTotalSeconds(match.phase)} />
          <div className="text-dim">{state.statusMessage}</div>

          {state.myPlayer && state.opponentPlayer ? (
            <div className="score-row">
              <ScoreBadge label={state.myPlayer.displayName} score={state.myPlayer.score} />
              <ScoreBadge label={state.opponentPlayer.displayName} score={state.opponentPlayer.score} color="var(--gold)" />
            </div>
          ) : null}

          {state.lastError ? (
            <div className="card warning" style={{ display: "grid", gap: 10 }}>
              <div>Error: {state.lastError.message}</div>
              <ArcadeButton text="Dismiss" onClick={onDismissError} />
            </div>
          ) : null}

          {state.localValidationMessage ? (
            <div className="card" style={{ color: "var(--red)" }}>
              {state.localValidationMessage}
            </div>
          ) : null}

          {match.phase === "awaiting_letters_pick" ? (
            <>
              <div className="headline">{state.isMyTurnToPick ? "Your turn to pick" : "Opponent is picking"}</div>
              <LetterSlots letters={match.letters} />
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
                <ArcadeButton
                  text="Vowel"
                  onClick={() => {
                    void SoundManager.playTilePlace();
                    onPickVowel();
                  }}
                  disabled={!state.isMyTurnToPick || state.connectionState === "reconnecting"}
                />
                <ArcadeButton
                  text="Consonant"
                  onClick={() => {
                    void SoundManager.playTilePlace();
                    onPickConsonant();
                  }}
                  disabled={!state.isMyTurnToPick || state.connectionState === "reconnecting"}
                />
              </div>
            </>
          ) : null}

          {match.phase === "letters_solving" ? (
            <>
              <TapLetterComposer
                letters={match.letters}
                value={state.wordInput}
                onValueChange={onWordChange}
                disabled={state.hasSubmittedWord}
                onSubmit={() => {
                  void SoundManager.playWordSubmit();
                  onSubmitWord();
                }}
              />
              <ArcadeButton
                text={state.hasSubmittedWord ? "Submitted" : "Submit Word"}
                onClick={() => {
                  void SoundManager.playWordSubmit();
                  onSubmitWord();
                }}
                disabled={state.hasSubmittedWord}
              />
            </>
          ) : null}

          {match.phase === "conundrum_solving" ? (
            <>
              <NeonTitle text="Conundrum" />
              <div className="card" style={{ textAlign: "center", borderColor: "rgba(0,245,255,.45)" }}>
                <div
                  className="headline"
                  style={{
                    color: "var(--gold)",
                    letterSpacing: "0.5em",
                    fontSize: "clamp(18px, 5vw, 28px)",
                    lineHeight: 1.5
                  }}
                >
                  {match.scrambled?.toUpperCase()}
                </div>
              </div>
              <TapLetterComposer
                letters={(match.scrambled ?? "").toUpperCase().split("")}
                value={state.conundrumGuessInput.toUpperCase()}
                onValueChange={onConundrumGuessChange}
                onSubmit={onSubmitConundrumGuess}
              />
              <ArcadeButton text="Submit Guess" onClick={onSubmitConundrumGuess} />
              <div className="text-dim">Multiple guesses allowed. Respect rate limit.</div>
            </>
          ) : null}

          {match.phase === "round_result" ? (
            <>
              <NeonTitle text="Round Result" />
              {match.roundResults.at(-1) ? (
                <div className="card" style={{ display: "grid", gap: 12 }}>
                  {match.roundResults.at(-1)?.type === "letters" ? (
                    <>
                      <WordTiles label="Letters" word={match.roundResults.at(-1)?.letters?.join("") ?? ""} accent="var(--gold)" />
                      {match.players.map((player) => {
                        const submission = match.roundResults.at(-1)?.submissions?.[player.playerId];
                        const points = match.roundResults.at(-1)?.awardedScores[player.playerId] ?? 0;
                        const valid = submission?.isValid ?? false;
                        return (
                          <PlayerResultRow
                            key={player.playerId}
                            name={player.displayName}
                            word={submission?.word ?? "-"}
                            points={points}
                            extra={submission ? (valid ? "Valid" : "Invalid") : "No submission"}
                            extraColor={valid ? "var(--green)" : "var(--red)"}
                          />
                        );
                      })}
                    </>
                  ) : (
                    <>
                      <WordTiles label="Scramble" word={match.roundResults.at(-1)?.scrambled ?? ""} accent="var(--gold)" />
                      <WordTiles label="Answer" word={match.roundResults.at(-1)?.answer ?? ""} accent="var(--cyan)" />
                      {match.players.map((player) => {
                        const points = match.roundResults.at(-1)?.awardedScores[player.playerId] ?? 0;
                        const solved = match.roundResults.at(-1)?.firstCorrectPlayerId === player.playerId;
                        return (
                          <PlayerResultRow
                            key={player.playerId}
                            name={player.displayName}
                            word={solved ? (match.roundResults.at(-1)?.answer ?? "") : "-"}
                            points={points}
                            extra={solved ? "Solved first" : "Not solved"}
                            extraColor={solved ? "var(--green)" : "var(--dim)"}
                          />
                        );
                      })}
                    </>
                  )}
                </div>
              ) : null}
              <div className="card text-dim">Next round starts in 5 seconds...</div>
            </>
          ) : null}

          {match.phase === "finished" ? (
            <>
              <NeonTitle text="Final Result" />
              <div className="headline">{winnerLabel}</div>
              <div className="card" style={{ display: "grid", gap: 10 }}>
                <div style={{ display: "flex", justifyContent: "space-between", gap: 10, alignItems: "center" }}>
                  <div className="text-dim">Match Mode</div>
                  <div className="label" style={{ color: "var(--gold)" }}>
                    {(match.mode ?? "casual").toUpperCase()}
                  </div>
                </div>
                {state.myPlayer ? (
                  <>
                    <div style={{ display: "flex", justifyContent: "space-between", gap: 10, alignItems: "center" }}>
                      <div className="text-dim">Current Rank</div>
                      <RankBadge tier={state.myPlayer.rankTier} />
                    </div>
                    {(match.mode ?? "casual") === "ranked" ? (
                      <div style={{ display: "flex", justifyContent: "space-between", gap: 10, alignItems: "center" }}>
                        <div className="text-dim">Rating Change</div>
                        <div
                          className="headline"
                          style={{
                            color: animatedDelta >= 0 ? "var(--green)" : "var(--red)",
                            textShadow: `0 0 16px ${animatedDelta >= 0 ? "rgba(57,255,20,.4)" : "rgba(255,58,58,.45)"}`
                          }}
                        >
                          {animatedDelta >= 0 ? `+${animatedDelta}` : animatedDelta}
                        </div>
                      </div>
                    ) : null}
                  </>
                ) : null}
              </div>
              <div style={{ display: "grid", gap: 12, maxHeight: "48vh", overflow: "auto" }}>
                {match.roundResults.map((round) => (
                  <div key={`round-${round.roundNumber}`} className="card" style={{ display: "grid", gap: 10 }}>
                    <div className="headline" style={{ fontSize: "clamp(12px,1.5vw,14px)" }}>
                      Round {round.roundNumber} {round.type.toLowerCase()}
                    </div>
                    {round.type === "letters" ? (
                      <WordTiles label="Letters" word={round.letters?.join("") ?? ""} accent="var(--gold)" />
                    ) : (
                      <WordTiles label="Answer" word={round.answer ?? ""} accent="var(--gold)" />
                    )}
                    {match.players.map((player) => {
                      const word =
                        round.type === "letters"
                          ? (round.submissions?.[player.playerId]?.word ?? "-")
                          : round.firstCorrectPlayerId === player.playerId
                            ? (round.answer ?? "-")
                            : "-";
                      const points = round.awardedScores[player.playerId] ?? 0;
                      return <PlayerResultRow key={`${round.roundNumber}-${player.playerId}`} name={player.displayName} word={word} points={points} extra="" extraColor="var(--dim)" />;
                    })}
                  </div>
                ))}
              </div>
            </>
          ) : null}
        </>
      )}

      {showLeaveConfirm ? (
        <div className="modal-backdrop">
          <div className="modal">
            <div className="headline">Leave game?</div>
            <div className="text-dim">Leaving an active match counts as a forfeit and awards your opponent the win.</div>
            <ArcadeButton
              text="Forfeit & Leave"
              onClick={() => {
                setShowLeaveConfirm(false);
                onLeaveGame();
                onBackToHome();
              }}
            />
            <ArcadeButton text="Stay" onClick={() => setShowLeaveConfirm(false)} />
          </div>
        </div>
      ) : null}
    </ArcadeScaffold>
  );
};
