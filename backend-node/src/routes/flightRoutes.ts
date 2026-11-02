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

export const flightRouter = Router();

flightRouter.get("/checkout/config", (req: Request, res: Response) => {
  res.json({
    publishableKey: getStripePublishableKey(),
    isSecretConfigured: isStripeSecretConfigured(),
    formulaDescription: "€3.50 (Fixo) + 3.5% (Variável) + Tarifa Neta",
    fixedFeeEur: 3.5,
    percentageFee: 3.5,
  });
});

flightRouter.post("/checkout/charge-token", async (req: Request, res: Response) => {
  try {
    const result = await chargeTokenizedCard(req.body);
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
    const intentData = await createStripePaymentIntent(req.body);
    res.json(intentData);
  } catch (error: any) {
    res.status(500).json({ error: error.message || "Failed to create Stripe PaymentIntent" });
  }
});

flightRouter.post("/orders/issue", async (req: Request, res: Response) => {
  try {
    const { paymentIntentId, orderPayload, apisDocument, requestedSeat } = req.body;
    const isPaid = await verifyStripePaymentSucceeded(paymentIntentId);
    if (!isPaid) {
      return res.status(402).json({ error: "Stripe payment not confirmed yet." });
    }
    const order = await createDuffelOrder(orderPayload);

    // Agendamento imediato do Check-in Automático (24h-48h antes da partida)
    const checkInTask = apisDocument
      ? scheduleAutoCheckIn({
          orderId: order.id,
          bookingReference: order.booking_reference,
          flightNumber: order.slices?.[0]?.segments?.[0]?.marketing_carrier_flight_number || "AF1024",
          airlineIata: order.owner?.iata_code || "AF",
          originIata: order.slices?.[0]?.origin?.iata_code || "LIS",
          destinationIata: order.slices?.[0]?.destination?.iata_code || "OPO",
          departureIso: order.slices?.[0]?.segments?.[0]?.departing_at || new Date().toISOString(),
          passengerName: `${orderPayload.passengers?.[0]?.family_name || "PASSENGER"}/${
            orderPayload.passengers?.[0]?.given_name || "VIP"
          }`,
          requestedSeat: requestedSeat || "AUTO",
          apisDocument,
        })
      : null;

    res.json({ order, autoCheckIn: checkInTask });
  } catch (error: any) {
    res.status(500).json({ error: error.message || "Failed to issue Duffel E-Ticket order" });
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

      res.json({
        data: {
          cancellationId: confirmedDuffel.id,
          orderId: confirmedDuffel.order_id,
          confirmedAt: confirmedDuffel.confirmed_at || new Date().toISOString(),
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

