import { randomUUID } from "node:crypto";
import type { ConundrumEntry } from "./data.js";
import {
  allowedPickKinds,
  canConstructFromLetters,
  createGuestName,
  drawWeightedLetter,
  isAlphabetical,
  normalizeWord,
  pickLetterBonusTiles,
  scrambleWord,
  scoreWordFromLetters
} from "./rules.js";
import { computeEloDelta, ratingToTier } from "./ranking.js";
import type {
  ConundrumRoundState,
  FinishedMatchRecord,
  LiveRoundState,
  MatchMode,
  MatchServiceOptions,
  MatchState,
  PickKind,
  PlayerRuntime,
  RoundPlan,
  RoundResult,
  SerializedMatchState,
  SubmitGuessResult,
  SubmitWordResult,
  WordSubmission
} from "./types.js";

const defaultOptions: Omit<MatchServiceOptions, "onMatchUpdated" | "onQueueUpdated" | "onMatchFinished"> = {
  now: () => Date.now(),
  setTimer: (callback, delayMs) => setTimeout(callback, delayMs),
  clearTimer: (timer) => clearTimeout(timer as NodeJS.Timeout),
  pickDurationMs: 20_000,
  solveDurationMs: 30_000,
  resultDurationMs: 5_000,
  conundrumGuessCooldownMs: 750,
  logEvent: () => undefined
};

interface QueueEntry {
  playerId: string;
  joinedAtMs: number;
}

export class MatchService {
  private readonly players = new Map<string, PlayerRuntime>();
  private readonly queueByMode: Record<MatchMode, QueueEntry[]> = {
    casual: [],
    ranked: []
  };
  private readonly matches = new Map<string, MatchState>();
  private readonly timers = new Map<string, unknown>();
  private readonly dictionary: Set<string>;
  private readonly conundrums: ConundrumEntry[];
  private readonly options: MatchServiceOptions;

  constructor(dictionary: Set<string>, conundrums: ConundrumEntry[], options: Partial<MatchServiceOptions> = {}) {
    this.dictionary = dictionary;
    this.conundrums = conundrums;
    this.options = {
      ...defaultOptions,
      ...options,
      onMatchUpdated: options.onMatchUpdated ?? (() => undefined),
      onQueueUpdated: options.onQueueUpdated ?? (() => undefined),
      onMatchFinished: options.onMatchFinished ?? (() => undefined)
    };
  }

  connectPlayer(socketId: string, requestedPlayerId?: string, requestedDisplayName?: string, userId?: string | null): PlayerRuntime {
    const now = this.options.now();
    const normalizedRequestedId = requestedPlayerId?.trim();
    const playerId = normalizedRequestedId && normalizedRequestedId.length > 0 ? normalizedRequestedId : randomUUID();

    const existing = this.players.get(playerId);
    const player: PlayerRuntime =
      existing ?? {
        playerId,
        displayName: requestedDisplayName?.trim() || createGuestName(),
        userId: userId ?? null,
        socketId,
        connected: true,
        matchId: null,
        queuedMode: null,
        lastConundrumGuessAtMs: 0,
        rating: 1000,
        rankedGames: 0,
        rankedWins: 0,
        rankedLosses: 0,
        rankedDraws: 0,
        peakRating: 1000
      };

    player.socketId = socketId;
    player.connected = true;
    player.userId = userId ?? null;

    this.players.set(playerId, player);
    this.reconcilePlayerMatch(player);

    this.options.logEvent("Player connected", { playerId, socketId, connectedAtMs: now });
    return player;
  }

  hydratePlayerProfile(
    playerId: string,
    profile: {
      displayName?: string;
      rating: number;
      peakRating: number;
      rankedGames: number;
      rankedWins: number;
      rankedLosses: number;
      rankedDraws: number;
    }
  ): void {
    const player = this.players.get(playerId);
    if (!player) return;

    if (profile.displayName && profile.displayName.trim().length > 0) {
      player.displayName = profile.displayName.trim();
    }
    player.rating = profile.rating;
    player.peakRating = Math.max(profile.peakRating, profile.rating);
    player.rankedGames = profile.rankedGames;
    player.rankedWins = profile.rankedWins;
    player.rankedLosses = profile.rankedLosses;
    player.rankedDraws = profile.rankedDraws;
  }

