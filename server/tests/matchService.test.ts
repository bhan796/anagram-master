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

  it("forfeits match on disconnect and awards win to the remaining player", () => {
    const scheduler = new FakeScheduler();
    const service = makeService(scheduler);
    const { p1, p2, matchId } = startMatch(service);

    service.disconnectSocket("socket-1");

    const match = service.getMatch(matchId);
    expect(match).toBeTruthy();
    if (!match) return;
    expect(match.phase).toBe("finished");
    expect(match.winnerPlayerId).toBe(p2);
    expect(service.getMatchByPlayer(p1)).toBeUndefined();
    expect(service.getMatchByPlayer(p2)).toBeUndefined();
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

  it("never creates a match with the same player on both sides", () => {
    const scheduler = new FakeScheduler();
    const service = makeService(scheduler);

    const p1 = service.connectPlayer("socket-1", undefined, "One").playerId;
    const p2 = service.connectPlayer("socket-2", undefined, "Two").playerId;
    const p3 = service.connectPlayer("socket-3", undefined, "Three").playerId;

    // Simulate queue corruption with duplicate same player id.
    (service as unknown as { queueByMode: { casual: Array<{ playerId: string; joinedAtMs: number }> } }).queueByMode.casual.push(
      { playerId: p1, joinedAtMs: scheduler.now() },
      { playerId: p1, joinedAtMs: scheduler.now() }
    );
    expect(service.joinQueue(p2).ok).toBe(true);
    expect(service.joinQueue(p3).ok).toBe(true);

    const match = service.getMatchByPlayer(p2);
    expect(match).toBeTruthy();
    if (!match) return;
    expect(match.players[0]).not.toBe(match.players[1]);
    expect(new Set(match.players).size).toBe(2);
  });

  it("applies ranked Elo updates for ranked matches only", () => {
    const scheduler = new FakeScheduler();
    const service = makeService(scheduler);
    const p1 = service.connectPlayer("socket-1", undefined, "One").playerId;
    const p2 = service.connectPlayer("socket-2", undefined, "Two").playerId;

    expect(service.joinQueue(p1, "ranked").ok).toBe(true);
    expect(service.joinQueue(p2, "ranked").ok).toBe(true);

    let match = service.getMatchByPlayer(p1);
    expect(match?.mode).toBe("ranked");
    if (!match || match.liveRound.type !== "letters") return;

    for (let round = 0; round < 4; round += 1) {
      const current = service.getMatchByPlayer(p1);
      if (!current || current.liveRound.type !== "letters") return;
      const picker = current.liveRound.pickerPlayerId;

      for (let i = 0; i < 9; i += 1) {
        expect(service.pickLetter(picker, i < 8 ? "vowel" : "consonant").ok).toBe(true);
      }
      expect(service.submitWord(p1, "stone").ok).toBe(true);
      expect(service.submitWord(p2, "").ok).toBe(true);
      scheduler.advanceBy(250);
    }

    match = service.getMatchByPlayer(p1);
    expect(match?.phase).toBe("conundrum_solving");
    expect(service.submitConundrumGuess(p1, "algorithm").ok).toBe(true);
    scheduler.advanceBy(250);

    const finished = service.getMatch(match?.matchId ?? "");
    expect(finished?.phase).toBe("finished");
    expect(finished?.ratingChanges).toBeTruthy();

    const afterP1 = service.getPlayer(p1);
    const afterP2 = service.getPlayer(p2);
    expect(afterP1?.rating).not.toBe(1000);
    expect(afterP2?.rating).not.toBe(1000);
    expect((afterP1?.rankedGames ?? 0) + (afterP2?.rankedGames ?? 0)).toBe(2);
  });

  it("auto-fills remaining letters after 20s pick timer and starts solving phase", () => {
    const scheduler = new FakeScheduler();
    const service = makeService(scheduler);
    const { p1 } = startMatch(service);

    const before = service.getMatchByPlayer(p1);
    expect(before?.phase).toBe("awaiting_letters_pick");
    expect(before?.liveRound.type).toBe("letters");
    if (!before || before.liveRound.type !== "letters") return;
    expect(before.liveRound.letters.length).toBe(0);

    scheduler.advanceBy(20_100);

    const after = service.getMatchByPlayer(p1);
    expect(after?.phase).toBe("letters_solving");
    expect(after?.liveRound.type).toBe("letters");
    if (!after || after.liveRound.type !== "letters") return;
    expect(after.liveRound.letters.length).toBe(9);
    const vowels = after.liveRound.letters.filter((c) => "AEIOU".includes(c)).length;
    const consonants = after.liveRound.letters.length - vowels;
    expect(vowels).toBeGreaterThan(0);
    expect(consonants).toBeGreaterThan(0);
  });
});
