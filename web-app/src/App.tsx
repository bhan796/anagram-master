import { useEffect, useMemo, useState } from "react";
import { HomeScreen } from "./screens/HomeScreen";
import { PracticeMenuScreen } from "./screens/PracticeMenuScreen";
import { LettersPracticeScreen } from "./screens/LettersPracticeScreen";
import { ConundrumPracticeScreen } from "./screens/ConundrumPracticeScreen";
import { MatchmakingScreen } from "./screens/MatchmakingScreen";
import { OnlineMatchScreen } from "./screens/OnlineMatchScreen";
import { ProfileScreen } from "./screens/ProfileScreen";
import { SettingsScreen } from "./screens/SettingsScreen";
import { loadConundrums, loadDictionary } from "./logic/loaders";
import { useOnlineMatch } from "./hooks/useOnlineMatch";

import type { ConundrumEntry } from "./logic/gameRules";

type Route =
  | "home"
  | "practice"
  | "practice_letters"
  | "practice_conundrum"
  | "online_matchmaking"
  | "online_match"
  | "profile"
  | "settings";

interface SettingsState {
  timerEnabled: boolean;
  soundEnabled: boolean;
  vibrationEnabled: boolean;
}

interface StatsSummary {
  playerId: string;
  displayName: string;
  matchesPlayed: number;
  wins: number;
  losses: number;
  draws: number;
  totalScore: number;
  rating: number;
  peakRating: number;
  rankTier: string;
  rankedGames: number;
  rankedWins: number;
  rankedLosses: number;
  rankedDraws: number;
}

interface MatchHistoryItem {
  matchId: string;
  createdAtMs: number;
  finishedAtMs: number;
  mode: "casual" | "ranked";
  winnerPlayerId: string | null;
  players: {
    playerId: string;
    displayName: string;
    score: number;
    ratingBefore: number;
    ratingAfter: number;
    ratingDelta: number;
    rankTier: string;
  }[];
  ratingChanges?: Record<string, number>;
}

const SETTINGS_KEY = "anagram.web.settings";

const parseStoredSettings = (): SettingsState => {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY);
    if (!raw) {
      return { timerEnabled: true, soundEnabled: true, vibrationEnabled: true };
    }
    const parsed = JSON.parse(raw) as Partial<SettingsState>;
    return {
      timerEnabled: parsed.timerEnabled ?? true,
      soundEnabled: parsed.soundEnabled ?? true,
      vibrationEnabled: parsed.vibrationEnabled ?? true
    };
  } catch {
    return { timerEnabled: true, soundEnabled: true, vibrationEnabled: true };
  }
};