  updateDisplayName(playerId: string, nextDisplayName: string): { ok: boolean; code?: string; displayName?: string } {
    const player = this.players.get(playerId);
    if (!player) return { ok: false, code: "UNKNOWN_PLAYER" };

    const normalized = nextDisplayName.trim();
    if (!normalized) return { ok: false, code: "INVALID_DISPLAY_NAME" };
    if (normalized.length < 3 || normalized.length > 20) return { ok: false, code: "INVALID_DISPLAY_NAME" };
    if (!/^[A-Za-z0-9_ ]+$/.test(normalized)) return { ok: false, code: "INVALID_DISPLAY_NAME" };

    const lower = normalized.toLowerCase();
    for (const other of this.players.values()) {
      if (other.playerId === playerId) continue;
      if (other.displayName.trim().toLowerCase() === lower) {
        return { ok: false, code: "DISPLAY_NAME_TAKEN" };
      }
    }

    player.displayName = normalized;
    this.options.logEvent("Display name updated", { playerId, displayName: normalized });
    return { ok: true, displayName: normalized };
  }

  disconnectSocket(socketId: string): void {
    for (const player of this.players.values()) {
      if (player.socketId !== socketId) continue;

      player.connected = false;
      player.socketId = null;
      this.leaveQueue(player.playerId);
      if (player.matchId) {
        this.forfeitMatch(player.playerId, "disconnect");
      }
      this.options.logEvent("Player disconnected", { playerId: player.playerId, socketId });
      break;
    }
  }

  joinQueue(playerId: string, mode: MatchMode = "casual"): { ok: boolean; code?: string; queueSize?: number } {
    const player = this.players.get(playerId);
    if (!player) return { ok: false, code: "UNKNOWN_PLAYER" };
    if (mode === "ranked" && !player.userId) return { ok: false, code: "AUTH_REQUIRED_RANKED" };

    this.reconcilePlayerMatch(player);
    if (player.matchId) return { ok: false, code: "ALREADY_IN_MATCH" };

    this.leaveQueue(playerId);
    this.pruneQueue(mode);

    const queue = this.queueByMode[mode];
    if (!queue.some((entry) => entry.playerId === playerId)) {
      queue.push({ playerId, joinedAtMs: this.options.now() });
      player.queuedMode = mode;
    }

    this.options.onQueueUpdated(playerId, queue.length, mode);
    this.tryMatchPlayers(mode);
    return { ok: true, queueSize: queue.length };
  }

  leaveQueue(playerId: string): void {
    const player = this.players.get(playerId);
    for (const mode of ["casual", "ranked"] as const) {
      const queue = this.queueByMode[mode];
      const next = queue.filter((entry) => entry.playerId !== playerId);
      if (next.length !== queue.length) {
        queue.splice(0, queue.length, ...next);
        this.options.onQueueUpdated(playerId, queue.length, mode);
      }
    }

    if (player) {
      player.queuedMode = null;
    }
  }

  getPlayer(playerId: string): PlayerRuntime | undefined {
    return this.players.get(playerId);
  }

  getMatchByPlayer(playerId: string): MatchState | undefined {
    const player = this.players.get(playerId);
    if (!player?.matchId) return undefined;

    const match = this.matches.get(player.matchId);
    if (!match || match.phase === "finished") {
      player.matchId = null;
      return undefined;
    }

    return match;
  }

  getMatch(matchId: string): MatchState | undefined {
    return this.matches.get(matchId);
  }

  getOnlinePlayerCount(): number {
    let count = 0;
    for (const player of this.players.values()) {
      if (player.connected) count += 1;
    }
    return count;
  }

  resumeMatch(playerId: string, matchId: string): { ok: boolean; code?: string } {
    const match = this.matches.get(matchId);
    if (!match) return { ok: false, code: "MATCH_NOT_FOUND" };
    if (!match.players.includes(playerId)) return { ok: false, code: "NOT_MATCH_PARTICIPANT" };

    const player = this.players.get(playerId);
    if (player) {
      player.matchId = matchId;
    }

    return { ok: true };
  }

