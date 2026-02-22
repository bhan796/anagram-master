import { useEffect, useMemo, useState, type ReactElement } from "react";
import { HomeScreen } from "./screens/HomeScreen";
import { PracticeMenuScreen } from "./screens/PracticeMenuScreen";
import { LettersPracticeScreen } from "./screens/LettersPracticeScreen";
import { ConundrumPracticeScreen } from "./screens/ConundrumPracticeScreen";
import { MatchmakingScreen } from "./screens/MatchmakingScreen";
import { OnlineMatchScreen } from "./screens/OnlineMatchScreen";
import { ProfileScreen } from "./screens/ProfileScreen";
import { SettingsScreen } from "./screens/SettingsScreen";
import { HowToPlayScreen } from "./screens/HowToPlayScreen";
import { MatchFoundScreen } from "./screens/MatchFoundScreen";
import { AuthScreen } from "./screens/AuthScreen";
import { loadConundrums, loadDictionary } from "./logic/loaders";
import { useOnlineMatch } from "./hooks/useOnlineMatch";
import * as SoundManager from "./sound/SoundManager";

import type { ConundrumEntry } from "./logic/gameRules";

type Route =
  | "home"
  | "auth"
  | "practice"
  | "practice_letters"
  | "practice_conundrum"
  | "online_matchmaking"
  | "online_match_found"
  | "online_match"
  | "profile"
  | "settings"
  | "how_to_play";

