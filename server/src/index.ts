import cors from "cors";
import dotenv from "dotenv";
import express from "express";
import { createServer } from "node:http";
import { createOriginChecker } from "./config/cors.js";
import { loadEnv } from "./config/env.js";
import { logger } from "./config/logger.js";
import { AuthService } from "./auth/authService.js";
import { createApiRouter } from "./routes.js";
import { createSocketServer } from "./events/socketServer.js";
import { MatchHistoryStore } from "./store/matchHistoryStore.js";
import { PresenceStore } from "./store/presenceStore.js";

dotenv.config();

const env = loadEnv();
const app = express();
const matchHistoryStore = new MatchHistoryStore();
const presenceStore = new PresenceStore();
const authService = new AuthService();
const isAllowedOrigin = createOriginChecker(env.CLIENT_ORIGIN);
await matchHistoryStore.initialize();

app.use(
  cors({
    origin: (origin, callback) => {
      if (isAllowedOrigin(origin)) {
        callback(null, true);
        return;
      }
      callback(new Error("Not allowed by CORS"));
    }
  })
);
app.use(express.json());

const httpServer = createServer(app);
const socketRuntime = createSocketServer(httpServer, matchHistoryStore, presenceStore, authService);
app.use("/api", createApiRouter(matchHistoryStore, presenceStore, socketRuntime.matchService, authService));

httpServer.listen(env.PORT, () => {
  logger.info({ port: env.PORT }, "Server started");
});
