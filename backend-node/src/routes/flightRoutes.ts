import { Router, Request, Response } from "express";
import {
  searchFlightsWithMarkup,
  getOfferSeatMaps,
  createDuffelOrder,
  searchPlacesSuggestions,
  createDuffelOrderCancellationQuote,
  confirmDuffelOrderCancellation,
} from "../services/duffelService";
import {
  createStripePaymentIntent,
  chargeTokenizedCard,
  verifyStripePaymentSucceeded,
  getStripePublishableKey,
  isStripeSecretConfigured,
  calculateStripeFinalFare,
  processCustomerStripeRefund,
} from "../services/stripeService";
import {
  calculateCancellationRefundBreakdown,
  AGUIAFLY_CANCELLATION_ADMIN_FEE_EUR,
} from "../middleware/markupMiddleware";
import {
  scheduleAutoCheckIn,
  executeCheckInAndIssueBoardingPass,
} from "../services/autoCheckInScheduler";
import {
  CheckoutFlowError,
  executeAtomicFlightCheckout,
  revalidateDuffelOfferPreCheckout,
} from "../services/atomicCheckoutOrchestrator";
import {
  PassengerValidationError,
  sanitizeAndValidatePassenger,
  sanitizeIataGdsName,
} from "../services/passengerSanitizer";
import {
  findBookingByAnyIdentifier,
  listUserBookingsForMyTravels,
  markBookingRefunded,
  toMyTravelsSummaryDto,
} from "../services/bookingPersistenceStore";
import {
  cancelStripePaymentIntentHold,
  captureStripePaymentIntent,
} from "../services/stripeService";
import { getEnvironmentSummary } from "../config/environment";

export const flightRouter = Router();

/**
 * Retorna o status de ambiente atual do backend (Sandbox / Test vs Live / Produção).
 */
flightRouter.get("/config/mode", (_req: Request, res: Response) => {
  res.json({
    success: true,
    data: getEnvironmentSummary(),
  });
});

/**
 * 1. RE-VALIDAÇÃO PRÉ-CHECKOUT DA OFERTA (GET/POST /api/flights/offers/:offerId/revalidate)
 * Consulta a oferta na Duffel (GET /air/offers/:id) antes de qualquer transação financeira.
 * Retorna erro amigável `OFFER_EXPIRED` ou `PRICE_CHANGED` caso tenha expirado ou sofrido alteração.
 */
flightRouter.post("/offers/:offerId/revalidate", async (req: Request, res: Response) => {
  try {
    const offerId = req.params.offerId;
    const expectedNetAmountEur = parseFloat(
      String(req.body.expectedNetAmountEur ?? req.query.expectedNetAmountEur ?? "0")
    );
    const ancillariesAmountEur = parseFloat(
      String(req.body.ancillariesAmountEur ?? req.query.ancillariesAmountEur ?? "0")
    );

    const result = await revalidateDuffelOfferPreCheckout({
      offerId,
      expectedNetAmountEur,
      ancillariesAmountEur,
      originIata: req.body.originIata,
      destinationIata: req.body.destinationIata,
    });

    return res.json({
      status: "VALID",
      data: result,
    });
  } catch (error: any) {
    if (error instanceof CheckoutFlowError) {
      return res.status(error.httpStatus).json({
        error: error.message,
        errorCode: error.code, // 'OFFER_EXPIRED' | 'PRICE_CHANGED'
        details: error.details || {},
      });
    }
    return res.status(500).json({
      error: error.message || "Falha ao re-validar oferta na Duffel API",
      errorCode: "INTERNAL_CHECKOUT_ERROR",
    });
  }
});

/**
 * 2. SANITIZAÇÃO E VALIDAÇÃO DE PASSAGEIRO PADRÃO IATA/GDS (POST /api/flights/checkout/sanitize-passenger)
 * Remove acentos, cedilhas e caracteres especiais dos nomes e valida `identity_documents` em rotas internacionais.
 */
flightRouter.post("/checkout/sanitize-passenger", (req: Request, res: Response) => {
  try {
    const { passenger, requireIdentityDocument, flightDepartureDateIso } = req.body;
    const sanitized = sanitizeAndValidatePassenger(passenger, {
      requireIdentityDocument: Boolean(requireIdentityDocument),
      flightDepartureDateIso,
    });
    return res.json({
      valid: true,
      sanitizedPassenger: sanitized,
    });
  } catch (error: any) {
    if (error instanceof PassengerValidationError) {
      return res.status(400).json({
        valid: false,
        errorCode: error.code,
        field: error.field,
        error: error.message,
      });
    }
    return res.status(400).json({
      valid: false,
      errorCode: "INVALID_PASSENGER_DATA",
      error: error.message || "Dados do passageiro inválidos.",
    });
  }
});

