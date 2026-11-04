import { Router, Request, Response } from "express";
import crypto from "crypto";
import Stripe from "stripe";
import {
  appendBookingWebhookAuditEvent,
  ElectronicTicketDocument,
  findBookingByAnyIdentifier,
  markBookingFailedAndHoldReleased,
  markBookingRefunded,
  toMyTravelsSummaryDto,
} from "../services/bookingPersistenceStore";

export const webhookRouter = Router();

function getStripeClient(): Stripe {
  return new Stripe(process.env.STRIPE_SECRET_KEY || "", {
    apiVersion: "2024-06-20",
  });
}

/**
 * ============================================================================
 * 1. WEBHOOK HANDLER DA STRIPE (POST /api/webhooks/stripe)
 * ============================================================================
 * Escuta eventos críticos do ciclo de vida do PaymentIntent e Refunds:
 * - `payment_intent.amount_capturable_updated`: Cartão pré-autorizado (`capture_method: 'manual'`),
 *   aguardando emissão do bilhete na Duffel (`status: 'pending'`).
 * - `payment_intent.succeeded`: Captura definitiva concluída após emissão da Duffel (`status: 'confirmed'`).
 * - `payment_intent.canceled` / `payment_intent.payment_failed`: Autorização cancelada por rollback
 *   ou falha no cartão (`status: 'failed'`).
 * - `charge.refunded` / `refund.updated`: Estorno pós-venda processado (`status: 'refunded'`).
 */
webhookRouter.post("/stripe", async (req: Request, res: Response) => {
  const sig = req.headers["stripe-signature"] as string | undefined;
  const webhookSecret = process.env.STRIPE_WEBHOOK_SECRET || "";

  let event: any = req.body;

  try {
    if (webhookSecret && sig && (req as any).rawBody) {
      const stripe = getStripeClient();
      event = stripe.webhooks.constructEvent((req as any).rawBody, sig, webhookSecret);
    }
  } catch (err: any) {
    console.error("[Stripe Webhook] Falha na verificação de assinatura:", err.message);
    return res.status(400).json({
      received: false,
      error: `Webhook Signature Verification Failed: ${err.message}`,
    });
  }

  const eventType = String(event?.type || "");
  const dataObject = event?.data?.object || {};
  const paymentIntentId = String(
    dataObject.payment_intent || dataObject.id || ""
  );

  switch (eventType) {
    case "payment_intent.amount_capturable_updated": {
      const updated = appendBookingWebhookAuditEvent({
        identifier: paymentIntentId,
        newStatus: "pending",
        eventTitle: "STRIPE_WEBHOOK_MANUAL_HOLD_AUTHORIZED",
        details: `Stripe confirmou bloqueio de limite (amount_capturable=${
          (dataObject.amount_capturable || 0) / 100
        } EUR) para PaymentIntent ${paymentIntentId}.`,
      });
      return res.json({
        received: true,
        eventType,
        bookingStatus: updated?.status || "pending",
      });
    }

    case "payment_intent.succeeded": {
      const updated = appendBookingWebhookAuditEvent({
        identifier: paymentIntentId,
        newStatus: "confirmed",
        eventTitle: "STRIPE_WEBHOOK_PAYMENT_CAPTURED",
        details: `Stripe confirmou captura definitiva de €${(
          (dataObject.amount_received || dataObject.amount || 0) / 100
        ).toFixed(2)} para PaymentIntent ${paymentIntentId}.`,
      });
      return res.json({
        received: true,
        eventType,
        bookingStatus: updated?.status || "confirmed",
      });
    }

    case "payment_intent.canceled":
    case "payment_intent.payment_failed": {
      const booking = findBookingByAnyIdentifier(paymentIntentId);
      if (booking) {
        markBookingFailedAndHoldReleased({
          bookingId: booking.bookingId,
          failureCode: eventType,
          failureReason:
            dataObject.cancellation_reason ||
            dataObject.last_payment_error?.message ||
            "Autorização cancelada ou recusada na Stripe.",
          stripeCanceledSuccessfully: eventType === "payment_intent.canceled",
        });
      }
      return res.json({
        received: true,
        eventType,
        bookingStatus: "failed",
      });
    }

    case "charge.refunded":
    case "refund.created":
    case "refund.updated": {
      const refundedCents = dataObject.amount_refunded || dataObject.amount || 0;
      const refundedEur = Number((refundedCents / 100).toFixed(2));
      const updated = markBookingRefunded({
        bookingIdOrOrderIdOrPaymentIntentId: paymentIntentId,
        refundedAmountEur: refundedEur,
        reason: `Stripe Webhook (${eventType})`,
      });
      return res.json({
        received: true,
        eventType,
        bookingStatus: updated?.status || "refunded",
      });
    }

    default: {
      return res.json({
        received: true,
        eventType,
        unhandled: true,
      });
    }
  }
});

/**
 * Valida a assinatura HMAC-SHA256 enviada pela Duffel no header `X-Duffel-Signature`.
 * Formato esperado: `t=...,v1=...`
 */
