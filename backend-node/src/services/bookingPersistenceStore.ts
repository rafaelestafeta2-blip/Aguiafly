import fs from "fs";
import path from "path";
import { SanitizedDuffelPassenger } from "./passengerSanitizer";

/**
 * Estados oficiais do ciclo de vida da reserva aérea no AguiaFly:
 * - 'pending'   : Autorização de pagamento iniciada (capture_method: 'manual') / aguardando emissão na Duffel.
 * - 'confirmed' : Bilhete emitido na Duffel (POST /air/orders) e valor capturado na Stripe (paymentIntents.capture).
 * - 'failed'    : Falha na emissão na Duffel; autorização Stripe cancelada imediatamente (paymentIntents.cancel) sem cobrança.
 * - 'refunded'  : Reserva cancelada/reembolsada em pós-venda via Stripe Refunds / Duffel Order Cancellations.
 */
export type BookingLifecycleStatus = "pending" | "confirmed" | "failed" | "refunded";

export interface ElectronicTicketDocument {
  documentId: string;
  ticketNumber: string; // Número IATA E-Ticket (ex: "047-2948104921")
  passengerIds: string[];
  issuedAtIso: string;
}

export interface PersistedFlightBooking {
  bookingId: string;
  userId: string;
  status: BookingLifecycleStatus;
  offerId: string;
  duffelOrderId: string | null;
  bookingReference: string | null; // PNR / Localizador de 6 caracteres (ex: "AG8X9L")
  stripePaymentIntentId: string;
  stripeCaptureStatus: "requires_capture" | "captured" | "canceled" | "refunded" | "failed";
  financials: {
    currency: "EUR";
    netFareEur: number;
    otaMarkupEur: number;
    ancillariesEur: number;
    totalAuthorizedEur: number;
    totalCapturedEur: number;
    totalRefundedEur: number;
  };
  itinerary: {
    airlineName: string;
    airlineIata: string;
    flightNumber: string;
    originIata: string;
    originCity: string;
    destinationIata: string;
    destinationCity: string;
    departureIso: string;
    arrivalIso: string;
    cabinClass: string;
    assignedSeat: string;
  };
  passengers: SanitizedDuffelPassenger[];
  eTickets: ElectronicTicketDocument[];
  confirmationPdfUrl: string | null;
  iataBcbpPayload: string | null;
  failureCode?: string;
  failureReason?: string;
  auditTrail: Array<{
    timestampIso: string;
    event: string;
    details: string;
  }>;
  createdAtIso: string;
  updatedAtIso: string;
}

/**
 * Payload limpo e estruturado retornado para a aba 'Minhas Viagens' do aplicativo.
 */
export interface MyTravelsTripSummaryDto {
  bookingId: string;
  status: BookingLifecycleStatus;
  statusBadgeLabel: string;
  pnrLocator: string;
  duffelOrderId: string;
  airlineName: string;
  airlineIata: string;
  flightNumber: string;
  originIata: string;
  originCity: string;
  destinationIata: string;
  destinationCity: string;
  departureIso: string;
  arrivalIso: string;
  cabinClass: string;
  seatDesignator: string;
  passengerFullNameIata: string;
  eTicketNumbers: string[];
  eTickets: ElectronicTicketDocument[];
  confirmationPdfUrl: string;
  iataBcbpPayload: string;
  totalPaidEur: number;
  netFareEur: number;
  otaMarkupEur: number;
  ancillariesEur: number;
  stripePaymentIntentId: string;
  stripeCaptureStatus: string;
  createdAtIso: string;
  updatedAtIso: string;
}

const DB_FILE_PATH = path.resolve(__dirname, "../../.bookings-store.json");
const bookingsById = new Map<string, PersistedFlightBooking>();

function loadPersistedDiskStore(): void {
  try {
    if (fs.existsSync(DB_FILE_PATH)) {
      const raw = fs.readFileSync(DB_FILE_PATH, "utf-8");
      const parsed: PersistedFlightBooking[] = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        for (const item of parsed) {
          bookingsById.set(item.bookingId, item);
        }
      }
    }
  } catch (err) {
    console.warn("[BookingPersistenceStore] Aviso ao carregar cache em disco:", err);
  }
}

function flushToDiskStore(): void {
  try {
    const all = Array.from(bookingsById.values());
    fs.writeFileSync(DB_FILE_PATH, JSON.stringify(all, null, 2), "utf-8");
  } catch (err) {
    console.warn("[BookingPersistenceStore] Aviso ao persistir reserva em disco:", err);
  }
}

// Inicializa o banco de dados na subida do serviço
loadPersistedDiskStore();

/**
 * Cria um registo inicial da reserva com status 'pending' assim que o PaymentIntent manual é autorizado.
 */
