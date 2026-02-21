import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { io, type Socket } from "socket.io-client";
import {
  SocketEventNames,
  initialOnlineUiState,
  type ActionErrorPayload,
  type MatchStatePayload,
  type MatchmakingStatusPayload,
  type OnlineUiState,
  type SessionIdentifyPayload
} from "../types/online";

const PLAYER_ID_KEY = "anagram.playerId";
const DISPLAY_NAME_KEY = "anagram.displayName";
const MATCH_ID_KEY = "anagram.matchId";

const connectionUrl = (import.meta.env.VITE_SERVER_URL as string | undefined)?.trim() || "http://localhost:4000";

const isActiveMatch = (match: MatchStatePayload | null | undefined): boolean =>
  Boolean(match && match.phase !== "finished");

const computeRemainingSeconds = (match: MatchStatePayload | null, offsetMs: number): number => {
  if (!match?.phaseEndsAtMs) return 0;
  const now = Date.now() + offsetMs;
  return Math.max(0, Math.floor((match.phaseEndsAtMs - now) / 1000));
};

const reduceOnlineState = (
  previous: OnlineUiState,
  {
    session,
    matchmaking,
    match,
    actionError,
    connectionState,
    connectionError,
    clockOffsetMs
  }: {
    session?: SessionIdentifyPayload | null;
    matchmaking?: MatchmakingStatusPayload | null;
    match?: MatchStatePayload | null;
    actionError?: ActionErrorPayload | null;
    connectionState?: OnlineUiState["connectionState"];
    connectionError?: string | null;
    clockOffsetMs: number;
  }
): OnlineUiState => {
  const updatedMatch = match ?? previous.matchState;
  const resolvedPlayerId = session?.playerId ?? previous.playerId;
  const me = updatedMatch?.players.find((player) => player.playerId === resolvedPlayerId) ?? null;
  const opponent = updatedMatch?.players.find((player) => player.playerId !== resolvedPlayerId) ?? null;
  const isInMatchmaking = (matchmaking?.state ?? previous.queueState) === "searching";

  const statusMessage = (() => {
    if (connectionState === "reconnecting") return "Reconnecting...";
    if (connectionState === "failed") return "Connection failed. Retry connection.";
    if (actionError && !updatedMatch) return actionError.message;
    if (!updatedMatch && isInMatchmaking) return "Finding opponent...";
    if (!updatedMatch) return previous.statusMessage;

    switch (updatedMatch.phase) {
      case "awaiting_letters_pick":
        return "Letter picking in progress";
      case "letters_solving":
        return "Submit your best word before time expires";
      case "conundrum_solving":
        return "Solve the conundrum";
      case "round_result":
        return "Round result";
      case "finished":
        return "Match finished";
      default:
        return "";
    }
  })();

  return {
    ...previous,
    connectionState: connectionState ?? previous.connectionState,
    connectionError: connectionError ?? previous.connectionError,
    playerId: resolvedPlayerId,
    displayName: session?.displayName ?? previous.displayName,
    queueState: matchmaking?.state ?? previous.queueState,
    queueSize: matchmaking?.queueSize ?? previous.queueSize,
    isInMatchmaking,
    matchState: updatedMatch,
    matchId: updatedMatch?.matchId ?? previous.matchId,
    myPlayer: me,
    opponentPlayer: opponent,
    isMyTurnToPick: updatedMatch?.phase === "awaiting_letters_pick" && updatedMatch.pickerPlayerId === resolvedPlayerId,
    secondsRemaining: computeRemainingSeconds(updatedMatch, clockOffsetMs),
    statusMessage,
    lastError: updatedMatch ? null : actionError ?? previous.lastError
  };
};

