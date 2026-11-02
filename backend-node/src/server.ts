import express from "express";
import cors from "cors";
import dotenv from "dotenv";
import path from "path";
import { flightRouter } from "./routes/flightRoutes";

dotenv.config();
dotenv.config({ path: path.resolve(__dirname, "../../.env") });
dotenv.config({ path: path.resolve(__dirname, "../.env") });
import { priceAlertRouter } from "./routes/priceAlertRoutes";
import { startPriceAlertCronDaemon } from "./services/priceAlertCronService";

dotenv.config();

const app = express();
app.use(cors());
app.use(express.json());

app.use("/api/flights", flightRouter);
app.use("/api/alerts", priceAlertRouter);

// Start background cron routine to monitor promotional flight fares and send FCM push alerts
startPriceAlertCronDaemon();

const PORT = process.env.PORT || 4000;
app.listen(PORT, () => {
  console.log(`AguiaFly OTA Backend & FCM Price Alert Cron listening on port ${PORT}`);
});
