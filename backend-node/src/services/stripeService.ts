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
  captureMethod?: "manual" | "automatic";
}

/**
 * Cria um PaymentIntent seguro na Stripe utilizando a Chave Secreta no servidor.
 * Por padrão utiliza `capture_method: 'manual'` para reservar o limite sem capturar até a confirmação da Duffel.
 */
export async function createStripePaymentIntent(input: CreateCheckoutIntentInput) {
  const pricing = calculateStripeFinalFare(input.netAmountEur, input.ancillariesAmountEur || 0);
  const captureMethod = input.captureMethod || "manual";

  if (!isStripeSecretConfigured()) {
    console.log(
      `[Stripe Sandbox Server] Chave Secreta em modo placeholder. Simulando PaymentIntent (${captureMethod}) de €${pricing.grandTotalEur}`
    );
    return {
      paymentIntentId: `pi_mock_server_${Date.now()}`,
      clientSecret: `pi_mock_secret_${Date.now()}`,
      status: captureMethod === "manual" ? "requires_capture" : "succeeded",
      captureMethod,
      totalChargedEur: pricing.grandTotalEur,
      pricing,
    };
  }

  const stripe = getStripeClient();
  const paymentIntent = await stripe.paymentIntents.create({
    amount: pricing.amountInCents,
    currency: "eur",
    capture_method: captureMethod,
    receipt_email: input.customerEmail,
    automatic_payment_methods: { enabled: true },
    metadata: {
      duffel_offer_id: input.offerId,
      duffel_net_eur: pricing.netFareEur.toFixed(2),
      fixed_fee_eur: pricing.fixedFeeEur.toFixed(2),
      percentage_fee_eur: pricing.percentageFeeEur.toFixed(2),
      ota_markup_eur: pricing.markupTotalEur.toFixed(2),
      ancillaries_eur: pricing.ancillariesEur.toFixed(2),
      capture_method: captureMethod,
      pricing_formula: "€3.50 + 3.5% + Tarifa Neta",
    },
  });

  return {
    paymentIntentId: paymentIntent.id,
    clientSecret: paymentIntent.client_secret,
    status: paymentIntent.status,
    captureMethod,
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
  captureMethod?: "manual" | "automatic";
}

/**
 * Autoriza (capture_method: 'manual') ou cobra o cartão previamente tokenizado no frontend.
 */
export async function chargeTokenizedCard(input: ChargeTokenizedCardInput) {
  const pricing = calculateStripeFinalFare(input.netAmountEur, input.ancillariesAmountEur || 0);
  const captureMethod = input.captureMethod || "manual";

  if (!isStripeSecretConfigured()) {
    console.log(
      `[Stripe Backend Authorize] Autorizando cartão tokenizado (${input.token}, capture_method=${captureMethod}): €${pricing.grandTotalEur} para ${input.customerEmail}`
    );
    return {
      success: true,
      paymentIntentId: `pi_live_mock_${Date.now()}`,
      clientSecret: `pi_live_mock_sec_${Date.now()}`,
      status: captureMethod === "manual" ? "requires_capture" : "succeeded",
      captureMethod,
      totalChargedEur: pricing.grandTotalEur,
      pricing,
    };
  }

  const stripe = getStripeClient();
  const paymentIntent = await stripe.paymentIntents.create({
    amount: pricing.amountInCents,
    currency: "eur",
    capture_method: captureMethod,
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
      capture_method: captureMethod,
      pricing_formula: "€3.50 + 3.5% + Tarifa Neta",
    },
  });

  return {
    success:
      paymentIntent.status === "requires_capture" || paymentIntent.status === "succeeded",
    paymentIntentId: paymentIntent.id,
    clientSecret: paymentIntent.client_secret,
    status: paymentIntent.status,
    captureMethod,
    totalChargedEur: pricing.grandTotalEur,
    pricing,
  };
}

/**
 * Captura definitivamente um PaymentIntent previamente autorizado com `capture_method: 'manual'`
 * somente APÓS a emissão bem-sucedida da reserva na Duffel (POST /air/orders).
 */
