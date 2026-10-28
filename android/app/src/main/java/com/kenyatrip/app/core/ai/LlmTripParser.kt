package com.kenyatrip.app.core.ai

import com.kenyatrip.app.core.knowledge.KnowledgeBase
import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.Tier
import com.kenyatrip.app.core.model.TravelStyle
import com.kenyatrip.app.core.planner.ParseResult
import com.kenyatrip.app.core.planner.ParseSource
import com.kenyatrip.app.core.planner.RequestParser
import com.kenyatrip.app.core.planner.Slot
import com.kenyatrip.app.core.planner.TripTextParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.add
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.util.concurrent.TimeUnit

data class LlmConfig(val baseUrl: String, val apiKey: String, val model: String)

@Serializable
internal data class LlmTrip(
    val destination: String? = null,
    val month: Int? = null,
    val days: Int? = null,
    val adults: Int? = null,
    @SerialName("child_ages") val childAges: List<Int>? = null,
    @SerialName("budget_per_person_usd") val budgetPerPersonUsd: Int? = null,
    val styles: List<String>? = null,
    @SerialName("must_places") val mustPlaces: List<String>? = null,
    @SerialName("must_activities") val mustActivities: List<String>? = null,
    val tier: String? = null,
)

/**
 * Slot extraction through any OpenAI-compatible chat completions endpoint. The model only
 * fills structured fields; every value is checked against the knowledge base before use.
 */
class LlmTripParser(
    private val kb: KnowledgeBase,
    private val config: () -> LlmConfig?,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build(),
) : TripTextParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun parse(text: String, defaultDestination: Destination, fallback: ParseResult): ParseResult {
        val cfg = config() ?: return fallback
        val content = withContext(Dispatchers.IO) { complete(cfg, systemPrompt(defaultDestination), text) }
        val trip = json.decodeFromString(LlmTrip.serializer(), extractJson(content))
        return merge(trip, fallback)
    }

    fun complete(cfg: LlmConfig, system: String, user: String): String {
        val body = buildJsonObject {
            put("model", cfg.model)
            put("temperature", 0)
            putJsonObject("response_format") { put("type", "json_object") }
            putJsonArray("messages") {
                add(buildJsonObject { put("role", "system"); put("content", system) })
                add(buildJsonObject { put("role", "user"); put("content", user) })
            }
        }
        val request = Request.Builder()
            .url(cfg.baseUrl.trimEnd('/') + "/chat/completions")
            .header("Authorization", "Bearer ${cfg.apiKey}")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "LLM HTTP ${response.code}" }
            val root = json.parseToJsonElement(response.body!!.string()).jsonObject
            return root["choices"]!!.jsonArray[0].jsonObject["message"]!!.jsonObject["content"]!!.jsonPrimitive.content
        }
    }

    private fun extractJson(content: String): String {
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        require(start >= 0 && end > start) { "No JSON object in LLM output" }
        return content.substring(start, end + 1)
    }

    internal fun merge(t: LlmTrip, fallback: ParseResult): ParseResult {
        val base = fallback.request
        val recognized = fallback.recognized.toMutableSet()
        val destination = t.destination?.let { d -> Destination.entries.firstOrNull { it.name == d.uppercase() } } ?: base.destination
        val placeIds = kb.placesIn(destination).map { it.id }.toSet()
        val activityIds = kb.activities.filter { it.placeId in placeIds }.map { it.id }.toSet()

        val month = t.month?.takeIf { it in 1..12 }?.also { recognized += Slot.MONTH } ?: base.month
        val days = t.days?.takeIf { it in RequestParser.MIN_DAYS..RequestParser.MAX_DAYS }?.also { recognized += Slot.DAYS } ?: base.days
        val adults = t.adults?.takeIf { it in 1..20 }?.also { recognized += Slot.TRAVELERS } ?: base.adults
        val childAges = t.childAges?.filter { it in 0..17 } ?: base.childAges
        val budget = t.budgetPerPersonUsd?.takeIf { it in 100..100_000 }?.also { recognized += Slot.BUDGET } ?: base.budgetPerPersonUsd
        val styles = t.styles?.mapNotNull { s -> TravelStyle.entries.firstOrNull { it.name == s.uppercase() } }?.toSet()
            ?.takeIf { it.isNotEmpty() }?.also { recognized += Slot.STYLES } ?: base.styles
        val mustActivities = t.mustActivities?.filter { it in activityIds }?.toSet() ?: base.mustActivities
        val mustPlaces = (t.mustPlaces?.filter { it in placeIds }?.toSet() ?: base.mustPlaces) +
            mustActivities.map { kb.activity(it).placeId }
        if (mustPlaces.isNotEmpty()) recognized += Slot.PLACES
        val tier = t.tier?.let { s -> Tier.entries.firstOrNull { it.name == s.uppercase() } }?.also { recognized += Slot.TIER } ?: base.tier

        val request = base.copy(
            destination = destination,
            month = month,
            days = days,
            adults = adults,
            childAges = childAges,
            budgetPerPersonUsd = budget,
            styles = if (childAges.isNotEmpty()) styles + TravelStyle.FAMILY else styles,
            mustPlaces = if (destination == base.destination) mustPlaces else mustPlaces.filter { it in placeIds }.toSet(),
            mustActivities = mustActivities,
            tier = tier,
        )
        return ParseResult(request, recognized, ParseSource.AI)
    }

    private fun systemPrompt(defaultDestination: Destination): String {
        fun ids(d: Destination) = kb.placesIn(d).joinToString { it.id }
        fun acts(d: Destination) = kb.activities.filter { a -> kb.place(a.placeId).destination == d }.joinToString { it.id }
        return """
            You extract travel requirements for a China–Kenya travel planner. Reply with ONE JSON object only:
            {"destination":"KENYA|CHINA|null","month":1-12|null,"days":int|null,"adults":int|null,
             "child_ages":[int],"budget_per_person_usd":int|null,"styles":[...],"must_places":[...],
             "must_activities":[...],"tier":"BUDGET|COMFORT|LUXURY|null"}
            Rules: today is ${today()}; default destination is $defaultDestination; convert budgets to USD
            (1 USD = 7.2 CNY = 129 KES); a total budget must be divided by the number of travellers;
            styles ⊆ [${TravelStyle.entries.joinToString { it.name }}];
            KENYA places ⊆ [${ids(Destination.KENYA)}]; CHINA places ⊆ [${ids(Destination.CHINA)}];
            KENYA activities ⊆ [${acts(Destination.KENYA)}]; CHINA activities ⊆ [${acts(Destination.CHINA)}].
            Use null or [] when not stated. Never invent ids.
        """.trimIndent()
    }

    /** Lightweight connectivity check used by the settings screen. */
    suspend fun ping(cfg: LlmConfig): Boolean = withContext(Dispatchers.IO) {
        runCatching { complete(cfg, "Reply with {\"ok\":true}", "ping").contains("ok") }.getOrDefault(false)
    }
}