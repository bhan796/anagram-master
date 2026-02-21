export type MatchPhase =
  | "awaiting_letters_pick"
  | "letters_solving"
  | "conundrum_solving"
  | "round_result"
  | "finished";

export type RoundType = "letters" | "conundrum";
export type PickKind = "vowel" | "consonant";

export interface PlayerRuntime {
  playerId: string;
  displayName: string;
  socketId: string | null;
  connected: boolean;
  matchId: string | null;
  lastConundrumGuessAtMs: number;
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
  players: [string, string];
  phase: MatchPhase;
  phaseEndsAtMs: number | null;
  roundIndex: number;
  rounds: RoundPlan[];
  liveRound: LiveRoundState;
  roundResults: RoundResult[];
  scores: Record<string, number>;
  winnerPlayerId: string | null;
  endReason: "completed" | "forfeit_disconnect" | "forfeit_manual" | null;
  updatedAtMs: number;
}

export interface SerializedPlayer {
  playerId: string;
  displayName: string;
  connected: boolean;
  score: number;
}

export interface SerializedRoundResult extends RoundResult {}

export interface SerializedMatchState {
  matchId: string;
  phase: MatchPhase;
  phaseEndsAtMs: number | null;
  serverNowMs: number;
  roundNumber: number;
  roundType: RoundType;
  players: SerializedPlayer[];
  pickerPlayerId?: string;
  letters?: string[];
  scrambled?: string;
  roundResults: SerializedRoundResult[];
  winnerPlayerId: string | null;
  matchEndReason?: "completed" | "forfeit_disconnect" | "forfeit_manual";
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
  onQueueUpdated: (playerId: string, queueSize: number) => void;
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
  players: Array<{
    playerId: string;
    displayName: string;
    score: number;
  }>;
  winnerPlayerId: string | null;
  roundResults: SerializedRoundResult[];
}
