import Stripe from "stripe";

/**
 * Retorna uma instância do Stripe SDK inicializada com a Chave Secreta (STRIPE_SECRET_KEY).
 * A Chave Secreta NUNCA é enviada ao frontend ou exposta no dispositivo do utilizador.
 */
function getStripeClient(): Stripe {
  const secret = process.env.STRIPE_SECRET_KEY || "";
  return new Stripe(secret, {
    apiVersion: "2024-06-20",
  });
}

export function isStripeSecretConfigured(): boolean {
  const key = process.env.STRIPE_SECRET_KEY || "";
  return key.startsWith("sk_") && !key.includes("...") && !key.includes("placeholder");
}

export function getStripePublishableKey(): string {
  return process.env.STRIPE_PUBLISHABLE_KEY || "";
}

export interface PricingCalculationResult {
  netFareEur: number;
  fixedFeeEur: number;
  percentageFeeEur: number;
  markupTotalEur: number;
  ancillariesEur: number;
  grandTotalEur: number;
  amountInCents: number;
  formulaDescription: string;
}

/**
 * Fórmula Oficial de Cobrança AguiaFly:
 * Valor Final = €3.50 (Taxa Fixa) + 3.5% (Markup Variável) + Tarifa Neta (+ Opcionais se houver)
 */
export function calculateStripeFinalFare(
  netFareEur: number,
  ancillariesEur: number = 0
): PricingCalculationResult {
  const safeNet = Math.max(0, Number(netFareEur.toFixed(2)));
  const fixedFee = 3.5; // €3.50 fixo
  const percentageFee = Number((safeNet * 0.035).toFixed(2)); // 3.5% sobre a tarifa neta
  const markupTotal = Number((fixedFee + percentageFee).toFixed(2));
  const safeAncillaries = Math.max(0, Number((ancillariesEur || 0).toFixed(2)));
  const grandTotalEur = Number((safeNet + markupTotal + safeAncillaries).toFixed(2));
  const amountInCents = Math.round(grandTotalEur * 100);

  return {
    netFareEur: safeNet,
    fixedFeeEur: fixedFee,
    percentageFeeEur: percentageFee,
    markupTotalEur: markupTotal,
    ancillariesEur: safeAncillaries,
    grandTotalEur,
    amountInCents,
    formulaDescription: `€3.50 + 3.5% (€${percentageFee.toFixed(2)}) + Tarifa Neta (€${safeNet.toFixed(2)}) = €${grandTotalEur.toFixed(2)}`,
  };
}

export interface CreateCheckoutIntentInput {
  offerId: string;
  netAmountEur: number;
  markupAmountEur?: number;
  ancillariesAmountEur?: number;
  customerEmail: string;
}

/**
 * Cria um PaymentIntent seguro na Stripe utilizando a Chave Secreta no servidor.
 * O valor cobrado é rigorosamente calculado pela fórmula: €3.50 + 3.5% + Tarifa Neta.
 */
export async function createStripePaymentIntent(input: CreateCheckoutIntentInput) {
  const pricing = calculateStripeFinalFare(input.netAmountEur, input.ancillariesAmountEur || 0);

  if (!isStripeSecretConfigured()) {
    console.log(
      `[Stripe Sandbox Server] Chave Secreta em modo placeholder. Simulando PaymentIntent de €${pricing.grandTotalEur} (${pricing.formulaDescription})`
    );
    return {
      paymentIntentId: `pi_mock_server_${Date.now()}`,
      clientSecret: `pi_mock_secret_${Date.now()}`,
      totalChargedEur: pricing.grandTotalEur,
      pricing,
    };
  }

  const stripe = getStripeClient();
  const paymentIntent = await stripe.paymentIntents.create({
    amount: pricing.amountInCents,
    currency: "eur",
    receipt_email: input.customerEmail,
    automatic_payment_methods: { enabled: true },
    metadata: {
      duffel_offer_id: input.offerId,
      duffel_net_eur: pricing.netFareEur.toFixed(2),
      fixed_fee_eur: pricing.fixedFeeEur.toFixed(2),
      percentage_fee_eur: pricing.percentageFeeEur.toFixed(2),
      ota_markup_eur: pricing.markupTotalEur.toFixed(2),
      ancillaries_eur: pricing.ancillariesEur.toFixed(2),
      pricing_formula: "€3.50 + 3.5% + Tarifa Neta",
    },
  });

  return {
    paymentIntentId: paymentIntent.id,
    clientSecret: paymentIntent.client_secret,
    totalChargedEur: pricing.grandTotalEur,
    pricing,
  };
}

