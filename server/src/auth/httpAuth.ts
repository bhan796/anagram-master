import type { NextFunction, Request, Response } from "express";
import { verifyAccessToken } from "./jwt.js";

export interface AuthContext {
  userId: string;
  sessionId: string;
}

declare module "express-serve-static-core" {
  interface Request {
    auth?: AuthContext;
  }
}

const parseBearer = (header?: string): string | null => {
  if (!header) return null;
  const [scheme, token] = header.split(" ");
  if (!scheme || !token || scheme.toLowerCase() !== "bearer") return null;
  return token.trim();
};

export const attachOptionalAuth = (req: Request, _res: Response, next: NextFunction): void => {
  const token = parseBearer(req.header("authorization"));
  if (!token) {
    next();
    return;
  }

  try {
    const decoded = verifyAccessToken(token);
    req.auth = {
      userId: decoded.sub,
      sessionId: decoded.sid
    };
  } catch {
    // keep request unauthenticated for optional flow
  }

  next();
};

export const requireAuth = (req: Request, res: Response, next: NextFunction): void => {
  if (!req.auth) {
    res.status(401).json({ code: "AUTH_REQUIRED", message: "Authentication required." });
    return;
  }
  next();
};
