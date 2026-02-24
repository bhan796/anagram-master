import { describe, expect, it } from "vitest";
import { canMutateProfile, canReadOwnedProfile } from "../src/routes.js";

describe("route ownership authorization helpers", () => {
  it("allows public read when profile has no owner", () => {
    expect(canReadOwnedProfile(undefined, null)).toBe(true);
    expect(canReadOwnedProfile("user-1", null)).toBe(true);
  });

  it("allows only owner to read owned profiles", () => {
    expect(canReadOwnedProfile(undefined, "user-1")).toBe(false);
    expect(canReadOwnedProfile("user-2", "user-1")).toBe(false);
    expect(canReadOwnedProfile("user-1", "user-1")).toBe(true);
  });

  it("allows mutation only for authenticated owner (or claim of unowned profile)", () => {
    expect(canMutateProfile(undefined, null)).toBe(false);
    expect(canMutateProfile(undefined, "user-1")).toBe(false);
    expect(canMutateProfile("user-2", "user-1")).toBe(false);
    expect(canMutateProfile("user-1", "user-1")).toBe(true);
    expect(canMutateProfile("user-1", null)).toBe(true);
  });
});
