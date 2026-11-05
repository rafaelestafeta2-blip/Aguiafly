/**
 * Módulo de Validação e Sanitização Estrita de Passageiros (Padrão IATA / GDS / Duffel NDC)
 *
 * Responsabilidades:
 * 1. Validar todos os campos obrigatórios exigidos pelas companhias aéreas:
 *    given_name, family_name, born_on, gender, title, phone_number, email.
 * 2. Remover acentos, cedilhas (ç -> c) e caracteres especiais dos nomes para cumprir
 *    o padrão estrito de caracteres ASCII dos sistemas GDS/IATA (Amadeus, Sabre, Duffel NDC).
 * 3. Garantir a inclusão e validação de documentos de viagem (identity_documents)
 *    quando exigidos para rotas internacionais (passaporte com validade futura e país emissor ISO-3166-1 alpha-2).
 */

export type DuffelGender = "m" | "f";
export type DuffelTitle = "mr" | "ms" | "mrs" | "miss" | "dr";

export type DuffelIdentityDocumentType = "passport" | "identity_card" | "national_identity_card";

export interface RawIdentityDocumentInput {
  type?: DuffelIdentityDocumentType | string;
  unique_identifier: string;
  expires_on: string; // YYYY-MM-DD
  issuing_country_code: string; // ISO 3166-1 alpha-2 (ex: "PT", "BR", "ES")
}

export interface RawPassengerInput {
  id: string;
  given_name: string;
  family_name: string;
  born_on: string; // YYYY-MM-DD
  gender?: string;
  title?: string;
  phone_number: string; // Formato E.164 (ex: +351912345678)
  email: string;
  identity_documents?: RawIdentityDocumentInput[];
}

export interface SanitizedIdentityDocument {
  type: "passport" | "identity_card";
  unique_identifier: string;
  expires_on: string;
  issuing_country_code: string;
}

export interface SanitizedDuffelPassenger {
  id: string;
  given_name: string;
  family_name: string;
  born_on: string;
  gender: DuffelGender;
  title: DuffelTitle;
  phone_number: string;
  email: string;
  identity_documents?: SanitizedIdentityDocument[];
}

export class PassengerValidationError extends Error {
  public readonly code = "INVALID_PASSENGER_DATA";
  public readonly field: string;

  constructor(field: string, message: string) {
    super(message);
    this.name = "PassengerValidationError";
    this.field = field;
  }
}

/**
 * Remove acentos, cedilhas, til, tremas e caracteres especiais de nomes próprios,
 * convertendo para o alfabeto latino básico exigido pelos sistemas IATA/GDS.
 *
 * Exemplo:
 *   "João Gonçalves d'Ávila" -> "JOAO GONCALVES DAVILA"
 *   "Conceição Assunção-Müller" -> "CONCEICAO ASSUNCAO MULLER"
 */
export function sanitizeIataGdsName(rawName: string): string {
  if (!rawName || typeof rawName !== "string") {
    return "";
  }

  return rawName
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "") // Remove diacríticos combinados (acentos, til, trema)
    .replace(/[çÇ]/g, "C")
    .replace(/[ñÑ]/g, "N")
    .replace(/ß/g, "SS")
    .replace(/[æÆ]/g, "AE")
    .replace(/[œŒ]/g, "OE")
    .replace(/[øØ]/g, "O")
    .replace(/[^a-zA-Z\s-]/g, " ") // Remove apóstrofos, pontos, símbolos e números
    .replace(/-/g, " ") // Converte hífens em espaço simples conforme recomendação GDS
    .replace(/\s+/g, " ")
    .trim()
    .toUpperCase();
}

/**
 * Sanitiza e valida o número de telefone no padrão internacional E.164 (+[código país][número]),
 * exigido pela API da Duffel para notificações operacionais de voo (IATA).
 */
