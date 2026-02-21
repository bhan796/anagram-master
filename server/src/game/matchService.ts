import { randomUUID } from "node:crypto";
import type { ConundrumEntry } from "./data.js";
import {
  allowedPickKinds,
  canConstructFromLetters,
  createGuestName,
  drawWeightedLetter,
  isAlphabetical,
  normalizeWord,
  scoreWord
} from "./rules.js";
import type {
  ConundrumRoundState,
  FinishedMatchRecord,
  LiveRoundState,
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
  solveDurationMs: 30_000,
  resultDurationMs: 3_000,
  conundrumGuessCooldownMs: 750,
  logEvent: () => undefined
};

export class MatchService {
  private readonly players = new Map<string, PlayerRuntime>();
  private readonly queue: string[] = [];
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

  connectPlayer(socketId: string, requestedPlayerId?: string, requestedDisplayName?: string): PlayerRuntime {
    const now = this.options.now();
    const playerId = requestedPlayerId && this.players.has(requestedPlayerId) ? requestedPlayerId : randomUUID();

    const existing = this.players.get(playerId);
    const player: PlayerRuntime =
      existing ?? {
        playerId,
        displayName: requestedDisplayName?.trim() || createGuestName(),
        socketId,
        connected: true,
        matchId: null,
        lastConundrumGuessAtMs: 0
      };

    player.socketId = socketId;
    player.connected = true;

    if (requestedDisplayName?.trim()) {
      player.displayName = requestedDisplayName.trim();
    }

    this.players.set(playerId, player);

    this.options.logEvent("Player connected", { playerId, socketId, connectedAtMs: now });
    return player;
  }

  disconnectSocket(socketId: string): void {
    for (const player of this.players.values()) {
      if (player.socketId === socketId) {
        player.connected = false;
        player.socketId = null;
        this.options.logEvent("Player disconnected", { playerId: player.playerId, socketId });

        if (player.matchId) {
          this.options.onMatchUpdated(player.matchId);
        }

        break;
      }
    }
  }

  joinQueue(playerId: string): { ok: boolean; code?: string; queueSize?: number } {
    const player = this.players.get(playerId);
    if (!player) return { ok: false, code: "UNKNOWN_PLAYER" };
    if (player.matchId) return { ok: false, code: "ALREADY_IN_MATCH" };

    if (!this.queue.includes(playerId)) {
      this.queue.push(playerId);
      this.options.onQueueUpdated(playerId, this.queue.length);
    }

    this.tryMatchPlayers();
    return { ok: true, queueSize: this.queue.length };
  }

  leaveQueue(playerId: string): void {
    const idx = this.queue.indexOf(playerId);
    if (idx >= 0) {
      this.queue.splice(idx, 1);
      this.options.onQueueUpdated(playerId, this.queue.length);
    }
  }

  getPlayer(playerId: string): PlayerRuntime | undefined {
    return this.players.get(playerId);
  }

  getMatchByPlayer(playerId: string): MatchState | undefined {
    const player = this.players.get(playerId);
    if (!player?.matchId) return undefined;
    return this.matches.get(player.matchId);
  }

  getMatch(matchId: string): MatchState | undefined {
    return this.matches.get(matchId);
  }