  forfeitMatch(playerId: string, reason: "disconnect" | "manual_leave" = "manual_leave"): { ok: boolean; code?: string } {
    const match = this.getMatchByPlayer(playerId);
    if (!match) return { ok: false, code: "NOT_IN_ACTIVE_MATCH" };
    if (match.phase === "finished") return { ok: false, code: "INVALID_PHASE" };

    this.clearPhaseTimer(match.matchId);

    const winnerPlayerId = match.players.find((id) => id !== playerId) ?? null;
    match.phase = "finished";
    match.phaseEndsAtMs = null;
    match.winnerPlayerId = winnerPlayerId;
    match.endReason = reason === "disconnect" ? "forfeit_disconnect" : "forfeit_manual";

    if (match.mode === "ranked" && winnerPlayerId) {
      const loserPlayerId = match.players.find((id) => id !== winnerPlayerId) ?? playerId;
      this.applyRankedOutcome(match, winnerPlayerId, loserPlayerId, 1);
    } else {
      match.ratingChanges = {
        [match.players[0]]: 0,
        [match.players[1]]: 0
      };
    }

    match.updatedAtMs = this.options.now();

    for (const id of match.players) {
      const player = this.players.get(id);
      if (player) {
        player.matchId = null;
        player.queuedMode = null;
      }
    }

    this.options.logEvent("Match forfeited", {
      matchId: match.matchId,
      forfeitedBy: playerId,
      winnerPlayerId,
      reason,
      mode: match.mode,
      ratingChanges: match.ratingChanges
    });

    this.options.onMatchUpdated(match.matchId);
    this.options.onMatchFinished(this.buildFinishedMatchRecord(match));
    return { ok: true };
  }

  pickLetter(playerId: string, kind: PickKind): { ok: boolean; code?: string } {
    const match = this.getMatchByPlayer(playerId);
    if (!match) return { ok: false, code: "MATCH_NOT_FOUND" };

    if (match.phase !== "awaiting_letters_pick") return { ok: false, code: "INVALID_PHASE" };
    if (match.liveRound.type !== "letters") return { ok: false, code: "INVALID_ROUND" };
    if (match.liveRound.pickerPlayerId !== playerId) return { ok: false, code: "NOT_PICKER" };

    const vowels = match.liveRound.letters.filter((letter) => "AEIOU".includes(letter)).length;
    const consonants = match.liveRound.letters.length - vowels;
    const allowed = allowedPickKinds(match.liveRound.picks.length, vowels, consonants, 9);
    if (!allowed.has(kind)) return { ok: false, code: "PICK_CONSTRAINT_VIOLATION" };

    const letter = drawWeightedLetter(kind, Math.random);
    match.liveRound.picks.push(kind);
    match.liveRound.letters.push(letter);
    match.updatedAtMs = this.options.now();

    this.options.logEvent("Letter picked", { matchId: match.matchId, playerId, kind, letter, count: match.liveRound.letters.length });

    if (match.liveRound.letters.length === 9) {
      this.startLettersSolving(match);
    }

    this.options.onMatchUpdated(match.matchId);
    return { ok: true };
  }

  submitWord(playerId: string, rawWord: string): SubmitWordResult {
    const match = this.getMatchByPlayer(playerId);
    if (!match) return { ok: false, code: "MATCH_NOT_FOUND" };
    if (match.phase !== "letters_solving") return { ok: false, code: "LATE_SUBMISSION" };
    if (match.phaseEndsAtMs !== null && this.options.now() > match.phaseEndsAtMs) {
      return { ok: false, code: "LATE_SUBMISSION" };
    }

    if (match.liveRound.type !== "letters") return { ok: false, code: "INVALID_ROUND" };
    if (match.liveRound.submissions[playerId]) return { ok: false, code: "DUPLICATE_SUBMISSION" };

    const submission = this.evaluateWordSubmission(rawWord, match.liveRound.letters, match.liveRound.bonusTiles);
    match.liveRound.submissions[playerId] = submission;
    match.updatedAtMs = this.options.now();

    this.options.logEvent("Letters word submitted", {
      matchId: match.matchId,
      playerId,
      normalizedWord: submission.normalizedWord,
      isValid: submission.isValid,
      score: submission.score
    });

    if (Object.keys(match.liveRound.submissions).length >= 2) {
      this.finalizeLettersRound(match);
    }

    this.options.onMatchUpdated(match.matchId);
    return { ok: true };
  }