export function sanitizeE164PhoneNumber(rawPhone: string, defaultCountryDialCode = "+351"): string {
  if (!rawPhone || typeof rawPhone !== "string") {
    throw new PassengerValidationError("phone_number", "O telefone do passageiro é obrigatório.");
  }

  const trimmed = rawPhone.trim();
  const hasPlus = trimmed.startsWith("+");
  const digitsOnly = trimmed.replace(/\D/g, "");

  if (digitsOnly.length < 7 || digitsOnly.length > 15) {
    throw new PassengerValidationError(
      "phone_number",
      `Telefone inválido (${rawPhone}). Informe o número no padrão internacional E.164 (ex: +351912345678 ou +5511999999999).`
    );
  }

  if (hasPlus) {
    return `+${digitsOnly}`;
  }

  const cleanDefaultCode = defaultCountryDialCode.replace(/\D/g, "");
  if (digitsOnly.startsWith(cleanDefaultCode)) {
    return `+${digitsOnly}`;
  }

  return `+${cleanDefaultCode}${digitsOnly}`;
}

/**
 * Valida formato de data ISO-8601 (YYYY-MM-DD) e retorna o objeto Date UTC correspondente.
 */
/**
 * Normaliza formatos comuns (DD/MM/YYYY, DD-MM-YYYY) para YYYY-MM-DD se necessário e
 * valida o formato de data ISO-8601 (YYYY-MM-DD), retornando a string normalizada e o objeto Date UTC.
 */
export function normalizeToStrictIsoDateString(rawDateStr: string): string {
  const clean = (rawDateStr || "").trim();
  if (/^\d{4}-\d{2}-\d{2}$/.test(clean)) {
    return clean;
  }
  const ddmmyyyy = clean.match(/^(\d{1,2})[/.-](\d{1,2})[/.-](\d{4})$/);
  if (ddmmyyyy) {
    const [, d, m, y] = ddmmyyyy;
    return `${y}-${m.padStart(2, "0")}-${d.padStart(2, "0")}`;
  }
  const yyyymmdd = clean.match(/^(\d{4})[/.-](\d{1,2})[/.-](\d{1,2})$/);
  if (yyyymmdd) {
    const [, y, m, d] = yyyymmdd;
    return `${y}-${m.padStart(2, "0")}-${d.padStart(2, "0")}`;
  }
  if (clean.length >= 10 && /^\d{4}-\d{2}-\d{2}$/.test(clean.slice(0, 10))) {
    return clean.slice(0, 10);
  }
  return clean;
}

function parseStrictIsoDate(dateStr: string, fieldName: string): Date {
  const normalized = normalizeToStrictIsoDateString(dateStr);
  const isoRegex = /^\d{4}-\d{2}-\d{2}$/;
  if (!normalized || !isoRegex.test(normalized)) {
    throw new PassengerValidationError(
      fieldName,
      `O campo '${fieldName}' deve estar no formato estrito YYYY-MM-DD (recebido: '${dateStr}').`
    );
  }

  const parsed = new Date(`${normalized}T00:00:00Z`);
  if (Number.isNaN(parsed.getTime())) {
    throw new PassengerValidationError(fieldName, `Data inválida informada em '${fieldName}': ${dateStr}.`);
  }
  return parsed;
}

/**
 * Normaliza o título e o género para os valores estritos aceitos pela Duffel API v2 ("m" | "f").
 */
function normalizeGender(rawGender?: string): DuffelGender {
  const cleaned = (rawGender || "m").trim().toLowerCase();
  if (cleaned === "f" || cleaned === "female" || cleaned === "feminino") {
    return "f";
  }
  if (cleaned === "m" || cleaned === "male" || cleaned === "masculino") {
    return "m";
  }
  throw new PassengerValidationError("gender", "O género do passageiro deve ser estritamente 'm' ou 'f'.");
}

