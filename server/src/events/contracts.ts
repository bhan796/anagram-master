export const SocketEvents = {
  sessionIdentify: "session:identify",
  queueJoin: "queue:join",
  queueLeave: "queue:leave",
  matchResume: "match:resume",
  matchForfeit: "match:forfeit",
  roundPickLetter: "round:pick_letter",
  roundSubmitWord: "round:submit_word",
  roundSubmitConundrumGuess: "round:submit_conundrum_guess",
  matchState: "match:state",
  matchmakingStatus: "matchmaking:status",
  matchFound: "match:found",
  actionError: "action:error"
} as const;

export type SocketEventName = (typeof SocketEvents)[keyof typeof SocketEvents];

export interface ActionErrorPayload {
  code: string;
  message: string;
  action: string;
}

export const ErrorMessages: Record<string, string> = {
  UNKNOWN_PLAYER: "Player session is not recognized.",
  ALREADY_IN_MATCH: "Player is already in an active match.",
  MATCH_NOT_FOUND: "Match could not be found.",
  NOT_IN_ACTIVE_MATCH: "Player is not currently in an active match.",
  NOT_MATCH_PARTICIPANT: "Player is not part of this match.",
  INVALID_PHASE: "Action is not valid during current round phase.",
  INVALID_ROUND: "Action does not apply to current round type.",
  NOT_PICKER: "Only the designated picker may choose letters.",
  PICK_CONSTRAINT_VIOLATION: "Pick would break vowel/consonant constraints.",
  LATE_SUBMISSION: "Round submission window has ended.",
  DUPLICATE_SUBMISSION: "Letters round already has a submission from this player.",
  RATE_LIMITED: "Too many guesses. Wait briefly before trying again.",
  ALREADY_SOLVED: "Conundrum round has already been solved.",
  AUTH_REQUIRED_RANKED: "Create an account or sign in to play ranked mode.",
  DISPLAY_NAME_TAKEN: "That display name is already in use.",
  INVALID_DISPLAY_NAME: "Display name must be 3-20 characters and use letters, numbers, spaces, or underscores."
};

export const toActionError = (action: string, code: string): ActionErrorPayload => ({
  action,
  code,
  message: ErrorMessages[code] ?? "Action failed."
});
