export type MatchPhase =
  | "awaiting_letters_pick"
  | "letters_solving"
  | "conundrum_solving"
  | "round_result"
  | "finished"
  | "UNKNOWN";

export type RoundType = "letters" | "conundrum" | "UNKNOWN";

export interface SessionIdentifyPayload {
  playerId: string;
  displayName: string;
  serverNowMs: number;
}

export interface MatchmakingStatusPayload {
  queueSize: number;
  state: string;
}

export interface MatchFoundPayload {
  matchId: string;
  serverNowMs: number;
}

export interface ActionErrorPayload {
  action: string;
  code: string;
  message: string;
}

export interface PlayerSnapshot {
  playerId: string;
  displayName: string;
  connected: boolean;
  score: number;
}

export interface WordSubmissionSnapshot {
  word: string;
  normalizedWord: string;
  isValid: boolean;
  failureCode: string | null;
  score: number;
  submittedAtMs: number;
}

export interface RoundResultSnapshot {
  roundNumber: number;
  type: RoundType;
  awardedScores: Record<string, number>;
  letters?: string[];
  submissions?: Record<string, WordSubmissionSnapshot>;
  scrambled?: string | null;
  answer?: string | null;
  firstCorrectPlayerId?: string | null;
  firstCorrectAtMs?: number | null;
}

export interface MatchStatePayload {
  matchId: string;
  phase: MatchPhase;
  phaseEndsAtMs: number | null;
  serverNowMs: number;
  roundNumber: number;
  roundType: RoundType;
  players: PlayerSnapshot[];
  pickerPlayerId: string | null;
  letters: string[];
  scrambled: string | null;
  roundResults: RoundResultSnapshot[];
  winnerPlayerId: string | null;
}

export interface OnlineUiState {
  connectionState: "disconnected" | "connecting" | "connected" | "reconnecting" | "failed";
  playerId: string | null;
  displayName: string | null;
  queueState: string;
  queueSize: number;
  isInMatchmaking: boolean;
  matchId: string | null;
  matchState: MatchStatePayload | null;
  myPlayer: PlayerSnapshot | null;
  opponentPlayer: PlayerSnapshot | null;
  isMyTurnToPick: boolean;
  secondsRemaining: number;
  wordInput: string;
  conundrumGuessInput: string;
  hasSubmittedWord: boolean;
  lastError: ActionErrorPayload | null;
  statusMessage: string;
  localValidationMessage: string | null;
  connectionError: string | null;
}

export const initialOnlineUiState: OnlineUiState = {
  connectionState: "disconnected",
  playerId: null,
  displayName: null,
  queueState: "idle",
  queueSize: 0,
  isInMatchmaking: false,
  matchId: null,
  matchState: null,
  myPlayer: null,
  opponentPlayer: null,
  isMyTurnToPick: false,
  secondsRemaining: 0,
  wordInput: "",
  conundrumGuessInput: "",
  hasSubmittedWord: false,
  lastError: null,
  statusMessage: "",
  localValidationMessage: null,
  connectionError: null
};

export const SocketEventNames = {
  SESSION_IDENTIFY: "session:identify",
  QUEUE_JOIN: "queue:join",
  QUEUE_LEAVE: "queue:leave",
  MATCH_RESUME: "match:resume",
  MATCH_FORFEIT: "match:forfeit",
  ROUND_PICK_LETTER: "round:pick_letter",
  ROUND_SUBMIT_WORD: "round:submit_word",
  ROUND_SUBMIT_CONUNDRUM_GUESS: "round:submit_conundrum_guess",
  MATCH_STATE: "match:state",
  MATCHMAKING_STATUS: "matchmaking:status",
  MATCH_FOUND: "match:found",
  ACTION_ERROR: "action:error"
} as const;
