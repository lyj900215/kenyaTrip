package com.kenyatrip.app.core.planner

import com.kenyatrip.app.core.knowledge.KnowledgeBase
import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.Itinerary
import com.kenyatrip.app.core.model.Text
import com.kenyatrip.app.core.model.TripRequest
import com.kenyatrip.app.core.pricing.PricingEngine
import java.time.LocalDate

/** Something that can turn free text into a trip request, e.g. an LLM. */
fun interface TripTextParser {
    suspend fun parse(text: String, defaultDestination: Destination, fallback: ParseResult): ParseResult
}

sealed interface ModifyOutcome {
    data class Updated(val itinerary: Itinerary, val changes: List<Text>) : ModifyOutcome
    data class NotUnderstood(val hint: Text) : ModifyOutcome
    data class NoFeasibleRoute(val changes: List<Text>) : ModifyOutcome
}

class PlannerService(
    val kb: KnowledgeBase = KnowledgeBase.default,
    pricing: PricingEngine = PricingEngine(),
    today: () -> LocalDate = { LocalDate.now() },
    private val aiParser: TripTextParser? = null,
) {
    private val ruleParser = RequestParser(kb, today)
    private val builder = ItineraryBuilder(kb, pricing)
    private val validator = ItineraryValidator(kb, today)
    val planner = RoutePlanner(kb, builder, validator, today)
    private val modifier = ItineraryModifier(kb, planner)

    suspend fun understand(text: String, defaultDestination: Destination): ParseResult {
        val local = ruleParser.parse(text, defaultDestination)
        val ai = aiParser ?: return local
        return runCatching { ai.parse(text, defaultDestination, local) }.getOrDefault(local)
    }

    fun recommend(req: TripRequest): List<Itinerary> = planner.recommend(req)

    fun modify(current: Itinerary, text: String): ModifyOutcome {
        val mod = modifier.interpret(current, text)
        if (!mod.understood) {
            return ModifyOutcome.NotUnderstood(
                Text(
                    "可以试试：\"轻松一点\"、\"加热气球\"、\"不去安博塞利\"、\"便宜一点\"、\"多一天\"",
                    "Try: \"more relaxed\", \"add balloon\", \"skip Amboseli\", \"cheaper\", \"one more day\"",
                ),
            )
        }
        val best = planner.recommend(mod.request, count = 1).firstOrNull()
            ?: return ModifyOutcome.NoFeasibleRoute(mod.changes)
        return ModifyOutcome.Updated(best, mod.changes)
    }
}