  submitConundrumGuess(playerId: string, guess: string): SubmitGuessResult {
    const match = this.getMatchByPlayer(playerId);
    if (!match) return { ok: false, code: "MATCH_NOT_FOUND" };
    if (match.phase !== "conundrum_solving") return { ok: false, code: "LATE_SUBMISSION" };
    if (match.phaseEndsAtMs !== null && this.options.now() > match.phaseEndsAtMs) {
      return { ok: false, code: "LATE_SUBMISSION" };
    }

    if (match.liveRound.type !== "conundrum") return { ok: false, code: "INVALID_ROUND" };

    const player = this.players.get(playerId);
    if (!player) return { ok: false, code: "UNKNOWN_PLAYER" };

    const now = this.options.now();
    if (now - player.lastConundrumGuessAtMs < this.options.conundrumGuessCooldownMs) {
      return { ok: false, code: "RATE_LIMITED" };
    }

    player.lastConundrumGuessAtMs = now;
    match.liveRound.guessesByPlayer[playerId] = (match.liveRound.guessesByPlayer[playerId] ?? 0) + 1;

    const normalizedGuess = normalizeWord(guess);
    if (!normalizedGuess || !isAlphabetical(normalizedGuess)) {
      this.options.onMatchUpdated(match.matchId);
      return { ok: true };
    }

    if (match.liveRound.firstCorrectPlayerId !== null) {
      return { ok: false, code: "ALREADY_SOLVED" };
    }

    if (normalizedGuess === normalizeWord(match.liveRound.answer)) {
      match.liveRound.firstCorrectPlayerId = playerId;
      match.liveRound.firstCorrectAtMs = now;
      this.finalizeConundrumRound(match, true);
      this.options.logEvent("Conundrum solved", { matchId: match.matchId, playerId, guess: normalizedGuess });
    }

    this.options.onMatchUpdated(match.matchId);
    return { ok: true };
  }

  serializeForPlayer(match: MatchState): SerializedMatchState {
    const now = this.options.now();
    const playerViews = match.players.map((playerId) => {
      const player = this.players.get(playerId);
      const rating = player?.rating ?? match.startRatings[playerId] ?? 1000;
      return {
        playerId,
        displayName: player?.displayName ?? "Guest",
        connected: player?.connected ?? false,
        score: match.scores[playerId] ?? 0,
        rating,
        rankTier: ratingToTier(rating)
      };
    });

    const payload: SerializedMatchState = {
      matchId: match.matchId,
      phase: match.phase,
      phaseEndsAtMs: match.phaseEndsAtMs,
      serverNowMs: now,
      roundNumber: match.liveRound.roundNumber,
      roundType: match.liveRound.type,
      mode: match.mode,
      players: playerViews,
      roundResults: match.roundResults,
      winnerPlayerId: match.winnerPlayerId,
      ratingChanges: match.ratingChanges ?? undefined
    };

    if (match.endReason) {
      payload.matchEndReason = match.endReason;
    }

    if (match.liveRound.type === "letters") {
      payload.letters = [...match.liveRound.letters];
      payload.pickerPlayerId = match.liveRound.pickerPlayerId;
      payload.bonusTiles = { ...match.liveRound.bonusTiles };
    } else {
      payload.scrambled = match.liveRound.scrambled;
      if (match.phase !== "conundrum_solving") {
        payload.roundResults = match.roundResults;
      }
    }

    return payload;
  }

  private tryMatchPlayers(mode: MatchMode): void {
    this.pruneQueue(mode);

    const queue = this.queueByMode[mode];
    let matched = true;
    while (queue.length >= 2 && matched) {
      matched = false;
      const pair = this.findMatchPair(mode, queue);
      if (!pair) break;

      const [firstIdx, secondIdx] = pair;
      const first = queue[firstIdx];
      const second = queue[secondIdx];
      queue.splice(secondIdx, 1);
      queue.splice(firstIdx, 1);

      const playerA = this.players.get(first.playerId);
      const playerB = this.players.get(second.playerId);
      if (!playerA || !playerB || playerA.matchId || playerB.matchId) {
        continue;
      }

      const match = this.createMatch(mode, playerA.playerId, playerB.playerId);
      playerA.matchId = match.matchId;
      playerB.matchId = match.matchId;
      playerA.queuedMode = null;
      playerB.queuedMode = null;

      this.options.onQueueUpdated(playerA.playerId, queue.length, mode);
      this.options.onQueueUpdated(playerB.playerId, queue.length, mode);

      this.options.logEvent("Match created", {
        matchId: match.matchId,
        mode,
        players: match.players,
        ratings: {
          [playerA.playerId]: playerA.rating,
          [playerB.playerId]: playerB.rating
        }
      });

      this.options.onMatchUpdated(match.matchId);
      matched = true;
    }
  }

