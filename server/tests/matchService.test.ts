import { describe, expect, it } from "vitest";
import { MatchService } from "../src/game/matchService.js";

class FakeScheduler {
  private nowMs = 0;
  private tasks: Array<{ id: number; runAt: number; callback: () => void }> = [];
  private nextId = 1;

  now = (): number => this.nowMs;

  setTimer = (callback: () => void, delayMs: number): number => {
    const id = this.nextId++;
    this.tasks.push({ id, runAt: this.nowMs + delayMs, callback });
    return id;
  };

  clearTimer = (timer: unknown): void => {
    const id = timer as number;
    this.tasks = this.tasks.filter((task) => task.id !== id);
  };

  advanceBy(ms: number): void {
    this.nowMs += ms;

    let ranTask = true;
    while (ranTask) {
      ranTask = false;
      const due = this.tasks
        .filter((task) => task.runAt <= this.nowMs)
        .sort((a, b) => a.runAt - b.runAt);

      for (const task of due) {
        this.tasks = this.tasks.filter((t) => t.id !== task.id);
        task.callback();
        ranTask = true;
      }
    }
  }
}

const makeService = (scheduler: FakeScheduler): MatchService => {
  const dictionary = new Set(["stone", "notes", "algorithm", "streaming"]);
  const conundrums = [
    {
      id: "1",
      scrambled: "LAROGIMTH",
      answer: "algorithm"
    }
  ];

  return new MatchService(dictionary, conundrums, {
    now: scheduler.now,
    setTimer: scheduler.setTimer,
    clearTimer: scheduler.clearTimer,
    solveDurationMs: 3_000,
    resultDurationMs: 200,
    conundrumGuessCooldownMs: 500
  });
};

const startMatch = (service: MatchService): { p1: string; p2: string; matchId: string } => {
  const p1 = service.connectPlayer("socket-1", undefined, "One").playerId;
  const p2 = service.connectPlayer("socket-2", undefined, "Two").playerId;
  service.joinQueue(p1);
  service.joinQueue(p2);

  const match = service.getMatchByPlayer(p1);
  if (!match) {
    throw new Error("match not created");
  }

  return { p1, p2, matchId: match.matchId };
};

const completeLettersRound = (service: MatchService, scheduler: FakeScheduler, p1: string, p2: string): void => {
  const match = service.getMatchByPlayer(p1);
  if (!match || match.liveRound.type !== "letters") {
    throw new Error("expected letters round");
  }

  const picker = match.liveRound.pickerPlayerId;
  for (let i = 0; i < 9; i += 1) {
    const kind = i < 8 ? "vowel" : "consonant";
    const result = service.pickLetter(picker, kind);
    expect(result.ok).toBe(true);
  }

  expect(service.submitWord(p1, "").ok).toBe(true);
  expect(service.submitWord(p2, "").ok).toBe(true);

  scheduler.advanceBy(250);
};