  resumeMatch(playerId: string, matchId: string): { ok: boolean; code?: string } {
    const match = this.matches.get(matchId);
    if (!match) return { ok: false, code: "MATCH_NOT_FOUND" };
    if (!match.players.includes(playerId)) return { ok: false, code: "NOT_MATCH_PARTICIPANT" };
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

    const submission = this.evaluateWordSubmission(rawWord, match.liveRound.letters);
    match.liveRound.submissions[playerId] = submission;
    match.updatedAtMs = this.options.now();

    this.options.logEvent(
      "Letters word submitted",
      {
        matchId: match.matchId,
        playerId,
        normalizedWord: submission.normalizedWord,
        isValid: submission.isValid,
        score: submission.score
      }
    );

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

  serializeForPlayer(match: MatchState, requesterPlayerId?: string): SerializedMatchState {
    const now = this.options.now();
    const playerViews = match.players.map((playerId) => {
      const player = this.players.get(playerId);
      return {
        playerId,
        displayName: player?.displayName ?? "Guest",
        connected: player?.connected ?? false,
        score: match.scores[playerId] ?? 0
      };
    });

    const payload: SerializedMatchState = {
      matchId: match.matchId,
      phase: match.phase,
      phaseEndsAtMs: match.phaseEndsAtMs,
      serverNowMs: now,
      roundNumber: match.liveRound.roundNumber,
      roundType: match.liveRound.type,
      players: playerViews,
      roundResults: match.roundResults,
      winnerPlayerId: match.winnerPlayerId
    };

    if (match.liveRound.type === "letters") {
      payload.letters = [...match.liveRound.letters];
      payload.pickerPlayerId = match.liveRound.pickerPlayerId;
    } else {
      payload.scrambled = match.liveRound.scrambled;
      if (match.phase !== "conundrum_solving") {
        payload.roundResults = match.roundResults;
      }
    }

    if (requesterPlayerId && match.liveRound.type === "letters" && match.phase === "round_result") {
      payload.roundResults = match.roundResults;
    }

    return payload;
  }

  private tryMatchPlayers(): void {
    while (this.queue.length >= 2) {
      const playerAId = this.queue.shift();
      const playerBId = this.queue.shift();
      if (!playerAId || !playerBId) break;

      const playerA = this.players.get(playerAId);
      const playerB = this.players.get(playerBId);
      if (!playerA || !playerB || playerA.matchId || playerB.matchId) {
        continue;
      }

      const match = this.createMatch(playerAId, playerBId);
      playerA.matchId = match.matchId;
      playerB.matchId = match.matchId;

      this.options.logEvent("Match created", { matchId: match.matchId, players: match.players });
      this.options.onMatchUpdated(match.matchId);
    }
  }

  private createMatch(playerAId: string, playerBId: string): MatchState {
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
      submissions: {}
    };

    const match: MatchState = {
      matchId,
      createdAtMs: now,
      players: [playerAId, playerBId],
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
      updatedAtMs: now
    };

    this.matches.set(matchId, match);
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
        match.liveRound.submissions[playerId] = this.evaluateWordSubmission("", match.liveRound.letters);
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
      awardedScores[match.liveRound.firstCorrectPlayerId] = 12;
      match.scores[match.liveRound.firstCorrectPlayerId] += 12;
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

    this.options.logEvent(
      "Conundrum round finalized",
      {
        matchId: match.matchId,
        roundNumber: match.liveRound.roundNumber,
        solvedEarly,
        firstCorrectPlayerId: match.liveRound.firstCorrectPlayerId
      }
    );

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

      const [playerA, playerB] = match.players;
      const scoreA = match.scores[playerA];
      const scoreB = match.scores[playerB];
      match.winnerPlayerId = scoreA === scoreB ? null : scoreA > scoreB ? playerA : playerB;

      for (const playerId of match.players) {
        const player = this.players.get(playerId);
        if (player) {
          player.matchId = null;
        }
      }

      this.options.logEvent("Match finished", { matchId: match.matchId, scores: match.scores, winnerPlayerId: match.winnerPlayerId });
      this.options.onMatchFinished(this.buildFinishedMatchRecord(match));
      return;
    }

    match.roundIndex += 1;
    const nextPlan = match.rounds[match.roundIndex];

    if (nextPlan.type === "letters") {
      match.phase = "awaiting_letters_pick";
      match.phaseEndsAtMs = null;
      match.liveRound = {
        type: "letters",
        roundNumber: nextPlan.roundNumber,
        pickerPlayerId: nextPlan.pickerPlayerId ?? match.players[0],
        picks: [],
        letters: [],
        submissions: {}
      };
      match.updatedAtMs = this.options.now();
      this.options.logEvent("Advanced to letters round", { matchId: match.matchId, roundNumber: nextPlan.roundNumber });
      return;
    }

    const conundrum = this.conundrums[Math.floor(Math.random() * this.conundrums.length)];
    const conundrumRound: ConundrumRoundState = {
      type: "conundrum",
      roundNumber: nextPlan.roundNumber,
      scrambled: conundrum.scrambled,
      answer: conundrum.answer,
      firstCorrectPlayerId: null,
      firstCorrectAtMs: null,
      guessesByPlayer: {}
    };

    this.startConundrumSolving(match, conundrumRound);
  }

  private evaluateWordSubmission(rawWord: string, letters: string[]): WordSubmission {
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
      score: scoreWord(normalizedWord.length),
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
    return {
      matchId: match.matchId,
      createdAtMs: match.createdAtMs,
      finishedAtMs: this.options.now(),
      players: match.players.map((playerId) => ({
        playerId,
        displayName: this.players.get(playerId)?.displayName ?? "Guest",
        score: match.scores[playerId] ?? 0
      })),
      winnerPlayerId: match.winnerPlayerId,
      roundResults: match.roundResults
    };
  }
}
