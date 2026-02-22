import { OAuth2Client } from "google-auth-library";
import { loadEnv } from "../config/env.js";

const env = loadEnv();

type OauthProvider = "google" | "facebook";

export interface OauthIdentity {
  provider: OauthProvider;
  providerUserId: string;
  email: string | null;
}

const googleAudience = env.GOOGLE_OAUTH_CLIENT_IDS.split(",")
  .map((entry) => entry.trim())
  .filter((entry) => entry.length > 0);

const googleClient = new OAuth2Client();

const verifyGoogle = async (idToken: string): Promise<OauthIdentity> => {
  if (googleAudience.length === 0) {
    throw new Error("Google OAuth is not configured.");
  }

  try {
    const ticket = await googleClient.verifyIdToken({
      idToken,
      audience: googleAudience
    });
    const payload = ticket.getPayload();
    if (!payload?.sub) {
      throw new Error("Invalid Google token.");
    }

    return {
      provider: "google",
      providerUserId: payload.sub,
      email: payload.email ?? null
    };
  } catch {
    const tokenInfo = await fetch(
      `https://oauth2.googleapis.com/tokeninfo?access_token=${encodeURIComponent(idToken)}`
    );
    if (!tokenInfo.ok) {
      throw new Error("Invalid Google token.");
    }
    const payload = (await tokenInfo.json()) as { sub?: string; email?: string; aud?: string };
    if (!payload.sub) {
      throw new Error("Invalid Google token.");
    }
    if (payload.aud && !googleAudience.includes(payload.aud)) {
      throw new Error("Google token audience is invalid.");
    }

    return {
      provider: "google",
      providerUserId: payload.sub,
      email: payload.email ?? null
    };
  }
};

const verifyFacebook = async (accessToken: string): Promise<OauthIdentity> => {
  if (!env.FACEBOOK_APP_ID || !env.FACEBOOK_APP_SECRET) {
    throw new Error("Facebook OAuth is not configured.");
  }

  const appToken = `${env.FACEBOOK_APP_ID}|${env.FACEBOOK_APP_SECRET}`;
  const debugRes = await fetch(
    `https://graph.facebook.com/debug_token?input_token=${encodeURIComponent(accessToken)}&access_token=${encodeURIComponent(appToken)}`
  );
  if (!debugRes.ok) {
    throw new Error("Unable to validate Facebook token.");
  }

  const debugPayload = (await debugRes.json()) as {
    data?: { is_valid?: boolean; app_id?: string; user_id?: string };
  };
  const data = debugPayload.data;
  if (!data?.is_valid || !data.user_id || data.app_id !== env.FACEBOOK_APP_ID) {
    throw new Error("Invalid Facebook token.");
  }

  const profileRes = await fetch(
    `https://graph.facebook.com/me?fields=id,name,email&access_token=${encodeURIComponent(accessToken)}`
  );
  if (!profileRes.ok) {
    throw new Error("Unable to fetch Facebook profile.");
  }
  const profile = (await profileRes.json()) as { id?: string; email?: string | null };
  if (!profile.id) {
    throw new Error("Invalid Facebook profile.");
  }

  return {
    provider: "facebook",
    providerUserId: profile.id,
    email: profile.email ?? null
  };
};

export const verifyOauthToken = async (provider: OauthProvider, token: string): Promise<OauthIdentity> => {
  if (!token.trim()) {
    throw new Error("OAuth token is required.");
  }

  if (provider === "google") {
    return verifyGoogle(token);
  }

  return verifyFacebook(token);
};