function verifyDuffelWebhookSignature(
  rawPayload: string,
  signatureHeader: string | undefined,
  secret: string
): boolean {
  if (!secret || !signatureHeader) return true;
  const parts = signatureHeader.split(",");
  const timestampPart = parts.find((p) => p.trim().startsWith("t="));
  const v1Part = parts.find((p) => p.trim().startsWith("v1="));
  if (!timestampPart || !v1Part) return false;

  const timestamp = timestampPart.split("=")[1];
  const receivedHmac = v1Part.split("=")[1];
  const signedContent = `${timestamp}.${rawPayload}`;
  const expectedHmac = crypto
    .createHmac("sha256", secret)
    .update(signedContent, "utf8")
    .digest("hex");

  try {
    return crypto.timingSafeEqual(
      Buffer.from(receivedHmac, "hex"),
      Buffer.from(expectedHmac, "hex")
    );
  } catch {
    return false;
  }
}

/**
 * ============================================================================
 * 2. WEBHOOK HANDLER DA DUFFEL (POST /api/webhooks/duffel)
 * ============================================================================
 * Escuta eventos de emissão, alteração involuntária de malha aérea e cancelamento:
 * - `order.created`: Confirma emissão de PNR e documentos eletrónicos (`documents` / e-tickets).
 * - `order.creation_failed`: Emissão assíncrona falhou na companhia aérea -> aciona cancelamento/estorno.
 * - `order_cancellation.confirmed`: Cancelamento confirmado -> marca reserva como `refunded`.
 * - `air.order.changed`: Alteração de horário/portão pela companhia aérea -> atualiza trilha de auditoria.
 */
webhookRouter.post("/duffel", async (req: Request, res: Response) => {
  const signature = req.headers["x-duffel-signature"] as string | undefined;
  const duffelWebhookSecret = process.env.DUFFEL_WEBHOOK_SECRET || "";
  const rawString = JSON.stringify(req.body || {});

  if (
    duffelWebhookSecret &&
    !verifyDuffelWebhookSignature(rawString, signature, duffelWebhookSecret)
  ) {
    return res.status(401).json({
      received: false,
      error: "Invalid X-Duffel-Signature header",
    });
  }

  const eventType = String(req.body?.type || req.body?.event_type || "");
  const payloadData = req.body?.data || {};
  const orderId = String(payloadData.order_id || payloadData.id || "");

  switch (eventType) {
    case "order.created": {
      const pnr = payloadData.booking_reference
        ? String(payloadData.booking_reference).toUpperCase()
        : undefined;

      const rawDocs: any[] = payloadData.documents || [];
      const parsedTickets: ElectronicTicketDocument[] = rawDocs.map(
        (doc: any, idx: number) => ({
          documentId: doc.id || `doc_${idx + 1}`,
          ticketNumber:
            doc.unique_identifier || `047-${Date.now().toString().slice(-10)}`,
          passengerIds: doc.passenger_ids || [],
          issuedAtIso: new Date().toISOString(),
        })
      );

      const updated = appendBookingWebhookAuditEvent({
        identifier: orderId,
        newStatus: "confirmed",
        bookingReference: pnr,
        eTickets: parsedTickets.length > 0 ? parsedTickets : undefined,
        eventTitle: "DUFFEL_WEBHOOK_ORDER_CREATED",
        details: `Duffel confirmou emissão da ordem ${orderId} (PNR: ${
          pnr || "N/A"
        }, E-Tickets: ${parsedTickets.map((t) => t.ticketNumber).join(", ") || "Emitidos"}).`,
      });

      return res.json({
        received: true,
        eventType,
        booking: updated ? toMyTravelsSummaryDto(updated) : null,
      });
    }

    case "order.creation_failed": {
      const existing = findBookingByAnyIdentifier(orderId);
      if (existing) {
        markBookingFailedAndHoldReleased({
          bookingId: existing.bookingId,
          failureCode: "duffel_webhook_order_creation_failed",
          failureReason:
            "A companhia aérea reportou falha assíncrona na emissão do bilhete (order.creation_failed).",
          stripeCanceledSuccessfully: true,
        });
      }
      return res.json({
        received: true,
        eventType,
        bookingStatus: "failed",
      });
    }

    case "order_cancellation.confirmed": {
      const refundEur = parseFloat(payloadData.refund_amount || "0");
      const updated = markBookingRefunded({
        bookingIdOrOrderIdOrPaymentIntentId: orderId,
        refundedAmountEur: refundEur,
        reason: `Duffel Webhook order_cancellation.confirmed (id=${payloadData.id || "N/A"})`,
      });
      return res.json({
        received: true,
        eventType,
        bookingStatus: updated?.status || "refunded",
      });
    }

    case "air.order.changed": {
      const updated = appendBookingWebhookAuditEvent({
        identifier: orderId,
        eventTitle: "DUFFEL_WEBHOOK_AIRLINE_SCHEDULE_CHANGED",
        details: `Companhia aérea notificou atualização operacional na reserva ${orderId}.`,
      });
      return res.json({
        received: true,
        eventType,
        bookingStatus: updated?.status || "confirmed",
      });
    }

    default: {
      return res.json({
        received: true,
        eventType,
        unhandled: true,
      });
    }
  }
});
