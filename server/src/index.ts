import cors from "cors";
import dotenv from "dotenv";
import express from "express";
import { createServer } from "node:http";
import { loadEnv } from "./config/env.js";
import { logger } from "./config/logger.js";
import { healthRouter } from "./routes.js";
import { createSocketServer } from "./events/socketServer.js";

dotenv.config();

const env = loadEnv();
const app = express();

app.use(
  cors({
    origin: env.CLIENT_ORIGIN
  })
);
app.use(express.json());

app.use("/api", healthRouter);

const httpServer = createServer(app);
createSocketServer(httpServer);

httpServer.listen(env.PORT, () => {
  logger.info({ port: env.PORT }, "Server started");
});