describe("MatchService", () => {
  it("enforces round timer and rejects late letters submissions", () => {
    const scheduler = new FakeScheduler();
    const service = makeService(scheduler);
    const { p1, p2 } = startMatch(service);

    const match = service.getMatchByPlayer(p1);
    expect(match).toBeTruthy();
    if (!match || match.liveRound.type !== "letters") return;

    for (let i = 0; i < 9; i += 1) {
      const pick = service.pickLetter(match.liveRound.pickerPlayerId, i < 8 ? "vowel" : "consonant");
      expect(pick.ok).toBe(true);
    }

    scheduler.advanceBy(3_100);

    const expired = service.getMatchByPlayer(p1);
    expect(expired?.phase).toBe("round_result");

    const late = service.submitWord(p2, "stone");
    expect(late.ok).toBe(false);
    expect(late.code).toBe("LATE_SUBMISSION");
  });

  it("keeps score authoritative on server validation", () => {
    const scheduler = new FakeScheduler();
    const service = makeService(scheduler);
    const { p1, p2 } = startMatch(service);

    const match = service.getMatchByPlayer(p1);
    if (!match || match.liveRound.type !== "letters") return;

    for (let i = 0; i < 9; i += 1) {
      expect(service.pickLetter(match.liveRound.pickerPlayerId, i < 8 ? "vowel" : "consonant").ok).toBe(true);
    }

    expect(service.submitWord(p1, "zzzzzzzzz").ok).toBe(true);
    expect(service.submitWord(p2, "").ok).toBe(true);

    const after = service.getMatchByPlayer(p1);
    expect(after?.scores[p1]).toBe(0);
  });

  it("rejects duplicate submission in letters round", () => {
    const scheduler = new FakeScheduler();
    const service = makeService(scheduler);
    const { p1 } = startMatch(service);

    const match = service.getMatchByPlayer(p1);
    if (!match || match.liveRound.type !== "letters") return;

    for (let i = 0; i < 9; i += 1) {
      expect(service.pickLetter(match.liveRound.pickerPlayerId, i < 8 ? "vowel" : "consonant").ok).toBe(true);
    }

    expect(service.submitWord(p1, "stone").ok).toBe(true);
    const duplicate = service.submitWord(p1, "notes");
    expect(duplicate.ok).toBe(false);
    expect(duplicate.code).toBe("DUPLICATE_SUBMISSION");
  });

  it("resolves conundrum by first correct guess only", () => {
    const scheduler = new FakeScheduler();
    const service = makeService(scheduler);
    const { p1, p2 } = startMatch(service);

    completeLettersRound(service, scheduler, p1, p2);
    completeLettersRound(service, scheduler, p1, p2);
    completeLettersRound(service, scheduler, p1, p2);
    completeLettersRound(service, scheduler, p1, p2);

    const conundrumMatch = service.getMatchByPlayer(p1);
    expect(conundrumMatch?.phase).toBe("conundrum_solving");

    const first = service.submitConundrumGuess(p1, "algorithm");
    expect(first.ok).toBe(true);

    const second = service.submitConundrumGuess(p2, "algorithm");
    expect(second.ok).toBe(false);
    expect(["ALREADY_SOLVED", "LATE_SUBMISSION"]).toContain(second.code);

    const after = service.getMatchByPlayer(p1);
    expect(after?.scores[p1]).toBe(12);
    expect(after?.scores[p2]).toBe(0);
  });

  it("restores match state for reconnecting player", () => {
    const scheduler = new FakeScheduler();
    const service = makeService(scheduler);
    const { p1, matchId } = startMatch(service);

    service.disconnectSocket("socket-1");

    const reconnected = service.connectPlayer("socket-1b", p1, "One");
    expect(reconnected.playerId).toBe(p1);

    const resume = service.resumeMatch(p1, matchId);
    expect(resume.ok).toBe(true);

    const match = service.getMatch(matchId);
    expect(match).toBeTruthy();
    if (!match) return;

    const snapshot = service.serializeForPlayer(match, p1);
    expect(snapshot.matchId).toBe(matchId);
    expect(snapshot.players.length).toBe(2);
  });

  it("prevents a player from joining queue while already in an active match", () => {
    const scheduler = new FakeScheduler();
    const service = makeService(scheduler);
    const { p1 } = startMatch(service);

    const joinAgain = service.joinQueue(p1);
    expect(joinAgain.ok).toBe(false);
    expect(joinAgain.code).toBe("ALREADY_IN_MATCH");
  });

  it("matches only players who are searching and not already in a match", () => {
    const scheduler = new FakeScheduler();
    const service = makeService(scheduler);
    const { p1, p2 } = startMatch(service);

    const p3 = service.connectPlayer("socket-3", undefined, "Three").playerId;
    const p4 = service.connectPlayer("socket-4", undefined, "Four").playerId;

    const inMatchJoin = service.joinQueue(p1);
    expect(inMatchJoin.ok).toBe(false);
    expect(inMatchJoin.code).toBe("ALREADY_IN_MATCH");

    expect(service.joinQueue(p3).ok).toBe(true);
    expect(service.joinQueue(p4).ok).toBe(true);

    const secondMatch = service.getMatchByPlayer(p3);
    expect(secondMatch).toBeTruthy();
    if (!secondMatch) return;

    expect(secondMatch.players).toContain(p3);
    expect(secondMatch.players).toContain(p4);
    expect(secondMatch.players).not.toContain(p1);
    expect(secondMatch.players).not.toContain(p2);
  });

  it("removes disconnected players from queue so they are never paired", () => {
    const scheduler = new FakeScheduler();
    const service = makeService(scheduler);

    const p1 = service.connectPlayer("socket-1", undefined, "One").playerId;
    const p2 = service.connectPlayer("socket-2", undefined, "Two").playerId;
    const p3 = service.connectPlayer("socket-3", undefined, "Three").playerId;

    expect(service.joinQueue(p1).ok).toBe(true);
    service.disconnectSocket("socket-1");

    expect(service.joinQueue(p2).ok).toBe(true);
    expect(service.getMatchByPlayer(p2)).toBeUndefined();

    expect(service.joinQueue(p3).ok).toBe(true);
    const match = service.getMatchByPlayer(p2);
    expect(match).toBeTruthy();
    if (!match) return;

    expect(match.players).toContain(p2);
    expect(match.players).toContain(p3);
    expect(match.players).not.toContain(p1);
  });
});