export function createPendingBookingRecord(params: {
  bookingId: string;
  userId: string;
  offerId: string;
  stripePaymentIntentId: string;
  netFareEur: number;
  otaMarkupEur: number;
  ancillariesEur: number;
  totalAuthorizedEur: number;
  itinerary: PersistedFlightBooking["itinerary"];
  passengers: SanitizedDuffelPassenger[];
}): PersistedFlightBooking {
  const nowIso = new Date().toISOString();
  const record: PersistedFlightBooking = {
    bookingId: params.bookingId,
    userId: params.userId,
    status: "pending",
    offerId: params.offerId,
    duffelOrderId: null,
    bookingReference: null,
    stripePaymentIntentId: params.stripePaymentIntentId,
    stripeCaptureStatus: "requires_capture",
    financials: {
      currency: "EUR",
      netFareEur: params.netFareEur,
      otaMarkupEur: params.otaMarkupEur,
      ancillariesEur: params.ancillariesEur,
      totalAuthorizedEur: params.totalAuthorizedEur,
      totalCapturedEur: 0,
      totalRefundedEur: 0,
    },
    itinerary: params.itinerary,
    passengers: params.passengers,
    eTickets: [],
    confirmationPdfUrl: null,
    iataBcbpPayload: null,
    auditTrail: [
      {
        timestampIso: nowIso,
        event: "BOOKING_PENDING_CREATED",
        details: `Reserva criada em estado 'pending'. Stripe PaymentIntent ${params.stripePaymentIntentId} autorizado (capture_method: manual) no valor de €${params.totalAuthorizedEur.toFixed(2)}.`,
      },
    ],
    createdAtIso: nowIso,
    updatedAtIso: nowIso,
  };

  bookingsById.set(record.bookingId, record);
  flushToDiskStore();
  return record;
}

/**
 * Atualiza a reserva para 'confirmed' após emissão bem-sucedida na Duffel e captura definitiva na Stripe.
 */
export function markBookingConfirmed(params: {
  bookingId: string;
  duffelOrderId: string;
  bookingReference: string;
  eTickets: ElectronicTicketDocument[];
  confirmationPdfUrl: string;
  iataBcbpPayload: string;
  capturedAmountEur: number;
}): PersistedFlightBooking | null {
  const existing = bookingsById.get(params.bookingId);
  if (!existing) return null;

  const nowIso = new Date().toISOString();
  existing.status = "confirmed";
  existing.duffelOrderId = params.duffelOrderId;
  existing.bookingReference = params.bookingReference;
  existing.stripeCaptureStatus = "captured";
  existing.financials.totalCapturedEur = params.capturedAmountEur;
  existing.eTickets = params.eTickets;
  existing.confirmationPdfUrl = params.confirmationPdfUrl;
  existing.iataBcbpPayload = params.iataBcbpPayload;
  existing.updatedAtIso = nowIso;
  existing.auditTrail.push({
    timestampIso: nowIso,
    event: "BOOKING_CONFIRMED_AND_CAPTURED",
    details: `Ordem Duffel ${params.duffelOrderId} (PNR ${params.bookingReference}) emitida e Stripe PaymentIntent ${existing.stripePaymentIntentId} capturado (€${params.capturedAmountEur.toFixed(2)}).`,
  });

  bookingsById.set(existing.bookingId, existing);
  flushToDiskStore();
  return existing;
}

/**
 * Atualiza a reserva para 'failed' quando a Duffel recusa a emissão e a autorização Stripe é cancelada.
 */
export function markBookingFailedAndHoldReleased(params: {
  bookingId: string;
  failureCode: string;
  failureReason: string;
  stripeCanceledSuccessfully: boolean;
}): PersistedFlightBooking | null {
  const existing = bookingsById.get(params.bookingId);
  if (!existing) return null;

  const nowIso = new Date().toISOString();
  existing.status = "failed";
  existing.stripeCaptureStatus = params.stripeCanceledSuccessfully ? "canceled" : "failed";
  existing.failureCode = params.failureCode;
  existing.failureReason = params.failureReason;
  existing.updatedAtIso = nowIso;
  existing.auditTrail.push({
    timestampIso: nowIso,
    event: "BOOKING_FAILED_HOLD_RELEASED",
    details: `Falha na emissão Duffel [${params.failureCode}]: ${params.failureReason}. Autorização Stripe ${existing.stripePaymentIntentId} cancelada=${params.stripeCanceledSuccessfully}.`,
  });

  bookingsById.set(existing.bookingId, existing);
  flushToDiskStore();
  return existing;
}

/**
 * Atualiza a reserva para 'refunded' após confirmação de cancelamento/reembolso via Webhook ou Pós-Venda.
 */
export function markBookingRefunded(params: {
  bookingIdOrOrderIdOrPaymentIntentId: string;
  refundedAmountEur: number;
  reason: string;
}): PersistedFlightBooking | null {
  const target = findBookingByAnyIdentifier(params.bookingIdOrOrderIdOrPaymentIntentId);
  if (!target) return null;

  const nowIso = new Date().toISOString();
  target.status = "refunded";
  target.stripeCaptureStatus = "refunded";
  target.financials.totalRefundedEur = params.refundedAmountEur;
  target.updatedAtIso = nowIso;
  target.auditTrail.push({
    timestampIso: nowIso,
    event: "BOOKING_REFUNDED",
    details: `Reserva reembolsada (€${params.refundedAmountEur.toFixed(2)}). Motivo: ${params.reason}`,
  });

  bookingsById.set(target.bookingId, target);
  flushToDiskStore();
  return target;
}

