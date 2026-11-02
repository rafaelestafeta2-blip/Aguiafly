import { Duffel } from "@duffel/api";
import { calculateOfferMarkup, MarkupRuleConfig } from "../middleware/markupMiddleware";

const duffel = new Duffel({
  token: process.env.DUFFEL_API_TOKEN || "",
});

export interface SearchFlightsInput {
  origin: string;
  destination: string;
  departureDate: string;
  cabinClass: "economy" | "premium_economy" | "business" | "first";
  passengersCount: number;
}

/**
 * Cria um Offer Request na Duffel API (NDC Managed Content) em EUR
 * e aplica o middleware dinâmico de Markup antes de retornar ao app.
 */
export async function searchFlightsWithMarkup(
  input: SearchFlightsInput,
  markupConfig?: MarkupRuleConfig
) {
  const passengers = Array.from({ length: input.passengersCount }, () => ({
    type: "adult" as const,
  }));

  const offerRequest = await duffel.offerRequests.create({
    slices: [
      {
        origin: input.origin,
        destination: input.destination,
        departure_date: input.departureDate,
      } as any,
    ],
    passengers,
    cabin_class: input.cabinClass,
    return_offers: true,
  });

  const enrichedOffers = (offerRequest.data.offers || []).map((offer) => {
    const netAmountEur = parseFloat(offer.total_amount);
    const pricing = calculateOfferMarkup(netAmountEur, markupConfig);
    return {
      ...offer,
      aether_pricing: pricing,
    };
  });

  return {
    offerRequestId: offerRequest.data.id,
    offers: enrichedOffers,
  };
}

/**
 * Busca o mapa de assentos real de uma oferta na Duffel API.
 */
export async function getOfferSeatMaps(offerId: string) {
  const seatMaps = await duffel.seatMaps.get({ offer_id: offerId });
  return seatMaps.data;
}

/**
 * Emite o bilhete (Order) na Duffel utilizando o saldo balance da OTA
 * pelo valor NETO da tarifa após confirmação do pagamento Stripe.
 */
export async function createDuffelOrder(params: {
  selectedOfferId: string;
  netAmountEur: string;
  passengerId: string;
  givenName: string;
  familyName: string;
  email: string;
  phoneNumber: string;
  bornOn: string;
}) {
  const order = await duffel.orders.create({
    type: "instant",
    selected_offers: [params.selectedOfferId],
    payments: [
      {
        type: "balance",
        currency: "EUR",
        amount: params.netAmountEur,
      },
    ],
    passengers: [
      {
        id: params.passengerId,
        given_name: params.givenName,
        family_name: params.familyName,
        email: params.email,
        phone_number: params.phoneNumber,
        born_on: params.bornOn,
        title: "mr",
        gender: "m",
      },
    ],
  });

  return order.data;
}

/**
 * Consulta o endpoint Places Suggestions da Duffel API (GET /places/suggestions)
 * para auto-complete inteligente por cidade, país, nome do aeroporto ou código IATA.
 */
export async function searchPlacesSuggestions(query: string) {
  const response = await duffel.suggestions.list({ query: query.trim() });
  return (response.data || [])
    .filter((place: any) => Boolean(place.iata_code || place.city?.iata_code))
    .map((place: any) => {
      const iataCode = (place.iata_code || place.city?.iata_code || "").toUpperCase();
      const cityName = place.city_name || place.city?.name || place.name;
      const airportName = place.name;
      const countryName = place.country_name || "";
      return {
        id: place.id,
        iataCode,
        cityName,
        airportName,
        countryName,
        dropdownDisplayLabel: `${cityName} - ${airportName} (${iataCode})`,
      };
    });
}

export async function searchEuropeanNdcOffers(params: {
  originIata: string;
  destinationIata: string;
  departureDate: string;
  cabinClass: "economy" | "premium_economy" | "business" | "first";
}) {
  try {
    const res = await searchFlightsWithMarkup({
      origin: params.originIata,
      destination: params.destinationIata,
      departureDate: params.departureDate,
      cabinClass: params.cabinClass,
      passengersCount: 1,
    });
    return res.offers;
  } catch {
    return [];
  }
}