export async function captureStripePaymentIntent(
  paymentIntentId: string,
  metadata?: Record<string, string>
) {
  if (
    !isStripeSecretConfigured() ||
    paymentIntentId.startsWith("pi_mock_") ||
    paymentIntentId.startsWith("pi_live_mock_")
  ) {
    return {
      paymentIntentId,
      status: "succeeded",
      capturedAtIso: new Date().toISOString(),
    };
  }

  const stripe = getStripeClient();
  const captured = await stripe.paymentIntents.capture(paymentIntentId, {
    ...(metadata ? { metadata } : {}),
  });

  return {
    paymentIntentId: captured.id,
    status: captured.status,
    capturedAtIso: new Date().toISOString(),
  };
}

/**
 * Cancela imediatamente a pré-autorização na Stripe (`stripe.paymentIntents.cancel`)
 * quando a Duffel responde com erro (ex: assento esgotado), libertando o limite do cliente
 * na hora sem gerar cobrança nem estorno na fatura.
 */
export async function cancelStripePaymentIntentHold(
  paymentIntentId: string,
  reason: Stripe.PaymentIntentCancelParams.CancellationReason = "abandoned"
) {
  if (
    !isStripeSecretConfigured() ||
    paymentIntentId.startsWith("pi_mock_") ||
    paymentIntentId.startsWith("pi_live_mock_")
  ) {
    return {
      paymentIntentId,
      status: "canceled",
      canceledAtIso: new Date().toISOString(),
    };
  }

  const stripe = getStripeClient();
  const canceled = await stripe.paymentIntents.cancel(paymentIntentId, {
    cancellation_reason: reason,
  });

  return {
    paymentIntentId: canceled.id,
    status: canceled.status,
    canceledAtIso: new Date().toISOString(),
  };
}

/**
 * Verifica na Stripe se o pagamento está autorizado (`requires_capture`) ou liquidado (`succeeded`)
 * antes de emitir o bilhete na Duffel.
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
    return intent.status === "requires_capture" || intent.status === "succeeded";
  } catch (err) {
    console.warn(`[Stripe Verification] Failed to retrieve intent ${paymentIntentId}:`, err);
    return false;
  }
}

export interface ProcessCustomerStripeRefundInput {
  paymentIntentId?: string;
  orderId: string;
  bookingReference: string;
  airlineRefundAmountEur: number;
  aguiaFlyAdminFeeEur: number;
  retainedIntermediationFeeEur: number;
  netStripeCustomerRefundEur: number;
}

/**
 * Executa o estorno líquido no cartão de crédito do cliente via Stripe (POST /v1/refunds).
 * Retém a taxa operacional original (€3.50 + 3.5%) e desconta a taxa administrativa
 * do AguiaFly (€25.00) sobre o valor devolvido pela companhia aérea.
 */
export async function processCustomerStripeRefund(input: ProcessCustomerStripeRefundInput) {
  const netRefundEur = Number(Math.max(0, input.netStripeCustomerRefundEur).toFixed(2));
  const refundCents = Math.round(netRefundEur * 100);

  if (refundCents <= 0) {
    return {
      refundId: `re_zero_${Date.now()}`,
      status: "no_net_refund_due",
      netRefundedEur: 0,
      currency: "eur",
    };
  }

  if (
    !isStripeSecretConfigured() ||
    !input.paymentIntentId ||
    input.paymentIntentId.startsWith("pi_mock_") ||
    input.paymentIntentId.startsWith("pi_live_mock_")
  ) {
    return {
      refundId: `re_stripe_${Date.now()}`,
      status: "succeeded",
      netRefundedEur: netRefundEur,
      currency: "eur",
    };
  }

  const stripe = getStripeClient();
  const refund = await stripe.refunds.create({
    payment_intent: input.paymentIntentId,
    amount: refundCents,
    reason: "requested_by_customer",
    metadata: {
      duffel_order_id: input.orderId,
      booking_reference: input.bookingReference,
      airline_refund_eur: input.airlineRefundAmountEur.toFixed(2),
      aguiafly_admin_fee_eur: input.aguiaFlyAdminFeeEur.toFixed(2),
      retained_intermediation_fee_eur: input.retainedIntermediationFeeEur.toFixed(2),
      net_customer_refund_eur: netRefundEur.toFixed(2),
    },
  });

  return {
    refundId: refund.id,
    status: refund.status || "succeeded",
    netRefundedEur: netRefundEur,
    currency: refund.currency || "eur",
  };
}