/**
 * 3. ORQUESTRAÇÃO TRANSACIONAL ATÔMICA STRIPE (MANUAL CAPTURE) + DUFFEL (POST /api/flights/checkout/atomic-issue)
 * Executa os 4 passos críticos de forma atômica:
 *   - Passo 1: Re-valida oferta na Duffel (GET /air/offers/:id) -> bloqueia se OFFER_EXPIRED ou PRICE_CHANGED.
 *   - Passo 2: Sanitiza nomes do passageiro (IATA/GDS) e valida passaporte (`identity_documents`).
 *   - Passo 3: Cria Stripe PaymentIntent com `capture_method: 'manual'` (autorização sem captura imediata).
 *   - Passo 4: Cria pedido na Duffel (`POST /air/orders`):
 *       * SUCESSO -> `stripe.paymentIntents.capture` + persiste reserva `confirmed` + retorna PNR, E-Tickets e PDF.
 *       * ERRO    -> `stripe.paymentIntents.cancel` (liberta limite do cliente na hora sem cobrança) + persiste `failed`.
 */
flightRouter.post("/checkout/atomic-issue", async (req: Request, res: Response) => {
  try {
    const outcome = await executeAtomicFlightCheckout(req.body);
    return res.status(200).json(outcome);
  } catch (error: any) {
    if (error instanceof CheckoutFlowError) {
      return res.status(error.httpStatus).json({
        success: false,
        errorCode: error.code,
        error: error.message,
        details: error.details || {},
      });
    }
    return res.status(500).json({
      success: false,
      errorCode: "INTERNAL_CHECKOUT_ERROR",
      error: error.message || "Erro inesperado durante a orquestração atômica do checkout.",
    });
  }
});

/**
 * 4. LISTAGEM LIMPA PARA A ABA 'MINHAS VIAGENS' (GET /api/flights/trips e GET /api/flights/trips/:userId)
 * Retorna todas as reservas persistidas (pending, confirmed, failed, refunded) com PNR/Localizador,
 * números de e-ticket IATA e PDF de confirmação.
 */
flightRouter.get("/trips", (req: Request, res: Response) => {
  const userId = req.query.userId ? String(req.query.userId) : undefined;
  const trips = listUserBookingsForMyTravels(userId);
  res.json({ data: trips });
});

flightRouter.get("/trips/:userId", (req: Request, res: Response) => {
  const trips = listUserBookingsForMyTravels(req.params.userId);
  res.json({ data: trips });
});

flightRouter.get("/orders/:orderId/confirmation.pdf", (req: Request, res: Response) => {
  const booking = findBookingByAnyIdentifier(req.params.orderId);
  res.json({
    documentType: "IATA_NDC_ELECTRONIC_TICKET_RECEIPT_PDF",
    orderId: req.params.orderId,
    pnrLocator: booking?.bookingReference || String(req.query.pnr || "AG8X9L"),
    status: booking?.status || "confirmed",
    eTickets: booking?.eTickets || [],
    bookingSummary: booking ? toMyTravelsSummaryDto(booking) : null,
  });
});

flightRouter.get("/checkout/config", (req: Request, res: Response) => {
  res.json({
    publishableKey: getStripePublishableKey(),
    isSecretConfigured: isStripeSecretConfigured(),
    captureMethod: "manual",
    formulaDescription: "€3.50 (Fixo) + 3.5% (Variável) + Tarifa Neta",
    fixedFeeEur: 3.5,
    percentageFee: 3.5,
  });
});

flightRouter.post("/checkout/charge-token", async (req: Request, res: Response) => {
  try {
    const result = await chargeTokenizedCard({
      ...req.body,
      captureMethod: req.body.captureMethod || "manual",
    });
    res.json(result);
  } catch (error: any) {
    res.status(500).json({ error: error.message || "Failed to process tokenized card payment on Stripe" });
  }
});

flightRouter.post("/checkout/calculate", (req: Request, res: Response) => {
  const netFareEur = parseFloat(req.body.netFareEur || "0");
  const ancillariesEur = parseFloat(req.body.ancillariesEur || "0");
  const result = calculateStripeFinalFare(netFareEur, ancillariesEur);
  res.json(result);
});

