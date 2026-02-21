import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { describe, expect, it } from "vitest";
import type { FinishedMatchRecord } from "../src/game/types.js";
import { MatchHistoryStore } from "../src/store/matchHistoryStore.js";

const sampleMatch = (): FinishedMatchRecord => ({
  matchId: "m1",
  createdAtMs: 100,
  finishedAtMs: 200,
  players: [
    { playerId: "p1", displayName: "One", score: 25 },
    { playerId: "p2", displayName: "Two", score: 10 }
  ],
  winnerPlayerId: "p1",
  roundResults: []
});

describe("MatchHistoryStore", () => {
  it("records match and computes player stats", () => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), "anagram-history-"));
    const store = new MatchHistoryStore(path.join(dir, "history.json"));

    store.recordMatch(sampleMatch());

    const stats = store.getPlayerStats("p1");
    expect(stats).toBeTruthy();
    expect(stats?.matchesPlayed).toBe(1);
    expect(stats?.wins).toBe(1);
    expect(stats?.totalScore).toBe(25);

    const history = store.getPlayerMatchHistory("p1");
    expect(history.length).toBe(1);
    expect(history[0].matchId).toBe("m1");
  });
});