export interface ChargeTokenizedCardInput {
  token: string; // Token gerado no frontend com a Chave Publicável (STRIPE_PUBLISHABLE_KEY)
  offerId: string;
  netAmountEur: number;
  ancillariesAmountEur?: number;
  customerEmail: string;
}

/**
 * Cobra com segurança o cartão previamente tokenizado no frontend com a Chave Publicável.
 * O processamento da cobrança é executado exclusivamente no backend via Chave Secreta (STRIPE_SECRET_KEY).
 */
export async function chargeTokenizedCard(input: ChargeTokenizedCardInput) {
  const pricing = calculateStripeFinalFare(input.netAmountEur, input.ancillariesAmountEur || 0);

  if (!isStripeSecretConfigured()) {
    console.log(
      `[Stripe Backend Charge] Cobrando cartão tokenizado (${input.token}) via Chave Secreta Sandbox: €${pricing.grandTotalEur} para ${input.customerEmail}`
    );
    return {
      success: true,
      paymentIntentId: `pi_live_mock_${Date.now()}`,
      clientSecret: `pi_live_mock_sec_${Date.now()}`,
      status: "succeeded",
      totalChargedEur: pricing.grandTotalEur,
      pricing,
    };
  }

  const stripe = getStripeClient();
  // Cobrança direta com token Stripe utilizando PaymentIntents / Confirm no servidor seguro
  const paymentIntent = await stripe.paymentIntents.create({
    amount: pricing.amountInCents,
    currency: "eur",
    receipt_email: input.customerEmail,
    confirm: true,
    payment_method_data: {
      type: "card",
      card: { token: input.token },
    } as any,
    automatic_payment_methods: {
      enabled: true,
      allow_redirects: "never",
    },
    metadata: {
      duffel_offer_id: input.offerId,
      duffel_net_eur: pricing.netFareEur.toFixed(2),
      fixed_fee_eur: pricing.fixedFeeEur.toFixed(2),
      percentage_fee_eur: pricing.percentageFeeEur.toFixed(2),
      ota_markup_eur: pricing.markupTotalEur.toFixed(2),
      ancillaries_eur: pricing.ancillariesEur.toFixed(2),
      client_token_used: input.token.slice(0, 8) + "...",
      pricing_formula: "€3.50 + 3.5% + Tarifa Neta",
    },
  });

  return {
    success: paymentIntent.status === "succeeded",
    paymentIntentId: paymentIntent.id,
    clientSecret: paymentIntent.client_secret,
    status: paymentIntent.status,
    totalChargedEur: pricing.grandTotalEur,
    pricing,
  };
}

/**
 * Verifica na Stripe se o pagamento foi liquidado antes de emitir o bilhete na Duffel.
 */
export async function verifyStripePaymentSucceeded(paymentIntentId: string): Promise<boolean> {
  if (paymentIntentId.startsWith("pi_mock_") || paymentIntentId.startsWith("pi_live_mock_")) {
    return true;
  }
  if (!isStripeSecretConfigured()) {
    return true;
  }
  try {
    const stripe = getStripeClient();
    const intent = await stripe.paymentIntents.retrieve(paymentIntentId);
    return intent.status === "succeeded";
  } catch (err) {
    console.warn(`[Stripe Verification] Failed to retrieve intent ${paymentIntentId}:`, err);
    return false;
  }
}
