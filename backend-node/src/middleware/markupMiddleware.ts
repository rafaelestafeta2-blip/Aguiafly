export const ANCILLARY_TRANSPARENT_MARGIN_EUR = 2.0;

export interface MarkupRuleConfig {
  fixedFeeEur: number; // Padrão de lançamento: €3.50 fixo
  percentageFee: number; // Padrão de lançamento: 3.5% sobre a tarifa neta
  ancillaryServiceMarginEur: number; // Padrão de lançamento: €2.00 sobre bagagem ou assento premium
  ancillaryFeesEur: {
    seatSelection: number; // Custo total ao cliente (inclui €2.00 de margem de serviço)
    extraBaggage: number; // Custo total ao cliente (inclui €2.00 de margem de serviço)
    priorityBoarding: number;
  };
}

export interface SelectedAncillariesInput {
  seatDesignator?: string; // "AUTO" (padrão gratuito €0.00) ou assento premium ex: "2A"
  includeExtraBaggage23Kg?: boolean; // false por padrão
  includePriorityLounge?: boolean; // false por padrão
}

export interface PricedOfferBreakdown {
  netAmountEur: number;
  fixedMarkupEur: number;
  percentageMarkupEur: number;
  unifiedManagementFeeEur: number;
  markupAmountEur: number; // Alias para compatibilidade com rotas existentes
  displayBaseFareEur: number;
  seatAirlineBaseEur: number;
  seatServiceMarginEur: number;
  seatFeeEur: number;
  baggageAirlineBaseEur: number;
  baggageServiceMarginEur: number;
  baggageFeeEur: number;
  priorityBoardingFeeEur: number;
  ancillaryServiceMarginTotalEur: number;
  totalAncillariesEur: number;
  estimatedGatewayAndDuffelFeeEur: number;
  estimatedNetProfitEur: number;
  grandTotalEur: number;
  currency: "EUR";
}

/**
 * Fórmula Oficial de Lançamento AguiaFly:
 * 1. Taxa de Gestão Unificada (Markup): €3.50 (fixo) + 3.5% sobre o valor da tarifa neta do voo.
 *    Garante cobertura integral dos custos de processamento Stripe e Duffel Order Fee com lucro líquido positivo.
 * 2. Serviços Opcionais (Ancillaries): Margem de serviço transparente de €2.00 sobre bagagem despachada
 *    ou seleção de assentos premium.
 */
export const defaultMarkupConfig: MarkupRuleConfig = {
  fixedFeeEur: 3.5,
  percentageFee: 3.5,
  ancillaryServiceMarginEur: ANCILLARY_TRANSPARENT_MARGIN_EUR,
  ancillaryFeesEur: {
    seatSelection: 14.0, // €12.00 custo base cia aérea + €2.00 margem de serviço AguiaFly
    extraBaggage: 22.0, // €20.00 custo base cia aérea + €2.00 margem de serviço AguiaFly
    priorityBoarding: 16.0,
  },
};

export function calculateOfferMarkup(
  netAmountEur: number,
  config: MarkupRuleConfig = defaultMarkupConfig,
  ancillaries: SelectedAncillariesInput = {
    seatDesignator: "AUTO",
    includeExtraBaggage23Kg: false,
    includePriorityLounge: false,
  }
): PricedOfferBreakdown {
  const safeNet = Number(Math.max(0, netAmountEur).toFixed(2));
  const fixedMarkupEur = Number(Math.max(0, config.fixedFeeEur).toFixed(2));
  const percentageMarkupEur = Number(
    (safeNet * (Math.max(0, config.percentageFee) / 100)).toFixed(2)
  );
  const unifiedManagementFeeEur = Number((fixedMarkupEur + percentageMarkupEur).toFixed(2));
  const displayBaseFareEur = Number((safeNet + unifiedManagementFeeEur).toFixed(2));

  const marginPerAncillary = Number(
    (config.ancillaryServiceMarginEur ?? ANCILLARY_TRANSPARENT_MARGIN_EUR).toFixed(2)
  );

  const seatCode = (ancillaries.seatDesignator || "AUTO").trim().toUpperCase();
  const isPaidSeat = seatCode.length > 0 && seatCode !== "AUTO";

  const seatFeeEur = isPaidSeat ? Number(config.ancillaryFeesEur.seatSelection.toFixed(2)) : 0.0;
  const seatServiceMarginEur = isPaidSeat ? Math.min(marginPerAncillary, seatFeeEur) : 0.0;
  const seatAirlineBaseEur = isPaidSeat
    ? Number(Math.max(0, seatFeeEur - seatServiceMarginEur).toFixed(2))
    : 0.0;

  const baggageFeeEur = ancillaries.includeExtraBaggage23Kg
    ? Number(config.ancillaryFeesEur.extraBaggage.toFixed(2))
    : 0.0;
  const baggageServiceMarginEur = ancillaries.includeExtraBaggage23Kg
    ? Math.min(marginPerAncillary, baggageFeeEur)
    : 0.0;
  const baggageAirlineBaseEur = ancillaries.includeExtraBaggage23Kg
    ? Number(Math.max(0, baggageFeeEur - baggageServiceMarginEur).toFixed(2))
    : 0.0;

  const priorityBoardingFeeEur = ancillaries.includePriorityLounge
    ? Number(config.ancillaryFeesEur.priorityBoarding.toFixed(2))
    : 0.0;

  const ancillaryServiceMarginTotalEur = Number(
    (seatServiceMarginEur + baggageServiceMarginEur).toFixed(2)
  );
  const totalAncillariesEur = Number(
    (seatFeeEur + baggageFeeEur + priorityBoardingFeeEur).toFixed(2)
  );
  const grandTotalEur = Number((displayBaseFareEur + totalAncillariesEur).toFixed(2));

  // Estimativa de cobertura de custos operacionais (Stripe EU ~1.5% + €0.25 e Duffel Order Fee ~€2.00)
  const estimatedGatewayAndDuffelFeeEur = Number(
    (2.0 + 0.25 + grandTotalEur * 0.015).toFixed(2)
  );
  const totalGrossMargin = unifiedManagementFeeEur + ancillaryServiceMarginTotalEur;
  const estimatedNetProfitEur = Number(
    Math.max(0.5, totalGrossMargin - estimatedGatewayAndDuffelFeeEur).toFixed(2)
  );

  return {
    netAmountEur: safeNet,
    fixedMarkupEur,
    percentageMarkupEur,
    unifiedManagementFeeEur,
    markupAmountEur: unifiedManagementFeeEur,
    displayBaseFareEur,
    seatAirlineBaseEur,
    seatServiceMarginEur,
    seatFeeEur,
    baggageAirlineBaseEur,
    baggageServiceMarginEur,
    baggageFeeEur,
    priorityBoardingFeeEur,
    ancillaryServiceMarginTotalEur,
    totalAncillariesEur,
    estimatedGatewayAndDuffelFeeEur,
    estimatedNetProfitEur,
    grandTotalEur,
    currency: "EUR",
  };
}

export function applyDynamicMarkup(netAmountEur: number) {
  const breakdown = calculateOfferMarkup(netAmountEur);
  return {
    ...breakdown,
    finalDisplayPriceEur: breakdown.grandTotalEur,
  };
}

