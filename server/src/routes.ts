import type { Request, Response } from "express";
import { Router } from "express";

export const healthRouter = Router();

healthRouter.get("/health", (_req: Request, res: Response) => {
  res.json({
    status: "ok",
    service: "anagram-server",
    timestamp: new Date().toISOString()
  });
});