  private findMatchPair(mode: MatchMode, queue: QueueEntry[]): [number, number] | null {
    if (queue.length < 2) return null;

    if (mode === "casual") {
      for (let i = 0; i < queue.length - 1; i += 1) {
        const a = queue[i];
        const b = queue[i + 1];
        if (a.playerId !== b.playerId) {
          return [i, i + 1];
        }
      }
      return null;
    }

    const now = this.options.now();
    for (let i = 0; i < queue.length - 1; i += 1) {
      const a = queue[i];
      const playerA = this.players.get(a.playerId);
      if (!playerA) continue;

      for (let j = i + 1; j < queue.length; j += 1) {
        const b = queue[j];
        if (a.playerId === b.playerId) continue;

        const playerB = this.players.get(b.playerId);
        if (!playerB) continue;

        const ratingDiff = Math.abs(playerA.rating - playerB.rating);
        const waitMs = Math.max(now - a.joinedAtMs, now - b.joinedAtMs);
        const allowedDiff = this.allowedRankedRatingDiff(waitMs);
        if (ratingDiff <= allowedDiff) {
          return [i, j];
        }
      }
    }

    return null;
  }

  private allowedRankedRatingDiff(waitMs: number): number {
    const base = 75;
    const widened = Math.floor(waitMs / 5_000) * 25;
    return Math.min(400, base + widened);
  }

  private pruneQueue(mode: MatchMode): void {
    const queue = this.queueByMode[mode];
    const seen = new Set<string>();
    const filtered = queue.filter((entry) => {
      if (seen.has(entry.playerId)) return false;
      seen.add(entry.playerId);

      const player = this.players.get(entry.playerId);
      return Boolean(
        player &&
          player.connected &&
          player.socketId &&
          !player.matchId &&
          player.queuedMode === mode
      );
    });

    queue.splice(0, queue.length, ...filtered);
  }

  private createMatch(mode: MatchMode, playerAId: string, playerBId: string): MatchState {
    const matchId = randomUUID();
    const now = this.options.now();
    const rounds = this.buildRoundPlans(playerAId, playerBId);
    const firstRound = rounds[0];

    const liveRound: LiveRoundState = {
      type: "letters",
      roundNumber: firstRound.roundNumber,
      pickerPlayerId: firstRound.pickerPlayerId ?? playerAId,
      picks: [],
      letters: [],
      bonusTiles: pickLetterBonusTiles(),
      submissions: {}
    };

    const playerARating = this.players.get(playerAId)?.rating ?? 1000;
    const playerBRating = this.players.get(playerBId)?.rating ?? 1000;

    const match: MatchState = {
      matchId,
      createdAtMs: now,
      mode,
      players: [playerAId, playerBId],
      startRatings: {
        [playerAId]: playerARating,
        [playerBId]: playerBRating
      },
      phase: "awaiting_letters_pick",
      phaseEndsAtMs: null,
      roundIndex: 0,
      rounds,
      liveRound,
      roundResults: [],
      scores: {
        [playerAId]: 0,
        [playerBId]: 0
      },
      winnerPlayerId: null,
      endReason: null,
      ratingChanges: null,
      updatedAtMs: now
    };

    this.matches.set(matchId, match);
    this.startLettersPickPhase(match);
    return match;
  }

  private buildRoundPlans(playerAId: string, playerBId: string): RoundPlan[] {
    return [
      { roundNumber: 1, type: "letters", pickerPlayerId: playerAId },
      { roundNumber: 2, type: "letters", pickerPlayerId: playerBId },
      { roundNumber: 3, type: "letters", pickerPlayerId: playerAId },
      { roundNumber: 4, type: "letters", pickerPlayerId: playerBId },
      { roundNumber: 5, type: "conundrum" }
    ];
  }

  private startLettersSolving(match: MatchState): void {
    match.phase = "letters_solving";
    match.phaseEndsAtMs = this.options.now() + this.options.solveDurationMs;
    match.updatedAtMs = this.options.now();

    this.schedulePhaseTimer(match.matchId, this.options.solveDurationMs, () => {
      const current = this.matches.get(match.matchId);
      if (!current || current.phase !== "letters_solving") return;
      this.finalizeLettersRound(current);
      this.options.onMatchUpdated(current.matchId);
    });

    this.options.logEvent("Letters solving phase started", { matchId: match.matchId, phaseEndsAtMs: match.phaseEndsAtMs });
  }

