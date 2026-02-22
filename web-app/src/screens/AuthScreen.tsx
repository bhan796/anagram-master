import { useState } from "react";
import { ArcadeBackButton, ArcadeButton, ArcadeScaffold, NeonTitle } from "../components/ArcadeComponents";

interface AuthScreenProps {
  isSubmitting: boolean;
  error: string | null;
  onBack: () => void;
  onLogin: (email: string, password: string) => Promise<void>;
  onRegister: (email: string, password: string) => Promise<void>;
  onContinueGuest: () => void;
  onGoogleAuth: () => Promise<void>;
  onFacebookAuth: () => Promise<void>;
  googleEnabled: boolean;
  facebookEnabled: boolean;
}

export const AuthScreen = ({
  isSubmitting,
  error,
  onBack,
  onLogin,
  onRegister,
  onContinueGuest,
  onGoogleAuth,
  onFacebookAuth,
  googleEnabled,
  facebookEnabled
}: AuthScreenProps) => {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const submit = async () => {
    if (!email.trim() || !password.trim()) return;
    if (mode === "login") {
      await onLogin(email.trim(), password);
    } else {
      await onRegister(email.trim(), password);
    }
  };

  return (
    <ArcadeScaffold className="accent-gold">
      <ArcadeBackButton onClick={onBack} />
      <NeonTitle text={mode === "login" ? "Sign In" : "Create Account"} />

      <div className="card" style={{ display: "grid", gap: 12 }}>
        <div className="mode-select-row">
          <button
            type="button"
            className={`mode-select-btn ${mode === "login" ? "selected" : "unselected"}`}
            onClick={() => setMode("login")}
            disabled={isSubmitting}
          >
            Sign In
          </button>
          <button
            type="button"
            className={`mode-select-btn ${mode === "register" ? "selected" : "unselected"}`}
            onClick={() => setMode("register")}
            disabled={isSubmitting}
          >
            Create
          </button>
        </div>

        <input className="input" value={email} onChange={(event) => setEmail(event.currentTarget.value)} placeholder="Email" />
        <input
          className="input"
          value={password}
          onChange={(event) => setPassword(event.currentTarget.value)}
          placeholder="Password"
          type="password"
        />
        {error ? <div className="text-dim" style={{ color: "var(--red)" }}>{error}</div> : null}

        <ArcadeButton
          text={isSubmitting ? "Please wait..." : mode === "login" ? "Sign In" : "Create Account"}
          onClick={() => void submit()}
          disabled={isSubmitting}
        />
        <ArcadeButton
          text="Continue with Google"
          onClick={() => void onGoogleAuth()}
          disabled={isSubmitting || !googleEnabled}
          accent="green"
        />
        <ArcadeButton
          text="Continue with Facebook"
          onClick={() => void onFacebookAuth()}
          disabled={isSubmitting || !facebookEnabled}
          accent="magenta"
        />
        <ArcadeButton text="Continue as Guest" onClick={onContinueGuest} disabled={isSubmitting} />
      </div>
    </ArcadeScaffold>
  );
};
