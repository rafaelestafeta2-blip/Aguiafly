import express from "express";
import cors from "cors";
import dotenv from "dotenv";
import path from "path";
import { flightRouter } from "./routes/flightRoutes";

dotenv.config();
dotenv.config({ path: path.resolve(__dirname, "../../.env") });
dotenv.config({ path: path.resolve(__dirname, "../.env") });
import { priceAlertRouter } from "./routes/priceAlertRoutes";
import { webhookRouter } from "./routes/webhookRoutes";
import { startPriceAlertCronDaemon } from "./services/priceAlertCronService";

dotenv.config();

const app = express();
app.use(cors());
app.use(
  express.json({
    verify: (req: any, _res, buf) => {
      req.rawBody = buf;
    },
  })
);

app.use("/api/flights", flightRouter);
app.use("/api/alerts", priceAlertRouter);
app.use("/api/webhooks", webhookRouter);

// Start background cron routine to monitor promotional flight fares and send FCM push alerts
startPriceAlertCronDaemon();

const PORT = process.env.PORT || 4000;
app.listen(PORT, () => {
  console.log(`AguiaFly OTA Backend, Atomic Checkout & Webhooks listening on port ${PORT}`);
});