export const useOnlineMatch = () => {
  const [state, setState] = useState<OnlineUiState>(initialOnlineUiState);
  const [clockOffsetMs, setClockOffsetMs] = useState(0);
  const socketRef = useRef<Socket | null>(null);

  useEffect(() => {
    const socket = io(connectionUrl, {
      reconnection: true,
      reconnectionAttempts: Infinity,
      reconnectionDelay: 500,
      reconnectionDelayMax: 5000,
      timeout: 10000
    });

    socketRef.current = socket;

    socket.on("connect", () => {
      setState((previous) => reduceOnlineState(previous, { connectionState: "connected", connectionError: null, clockOffsetMs }));

      const playerId = localStorage.getItem(PLAYER_ID_KEY);
      const displayName = localStorage.getItem(DISPLAY_NAME_KEY);
      const identifyPayload: Record<string, string> = {};
      if (playerId) identifyPayload.playerId = playerId;
      if (displayName) identifyPayload.displayName = displayName;
      socket.emit(SocketEventNames.SESSION_IDENTIFY, identifyPayload);

      const lastMatch = localStorage.getItem(MATCH_ID_KEY);
      if (lastMatch) {
        socket.emit(SocketEventNames.MATCH_RESUME, { matchId: lastMatch });
      }
    });

    socket.on("disconnect", () => {
      setState((previous) => reduceOnlineState(previous, { connectionState: "disconnected", clockOffsetMs }));
    });

    socket.io.on("reconnect_attempt", () => {
      setState((previous) => reduceOnlineState(previous, { connectionState: "reconnecting", clockOffsetMs }));
    });

    socket.on("connect_error", (reason: Error) => {
      setState((previous) =>
        reduceOnlineState(previous, {
          connectionState: "failed",
          connectionError: reason.message,
          clockOffsetMs
        })
      );
    });

    socket.on(SocketEventNames.SESSION_IDENTIFY, (payload: SessionIdentifyPayload) => {
      localStorage.setItem(PLAYER_ID_KEY, payload.playerId);
      localStorage.setItem(DISPLAY_NAME_KEY, payload.displayName);
      setClockOffsetMs(payload.serverNowMs - Date.now());
      setState((previous) => reduceOnlineState(previous, { session: payload, clockOffsetMs }));
    });

    socket.on(SocketEventNames.MATCHMAKING_STATUS, (payload: MatchmakingStatusPayload) => {
      setState((previous) => reduceOnlineState(previous, { matchmaking: payload, clockOffsetMs }));
    });

    socket.on(SocketEventNames.MATCH_FOUND, (payload: { matchId: string; serverNowMs: number }) => {
      localStorage.setItem(MATCH_ID_KEY, payload.matchId);
      setClockOffsetMs(payload.serverNowMs - Date.now());
      setState((previous) => ({ ...previous, matchId: payload.matchId }));
    });

    socket.on(SocketEventNames.MATCH_STATE, (payload: MatchStatePayload) => {
      const offset = payload.serverNowMs - Date.now();
      setClockOffsetMs((previous) => Math.round(previous * 0.7 + offset * 0.3));
      if (payload.phase === "finished") {
        localStorage.removeItem(MATCH_ID_KEY);
      } else {
        localStorage.setItem(MATCH_ID_KEY, payload.matchId);
      }

      setState((previous) => {
        const resetWord =
          previous.matchState?.roundNumber !== payload.roundNumber || previous.matchState?.phase !== payload.phase;
        const reduced = reduceOnlineState(previous, { match: payload, clockOffsetMs });
        return {
          ...reduced,
          wordInput: resetWord ? "" : previous.wordInput,
          conundrumGuessInput: resetWord ? "" : previous.conundrumGuessInput,
          hasSubmittedWord: resetWord ? false : previous.hasSubmittedWord,
          localValidationMessage: resetWord ? null : previous.localValidationMessage
        };
      });
    });

    socket.on(SocketEventNames.ACTION_ERROR, (payload: ActionErrorPayload) => {
      setState((previous) => reduceOnlineState(previous, { actionError: payload, clockOffsetMs }));
    });

    return () => {
      socket.disconnect();
    };
  }, []);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setState((previous) => ({ ...previous, secondsRemaining: computeRemainingSeconds(previous.matchState, clockOffsetMs) }));
    }, 1000);

    return () => window.clearInterval(timer);
  }, [clockOffsetMs]);

  const clearError = useCallback(() => {
    setState((previous) => ({ ...previous, lastError: null, localValidationMessage: null }));
  }, []);

  const setWordInput = useCallback((value: string) => {
    setState((previous) => ({ ...previous, wordInput: value }));
  }, []);

  const setConundrumGuessInput = useCallback((value: string) => {
    setState((previous) => ({ ...previous, conundrumGuessInput: value }));
  }, []);

  const startQueue = useCallback((displayName?: string) => {
    const socket = socketRef.current;
    if (!socket) return;
    if (isActiveMatch(state.matchState)) {
      setState((previous) => ({ ...previous, localValidationMessage: "You are already in an active match." }));
      return;
    }

    const identifyPayload: Record<string, string> = {};
    if (state.playerId) identifyPayload.playerId = state.playerId;
    if (displayName?.trim()) {
      identifyPayload.displayName = displayName.trim();
      localStorage.setItem(DISPLAY_NAME_KEY, displayName.trim());
    }
    socket.emit(SocketEventNames.SESSION_IDENTIFY, identifyPayload);
    socket.emit(SocketEventNames.QUEUE_JOIN);
  }, [state.matchState, state.playerId]);

  const cancelQueue = useCallback(() => {
    socketRef.current?.emit(SocketEventNames.QUEUE_LEAVE);
  }, []);

  const retryConnect = useCallback(() => {
    socketRef.current?.connect();
  }, []);

  const pickVowel = useCallback(() => {
    socketRef.current?.emit(SocketEventNames.ROUND_PICK_LETTER, { kind: "vowel" });
  }, []);

  const pickConsonant = useCallback(() => {
    socketRef.current?.emit(SocketEventNames.ROUND_PICK_LETTER, { kind: "consonant" });
  }, []);

  const submitWord = useCallback(() => {
    if (!state.wordInput.trim()) {
      setState((previous) => ({ ...previous, localValidationMessage: "Enter a word before submitting." }));
      return;
    }
    socketRef.current?.emit(SocketEventNames.ROUND_SUBMIT_WORD, { word: state.wordInput });
    setState((previous) => ({ ...previous, hasSubmittedWord: true, localValidationMessage: null }));
  }, [state.wordInput]);

  const submitConundrumGuess = useCallback(() => {
    if (!state.conundrumGuessInput.trim()) {
      setState((previous) => ({ ...previous, localValidationMessage: "Enter a guess before submitting." }));
      return;
    }
    socketRef.current?.emit(SocketEventNames.ROUND_SUBMIT_CONUNDRUM_GUESS, { guess: state.conundrumGuessInput });
    setState((previous) => ({ ...previous, localValidationMessage: null }));
  }, [state.conundrumGuessInput]);

  const forfeitMatch = useCallback(() => {
    socketRef.current?.emit(SocketEventNames.MATCH_FORFEIT);
    localStorage.removeItem(MATCH_ID_KEY);
    setState((previous) => ({
      ...previous,
      matchId: null,
      matchState: null,
      myPlayer: null,
      opponentPlayer: null,
      isMyTurnToPick: false,
      secondsRemaining: 0,
      hasSubmittedWord: false,
      wordInput: "",
      conundrumGuessInput: "",
      localValidationMessage: null,
      lastError: null
    }));
  }, []);

  const value = useMemo(
    () => ({
      state,
      actions: {
        startQueue,
        cancelQueue,
        retryConnect,
        pickVowel,
        pickConsonant,
        setWordInput,
        submitWord,
        setConundrumGuessInput,
        submitConundrumGuess,
        clearError,
        forfeitMatch
      }
    }),
    [
      state,
      startQueue,
      cancelQueue,
      retryConnect,
      pickVowel,
      pickConsonant,
      setWordInput,
      submitWord,
      setConundrumGuessInput,
      submitConundrumGuess,
      clearError,
      forfeitMatch
    ]
  );

  return value;
};