flightRouter.get("/places", async (req: Request, res: Response) => {
  try {
    const query = String(req.query.query || "");
    if (!query.trim()) {
      return res.json({ data: [] });
    }
    const suggestions = await searchPlacesSuggestions(query);
    res.json({ data: suggestions });
  } catch (error: any) {
    res.status(500).json({ error: error.message || "Failed to fetch Duffel places suggestions" });
  }
});

flightRouter.post("/search", async (req: Request, res: Response) => {
  try {
    const { origin, destination, departureDate, cabinClass, passengersCount, markupConfig } = req.body;
    const result = await searchFlightsWithMarkup(
      { origin, destination, departureDate, cabinClass, passengersCount },
      markupConfig
    );
    res.json(result);
  } catch (error: any) {
    res.status(500).json({ error: error.message || "Failed to search flights on Duffel" });
  }
});

flightRouter.get("/offers/:offerId/seat-maps", async (req: Request, res: Response) => {
  try {
    const seatMaps = await getOfferSeatMaps(req.params.offerId);
    res.json({ data: seatMaps });
  } catch (error: any) {
    res.status(500).json({ error: error.message || "Failed to load seat maps" });
  }
});

flightRouter.post("/checkout/payment-intent", async (req: Request, res: Response) => {
  try {
    const intentData = await createStripePaymentIntent({
      ...req.body,
      captureMethod: req.body.captureMethod || "manual",
    });
    res.json(intentData);
  } catch (error: any) {
    res.status(500).json({ error: error.message || "Failed to create Stripe PaymentIntent" });
  }
});

flightRouter.post("/orders/issue", async (req: Request, res: Response) => {
  const { paymentIntentId, orderPayload, apisDocument, requestedSeat } = req.body;
  try {
    const isAuthorized = await verifyStripePaymentSucceeded(paymentIntentId);
    if (!isAuthorized) {
      return res.status(402).json({
        error: "Stripe payment authorization (requires_capture) not confirmed yet.",
        errorCode: "PAYMENT_AUTHORIZATION_FAILED",
      });
    }

    // Cria o pedido na Duffel (POST /air/orders) com nomes sanitizados no padrão IATA/GDS
    const order = await createDuffelOrder(orderPayload);

    // Captura definitiva do dinheiro na Stripe (stripe.paymentIntents.capture) após sucesso na Duffel
    const captureResult = await captureStripePaymentIntent(paymentIntentId, {
      duffel_order_id: order.id,
      booking_reference: order.booking_reference,
    });

    // Agendamento imediato do Check-in Automático (24h-48h antes da partida)
    const cleanFamily = sanitizeIataGdsName(orderPayload.familyName || orderPayload.passengers?.[0]?.family_name || "PASSENGER");
    const cleanGiven = sanitizeIataGdsName(orderPayload.givenName || orderPayload.passengers?.[0]?.given_name || "VIP");
    const checkInTask = apisDocument
      ? scheduleAutoCheckIn({
          orderId: order.id,
          bookingReference: order.booking_reference,
          flightNumber: order.slices?.[0]?.segments?.[0]?.marketing_carrier_flight_number || "AF1024",
          airlineIata: order.owner?.iata_code || "AF",
          originIata: order.slices?.[0]?.origin?.iata_code || "LIS",
          destinationIata: order.slices?.[0]?.destination?.iata_code || "OPO",
          departureIso: order.slices?.[0]?.segments?.[0]?.departing_at || new Date().toISOString(),
          passengerName: `${cleanFamily}/${cleanGiven}`,
          requestedSeat: requestedSeat || "AUTO",
          apisDocument,
        })
      : null;

    res.json({ order, stripeCapture: captureResult, autoCheckIn: checkInTask });
  } catch (error: any) {
    // SE a Duffel responder com ERRO: Cancela imediatamente a autorização na Stripe (stripe.paymentIntents.cancel)
    let holdCancellationResult = null;
    if (paymentIntentId) {
      try {
        holdCancellationResult = await cancelStripePaymentIntentHold(paymentIntentId, "abandoned");
      } catch (cancelErr) {
        console.error("[Orders Issue Rollback] Failed to cancel Stripe PaymentIntent hold:", cancelErr);
      }
    }
    res.status(422).json({
      error: error.message || "Failed to issue Duffel E-Ticket order",
      errorCode: "DUFFEL_ORDER_CREATION_FAILED",
      stripeHoldReleased: Boolean(holdCancellationResult),
      stripeHoldCancellation: holdCancellationResult,
    });
  }
});

