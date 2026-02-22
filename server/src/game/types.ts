export type MatchPhase =
  | "awaiting_letters_pick"
  | "letters_solving"
  | "conundrum_solving"
  | "round_result"
  | "finished";

export type RoundType = "letters" | "conundrum";
export type PickKind = "vowel" | "consonant";
export type MatchMode = "casual" | "ranked";

export interface PlayerRuntime {
  playerId: string;
  displayName: string;
  userId: string | null;
  socketId: string | null;
  connected: boolean;
  matchId: string | null;
  queuedMode: MatchMode | null;
  lastConundrumGuessAtMs: number;
  rating: number;
  rankedGames: number;
  rankedWins: number;
  rankedLosses: number;
  rankedDraws: number;
  peakRating: number;
}

export interface RoundPlan {
  roundNumber: number;
  type: RoundType;
  pickerPlayerId?: string;
}

export interface WordSubmission {
  word: string;
  normalizedWord: string;
  isValid: boolean;
  failureCode?: "empty" | "non_alphabetical" | "not_in_dictionary" | "not_constructable";
  score: number;
  submittedAtMs: number;
}

export interface LettersRoundState {
  type: "letters";
  roundNumber: number;
  pickerPlayerId: string;
  picks: PickKind[];
  letters: string[];
  submissions: Record<string, WordSubmission>;
}

export interface ConundrumRoundState {
  type: "conundrum";
  roundNumber: number;
  scrambled: string;
  answer: string;
  firstCorrectPlayerId: string | null;
  firstCorrectAtMs: number | null;
  guessesByPlayer: Record<string, number>;
}

export type LiveRoundState = LettersRoundState | ConundrumRoundState;

export interface RoundResult {
  roundNumber: number;
  type: RoundType;
  awardedScores: Record<string, number>;
  details:
    | {
        letters: string[];
        submissions: Record<string, WordSubmission>;
      }
    | {
        scrambled: string;
        answer: string;
        firstCorrectPlayerId: string | null;
        firstCorrectAtMs: number | null;
      };
}

export interface MatchState {
  matchId: string;
  createdAtMs: number;
  mode: MatchMode;
  players: [string, string];
  startRatings: Record<string, number>;
  phase: MatchPhase;
  phaseEndsAtMs: number | null;
  roundIndex: number;
  rounds: RoundPlan[];
  liveRound: LiveRoundState;
  roundResults: RoundResult[];
  scores: Record<string, number>;
  winnerPlayerId: string | null;
  endReason: "completed" | "forfeit_disconnect" | "forfeit_manual" | null;
  ratingChanges: Record<string, number> | null;
  updatedAtMs: number;
}

export interface SerializedPlayer {
  playerId: string;
  displayName: string;
  connected: boolean;
  score: number;
  rating: number;
  rankTier: string;
}

export interface SerializedRoundResult extends RoundResult {}

export interface SerializedMatchState {
  matchId: string;
  phase: MatchPhase;
  phaseEndsAtMs: number | null;
  serverNowMs: number;
  roundNumber: number;
  roundType: RoundType;
  mode: MatchMode;
  players: SerializedPlayer[];
  pickerPlayerId?: string;
  letters?: string[];
  scrambled?: string;
  roundResults: SerializedRoundResult[];
  winnerPlayerId: string | null;
  matchEndReason?: "completed" | "forfeit_disconnect" | "forfeit_manual";
  ratingChanges?: Record<string, number>;
}

export interface MatchServiceOptions {
  now: () => number;
  setTimer: (callback: () => void, delayMs: number) => unknown;
  clearTimer: (timer: unknown) => void;
  pickDurationMs: number;
  solveDurationMs: number;
  resultDurationMs: number;
  conundrumGuessCooldownMs: number;
  logEvent: (message: string, details?: Record<string, unknown>) => void;
  onMatchUpdated: (matchId: string) => void;
  onQueueUpdated: (playerId: string, queueSize: number, mode: MatchMode) => void;
  onMatchFinished: (record: FinishedMatchRecord) => void;
}

export interface SubmitWordResult {
  ok: boolean;
  code?: string;
}

export interface SubmitGuessResult {
  ok: boolean;
  code?: string;
}

export interface FinishedMatchRecord {
  matchId: string;
  createdAtMs: number;
  finishedAtMs: number;
  mode: MatchMode;
  players: Array<{
    playerId: string;
    displayName: string;
    score: number;
    ratingBefore: number;
    ratingAfter: number;
    ratingDelta: number;
    rankTier: string;
  }>;
  winnerPlayerId: string | null;
  matchEndReason: "completed" | "forfeit_disconnect" | "forfeit_manual" | null;
  ratingChanges: Record<string, number>;
  roundResults: SerializedRoundResult[];
}