function normalizeTitle(rawTitle?: string, gender: DuffelGender = "m"): DuffelTitle {
  const cleaned = (rawTitle || "").trim().toLowerCase().replace(/\./g, "");
  const allowed: DuffelTitle[] = ["mr", "ms", "mrs", "miss", "dr"];
  if (allowed.includes(cleaned as DuffelTitle)) {
    return cleaned as DuffelTitle;
  }
  return gender === "f" ? "ms" : "mr";
}

function normalizeIdentityDocumentType(rawType?: string): "passport" | "identity_card" {
  const cleaned = (rawType || "passport").trim().toLowerCase();
  if (
    cleaned === "identity_card" ||
    cleaned === "national_identity_card" ||
    cleaned === "citizen_card" ||
    cleaned === "cc" ||
    cleaned === "id"
  ) {
    return "identity_card";
  }
  return "passport";
}

/**
 * Verifica se uma rota aérea é internacional comparando os códigos IATA/países de origem e destino.
 */
export function isInternationalRouteByIata(originIata: string, destinationIata: string): boolean {
  const airportCountryMap: Record<string, string> = {
    LIS: "PT",
    OPO: "PT",
    FAO: "PT",
    FNC: "PT",
    PDL: "PT",
    TER: "PT",
    MAD: "ES",
    BCN: "ES",
    IBZ: "ES",
    AGP: "ES",
    PMI: "ES",
    CDG: "FR",
    ORY: "FR",
    NCE: "FR",
    LHR: "GB",
    LGW: "GB",
    STN: "GB",
    DUB: "IE",
    FCO: "IT",
    MXP: "IT",
    VCE: "IT",
    ZRH: "CH",
    GVA: "CH",
    AMS: "NL",
    BRU: "BE",
    FRA: "DE",
    MUC: "DE",
    BER: "DE",
    VIE: "AT",
    ATH: "GR",
    PRG: "CZ",
    CPH: "DK",
    DXB: "AE",
    GRU: "BR",
    GIG: "BR",
    VCP: "BR",
    CNF: "BR",
    BSB: "BR",
    SSA: "BR",
    REC: "BR",
    FOR: "BR",
    JFK: "US",
    EWR: "US",
    MIA: "US",
    LAX: "US",
  };

  const orig = (originIata || "").trim().toUpperCase();
  const dest = (destinationIata || "").trim().toUpperCase();
  const origCountry = airportCountryMap[orig];
  const destCountry = airportCountryMap[dest];

  if (origCountry && destCountry) {
    return origCountry !== destCountry;
  }
  // Por segurança (compliance APIS/IATA), se o aeroporto não estiver mapeado e origem != destino, trata como internacional
  return orig !== dest;
}

/**
 * Valida e sanitiza todos os dados obrigatórios do passageiro e seus documentos (APIS / Passaporte / Cartão de Cidadão)
 * antes de enviar para o endpoint POST /air/orders da Duffel.
 */
