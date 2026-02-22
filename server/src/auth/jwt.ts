import jwt from "jsonwebtoken";
import { loadEnv } from "../config/env.js";

export interface AccessTokenPayload {
  sub: string;
  sid: string;
  typ: "access";
}

export interface RefreshTokenPayload {
  sub: string;
  sid: string;
  typ: "refresh";
}

const env = loadEnv();

export const signAccessToken = (payload: Omit<AccessTokenPayload, "typ">): string =>
  jwt.sign(
    {
      ...payload,
      typ: "access"
    } satisfies AccessTokenPayload,
    env.JWT_ACCESS_SECRET,
    {
      expiresIn: env.ACCESS_TOKEN_TTL_SECONDS
    }
  );

export const signRefreshToken = (payload: Omit<RefreshTokenPayload, "typ">): string =>
  jwt.sign(
    {
      ...payload,
      typ: "refresh"
    } satisfies RefreshTokenPayload,
    env.JWT_REFRESH_SECRET,
    {
      expiresIn: env.REFRESH_TOKEN_TTL_SECONDS
    }
  );

export const verifyAccessToken = (token: string): AccessTokenPayload => {
  const decoded = jwt.verify(token, env.JWT_ACCESS_SECRET) as AccessTokenPayload;
  if (decoded.typ !== "access") {
    throw new Error("Invalid token type");
  }
  return decoded;
};

export const verifyRefreshToken = (token: string): RefreshTokenPayload => {
  const decoded = jwt.verify(token, env.JWT_REFRESH_SECRET) as RefreshTokenPayload;
  if (decoded.typ !== "refresh") {
    throw new Error("Invalid token type");
  }
  return decoded;
};
