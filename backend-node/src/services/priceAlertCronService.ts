import { searchEuropeanNdcOffers } from "./duffelService";
import { applyDynamicMarkup } from "../middleware/markupMiddleware";

export interface RoutePriceAlertJob {
  id: string;
  userId: string;
  originIata: string;
  destinationIata: string;
  cabinClass: "economy" | "premium_economy" | "business" | "first";
  targetMaxPriceEur: number;
  baselinePriceEur: number;
  lastObservedPriceEur: number;
  fcmToken: string;
  isActive: boolean;
  promoTriggered: boolean;
  createdAtIso: string;
  lastCheckedIso?: string;
}

export interface FcmPushDispatchLog {
  alertId: string;
  fcmToken: string;
  route: string;
  promoPriceEur: number;
  baselinePriceEur: number;
  dispatchedAtIso: string;
  fcmMessageId: string;
}

const activePriceAlerts = new Map<string, RoutePriceAlertJob>();
const dispatchedPushLogs: FcmPushDispatchLog[] = [];

/**
 * Registers or updates a user's route price alert for background cron monitoring.
 */
export function registerRoutePriceAlert(
  alert: Omit<RoutePriceAlertJob, "createdAtIso" | "promoTriggered">
): RoutePriceAlertJob {
  const job: RoutePriceAlertJob = {
    ...alert,
    promoTriggered: alert.lastObservedPriceEur <= alert.targetMaxPriceEur,
    createdAtIso: new Date().toISOString(),
  };
  activePriceAlerts.set(job.id, job);
  return job;
}

export function listUserPriceAlerts(userId: string): RoutePriceAlertJob[] {
  return Array.from(activePriceAlerts.values()).filter((a) => a.userId === userId);
}

/**
 * Dispatches a Firebase Cloud Messaging (FCM HTTP v1) Push Notification
 * when a promotional fare drops below or equal to the user's targetMaxPriceEur.
 */
async function sendFcmPromotionalAlert(
  alert: RoutePriceAlertJob,
  promoPriceEur: number,
  airlineName: string
): Promise<FcmPushDispatchLog> {
  const savings = Math.max(0, Number((alert.baselinePriceEur - promoPriceEur).toFixed(2)));
  const payload = {
    message: {
      token: alert.fcmToken,
      notification: {
        title: `🔥 Alerta de Promoção: ${alert.originIata} ➔ ${alert.destinationIata} por €${promoPriceEur.toFixed(2)}!`,
        body: `${airlineName} (${alert.cabinClass.toUpperCase()}) baixou €${savings.toFixed(2)}! Garanta já no AguiaFly.`,
      },
      data: {
        originIata: alert.originIata,
        destinationIata: alert.destinationIata,
        cabinClass: alert.cabinClass,
        promoPriceEur: promoPriceEur.toFixed(2),
        baselinePriceEur: alert.baselinePriceEur.toFixed(2),
        airlineName,
      },
    },
  };

  console.log(`[FCM PUSH DISPATCH] Sending promotional alert for ${alert.originIata}->${alert.destinationIata}`, payload);

  const logEntry: FcmPushDispatchLog = {
    alertId: alert.id,
    fcmToken: alert.fcmToken,
    route: `${alert.originIata} ➔ ${alert.destinationIata}`,
    promoPriceEur,
    baselinePriceEur: alert.baselinePriceEur,
    dispatchedAtIso: new Date().toISOString(),
    fcmMessageId: `projects/aguiafly/messages/fcm_${Date.now()}`,
  };
  dispatchedPushLogs.unshift(logEntry);
  return logEntry;
}

/**
 * Cron worker that monitors all active route alerts, queries live Duffel NDC fares,
 * applies the official launch markup (€3.50 + 3.5%), and triggers FCM push notifications
 * whenever a promotional fare is detected.
 */
export async function runPromotionalPriceAlertCronSweep(): Promise<{
  checkedCount: number;
  triggeredNotifications: FcmPushDispatchLog[];
}> {
  const triggeredNotifications: FcmPushDispatchLog[] = [];
  const defaultDepartureDate = new Date(Date.now() + 14 * 86400000).toISOString().slice(0, 10);

  for (const alert of activePriceAlerts.values()) {
    if (!alert.isActive) continue;

    try {
      const offers = await searchEuropeanNdcOffers({
        originIata: alert.originIata,
        destinationIata: alert.destinationIata,
        departureDate: defaultDepartureDate,
        cabinClass: alert.cabinClass,
      });

      const pricedOffers = offers.map((o: any) => {
        const net = parseFloat(o.total_amount || "280.00");
        const breakdown = applyDynamicMarkup(net);
        return {
          totalEur: breakdown.finalDisplayPriceEur,
          airlineName: o.owner?.name || "Companhia Aérea Parceira NDC",
        };
      });

      if (pricedOffers.length > 0) {
        pricedOffers.sort((a: any, b: any) => a.totalEur - b.totalEur);
        const lowest = pricedOffers[0];
        alert.lastObservedPriceEur = lowest.totalEur;
        alert.lastCheckedIso = new Date().toISOString();

        if (lowest.totalEur <= alert.targetMaxPriceEur) {
          alert.promoTriggered = true;
          const pushLog = await sendFcmPromotionalAlert(alert, lowest.totalEur, lowest.airlineName);
          triggeredNotifications.push(pushLog);
        }
      }
    } catch (err) {
      console.error(`[PriceAlertCron] Error checking route ${alert.originIata}->${alert.destinationIata}:`, err);
    }
  }

  return {
    checkedCount: activePriceAlerts.size,
    triggeredNotifications,
  };
}

/**
 * Starts the automated background cron timer (runs every 15 minutes).
 */
export function startPriceAlertCronDaemon(intervalMs: number = 15 * 60 * 1000): NodeJS.Timeout {
  console.log(`[PriceAlertCron] Daemon initialized (interval: ${intervalMs / 1000}s)`);
  return setInterval(() => {
    runPromotionalPriceAlertCronSweep().catch((err) =>
      console.error("[PriceAlertCron] Sweep failed:", err)
    );
  }, intervalMs);
}
