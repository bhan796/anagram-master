import type { AvatarState } from "../avatars/avatarTypes";

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
  rating?: number;
  rankTier?: string;
  isAuthenticated?: boolean;
  serverNowMs: number;
  equippedCosmetic?: string | null;
  equippedAvatar?: string;
}

export interface MatchmakingStatusPayload {
  queueSize: number;
  state: string;
  mode?: "casual" | "ranked";
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
  equippedCosmetic?: string | null;
  equippedAvatar?: string;
  connected: boolean;
  score: number;
  rating?: number;
  rankTier?: string;
}

export interface BonusTilesSnapshot {
  doubleIndex: number;
  tripleIndex: number;
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
  bonusTiles?: BonusTilesSnapshot | null;
  submissions?: Record<string, WordSubmissionSnapshot>;
  scrambled?: string | null;
  answer?: string | null;
  conundrumSubmissions?: Record<
    string,
    {
      guess: string;
      normalizedGuess: string;
      isCorrect: boolean;
      submittedAtMs: number;
    }
  >;
  correctPlayerIds?: string[];
}

export interface MatchStatePayload {
  matchId: string;
  phase: MatchPhase;
  phaseEndsAtMs: number | null;
  serverNowMs: number;
  roundNumber: number;
  roundType: RoundType;
  mode?: "casual" | "ranked";
  players: PlayerSnapshot[];
  pickerPlayerId: string | null;
  letters: string[];
  bonusTiles: BonusTilesSnapshot | null;
  scrambled: string | null;
  conundrumGuessSubmittedPlayerIds?: string[];
  roundResults: RoundResultSnapshot[];
  winnerPlayerId: string | null;
  matchEndReason?: "completed" | "forfeit_disconnect" | "forfeit_manual";
  ratingChanges?: Record<string, number>;
}

export interface OnlineUiState {
  connectionState: "disconnected" | "connecting" | "connected" | "reconnecting" | "failed";
  playerId: string | null;
  displayName: string | null;
  isAuthenticated: boolean;
  playerRating: number;
  playerRankTier: string;
  queueState: string;
  queueSize: number;
  queueMode: "casual" | "ranked";
  isInMatchmaking: boolean;
  matchId: string | null;
  matchState: MatchStatePayload | null;
  myPlayer: PlayerSnapshot | null;
  opponentPlayer: PlayerSnapshot | null;
  myAvatarId: string;
  oppAvatarId: string;
  myAvatarState: AvatarState;
  oppAvatarState: AvatarState;
  isMyTurnToPick: boolean;
  secondsRemaining: number;
  wordInput: string;
  conundrumGuessInput: string;
  hasSubmittedWord: boolean;
  hasSubmittedConundrumGuess: boolean;
  opponentSubmittedConundrumGuess: boolean;
  lastError: ActionErrorPayload | null;
  statusMessage: string;
  localValidationMessage: string | null;
  connectionError: string | null;
  playerRewards: PlayerRewardsPayload | null;
}

export interface PlayerRewardsPayload {
  runesEarned: number;
  newAchievements: Array<{
    id: string;
    name: string;
    description: string;
    tier: string;
    runesReward: number;
  }>;
}

export const initialOnlineUiState: OnlineUiState = {
  connectionState: "disconnected",
  playerId: null,
  displayName: null,
  isAuthenticated: false,
  playerRating: 1000,
  playerRankTier: "silver",
  queueState: "idle",
  queueSize: 0,
  queueMode: "casual",
  isInMatchmaking: false,
  matchId: null,
  matchState: null,
  myPlayer: null,
  opponentPlayer: null,
  myAvatarId: "default_rookie",
  oppAvatarId: "default_rookie",
  myAvatarState: "idle",
  oppAvatarState: "idle",
  isMyTurnToPick: false,
  secondsRemaining: 0,
  wordInput: "",
  conundrumGuessInput: "",
  hasSubmittedWord: false,
  hasSubmittedConundrumGuess: false,
  opponentSubmittedConundrumGuess: false,
  lastError: null,
  statusMessage: "",
  localValidationMessage: null,
  connectionError: null,
  playerRewards: null
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
  ACTION_ERROR: "action:error",
  PLAYER_REWARDS: "player:rewards"
} as const;