/**
 * Regista eventos assíncronos recebidos via Webhooks da Stripe ou da Duffel.
 */
export function appendBookingWebhookAuditEvent(params: {
  identifier: string;
  newStatus?: BookingLifecycleStatus;
  eventTitle: string;
  details: string;
  bookingReference?: string;
  eTickets?: ElectronicTicketDocument[];
}): PersistedFlightBooking | null {
  const target = findBookingByAnyIdentifier(params.identifier);
  if (!target) return null;

  const nowIso = new Date().toISOString();
  if (params.newStatus) {
    target.status = params.newStatus;
  }
  if (params.bookingReference) {
    target.bookingReference = params.bookingReference;
  }
  if (params.eTickets && params.eTickets.length > 0) {
    target.eTickets = params.eTickets;
  }
  target.updatedAtIso = nowIso;
  target.auditTrail.push({
    timestampIso: nowIso,
    event: params.eventTitle,
    details: params.details,
  });

  bookingsById.set(target.bookingId, target);
  flushToDiskStore();
  return target;
}

export function findBookingByAnyIdentifier(identifier: string): PersistedFlightBooking | null {
  if (!identifier) return null;
  const direct = bookingsById.get(identifier);
  if (direct) return direct;

  for (const booking of bookingsById.values()) {
    if (
      booking.duffelOrderId === identifier ||
      booking.stripePaymentIntentId === identifier ||
      booking.bookingReference === identifier ||
      booking.offerId === identifier
    ) {
      return booking;
    }
  }
  return null;
}

/**
 * Formata uma reserva persistida para exibição limpa na aba 'Minhas Viagens' do utilizador,
 * incluindo PNR/Localizador, números de e-ticket IATA e URL/metadados do PDF de confirmação.
 */
export function toMyTravelsSummaryDto(booking: PersistedFlightBooking): MyTravelsTripSummaryDto {
  const primaryPassenger = booking.passengers[0];
  const passengerFullNameIata = primaryPassenger
    ? `${primaryPassenger.family_name}/${primaryPassenger.given_name}`
    : "PASSENGER/VIP";

  const statusLabels: Record<BookingLifecycleStatus, string> = {
    pending: "PROCESSANDO EMISSÃO",
    confirmed: "CONFIRMADO • BILHETE EMITIDO",
    failed: "FALHA • LIMITE LIBERTADO",
    refunded: "REEMBOLSADO • ESTORNADO",
  };

  const pnr = booking.bookingReference || "PENDENTE";
  const pdfUrl =
    booking.confirmationPdfUrl ||
    `/api/flights/orders/${booking.duffelOrderId || booking.bookingId}/confirmation.pdf`;

  return {
    bookingId: booking.bookingId,
    status: booking.status,
    statusBadgeLabel: statusLabels[booking.status],
    pnrLocator: pnr,
    duffelOrderId: booking.duffelOrderId || "",
    airlineName: booking.itinerary.airlineName,
    airlineIata: booking.itinerary.airlineIata,
    flightNumber: booking.itinerary.flightNumber,
    originIata: booking.itinerary.originIata,
    originCity: booking.itinerary.originCity,
    destinationIata: booking.itinerary.destinationIata,
    destinationCity: booking.itinerary.destinationCity,
    departureIso: booking.itinerary.departureIso,
    arrivalIso: booking.itinerary.arrivalIso,
    cabinClass: booking.itinerary.cabinClass,
    seatDesignator: booking.itinerary.assignedSeat,
    passengerFullNameIata,
    eTicketNumbers: booking.eTickets.map((t) => t.ticketNumber),
    eTickets: booking.eTickets,
    confirmationPdfUrl: pdfUrl,
    iataBcbpPayload: booking.iataBcbpPayload || "",
    totalPaidEur:
      booking.status === "confirmed"
        ? booking.financials.totalCapturedEur
        : booking.financials.totalAuthorizedEur,
    netFareEur: booking.financials.netFareEur,
    otaMarkupEur: booking.financials.otaMarkupEur,
    ancillariesEur: booking.financials.ancillariesEur,
    stripePaymentIntentId: booking.stripePaymentIntentId,
    stripeCaptureStatus: booking.stripeCaptureStatus,
    createdAtIso: booking.createdAtIso,
    updatedAtIso: booking.updatedAtIso,
  };
}

/**
 * Lista todas as viagens de um utilizador ordenadas da mais recente para a mais antiga.
 */
export function listUserBookingsForMyTravels(userId?: string): MyTravelsTripSummaryDto[] {
  const all = Array.from(bookingsById.values());
  const filtered = userId ? all.filter((b) => b.userId === userId) : all;
  return filtered
    .sort((a, b) => b.createdAtIso.localeCompare(a.createdAtIso))
    .map(toMyTravelsSummaryDto);
}
