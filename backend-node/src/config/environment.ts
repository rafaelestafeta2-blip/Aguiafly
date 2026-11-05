/**
 * Configuração Centralizada de Ambientes (Sandbox / Test Mode vs Live / Produção).
 * 
 * Regra de Seleção Automática baseada em Variáveis de Ambiente (.env):
 * - Se `NODE_ENV=development` OU `USE_TEST_APIS=true`:
 *   O sistema opera em MODO TESTE (Sandbox), selecionando automaticamente as chaves
 *   `_TEST` da Duffel (`duffel_test_...`) e da Stripe (`pk_test_...`, `sk_test_...`).
 * - Caso contrário (`NODE_ENV=production` e `USE_TEST_APIS=false`):
 *   O sistema opera em MODO PRODUÇÃO, selecionando automaticamente as chaves
 *   `_LIVE` da Duffel (`duffel_live_...`) e da Stripe (`pk_live_...`, `sk_live_...`).
 */

function clean(val: string | undefined): string {
  return (val || "").trim();
}

function isValidKey(key: string): boolean {
  const k = clean(key);
  return k.length > 0 && !k.includes("...") && !k.toLowerCase().includes("placeholder");
}

export function isTestMode(): boolean {
  const nodeEnv = clean(process.env.NODE_ENV).toLowerCase();
  const useTest = clean(process.env.USE_TEST_APIS).toLowerCase();
  return useTest === "true" || useTest === "1" || nodeEnv === "development";
}

export function getDuffelToken(): string {
  const inTestMode = isTestMode();
  
  if (inTestMode) {
    // 1. Chaves específicas de teste/sandbox
    const testToken = clean(process.env.DUFFEL_API_TOKEN_TEST || process.env.DUFFEL_TEST_TOKEN);
    if (isValidKey(testToken)) return testToken;

    // 2. Chaves genéricas que comecem por duffel_test_
    const candidate = clean(process.env.DUFFEL_API_TOKEN || process.env.DUFFEL_API_KEY || process.env.DU);
    if (candidate.startsWith("duffel_test_") && isValidKey(candidate)) return candidate;

    // 3. Fallback genérico caso só haja uma chave configurada
    if (isValidKey(candidate)) return candidate;
  } else {
    // 1. Chaves específicas de produção
    const liveToken = clean(process.env.DUFFEL_API_TOKEN_LIVE || process.env.DUFFEL_LIVE_TOKEN);
    if (isValidKey(liveToken)) return liveToken;

    // 2. Chaves genéricas que comecem por duffel_live_
    const candidate = clean(process.env.DUFFEL_API_TOKEN || process.env.DUFFEL_API_KEY || process.env.DU);
    if (candidate.startsWith("duffel_live_") && isValidKey(candidate)) return candidate;

    // 3. Fallback genérico
    if (isValidKey(candidate)) return candidate;
  }

  return clean(process.env.DUFFEL_API_TOKEN || process.env.DUFFEL_API_KEY || process.env.DU);
}

export function isDuffelConfigured(): boolean {
  const token = getDuffelToken();
  return isValidKey(token);
}

export function getStripeSecretKey(): string {
  const inTestMode = isTestMode();

  if (inTestMode) {
    const testSecret = clean(process.env.STRIPE_SECRET_KEY_TEST || process.env.STRIPE_TEST_SECRET_KEY);
    if (isValidKey(testSecret) && testSecret.startsWith("sk_")) return testSecret;

    const candidate = clean(process.env.STRIPE_SECRET_KEY);
    if (candidate.startsWith("sk_test_") && isValidKey(candidate)) return candidate;

    if (isValidKey(candidate) && candidate.startsWith("sk_")) return candidate;
  } else {
    const liveSecret = clean(process.env.STRIPE_SECRET_KEY_LIVE || process.env.STRIPE_LIVE_SECRET_KEY);
    if (isValidKey(liveSecret) && liveSecret.startsWith("sk_")) return liveSecret;

    const candidate = clean(process.env.STRIPE_SECRET_KEY);
    if (candidate.startsWith("sk_live_") && isValidKey(candidate)) return candidate;

    if (isValidKey(candidate) && candidate.startsWith("sk_")) return candidate;
  }

  return clean(process.env.STRIPE_SECRET_KEY);
}

export function isStripeSecretConfigured(): boolean {
  const key = getStripeSecretKey();
  return isValidKey(key) && key.startsWith("sk_");
}

export function getStripePublishableKey(): string {
  const inTestMode = isTestMode();

  if (inTestMode) {
    const testPk = clean(process.env.STRIPE_PUBLISHABLE_KEY_TEST || process.env.STRIPE_TEST_PUBLISHABLE_KEY);
    if (isValidKey(testPk) && testPk.startsWith("pk_")) return testPk;

    const candidate = clean(process.env.STRIPE_PUBLISHABLE_KEY);
    if (candidate.startsWith("pk_test_") && isValidKey(candidate)) return candidate;

    if (isValidKey(candidate) && candidate.startsWith("pk_")) return candidate;
  } else {
    const livePk = clean(process.env.STRIPE_PUBLISHABLE_KEY_LIVE || process.env.STRIPE_LIVE_PUBLISHABLE_KEY);
    if (isValidKey(livePk) && livePk.startsWith("pk_")) return livePk;

    const candidate = clean(process.env.STRIPE_PUBLISHABLE_KEY);
    if (candidate.startsWith("pk_live_") && isValidKey(candidate)) return candidate;

    if (isValidKey(candidate) && candidate.startsWith("pk_")) return candidate;
  }

  return clean(process.env.STRIPE_PUBLISHABLE_KEY);
}

export function isStripePublishableConfigured(): boolean {
  const key = getStripePublishableKey();
  return isValidKey(key) && key.startsWith("pk_");
}

export function getEnvironmentSummary() {
  const testMode = isTestMode();
  const duffelToken = getDuffelToken();
  const stripeSecret = getStripeSecretKey();
  const stripePk = getStripePublishableKey();

  return {
    isTestMode: testMode,
    modeLabel: testMode ? "SANDBOX / TEST MODE" : "PRODUCTION / LIVE",
    nodeEnv: clean(process.env.NODE_ENV) || "development",
    useTestApis: clean(process.env.USE_TEST_APIS) || (testMode ? "true" : "false"),
    duffel: {
      configured: isDuffelConfigured(),
      keyType: duffelToken.startsWith("duffel_live_")
        ? "LIVE (Produção)"
        : duffelToken.startsWith("duffel_test_")
        ? "TEST (Sandbox)"
        : isValidKey(duffelToken)
        ? "CUSTOM_TOKEN"
        : "UNCONFIGURED",
      tokenPrefix: duffelToken ? duffelToken.slice(0, 12) + "..." : "NONE",
    },
    stripe: {
      secretConfigured: isStripeSecretConfigured(),
      secretKeyType: stripeSecret.startsWith("sk_live_")
        ? "LIVE (Cobrança Real)"
        : stripeSecret.startsWith("sk_test_")
        ? "TEST (Modo Teste)"
        : isValidKey(stripeSecret)
        ? "CUSTOM_KEY"
        : "UNCONFIGURED",
      publishableConfigured: isStripePublishableConfigured(),
      publishableKeyType: stripePk.startsWith("pk_live_")
        ? "LIVE"
        : stripePk.startsWith("pk_test_")
        ? "TEST"
        : isValidKey(stripePk)
        ? "CUSTOM_KEY"
        : "UNCONFIGURED",
    },
  };
}