interface SettingsState {
  timerEnabled: boolean;
  soundEnabled: boolean;
  vibrationEnabled: boolean;
  masterMuted: boolean;
  sfxVolume: number;
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

interface AuthSessionPair {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  refreshExpiresInSeconds: number;
}

interface AuthState {
  status: "guest" | "authenticated";
  userId: string | null;
  email: string | null;
  loading: boolean;
  error: string | null;
}

const SETTINGS_KEY = "anagram.web.settings";
const ACCESS_TOKEN_KEY = "anagram.auth.accessToken";
const REFRESH_TOKEN_KEY = "anagram.auth.refreshToken";
const AUTH_USER_ID_KEY = "anagram.auth.userId";
const AUTH_EMAIL_KEY = "anagram.auth.email";
const PLAYER_ID_KEY = "anagram.playerId";
const DISPLAY_NAME_KEY = "anagram.displayName";
const MATCH_ID_KEY = "anagram.matchId";

const parseStoredSettings = (): SettingsState => {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY);
    if (!raw) {
      return {
        timerEnabled: true,
        soundEnabled: true,
        vibrationEnabled: true,
        masterMuted: false,
        sfxVolume: 0.85
      };
    }
    const parsed = JSON.parse(raw) as Partial<SettingsState>;
    return {
      timerEnabled: parsed.timerEnabled ?? true,
      soundEnabled: parsed.soundEnabled ?? true,
      vibrationEnabled: parsed.vibrationEnabled ?? true,
      masterMuted: parsed.masterMuted ?? false,
      sfxVolume: parsed.sfxVolume ?? 0.85
    };
  } catch {
    return {
      timerEnabled: true,
      soundEnabled: true,
      vibrationEnabled: true,
      masterMuted: false,
      sfxVolume: 0.85
    };
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
  const [homeIntroSeen, setHomeIntroSeen] = useState(false);
  const [auth, setAuth] = useState<AuthState>(() => ({
    status: localStorage.getItem(ACCESS_TOKEN_KEY) ? "authenticated" : "guest",
    userId: localStorage.getItem(AUTH_USER_ID_KEY),
    email: localStorage.getItem(AUTH_EMAIL_KEY),
    loading: false,
    error: null
  }));

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

  const storeAuthSession = (session: AuthSessionPair, userId: string, email: string) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, session.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, session.refreshToken);
    localStorage.setItem(AUTH_USER_ID_KEY, userId);
    localStorage.setItem(AUTH_EMAIL_KEY, email);
    setAuth({ status: "authenticated", userId, email, loading: false, error: null });
  };

  const clearAuthSession = () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(AUTH_USER_ID_KEY);
    localStorage.removeItem(AUTH_EMAIL_KEY);
    localStorage.removeItem(PLAYER_ID_KEY);
    localStorage.removeItem(DISPLAY_NAME_KEY);
    localStorage.removeItem(MATCH_ID_KEY);
    setStats(null);
    setHistory([]);
    setProfileError(null);
    setLeaderboard([]);
    setAuth({ status: "guest", userId: null, email: null, loading: false, error: null });
  };

  const refreshAuthSession = async (): Promise<string | null> => {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    if (!refreshToken) {
      clearAuthSession();
      return null;
    }

    try {
      const refreshRes = await fetch(`${apiBaseUrl}/api/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken })
      });
      if (!refreshRes.ok) {
        clearAuthSession();
        return null;
      }

      const payload = (await refreshRes.json()) as { session: AuthSessionPair };
      localStorage.setItem(ACCESS_TOKEN_KEY, payload.session.accessToken);
      localStorage.setItem(REFRESH_TOKEN_KEY, payload.session.refreshToken);
      return payload.session.accessToken;
    } catch {
      clearAuthSession();
      return null;
    }
  };

  const fetchWithAuth = async (path: string, init: RequestInit = {}): Promise<Response> => {
    const attach = (token: string): RequestInit => ({
      ...init,
      headers: {
        ...(init.headers ?? {}),
        Authorization: `Bearer ${token}`
      }
    });

    let accessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
    if (!accessToken) {
      accessToken = await refreshAuthSession();
      if (!accessToken) throw new Error("Authentication required.");
    }

    let response = await fetch(`${apiBaseUrl}${path}`, attach(accessToken));
    if (response.status !== 401) return response;

    const refreshed = await refreshAuthSession();
    if (!refreshed) return response;
    response = await fetch(`${apiBaseUrl}${path}`, attach(refreshed));
    return response;
  };

  useEffect(() => {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
    SoundManager.setSoundEnabled(settings.soundEnabled);
    SoundManager.setMasterMuted(settings.masterMuted);
    SoundManager.setSfxVolume(settings.sfxVolume);
  }, [settings]);

  useEffect(() => {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    if (!refreshToken) {
      clearAuthSession();
      return;
    }

    let cancelled = false;
    const bootstrap = async () => {
      try {
        const meRes = await fetchWithAuth("/api/auth/me");
        if (meRes.ok) {
          const me = (await meRes.json()) as { userId: string; email: string };
          if (!cancelled) {
            setAuth({ status: "authenticated", userId: me.userId, email: me.email, loading: false, error: null });
          }
          return;
        }
      } catch {
        if (!cancelled) clearAuthSession();
      }
    };

    void bootstrap();
    return () => {
      cancelled = true;
    };
  }, []);

  const renderWithMute = (screen: ReactElement) => (
    <>
      {screen}
      <button
        type="button"
        className={`arcade-mute-button ${settings.masterMuted ? "muted" : ""}`.trim()}
        onClick={() => setSettings((previous) => ({ ...previous, masterMuted: !previous.masterMuted }))}
        aria-label={settings.masterMuted ? "Unmute audio" : "Mute audio"}
        title={settings.masterMuted ? "Unmute" : "Mute"}
      >
        <span className="arcade-mute-icon" aria-hidden>
          {settings.masterMuted ? "MUTE" : "SOUND"}
        </span>
      </button>
    </>
  );

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
          auth.status === "authenticated"
            ? fetchWithAuth(`/api/profiles/${online.state.playerId}/stats`)
            : fetch(`${apiBaseUrl}/api/profiles/${online.state.playerId}/stats`),
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
    [auth.status, online.state.playerId]
  );

  const updateDisplayName = useMemo(
    () => async (displayName: string) => {
      if (!online.state.playerId) {
        throw new Error("Play online once to create a player profile.");
      }

      const response = await fetch(`${apiBaseUrl}/api/profiles/${online.state.playerId}/display-name`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ displayName })
      });

      if (!response.ok) {
        const payload = (await response.json().catch(() => ({}))) as { message?: string };
        throw new Error(payload.message ?? "Unable to update username.");
      }

      online.actions.refreshSession();
      await loadProfile();
    },
    [online.actions, online.state.playerId, loadProfile]
  );

  const authenticate = useMemo(
    () =>
      async (mode: "login" | "register", email: string, password: string) => {
        setAuth((previous) => ({ ...previous, loading: true, error: null }));
        try {
          const endpoint = mode === "login" ? "login" : "register";
          const response = await fetch(`${apiBaseUrl}/api/auth/${endpoint}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
              email,
              password,
              playerId: online.state.playerId ?? undefined
            })
          });
          if (!response.ok) {
            const payload = (await response.json().catch(() => ({}))) as { message?: string };
            throw new Error(payload.message ?? "Authentication failed.");
          }
          const payload = (await response.json()) as { userId: string; email: string; session: AuthSessionPair };
          storeAuthSession(payload.session, payload.userId, payload.email);
          online.actions.refreshSession();
          setRoute("online_matchmaking");
        } catch (error) {
          setAuth((previous) => ({
            ...previous,
            loading: false,
            error: error instanceof Error ? error.message : "Authentication failed."
          }));
        }
      },
    [online.actions, online.state.playerId]
  );

  const logout = useMemo(
    () => async () => {
      const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
      if (refreshToken) {
        await fetch(`${apiBaseUrl}/api/auth/logout`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken })
        }).catch(() => undefined);
      }
      clearAuthSession();
      online.actions.resetIdentity();
      setRoute("home");
    },
    [online.actions]
  );

  useEffect(() => {
    if (auth.status === "authenticated") return;
    setStats(null);
    setHistory([]);
    setProfileError(null);
    setLeaderboard([]);
  }, [auth.status]);

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
    if (auth.status !== "authenticated") {
      setLeaderboard([]);
      return;
    }
    let cancelled = false;
    const pullLeaderboard = async () => {
      try {
        const response = await fetchWithAuth("/api/leaderboard?limit=20");
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
  }, [auth.status]);

  useEffect(() => {
    if (route === "profile") {
      void loadProfile();
    }
  }, [route, loadProfile]);

  useEffect(() => {
    const hasActiveMatch = Boolean(online.state.matchState && online.state.matchState.phase !== "finished");
    if (!hasActiveMatch) return;
    if (route === "online_match" || route === "online_match_found") return;
    setRoute("online_match_found");
  }, [online.state.matchState, route]);

  if (route === "home") {
    return renderWithMute(
      <HomeScreen
        onPlayOnline={() => {
          online.actions.clearFinishedMatch();
          setRoute("online_matchmaking");
        }}
        onPracticeMode={() => setRoute("practice")}
        onProfile={() => setRoute("profile")}
        onSettings={() => setRoute("settings")}
        onHowToPlay={() => setRoute("how_to_play")}
        isAuthenticated={auth.status === "authenticated"}
        authEmail={auth.email}
        onAuthAction={() => {
          if (auth.status === "authenticated") {
            void logout();
            return;
          }
          setRoute("auth");
        }}
        playersOnline={playersOnline}
        playIntro={!homeIntroSeen}
        onIntroComplete={() => setHomeIntroSeen(true)}
      />
    );
  }

  if (route === "auth") {
    return renderWithMute(
      <AuthScreen
        isSubmitting={auth.loading}
        error={auth.error}
        onBack={() => setRoute("online_matchmaking")}
        onLogin={async (email, password) => authenticate("login", email, password)}
        onRegister={async (email, password) => authenticate("register", email, password)}
        onContinueGuest={() => {
          clearAuthSession();
          online.actions.resetIdentity();
          setRoute("online_matchmaking");
        }}
      />
    );
  }

  if (route === "how_to_play") {
    return renderWithMute(<HowToPlayScreen onBack={() => setRoute("home")} />);
  }

  if (route === "practice") {
    return renderWithMute(
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
    return renderWithMute(
      <LettersPracticeScreen
        timerEnabled={settings.timerEnabled}
        dictionary={dictionary}
        dictionaryError={dictionaryError}
        onBack={() => setRoute("practice")}
      />
    );
  }

  if (route === "practice_conundrum") {
    return renderWithMute(
      <ConundrumPracticeScreen
        timerEnabled={settings.timerEnabled}
        conundrums={conundrums}
        conundrumError={conundrumError}
        onBack={() => setRoute("practice")}
      />
    );
  }

  if (route === "online_matchmaking") {
    return renderWithMute(
      <MatchmakingScreen
        state={online.state}
        leaderboard={leaderboard}
        isAuthenticated={auth.status === "authenticated"}
        onRequireAuth={() => setRoute("auth")}
        onBack={() => setRoute("home")}
        onJoinQueue={online.actions.startQueue}
        onCancelQueue={online.actions.cancelQueue}
        onRetryConnection={online.actions.retryConnect}
        onMatchReady={() => setRoute("online_match_found")}
      />
    );
  }

  if (route === "online_match_found") {
    return renderWithMute(<MatchFoundScreen state={online.state} onDone={() => setRoute("online_match")} />);
  }

  if (route === "online_match") {
    return renderWithMute(
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
        onPlayAgain={() => {
          const mode = online.state.matchState?.mode ?? "casual";
          online.actions.clearFinishedMatch();
          setRoute("online_matchmaking");
          online.actions.startQueue(mode);
        }}
        onBackToHome={() => {
          online.actions.clearFinishedMatch();
          setRoute("home");
        }}
      />
    );
  }

  if (route === "profile") {
    return renderWithMute(
      <ProfileScreen
        isLoading={profileLoading}
        error={profileError}
        stats={stats}
        history={history}
        onBack={() => setRoute("home")}
        onRetry={() => void loadProfile()}
        onUpdateDisplayName={updateDisplayName}
        isAuthenticated={auth.status === "authenticated"}
      />
    );
  }

  return renderWithMute(
    <SettingsScreen
      timerEnabled={settings.timerEnabled}
      masterMuted={settings.masterMuted}
      sfxVolume={settings.sfxVolume}
      onBack={() => setRoute("home")}
      onTimerToggle={(value) => setSettings((previous) => ({ ...previous, timerEnabled: value }))}
      onMasterMuteToggle={(value) => setSettings((previous) => ({ ...previous, masterMuted: value }))}
      onSfxVolumeChange={(value) => setSettings((previous) => ({ ...previous, sfxVolume: value }))}
    />
  );
};