  private startLettersPickPhase(match: MatchState): void {
    if (match.liveRound.type !== "letters") return;

    match.phase = "awaiting_letters_pick";
    match.phaseEndsAtMs = this.options.now() + this.options.pickDurationMs;
    match.updatedAtMs = this.options.now();

    this.schedulePhaseTimer(match.matchId, this.options.pickDurationMs, () => {
      const current = this.matches.get(match.matchId);
      if (!current || current.phase !== "awaiting_letters_pick" || current.liveRound.type !== "letters") return;

      this.autoCompleteLettersPick(current);
      this.startLettersSolving(current);
      this.options.onMatchUpdated(current.matchId);
    });

    this.options.logEvent("Letters pick phase started", { matchId: match.matchId, phaseEndsAtMs: match.phaseEndsAtMs });
  }

  private autoCompleteLettersPick(match: MatchState): void {
    if (match.liveRound.type !== "letters") return;

    while (match.liveRound.letters.length < 9) {
      const vowels = match.liveRound.letters.filter((letter) => "AEIOU".includes(letter)).length;
      const consonants = match.liveRound.letters.length - vowels;
      const allowed = Array.from(allowedPickKinds(match.liveRound.picks.length, vowels, consonants, 9));
      const pickKind = allowed[Math.floor(Math.random() * allowed.length)] as PickKind | undefined;
      if (!pickKind) break;

      const letter = drawWeightedLetter(pickKind, Math.random);
      match.liveRound.picks.push(pickKind);
      match.liveRound.letters.push(letter);
    }

    match.updatedAtMs = this.options.now();
    this.options.logEvent("Letters auto-completed after pick timeout", {
      matchId: match.matchId,
      letters: match.liveRound.letters.join("")
    });
  }

  private startConundrumSolving(match: MatchState, round: ConundrumRoundState): void {
    match.phase = "conundrum_solving";
    match.phaseEndsAtMs = this.options.now() + this.options.solveDurationMs;
    match.liveRound = round;
    match.updatedAtMs = this.options.now();

    this.schedulePhaseTimer(match.matchId, this.options.solveDurationMs, () => {
      const current = this.matches.get(match.matchId);
      if (!current || current.phase !== "conundrum_solving") return;
      this.finalizeConundrumRound(current, false);
      this.options.onMatchUpdated(current.matchId);
    });

    this.options.logEvent("Conundrum solving phase started", { matchId: match.matchId, phaseEndsAtMs: match.phaseEndsAtMs });
  }

  private finalizeLettersRound(match: MatchState): void {
    if (match.liveRound.type !== "letters") return;
    this.clearPhaseTimer(match.matchId);

    for (const playerId of match.players) {
      if (!match.liveRound.submissions[playerId]) {
        match.liveRound.submissions[playerId] = this.evaluateWordSubmission(
          "",
          match.liveRound.letters,
          match.liveRound.bonusTiles
        );
      }
    }

    const awardedScores: Record<string, number> = {};
    for (const playerId of match.players) {
      const submission = match.liveRound.submissions[playerId];
      awardedScores[playerId] = submission.score;
      match.scores[playerId] = (match.scores[playerId] ?? 0) + submission.score;
    }

    const roundResult: RoundResult = {
      roundNumber: match.liveRound.roundNumber,
      type: "letters",
      awardedScores,
      details: {
        letters: [...match.liveRound.letters],
        bonusTiles: { ...match.liveRound.bonusTiles },
        submissions: { ...match.liveRound.submissions }
      }
    };

    match.roundResults.push(roundResult);
    match.phase = "round_result";
    match.phaseEndsAtMs = this.options.now() + this.options.resultDurationMs;
    match.updatedAtMs = this.options.now();

    this.options.logEvent("Letters round finalized", { matchId: match.matchId, roundNumber: match.liveRound.roundNumber, awardedScores });

    this.schedulePhaseTimer(match.matchId, this.options.resultDurationMs, () => {
      const current = this.matches.get(match.matchId);
      if (!current || current.phase !== "round_result") return;
      this.advanceRound(current);
      this.options.onMatchUpdated(current.matchId);
    });
  }

