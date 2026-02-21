import type { Server as HttpServer } from "node:http";
import { Server } from "socket.io";
import { loadEnv } from "../config/env.js";
import { logger } from "../config/logger.js";

export const createSocketServer = (httpServer: HttpServer): Server => {
  const env = loadEnv();

  const io = new Server(httpServer, {
    cors: {
      origin: env.CLIENT_ORIGIN,
      methods: ["GET", "POST"]
    }
  });

  io.on("connection", (socket) => {
    logger.info({ socketId: socket.id }, "Client connected");

    socket.on("disconnect", (reason) => {
      logger.info({ socketId: socket.id, reason }, "Client disconnected");
    });
  });

  return io;
};