import { describe, expect, it } from "vitest";
import { resolveAuthenticatedUserIdFromAccessToken } from "../src/events/socketServer.js";

describe("socket access-token auth resolution", () => {
  it("returns null when no token is provided", async () => {
    const userId = await resolveAuthenticatedUserIdFromAccessToken(undefined, {
      userExists: async () => true
    } as never);
    expect(userId).toBeNull();
  });

  it("returns null when verifier throws (invalid token)", async () => {
    const userId = await resolveAuthenticatedUserIdFromAccessToken(
      "bad-token",
      { userExists: async () => true } as never,
      () => {
        throw new Error("invalid");
      }
    );
    expect(userId).toBeNull();
  });

  it("returns null when token user no longer exists", async () => {
    const userId = await resolveAuthenticatedUserIdFromAccessToken(
      "token",
      { userExists: async () => false } as never,
      () => ({ sub: "deleted-user" })
    );
    expect(userId).toBeNull();
  });

  it("returns user id when token is valid and user exists", async () => {
    const userId = await resolveAuthenticatedUserIdFromAccessToken(
      "token",
      { userExists: async () => true } as never,
      () => ({ sub: "active-user" })
    );
    expect(userId).toBe("active-user");
  });
});
