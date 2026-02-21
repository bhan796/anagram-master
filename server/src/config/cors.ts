const escapeRegex = (value: string): string => value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

const tokenToRegex = (token: string): RegExp => {
  const parts = token.split("*").map((part) => escapeRegex(part));
  return new RegExp(`^${parts.join(".*")}$`, "i");
};

export const createOriginChecker = (rawOrigins: string) => {
  const tokens = rawOrigins
    .split(",")
    .map((token) => token.trim())
    .filter((token) => token.length > 0);

  const allowAll = tokens.includes("*");
  const regexTokens = tokens
    .filter((token) => token.includes("*") && token !== "*")
    .map((token) => tokenToRegex(token));
  const exactTokens = new Set(tokens.filter((token) => !token.includes("*")));

  return (origin: string | undefined): boolean => {
    if (!origin) return true;
    if (allowAll) return true;
    if (exactTokens.has(origin)) return true;
    return regexTokens.some((regex) => regex.test(origin));
  };
};