  private finalizeConundrumRound(match: MatchState, solvedEarly: boolean): void {
    if (match.liveRound.type !== "conundrum") return;
    this.clearPhaseTimer(match.matchId);

    const awardedScores: Record<string, number> = {
      [match.players[0]]: 0,
      [match.players[1]]: 0
    };

    if (match.liveRound.firstCorrectPlayerId) {
      awardedScores[match.liveRound.firstCorrectPlayerId] = 10;
      match.scores[match.liveRound.firstCorrectPlayerId] += 10;
    }

    const roundResult: RoundResult = {
      roundNumber: match.liveRound.roundNumber,
      type: "conundrum",
      awardedScores,
      details: {
        scrambled: match.liveRound.scrambled,
        answer: match.liveRound.answer,
        firstCorrectPlayerId: match.liveRound.firstCorrectPlayerId,
        firstCorrectAtMs: match.liveRound.firstCorrectAtMs
      }
    };

    match.roundResults.push(roundResult);
    match.phase = "round_result";
    match.phaseEndsAtMs = this.options.now() + this.options.resultDurationMs;
    match.updatedAtMs = this.options.now();

    this.options.logEvent("Conundrum round finalized", {
      matchId: match.matchId,
      roundNumber: match.liveRound.roundNumber,
      solvedEarly,
      firstCorrectPlayerId: match.liveRound.firstCorrectPlayerId
    });

    this.schedulePhaseTimer(match.matchId, this.options.resultDurationMs, () => {
      const current = this.matches.get(match.matchId);
      if (!current || current.phase !== "round_result") return;
      this.advanceRound(current);
      this.options.onMatchUpdated(current.matchId);
    });
  }

  private advanceRound(match: MatchState): void {
    this.clearPhaseTimer(match.matchId);

    if (match.roundIndex >= match.rounds.length - 1) {
      match.phase = "finished";
      match.phaseEndsAtMs = null;
      match.updatedAtMs = this.options.now();

      const [playerAId, playerBId] = match.players;
      const scoreA = match.scores[playerAId];
      const scoreB = match.scores[playerBId];

      match.winnerPlayerId = scoreA === scoreB ? null : scoreA > scoreB ? playerAId : playerBId;
      match.endReason = "completed";

      if (match.mode === "ranked") {
        if (scoreA === scoreB) {
          this.applyRankedOutcome(match, playerAId, playerBId, 0.5);
        } else if (scoreA > scoreB) {
          this.applyRankedOutcome(match, playerAId, playerBId, 1);
        } else {
          this.applyRankedOutcome(match, playerAId, playerBId, 0);
        }
      } else {
        match.ratingChanges = {
          [playerAId]: 0,
          [playerBId]: 0
        };
      }

      for (const playerId of match.players) {
        const player = this.players.get(playerId);
        if (player) {
          player.matchId = null;
          player.queuedMode = null;
        }
      }

      this.options.logEvent("Match finished", {
        matchId: match.matchId,
        mode: match.mode,
        scores: match.scores,
        winnerPlayerId: match.winnerPlayerId,
        ratingChanges: match.ratingChanges
      });

      this.options.onMatchUpdated(match.matchId);
      this.options.onMatchFinished(this.buildFinishedMatchRecord(match));
      return;
    }

    match.roundIndex += 1;
    const nextPlan = match.rounds[match.roundIndex];

    if (nextPlan.type === "letters") {
      match.liveRound = {
        type: "letters",
        roundNumber: nextPlan.roundNumber,
        pickerPlayerId: nextPlan.pickerPlayerId ?? match.players[0],
        picks: [],
        letters: [],
        bonusTiles: pickLetterBonusTiles(),
        submissions: {}
      };
      this.startLettersPickPhase(match);
      this.options.logEvent("Advanced to letters round", {
        matchId: match.matchId,
        roundNumber: nextPlan.roundNumber,
        phaseEndsAtMs: match.phaseEndsAtMs
      });
      return;
    }

    const conundrum = this.conundrums[Math.floor(Math.random() * this.conundrums.length)];
    const conundrumRound: ConundrumRoundState = {
      type: "conundrum",
      roundNumber: nextPlan.roundNumber,
      scrambled: scrambleWord(conundrum.answer, Math.random),
      answer: conundrum.answer,
      firstCorrectPlayerId: null,
      firstCorrectAtMs: null,
      guessesByPlayer: {}
    };

    this.startConundrumSolving(match, conundrumRound);
  }

