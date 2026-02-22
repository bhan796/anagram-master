import type { Server as HttpServer } from "node:http";
import { Server } from "socket.io";
import { createOriginChecker } from "../config/cors.js";
import { loadEnv } from "../config/env.js";
import { logger } from "../config/logger.js";
import { AuthService } from "../auth/authService.js";
import { verifyAccessToken } from "../auth/jwt.js";
import { loadConundrums, loadDictionarySet } from "../game/data.js";
import { MatchService } from "../game/matchService.js";
import { ratingToTier } from "../game/ranking.js";
import type { MatchMode } from "../game/types.js";
import type { MatchHistoryStore } from "../store/matchHistoryStore.js";
import type { PresenceStore } from "../store/presenceStore.js";
import { SocketEvents, toActionError } from "./contracts.js";

export const createSocketServer = (
  httpServer: HttpServer,
  matchHistoryStore: MatchHistoryStore,
  presenceStore: PresenceStore,
  authService: AuthService
): { io: Server; matchService: MatchService } => {
  const env = loadEnv();
  const isAllowedOrigin = createOriginChecker(env.CLIENT_ORIGIN);

  const io = new Server(httpServer, {
    cors: {
      origin: (origin, callback) => {
        callback(null, isAllowedOrigin(origin));
      },
      methods: ["GET", "POST"]
    }
  });

  const service = new MatchService(loadDictionarySet(), loadConundrums(), {
    logEvent: (message, details) => logger.info(details ?? {}, message),
    onMatchFinished: (record) => {
      matchHistoryStore.recordMatch(record);
      logger.info({ matchId: record.matchId }, "Persisted finished match record");
    },
    onMatchUpdated: (matchId) => {
      const match = service.getMatch(matchId);
      if (!match) return;

      for (const participantId of match.players) {
        const player = service.getPlayer(participantId);
        if (!player?.socketId) continue;

        const payload = service.serializeForPlayer(match);
        io.to(player.socketId).emit(SocketEvents.matchState, payload);
      }
    },
    onQueueUpdated: (updatedPlayerId, queueSize, mode) => {
      const player = service.getPlayer(updatedPlayerId);
      if (!player?.socketId) return;
      io.to(player.socketId).emit(SocketEvents.matchmakingStatus, {
        queueSize,
        state: queueSize > 0 ? "searching" : "idle",
        mode
      });
    }
  });

  io.on("connection", (socket) => {
    logger.info({ socketId: socket.id }, "Client connected");

    let playerId: string | null = null;

    socket.on(SocketEvents.sessionIdentify, async (payload: { playerId?: string; displayName?: string; accessToken?: string } = {}) => {
      let authenticatedUserId: string | null = null;
      let resolvedPlayerId = payload.playerId;
      if (payload.accessToken) {
        try {
          const decoded = verifyAccessToken(payload.accessToken);
          authenticatedUserId = decoded.sub;
        } catch {
          authenticatedUserId = null;
        }
      }

      if (authenticatedUserId) {
        if (resolvedPlayerId) {
          const ownerUserId = await authService.resolveUserIdForPlayer(resolvedPlayerId);
          if (ownerUserId && ownerUserId !== authenticatedUserId) {
            resolvedPlayerId = undefined;
          }
        }

        if (!resolvedPlayerId) {
          resolvedPlayerId = (await authService.resolvePrimaryPlayerIdForUser(authenticatedUserId)) ?? undefined;
        }
      }

      const player = service.connectPlayer(socket.id, resolvedPlayerId, payload.displayName, authenticatedUserId);
      playerId = player.playerId;
      presenceStore.markOnline(player.playerId);
      matchHistoryStore.touchPlayer(player);
      await authService.upsertPlayerIdentity(player.playerId, player.displayName, player.userId);

      socket.emit(SocketEvents.sessionIdentify, {
        playerId: player.playerId,
        displayName: player.displayName,
        rating: player.rating,
        rankTier: ratingToTier(player.rating),
        isAuthenticated: Boolean(player.userId),
        serverNowMs: Date.now()
      });

      const activeMatch = service.getMatchByPlayer(player.playerId);
      if (activeMatch) {
        socket.emit(SocketEvents.matchState, service.serializeForPlayer(activeMatch));
      }
    });

    socket.on(SocketEvents.queueJoin, (payload: { mode?: MatchMode } = {}) => {
      if (!playerId) {
        socket.emit(SocketEvents.actionError, toActionError(SocketEvents.queueJoin, "UNKNOWN_PLAYER"));
        return;
      }

      const mode: MatchMode = payload.mode === "ranked" ? "ranked" : "casual";
      const result = service.joinQueue(playerId, mode);
      if (!result.ok && result.code) {
        socket.emit(SocketEvents.actionError, toActionError(SocketEvents.queueJoin, result.code));
        return;
      }

      const match = service.getMatchByPlayer(playerId);
      if (match) {
        socket.emit(SocketEvents.matchFound, {
          matchId: match.matchId,
          serverNowMs: Date.now()
        });
        socket.emit(SocketEvents.matchState, service.serializeForPlayer(match));

        for (const otherPlayerId of match.players) {
          if (otherPlayerId === playerId) continue;
          const other = service.getPlayer(otherPlayerId);
          if (other?.socketId) {
            io.to(other.socketId).emit(SocketEvents.matchFound, {
              matchId: match.matchId,
              serverNowMs: Date.now()
            });
          }
        }
      }
    });

    socket.on(SocketEvents.queueLeave, () => {
      if (!playerId) return;
      const player = service.getPlayer(playerId);
      const mode = player?.queuedMode ?? "casual";
      service.leaveQueue(playerId);
      socket.emit(SocketEvents.matchmakingStatus, {
        queueSize: 0,
        state: "idle",
        mode
      });
    });

    socket.on(SocketEvents.matchResume, (payload: { matchId: string }) => {
      if (!playerId) {
        socket.emit(SocketEvents.actionError, toActionError(SocketEvents.matchResume, "UNKNOWN_PLAYER"));
        return;
      }

      const result = service.resumeMatch(playerId, payload.matchId);
      if (!result.ok && result.code) {
        socket.emit(SocketEvents.actionError, toActionError(SocketEvents.matchResume, result.code));
        return;
      }

      const match = service.getMatch(payload.matchId);
      if (match) {
        socket.emit(SocketEvents.matchState, service.serializeForPlayer(match));
      }
    });

    socket.on(SocketEvents.matchForfeit, () => {
      if (!playerId) {
        socket.emit(SocketEvents.actionError, toActionError(SocketEvents.matchForfeit, "UNKNOWN_PLAYER"));
        return;
      }

      const result = service.forfeitMatch(playerId, "manual_leave");
      if (!result.ok && result.code) {
        socket.emit(SocketEvents.actionError, toActionError(SocketEvents.matchForfeit, result.code));
      }
    });

    socket.on(SocketEvents.roundPickLetter, (payload: { kind: "vowel" | "consonant" }) => {
      if (!playerId) {
        socket.emit(SocketEvents.actionError, toActionError(SocketEvents.roundPickLetter, "UNKNOWN_PLAYER"));
        return;
      }

      const result = service.pickLetter(playerId, payload.kind);
      if (!result.ok && result.code) {
        socket.emit(SocketEvents.actionError, toActionError(SocketEvents.roundPickLetter, result.code));
      }
    });

    socket.on(SocketEvents.roundSubmitWord, (payload: { word: string }) => {
      if (!playerId) {
        socket.emit(SocketEvents.actionError, toActionError(SocketEvents.roundSubmitWord, "UNKNOWN_PLAYER"));
        return;
      }

      const result = service.submitWord(playerId, payload.word ?? "");
      if (!result.ok && result.code) {
        socket.emit(SocketEvents.actionError, toActionError(SocketEvents.roundSubmitWord, result.code));
      }
    });

    socket.on(SocketEvents.roundSubmitConundrumGuess, (payload: { guess: string }) => {
      if (!playerId) {
        socket.emit(SocketEvents.actionError, toActionError(SocketEvents.roundSubmitConundrumGuess, "UNKNOWN_PLAYER"));
        return;
      }

      const result = service.submitConundrumGuess(playerId, payload.guess ?? "");
      if (!result.ok && result.code) {
        socket.emit(SocketEvents.actionError, toActionError(SocketEvents.roundSubmitConundrumGuess, result.code));
      }
    });

    socket.on("disconnect", (reason) => {
      if (playerId) {
        presenceStore.markOffline(playerId);
      }
      service.disconnectSocket(socket.id);
      logger.info({ socketId: socket.id, playerId, reason }, "Client disconnected");
    });
  });

  return { io, matchService: service };
};
