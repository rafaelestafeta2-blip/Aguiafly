import { Router, Request, Response } from "express";
import {
  listUserPriceAlerts,
  registerRoutePriceAlert,
  runPromotionalPriceAlertCronSweep,
} from "../services/priceAlertCronService";

export const priceAlertRouter = Router();

/**
 * POST /api/alerts/register
 * Registers a route price alert with the user's FCM token.
 */
priceAlertRouter.post("/register", (req: Request, res: Response) => {
  try {
    const {
      id,
      userId,
      originIata,
      destinationIata,
      cabinClass = "business",
      targetMaxPriceEur,
      baselinePriceEur,
      fcmToken,
    } = req.body;

    if (!userId || !originIata || !destinationIata || !fcmToken) {
      return res.status(400).json({ error: "Missing required price alert fields." });
    }

    const job = registerRoutePriceAlert({
      id: id || `alert_${Date.now()}`,
      userId,
      originIata: String(originIata).toUpperCase().slice(0, 3),
      destinationIata: String(destinationIata).toUpperCase().slice(0, 3),
      cabinClass,
      targetMaxPriceEur: Number(targetMaxPriceEur || 300),
      baselinePriceEur: Number(baselinePriceEur || 360),
      lastObservedPriceEur: Number(baselinePriceEur || 360),
      fcmToken: String(fcmToken),
      isActive: true,
    });

    return res.status(201).json({ status: "registered", alert: job });
  } catch (err: any) {
    return res.status(500).json({ error: err.message });
  }
});

/**
 * GET /api/alerts/:userId
 * Lists all active price alerts for a user.
 */
priceAlertRouter.get("/:userId", (req: Request, res: Response) => {
  const alerts = listUserPriceAlerts(req.params.userId);
  return res.json({ count: alerts.length, alerts });
});

/**
 * POST /api/alerts/cron-check
 * Triggers an immediate promotional fare sweep and dispatches FCM push notifications.
 */
priceAlertRouter.post("/cron-check", async (_req: Request, res: Response) => {
  try {
    const result = await runPromotionalPriceAlertCronSweep();
    return res.json({
      status: "completed",
      checkedAlerts: result.checkedCount,
      notificationsSent: result.triggeredNotifications.length,
      dispatchedLogs: result.triggeredNotifications,
    });
  } catch (err: any) {
    return res.status(500).json({ error: err.message });
  }
});