const normalizeBackendUrl = (raw: string | undefined): string => {
  const candidate = (raw ?? "").trim();
  if (!candidate) return "http://localhost:4000";
  if (/^https?:\/\//i.test(candidate)) return candidate.replace(/\/+$/, "");
  return `https://${candidate.replace(/\/+$/, "")}`;
};

const apiBaseUrl = normalizeBackendUrl(import.meta.env.VITE_SERVER_URL as string | undefined);

export const App = () => {
  const [route, setRoute] = useState<Route>("home");
  const [settings, setSettings] = useState<SettingsState>(() => parseStoredSettings());

  const [dictionary, setDictionary] = useState<Set<string> | null>(null);
  const [dictionaryError, setDictionaryError] = useState<string | null>(null);
  const [conundrums, setConundrums] = useState<ConundrumEntry[]>([]);
  const [conundrumError, setConundrumError] = useState<string | null>(null);

  const [stats, setStats] = useState<StatsSummary | null>(null);
  const [history, setHistory] = useState<MatchHistoryItem[]>([]);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [leaderboard, setLeaderboard] = useState<
    Array<{
      playerId: string;
      displayName: string;
      rating: number;
      rankTier: string;
      rankedGames: number;
      wins: number;
      losses: number;
      draws: number;
    }>
  >([]);
  const [playersOnline, setPlayersOnline] = useState(0);

  const online = useOnlineMatch();

  useEffect(() => {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
  }, [settings]);

  useEffect(() => {
    loadDictionary()
      .then(setDictionary)
      .catch((error: Error) => setDictionaryError(error.message));

    loadConundrums()
      .then(setConundrums)
      .catch((error: Error) => setConundrumError(error.message));
  }, []);

  const loadProfile = useMemo(
    () => async () => {
      if (!online.state.playerId) {
        setProfileError("Play online once to create a player profile.");
        return;
      }

      setProfileLoading(true);
      setProfileError(null);

      try {
        const [statsRes, historyRes] = await Promise.all([
          fetch(`${apiBaseUrl}/api/profiles/${online.state.playerId}/stats`),
          fetch(`${apiBaseUrl}/api/profiles/${online.state.playerId}/matches`)
        ]);

        if (!statsRes.ok || !historyRes.ok) {
          throw new Error("Failed to load profile stats from server.");
        }

        const statsPayload = (await statsRes.json()) as StatsSummary;
        const historyPayload = (await historyRes.json()) as { matches: MatchHistoryItem[] };
        setStats(statsPayload);
        setHistory(historyPayload.matches ?? []);

      } catch (error) {
        setProfileError(error instanceof Error ? error.message : "Unable to load profile");
      } finally {
        setProfileLoading(false);
      }
    },
    [online.state.playerId]
  );

  useEffect(() => {
    let cancelled = false;
    const pullPresence = async () => {
      try {
        const response = await fetch(`${apiBaseUrl}/api/presence`);
        if (!response.ok) return;
        const payload = (await response.json()) as { playersOnline?: number };
        if (!cancelled) setPlayersOnline(payload.playersOnline ?? 0);
      } catch {
        // ignore presence polling failures
      }
    };

    void pullPresence();
    const timer = window.setInterval(() => void pullPresence(), 10000);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    const pullLeaderboard = async () => {
      try {
        const response = await fetch(`${apiBaseUrl}/api/leaderboard?limit=20`);
        if (!response.ok) return;
        const payload = (await response.json()) as { entries?: typeof leaderboard };
        if (!cancelled) setLeaderboard(payload.entries ?? []);
      } catch {
        // ignore leaderboard polling failures
      }
    };

    void pullLeaderboard();
    const timer = window.setInterval(() => void pullLeaderboard(), 15000);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, []);

  useEffect(() => {
    if (route === "profile") {
      void loadProfile();
    }
  }, [route, loadProfile]);

  if (route === "home") {
    return (
      <HomeScreen
        onPlayOnline={() => {
          online.actions.clearFinishedMatch();
          setRoute("online_matchmaking");
        }}
        onPracticeMode={() => setRoute("practice")}
        onProfile={() => setRoute("profile")}
        onSettings={() => setRoute("settings")}
        playersOnline={playersOnline}
      />
    );
  }

  if (route === "practice") {
    return (
      <PracticeMenuScreen
        timerEnabled={settings.timerEnabled}
        onTimerToggle={(value) => setSettings((previous) => ({ ...previous, timerEnabled: value }))}
        onBack={() => setRoute("home")}
        onPracticeLetters={() => setRoute("practice_letters")}
        onPracticeConundrum={() => setRoute("practice_conundrum")}
      />
    );
  }

  if (route === "practice_letters") {
    return (
      <LettersPracticeScreen
        timerEnabled={settings.timerEnabled}
        dictionary={dictionary}
        dictionaryError={dictionaryError}
        onBack={() => setRoute("practice")}
      />
    );
  }

  if (route === "practice_conundrum") {
    return (
      <ConundrumPracticeScreen
        timerEnabled={settings.timerEnabled}
        conundrums={conundrums}
        conundrumError={conundrumError}
        onBack={() => setRoute("practice")}
      />
    );
  }

  if (route === "online_matchmaking") {
    return (
      <MatchmakingScreen
        state={online.state}
        leaderboard={leaderboard}
        onBack={() => setRoute("home")}
        onJoinQueue={online.actions.startQueue}
        onCancelQueue={online.actions.cancelQueue}
        onRetryConnection={online.actions.retryConnect}
        onMatchReady={() => setRoute("online_match")}
      />
    );
  }

  if (route === "online_match") {
    return (
      <OnlineMatchScreen
        state={online.state}
        onPickVowel={online.actions.pickVowel}
        onPickConsonant={online.actions.pickConsonant}
        onWordChange={online.actions.setWordInput}
        onSubmitWord={online.actions.submitWord}
        onConundrumGuessChange={online.actions.setConundrumGuessInput}
        onSubmitConundrumGuess={online.actions.submitConundrumGuess}
        onDismissError={online.actions.clearError}
        onLeaveGame={online.actions.forfeitMatch}
        onBackToHome={() => {
          online.actions.clearFinishedMatch();
          setRoute("home");
        }}
      />
    );
  }

  if (route === "profile") {
    return (
      <ProfileScreen
        isLoading={profileLoading}
        error={profileError}
        stats={stats}
        history={history}
        onBack={() => setRoute("home")}
        onRetry={() => void loadProfile()}
      />
    );
  }

  return (
    <SettingsScreen
      timerEnabled={settings.timerEnabled}
      soundEnabled={settings.soundEnabled}
      vibrationEnabled={settings.vibrationEnabled}
      onBack={() => setRoute("home")}
      onTimerToggle={(value) => setSettings((previous) => ({ ...previous, timerEnabled: value }))}
      onSoundToggle={(value) => setSettings((previous) => ({ ...previous, soundEnabled: value }))}
      onVibrationToggle={(value) => setSettings((previous) => ({ ...previous, vibrationEnabled: value }))}
    />
  );
};
