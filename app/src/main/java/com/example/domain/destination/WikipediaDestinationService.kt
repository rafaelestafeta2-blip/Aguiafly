package com.example.domain.destination

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object WikipediaDestinationService {

    private val memoryCache = mutableMapOf<String, Pair<String?, String?>>()

    /**
     * Busca dinamicamente a foto real e a descrição/país de qualquer cidade do mundo
     * usando a API pública da Wikipedia (pt.wikipedia.org / en.wikipedia.org).
     */
    suspend fun fetchCityCoverAndCountry(cityName: String): Pair<String?, String?> =
        withContext(Dispatchers.IO) {
            val cleanQuery = cityName.substringBefore("(").trim()
            if (cleanQuery.length < 2) return@withContext null to null
            memoryCache[cleanQuery.lowercase()]?.let { return@withContext it }

            val ptResult = queryWikiSummary("https://pt.wikipedia.org/api/rest_v1/page/summary/", cleanQuery)
            if (ptResult.first != null) {
                memoryCache[cleanQuery.lowercase()] = ptResult
                return@withContext ptResult
            }

            val enResult = queryWikiSummary("https://en.wikipedia.org/api/rest_v1/page/summary/", cleanQuery)
            if (enResult.first != null) {
                memoryCache[cleanQuery.lowercase()] = enResult
            }
            enResult
        }

    private fun queryWikiSummary(baseUrl: String, query: String): Pair<String?, String?> {
        return try {
            val encoded = URLEncoder.encode(query.replace(" ", "_"), "UTF-8")
            val connection = (URL("$baseUrl$encoded").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "AguiaFlyApp/1.0 (https://aguiafly.eu)")
            }
            if (connection.responseCode != 200) return null to null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val originalUrl = json.optJSONObject("originalimage")?.optString("source")
            val thumbUrl = json.optJSONObject("thumbnail")?.optString("source")
            val desc = json.optString("description").takeIf { it.isNotBlank() }
            val imageUrl = originalUrl?.takeIf { it.isNotBlank() } ?: thumbUrl?.takeIf { it.isNotBlank() }
            imageUrl to desc
        } catch (_: Exception) {
            null to null
        }
    }
}