  private applyRankedOutcome(match: MatchState, playerAId: string, playerBId: string, outcomeA: number): void {
    const playerA = this.players.get(playerAId);
    const playerB = this.players.get(playerBId);
    if (!playerA || !playerB) return;

    const beforeA = playerA.rating;
    const beforeB = playerB.rating;
    const { deltaA, deltaB } = computeEloDelta(beforeA, beforeB, outcomeA, playerA.rankedGames, playerB.rankedGames);

    playerA.rating += deltaA;
    playerB.rating += deltaB;
    playerA.peakRating = Math.max(playerA.peakRating, playerA.rating);
    playerB.peakRating = Math.max(playerB.peakRating, playerB.rating);

    playerA.rankedGames += 1;
    playerB.rankedGames += 1;

    if (outcomeA === 1) {
      playerA.rankedWins += 1;
      playerB.rankedLosses += 1;
    } else if (outcomeA === 0) {
      playerA.rankedLosses += 1;
      playerB.rankedWins += 1;
    } else {
      playerA.rankedDraws += 1;
      playerB.rankedDraws += 1;
    }

    match.ratingChanges = {
      [playerAId]: deltaA,
      [playerBId]: deltaB
    };

    this.options.logEvent("Ranked rating updated", {
      matchId: match.matchId,
      players: {
        [playerAId]: { before: beforeA, after: playerA.rating, delta: deltaA },
        [playerBId]: { before: beforeB, after: playerB.rating, delta: deltaB }
      }
    });
  }

  private evaluateWordSubmission(
    rawWord: string,
    letters: string[],
    bonusTiles: { doubleIndex: number; tripleIndex: number }
  ): WordSubmission {
    const submittedAtMs = this.options.now();
    const normalizedWord = normalizeWord(rawWord);

    if (!normalizedWord) {
      return {
        word: rawWord,
        normalizedWord,
        isValid: false,
        failureCode: "empty",
        score: 0,
        submittedAtMs
      };
    }

    if (!isAlphabetical(normalizedWord)) {
      return {
        word: rawWord,
        normalizedWord,
        isValid: false,
        failureCode: "non_alphabetical",
        score: 0,
        submittedAtMs
      };
    }

    if (!this.dictionary.has(normalizedWord)) {
      return {
        word: rawWord,
        normalizedWord,
        isValid: false,
        failureCode: "not_in_dictionary",
        score: 0,
        submittedAtMs
      };
    }

    if (!canConstructFromLetters(normalizedWord, letters)) {
      return {
        word: rawWord,
        normalizedWord,
        isValid: false,
        failureCode: "not_constructable",
        score: 0,
        submittedAtMs
      };
    }

    return {
      word: rawWord,
      normalizedWord,
      isValid: true,
      score: scoreWordFromLetters(normalizedWord, letters, bonusTiles),
      submittedAtMs
    };
  }

  private schedulePhaseTimer(matchId: string, delayMs: number, callback: () => void): void {
    this.clearPhaseTimer(matchId);
    const timer = this.options.setTimer(callback, delayMs);
    this.timers.set(matchId, timer);
  }

  private clearPhaseTimer(matchId: string): void {
    const timer = this.timers.get(matchId);
    if (timer) {
      this.options.clearTimer(timer);
      this.timers.delete(matchId);
    }
  }

  private buildFinishedMatchRecord(match: MatchState): FinishedMatchRecord {
    const changes = match.ratingChanges ?? {
      [match.players[0]]: 0,
      [match.players[1]]: 0
    };

    return {
      matchId: match.matchId,
      createdAtMs: match.createdAtMs,
      finishedAtMs: this.options.now(),
      mode: match.mode,
      players: match.players.map((playerId) => {
        const runtime = this.players.get(playerId);
        const ratingBefore = match.startRatings[playerId] ?? 1000;
        const ratingDelta = changes[playerId] ?? 0;
        const ratingAfter = runtime?.rating ?? ratingBefore + ratingDelta;

        return {
          playerId,
          displayName: runtime?.displayName ?? "Guest",
          score: match.scores[playerId] ?? 0,
          ratingBefore,
          ratingAfter,
          ratingDelta,
          rankTier: ratingToTier(ratingAfter)
        };
      }),
      winnerPlayerId: match.winnerPlayerId,
      matchEndReason: match.endReason,
      ratingChanges: changes,
      roundResults: match.roundResults
    };
  }

  private reconcilePlayerMatch(player: PlayerRuntime): void {
    if (!player.matchId) return;
    const match = this.matches.get(player.matchId);
    if (!match || match.phase === "finished") {
      player.matchId = null;
      player.queuedMode = null;
    }
  }
}