export function sanitizeAndValidatePassenger(
  raw: RawPassengerInput,
  options: {
    requireIdentityDocument: boolean;
    flightDepartureDateIso?: string;
  }
): SanitizedDuffelPassenger {
  if (!raw.id || !raw.id.trim()) {
    throw new PassengerValidationError("id", "O identificador do passageiro na oferta (passenger_id) é obrigatório.");
  }

  const sanitizedGivenName = sanitizeIataGdsName(raw.given_name);
  if (sanitizedGivenName.length < 2) {
    throw new PassengerValidationError(
      "given_name",
      "O primeiro nome do passageiro (given_name) é obrigatório e deve conter pelo menos 2 letras válidas."
    );
  }

  const sanitizedFamilyName = sanitizeIataGdsName(raw.family_name);
  if (sanitizedFamilyName.length < 2) {
    throw new PassengerValidationError(
      "family_name",
      "O apelido/sobrenome do passageiro (family_name) é obrigatório e deve conter pelo menos 2 letras válidas."
    );
  }

  // Validação e formatação da data de nascimento (born_on em YYYY-MM-DD)
  const normalizedBornOn = normalizeToStrictIsoDateString(raw.born_on);
  const bornDate = parseStrictIsoDate(normalizedBornOn, "born_on");
  const now = new Date();
  if (bornDate >= now) {
    throw new PassengerValidationError("born_on", "A data de nascimento (born_on) deve ser anterior à data atual.");
  }

  // Validação de e-mail
  const cleanEmail = (raw.email || "").trim().toLowerCase();
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(cleanEmail)) {
    throw new PassengerValidationError("email", `Endereço de e-mail inválido: '${raw.email}'.`);
  }

  // Sanitização de telefone E.164 (+351...)
  const sanitizedPhone = sanitizeE164PhoneNumber(raw.phone_number);

  // Normalização estrita de género ("m" | "f") e título IATA
  const gender = normalizeGender(raw.gender);
  const title = normalizeTitle(raw.title, gender);

  // Validação e sanitização de documentos de identificação (identity_documents)
  const rawDocs = (raw.identity_documents || []).filter(
    (d) => d && typeof d.unique_identifier === "string" && d.unique_identifier.trim().length > 0
  );
  if (options.requireIdentityDocument && rawDocs.length === 0) {
    throw new PassengerValidationError(
      "identity_documents",
      "Esta rota exige os dados de Passaporte ou Documento de Identificação (identity_documents)."
    );
  }

  let sanitizedDocs: SanitizedIdentityDocument[] | undefined = undefined;

  if (rawDocs.length > 0) {
    const referenceFlightDate = options.flightDepartureDateIso
      ? parseStrictIsoDate(options.flightDepartureDateIso.slice(0, 10), "flightDepartureDateIso")
      : now;

    sanitizedDocs = rawDocs.map((doc, index) => {
      const cleanDocNumber = (doc.unique_identifier || "")
        .normalize("NFD")
        .replace(/[^a-zA-Z0-9]/g, "")
        .toUpperCase();

      if (cleanDocNumber.length < 5 || cleanDocNumber.length > 20) {
        throw new PassengerValidationError(
          `identity_documents[${index}].unique_identifier`,
          "O número do passaporte/documento deve conter entre 5 e 20 caracteres alfanuméricos."
        );
      }

      const normalizedExpiresOn = normalizeToStrictIsoDateString(doc.expires_on);
      const expiresDate = parseStrictIsoDate(
        normalizedExpiresOn,
        `identity_documents[${index}].expires_on`
      );
      if (expiresDate <= referenceFlightDate) {
        throw new PassengerValidationError(
          `identity_documents[${index}].expires_on`,
          `O passaporte/documento (${cleanDocNumber}) expira em ${normalizedExpiresOn}, antes ou na data do voo.`
        );
      }

      const cleanCountry = (doc.issuing_country_code || "PT")
        .trim()
        .toUpperCase()
        .replace(/[^A-Z]/g, "");
      if (cleanCountry.length !== 2) {
        throw new PassengerValidationError(
          `identity_documents[${index}].issuing_country_code`,
          "O país emissor do documento deve ser um código ISO-2 de 2 letras (ex: PT, BR, ES, FR)."
        );
      }

      const normalizedDocType = normalizeIdentityDocumentType(doc.type);

      return {
        type: normalizedDocType,
        unique_identifier: cleanDocNumber,
        expires_on: normalizedExpiresOn,
        issuing_country_code: cleanCountry,
      };
    });
  }

  return {
    id: raw.id.trim(),
    given_name: sanitizedGivenName,
    family_name: sanitizedFamilyName,
    born_on: normalizedBornOn,
    gender,
    title,
    phone_number: sanitizedPhone,
    email: cleanEmail,
    ...(sanitizedDocs && sanitizedDocs.length > 0 ? { identity_documents: sanitizedDocs } : {}),
  };
}
