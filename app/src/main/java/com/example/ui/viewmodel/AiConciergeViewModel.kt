package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AirportPlaceSuggestion
import com.example.data.model.PlaceAutocompleteCatalog
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import java.text.Normalizer
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiConciergeRecommendation(
    val headline: String,
    val recommendedOriginIata: String,
    val recommendedDestinationIata: String,
    val recommendedDate: String,
    val recommendedCabinClass: String,
    val preferWidebody: Boolean,
    val aircraftAdvice: String
)

sealed class AiConciergeUiState {
    data object Idle : AiConciergeUiState()
    data object Loading : AiConciergeUiState()
    data class Success(val recommendation: AiConciergeRecommendation) : AiConciergeUiState()
    data class Error(val message: String) : AiConciergeUiState()
}

data class ConciergeChatMessage(
    val isUser: Boolean,
    val text: String,
    val recommendation: AiConciergeRecommendation? = null
)

class AiConciergeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AiConciergeUiState>(AiConciergeUiState.Idle)
    val uiState: StateFlow<AiConciergeUiState> = _uiState.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ConciergeChatMessage>>(
        listOf(
            ConciergeChatMessage(
                isUser = false,
                text = "Bem-vindo ao AguiaFly Concierge IA! Digite para onde e quando deseja voar — por exemplo: \"Voos mais baratos de Ibiza para o Porto em dezembro\" ou \"Voo executivo de Lisboa para Paris\" — e eu preparo a busca real na Duffel para você."
            )
        )
    )
    val chatHistory: StateFlow<List<ConciergeChatMessage>> = _chatHistory.asStateFlow()

    private var activeAskJob: Job? = null

    private val recommendationSchema = Schema.obj(
        mapOf(
            "headline" to Schema.string("Resumo executivo curto da recomendação em português"),
            "recommendedOriginIata" to Schema.string("Código IATA de 3 letras da origem (ex: IBZ, LIS, OPO, GRU)"),
            "recommendedDestinationIata" to Schema.string("Código IATA de 3 letras do destino (ex: OPO, CDG, MAD, LIS)"),
            "recommendedDate" to Schema.string("Data sugerida no formato YYYY-MM-DD"),
            "recommendedCabinClass" to Schema.enumeration(
                listOf("economy", "premium_economy", "business", "first"),
                "Classe de cabine sugerida"
            ),
            "preferWidebody" to Schema.boolean("Se o passageiro pediu aeronave de fuselagem larga (widebody)"),
            "aircraftAdvice" to Schema.string("Dica sobre a rota, aeronave ou melhor tarifa em português")
        )
    )

    fun askConcierge(userPrompt: String) {
        val trimmed = userPrompt.trim()
        if (trimmed.isEmpty()) return

        activeAskJob?.cancel()

        _chatHistory.value = _chatHistory.value + ConciergeChatMessage(isUser = true, text = trimmed)

        // Resposta instantânea para cumprimentos simples ou dúvidas sobre para que serve o Concierge
        val quickGreetingReply = buildConversationalGreetingOrHelp(trimmed)
        if (quickGreetingReply != null) {
            _chatHistory.value = _chatHistory.value + quickGreetingReply
            _uiState.value = if (quickGreetingReply.recommendation != null) {
                AiConciergeUiState.Success(quickGreetingReply.recommendation)
            } else {
                AiConciergeUiState.Idle
            }
            return
        }

        _uiState.value = AiConciergeUiState.Loading
        val localFallback = resolveLocalFlightRecommendation(trimmed)

        activeAskJob = viewModelScope.launch {
            try {
                val model = Firebase.ai.generativeModel(
                    modelName = "gemini-3.8-flash",
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        responseSchema = recommendationSchema
                    }
                )

                val catalogSummary = PlaceAutocompleteCatalog.GLOBAL_AIRPORTS.joinToString(", ") {
                    "${it.cityName}=${it.iataCode}"
                }

                val systemPrompt = """
                    Você é o AguiaFly Concierge IA, especialista em passagens aéreas reais via Duffel NDC.
                    Extraia a intenção de voo do cliente em português e retorne o JSON estruturado.
                    Aeroportos disponíveis no catálogo: $catalogSummary.
                    Sugestão pré-analisada para referência: origem=${localFallback.recommendedOriginIata}, destino=${localFallback.recommendedDestinationIata}, data=${localFallback.recommendedDate}, cabine=${localFallback.recommendedCabinClass}.
                    Se o cliente pedir "mais barato" ou "económico", use recommendedCabinClass = "economy" e preferWidebody = false.
                    Pedido do cliente: "$trimmed"
                """.trimIndent()

                val response = withTimeout(12_000L) {
                    model.generateContent(systemPrompt)
                }

                val rawText = response.text ?: throw IllegalStateException("Resposta vazia do modelo.")
                val json = JSONObject(rawText)

                val aiOrigin = json.optString("recommendedOriginIata", localFallback.recommendedOriginIata)
                    .uppercase()
                    .trim()
                val aiDest = json.optString("recommendedDestinationIata", localFallback.recommendedDestinationIata)
                    .uppercase()
                    .trim()

                // Se o parser local encontrou explicitamente cidades no texto do usuário, priorize os IATAs corretos
                val finalOrigin = if (localFallback.explicitOriginMatched) {
                    localFallback.recommendedOriginIata
                } else {
                    aiOrigin.takeIf { it.length == 3 } ?: localFallback.recommendedOriginIata
                }
                val finalDest = if (localFallback.explicitDestMatched) {
                    localFallback.recommendedDestinationIata
                } else {
                    aiDest.takeIf { it.length == 3 && it != finalOrigin } ?: localFallback.recommendedDestinationIata
                }

                val recommendation = AiConciergeRecommendation(
                    headline = json.optString("headline", localFallback.recommendation.headline),
                    recommendedOriginIata = finalOrigin,
                    recommendedDestinationIata = finalDest,
                    recommendedDate = json.optString("recommendedDate", localFallback.recommendedDate)
                        .takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
                        ?: localFallback.recommendedDate,
                    recommendedCabinClass = if (localFallback.explicitCabinMatched) {
                        localFallback.recommendedCabinClass
                    } else {
                        json.optString("recommendedCabinClass", localFallback.recommendedCabinClass).lowercase()
                    },
                    preferWidebody = json.optBoolean("preferWidebody", localFallback.preferWidebody),
                    aircraftAdvice = json.optString("aircraftAdvice", localFallback.recommendation.aircraftAdvice)
                )

                val originAirport = PlaceAutocompleteCatalog.resolveByIata(recommendation.recommendedOriginIata)
                val destAirport = PlaceAutocompleteCatalog.resolveByIata(recommendation.recommendedDestinationIata)

                _chatHistory.value = _chatHistory.value + ConciergeChatMessage(
                    isUser = false,
                    text = "${recommendation.headline}\n\n${recommendation.aircraftAdvice}\n\nRota pronta: ${originAirport.cityName} (${originAirport.iataCode}) → ${destAirport.cityName} (${destAirport.iataCode}) em ${recommendation.recommendedDate}.",
                    recommendation = recommendation
                )
                _uiState.value = AiConciergeUiState.Success(recommendation)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("AiConciergeVM", "Usando motor semântico instantâneo AguiaFly: ${e.message}")
                val rec = localFallback.recommendation
                val originAirport = PlaceAutocompleteCatalog.resolveByIata(rec.recommendedOriginIata)
                val destAirport = PlaceAutocompleteCatalog.resolveByIata(rec.recommendedDestinationIata)
                _chatHistory.value = _chatHistory.value + ConciergeChatMessage(
                    isUser = false,
                    text = "${rec.headline}\n\n${rec.aircraftAdvice}\n\nToque no botão abaixo para pesquisar agora os voos reais de ${originAirport.cityName} (${originAirport.iataCode}) para ${destAirport.cityName} (${destAirport.iataCode}) em ${rec.recommendedDate}.",
                    recommendation = rec
                )
                _uiState.value = AiConciergeUiState.Success(rec)
            }
        }
    }

    private fun buildConversationalGreetingOrHelp(prompt: String): ConciergeChatMessage? {
        val norm = normalize(prompt)
        val isShortGreeting = norm in setOf("oi", "ola", "bom dia", "boa tarde", "boa noite", "hey", "hi", "ajuda", "help")
        val isAskingPurpose = norm.contains("pra que serve") || norm.contains("para que serve") || norm.contains("como funciona")

        if (!isShortGreeting && !isAskingPurpose) return null

        // Se na conversa já havia uma recomendação anterior, reapresenta ou explica
        val lastRec = _chatHistory.value.lastOrNull { it.recommendation != null }?.recommendation
        val defaultRec = lastRec ?: AiConciergeRecommendation(
            headline = "Sugestão Rápida: Ibiza (IBZ) → Porto (OPO) com Menor Tarifa",
            recommendedOriginIata = "IBZ",
            recommendedDestinationIata = "OPO",
            recommendedDate = "2026-12-10",
            recommendedCabinClass = "economy",
            preferWidebody = false,
            aircraftAdvice = "Em dezembro, as melhores tarifas entre Ibiza (IBZ) e Porto (OPO) operam em Airbus A320/Boeing 737 na classe Económica."
        )

        return ConciergeChatMessage(
            isUser = false,
            text = "Olá! Eu sou o **AguiaFly Concierge IA**. Você pode me pedir qualquer rota, mês ou estilo de viagem (ex.: *\"voos mais baratos de Ibiza para o Porto em dezembro\"* ou *\"São Paulo para Lisboa em executiva\"*) e eu configuro os aeroportos, a data e a classe para você pesquisar os voos reais com 1 toque!",
            recommendation = defaultRec
        )
    }

    private data class LocalResolvedFlight(
        val recommendedOriginIata: String,
        val recommendedDestinationIata: String,
        val recommendedDate: String,
        val recommendedCabinClass: String,
        val preferWidebody: Boolean,
        val explicitOriginMatched: Boolean,
        val explicitDestMatched: Boolean,
        val explicitCabinMatched: Boolean,
        val recommendation: AiConciergeRecommendation
    )

    private fun resolveLocalFlightRecommendation(prompt: String): LocalResolvedFlight {
        val norm = normalize(prompt)

        // 1. Detectar aeroportos mencionados na frase na ordem em que aparecem
        val matchedAirports = mutableListOf<Pair<Int, AirportPlaceSuggestion>>()
        for (airport in PlaceAutocompleteCatalog.GLOBAL_AIRPORTS) {
            val cityNorm = normalize(airport.cityName)
            val iataNorm = normalize(airport.iataCode)
            val cityIdx = findWordIndex(norm, cityNorm)
            val iataIdx = findExactTokenIndex(norm, iataNorm)
            val bestIdx = listOfNotNull(cityIdx, iataIdx).minOrNull()
            if (bestIdx != null) {
                matchedAirports.add(bestIdx to airport)
            }
        }
        val orderedUniqueAirports = matchedAirports
            .sortedBy { it.first }
            .map { it.second }
            .distinctBy { it.iataCode }

        val explicitOrigin = orderedUniqueAirports.getOrNull(0)
        val explicitDest = orderedUniqueAirports.getOrNull(1)

        val originAirport = explicitOrigin ?: PlaceAutocompleteCatalog.resolveByIata("LIS")
        val destAirport = explicitDest ?: if (originAirport.iataCode == "OPO") {
            PlaceAutocompleteCatalog.resolveByIata("CDG")
        } else if (originAirport.iataCode == "IBZ") {
            PlaceAutocompleteCatalog.resolveByIata("OPO")
        } else {
            PlaceAutocompleteCatalog.resolveByIata("OPO")
        }

        // 2. Detectar classe / tarifa mais barata
        val isCheapestOrEconomy = norm.contains("barat") ||
            norm.contains("econom") ||
            norm.contains("promo") ||
            norm.contains("menor preco") ||
            norm.contains("baixo custo")
        val isFirst = norm.contains("primeira classe") || norm.contains("first")
        val isPremiumEco = norm.contains("premium economy") || norm.contains("economica premium")
        val isBusiness = norm.contains("executiv") || norm.contains("business")

        val cabin = when {
            isCheapestOrEconomy -> "economy"
            isFirst -> "first"
            isPremiumEco -> "premium_economy"
            isBusiness -> "business"
            else -> "economy"
        }
        val explicitCabinMatched = isCheapestOrEconomy || isFirst || isPremiumEco || isBusiness

        val preferWidebody = norm.contains("widebody") ||
            norm.contains("a350") ||
            norm.contains("a330") ||
            norm.contains("787") ||
            norm.contains("777") ||
            norm.contains("fuselagem larga")

        // 3. Detectar data ou mês solicitado
        val resolvedDate = resolveDateFromPrompt(norm)

        val cabinLabelPt = when (cabin) {
            "economy" -> if (isCheapestOrEconomy) "Económica (Menor Tarifa)" else "Económica"
            "premium_economy" -> "Premium Economy"
            "first" -> "First Class"
            else -> "Executiva Business"
        }

        val headline = "Rota Recomendada: ${originAirport.cityName} (${originAirport.iataCode}) → ${destAirport.cityName} (${destAirport.iataCode}) • $cabinLabelPt"
        val advice = if (isCheapestOrEconomy) {
            "Configurei sua pesquisa de ${originAirport.cityName} (${originAirport.iataCode}) para ${destAirport.cityName} (${destAirport.iataCode}) em $resolvedDate priorizando as opções mais econômicas disponíveis na Duffel."
        } else {
            "Preparei a busca de ${originAirport.cityName} (${originAirport.iataCode}) para ${destAirport.cityName} (${destAirport.iataCode}) em $resolvedDate na cabine $cabinLabelPt com tarifas reais NDC."
        }

        val rec = AiConciergeRecommendation(
            headline = headline,
            recommendedOriginIata = originAirport.iataCode,
            recommendedDestinationIata = destAirport.iataCode,
            recommendedDate = resolvedDate,
            recommendedCabinClass = cabin,
            preferWidebody = preferWidebody,
            aircraftAdvice = advice
        )

        return LocalResolvedFlight(
            recommendedOriginIata = originAirport.iataCode,
            recommendedDestinationIata = destAirport.iataCode,
            recommendedDate = resolvedDate,
            recommendedCabinClass = cabin,
            preferWidebody = preferWidebody,
            explicitOriginMatched = explicitOrigin != null,
            explicitDestMatched = explicitDest != null,
            explicitCabinMatched = explicitCabinMatched,
            recommendation = rec
        )
    }

    private fun resolveDateFromPrompt(norm: String): String {
        // Explicit YYYY-MM-DD
        Regex("(\\d{4}-\\d{2}-\\d{2})").find(norm)?.value?.let { return it }

        // Explicit DD/MM/YYYY or DD/MM
        Regex("(\\d{1,2})/(\\d{1,2})(?:/(\\d{4}))?").find(norm)?.let { match ->
            val day = match.groupValues[1].toIntOrNull()?.coerceIn(1, 28) ?: 15
            val month = match.groupValues[2].toIntOrNull()?.coerceIn(1, 12) ?: 12
            val year = match.groupValues[3].toIntOrNull() ?: 2026
            return "%04d-%02d-%02d".format(year, month, day)
        }

        val monthMap = mapOf(
            "janeiro" to 1,
            "fevereiro" to 2,
            "marco" to 3,
            "abril" to 4,
            "maio" to 5,
            "junho" to 6,
            "julho" to 7,
            "agosto" to 8,
            "setembro" to 9,
            "outubro" to 10,
            "novembro" to 11,
            "dezembro" to 12
        )

        for ((monthName, monthNum) in monthMap) {
            if (norm.contains(monthName)) {
                val dayMatch = Regex("dia\\s+(\\d{1,2})").find(norm)?.groupValues?.getOrNull(1)?.toIntOrNull()
                val day = (dayMatch ?: 10).coerceIn(1, 28)
                val year = if (monthNum < 11) 2027 else 2026
                return "%04d-%02d-%02d".format(year, monthNum, day)
            }
        }

        return try {
            LocalDate.now().plusDays(14).toString()
        } catch (_: Exception) {
            "2026-12-10"
        }
    }

    private fun findWordIndex(haystack: String, needle: String): Int? {
        if (needle.isBlank()) return null
        val regex = Regex("\\b${Regex.escape(needle)}\\b")
        return regex.find(haystack)?.range?.first
    }

    private fun findExactTokenIndex(haystack: String, iataLower: String): Int? {
        if (iataLower.length != 3) return null
        val regex = Regex("\\b${Regex.escape(iataLower)}\\b")
        return regex.find(haystack)?.range?.first
    }

    private fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase().trim(), Normalizer.Form.NFD)
        return decomposed.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }
}