flightRouter.post("/checkin/schedule", (req: Request, res: Response) => {
  try {
    const task = scheduleAutoCheckIn(req.body);
    res.json({ data: task });
  } catch (error: any) {
    res.status(500).json({ error: error.message || "Failed to schedule auto check-in" });
  }
});

flightRouter.post("/checkin/execute/:orderId", (req: Request, res: Response) => {
  const updated = executeCheckInAndIssueBoardingPass(req.params.orderId);
  if (!updated) {
    return res.status(404).json({ error: "Scheduled check-in task not found" });
  }
  res.json({ data: updated });
});

/**
 * Consulta a cotação de cancelamento na Duffel API (POST /air/order_cancellations)
 * e calcula o valor líquido de reembolso aplicando a Taxa Administrativa AguiaFly (€25.00)
 * e retendo as taxas operacionais originais (€3.50 + 3.5%).
 */
flightRouter.post("/orders/cancellations/quote", async (req: Request, res: Response) => {
  try {
    const { orderId, originalNetFareEur, originalMarkupEur } = req.body;
    if (!orderId) {
      return res.status(400).json({ error: "orderId is required to request a cancellation quote." });
    }
    const quote = await createDuffelOrderCancellationQuote({
      orderId: String(orderId),
      originalNetFareEur: parseFloat(originalNetFareEur || "0"),
      originalMarkupEur: originalMarkupEur ? parseFloat(originalMarkupEur) : undefined,
    });
    res.json({ data: quote });
  } catch (error: any) {
    res.status(500).json({
      error: error.message || "Failed to fetch cancellation quote from Duffel API",
    });
  }
});

/**
 * Confirma o cancelamento na Duffel API (POST /air/order_cancellations/:id/actions/confirm)
 * e processa o estorno líquido no cartão de crédito do cliente via Stripe.
 */
flightRouter.post(
  "/orders/cancellations/:cancellationId/confirm",
  async (req: Request, res: Response) => {
    try {
      const { cancellationId } = req.params;
      const {
        orderId,
        bookingReference,
        paymentIntentId,
        originalNetFareEur,
        originalMarkupEur,
        airlineRefundAmountEur,
      } = req.body;

      const confirmedDuffel = await confirmDuffelOrderCancellation(cancellationId);
      const verifiedAirlineRefundEur = parseFloat(
        confirmedDuffel.refund_amount || String(airlineRefundAmountEur || "0")
      );

      const refundBreakdown = calculateCancellationRefundBreakdown({
        originalNetFareEur: parseFloat(originalNetFareEur || "0"),
        airlineRefundAmountEur: verifiedAirlineRefundEur,
        originalMarkupEur: originalMarkupEur ? parseFloat(originalMarkupEur) : undefined,
      });

      const stripeRefund = await processCustomerStripeRefund({
        paymentIntentId,
        orderId: String(orderId || confirmedDuffel.order_id),
        bookingReference: String(bookingReference || ""),
        airlineRefundAmountEur: refundBreakdown.airlineRefundAmountEur,
        aguiaFlyAdminFeeEur: AGUIAFLY_CANCELLATION_ADMIN_FEE_EUR,
        retainedIntermediationFeeEur: refundBreakdown.retainedIntermediationFeeEur,
        netStripeCustomerRefundEur: refundBreakdown.netStripeCustomerRefundEur,
      });

      const refundedBooking = markBookingRefunded({
        bookingIdOrOrderIdOrPaymentIntentId: String(
          orderId || confirmedDuffel.order_id || paymentIntentId || bookingReference || ""
        ),
        refundedAmountEur: refundBreakdown.netStripeCustomerRefundEur,
        reason: `Order Cancellation Confirmed (${confirmedDuffel.id})`,
      });

      res.json({
        data: {
          cancellationId: confirmedDuffel.id,
          orderId: confirmedDuffel.order_id,
          confirmedAt: confirmedDuffel.confirmed_at || new Date().toISOString(),
          bookingStatus: refundedBooking?.status || "refunded",
          refundBreakdown,
          stripeRefund,
        },
      });
    } catch (error: any) {
      res.status(500).json({
        error: error.message || "Failed to confirm order cancellation and process Stripe refund",
      });
    }
  }
);

