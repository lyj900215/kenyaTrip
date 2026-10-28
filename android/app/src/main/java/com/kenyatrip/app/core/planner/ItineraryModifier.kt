package com.kenyatrip.app.core.planner

import com.kenyatrip.app.core.knowledge.KnowledgeBase
import com.kenyatrip.app.core.model.Itinerary
import com.kenyatrip.app.core.model.Text
import com.kenyatrip.app.core.model.TravelStyle
import com.kenyatrip.app.core.model.TripRequest

data class Modification(val request: TripRequest, val changes: List<Text>) {
    val understood: Boolean get() = changes.isNotEmpty()
}

/**
 * Translates a conversational edit ("make it more relaxed", "add the balloon", "skip Amboseli")
 * into constraint changes on the request, so the edited trip is re-solved and stays feasible.
 */
class ItineraryModifier(private val kb: KnowledgeBase, private val planner: RoutePlanner) {

    fun interpret(current: Itinerary, input: String): Modification {
        val text = input.lowercase()
        var req = current.request.copy(tier = current.request.tier ?: current.tier)
        val changes = mutableListOf<Text>()

        if (Regex("轻松|不累|慢一点|少坐车|别太赶|relax|easier|less driving|slower").containsMatchIn(text)) {
            val limit = (planner.maxDriveHours(req) - 1.5).coerceAtLeast(3.0)
            req = req.copy(maxDailyDriveHours = limit, styles = req.styles + TravelStyle.RELAXED)
            changes += Text("每日陆路上限调整为 ${limit} 小时，减少换住", "Daily overland limit lowered to $limit h, fewer hotel changes")
        }
        if (Regex("便宜|省钱|预算低|降低预算|cheaper|lower budget|save money").containsMatchIn(text)) {
            val tier = req.tier?.down()
            if (tier != null) {
                req = req.copy(tier = tier, budgetPerPersonUsd = null)
                changes += Text("住宿调整为${tier.label.zh}档", "Stays changed to ${tier.label.en}")
            }
        }
        if (Regex("升级|豪华|好一点|更好的|upgrade|luxury|nicer|better hotel").containsMatchIn(text)) {
            val tier = req.tier?.up()
            if (tier != null) {
                req = req.copy(tier = tier, budgetPerPersonUsd = null)
                changes += Text("住宿升级为${tier.label.zh}档", "Stays upgraded to ${tier.label.en}")
            }
        }

        val days = Regex("改成\\s*(\\d{1,2})\\s*天|(\\d{1,2})\\s*days").find(text)
            ?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }?.toInt()
            ?: when {
                Regex("多一天|加一天|延长|one more day|extra day|longer").containsMatchIn(text) -> req.days + 1
                Regex("少一天|减一天|缩短|one less day|shorter").containsMatchIn(text) -> req.days - 1
                else -> null
            }
        if (days != null) {
            val clamped = days.coerceIn(RequestParser.MIN_DAYS, RequestParser.MAX_DAYS)
            if (clamped != req.days) {
                req = req.copy(days = clamped)
                changes += Text("行程改为 $clamped 天", "Trip length changed to $clamped days")
            }
        }

        val removeMatch = Regex("(不去|去掉|不要|取消|删掉|\\bskip|\\bremove|\\bwithout|\\bdrop|\\bno\\b)\\s*(.{1,20})").find(text)
        val removed = removeMatch?.let { kb.matchPlaces(it.groupValues[2], req.destination) }.orEmpty()
        if (removed.isNotEmpty()) {
            req = req.copy(avoidPlaces = req.avoidPlaces + removed, mustPlaces = req.mustPlaces - removed)
            changes += Text(
                "去掉：" + removed.joinToString("、") { kb.place(it).name.zh },
                "Removed: " + removed.joinToString(", ") { kb.place(it).name.en },
            )
        }

        val activities = (kb.matchActivities(text, req.destination) - req.mustActivities)
            .filter { kb.activity(it).placeId !in removed }
            .toSet()
        if (activities.isNotEmpty()) {
            val places = activities.map { kb.activity(it).placeId }.toSet()
            req = req.copy(
                mustActivities = req.mustActivities + activities,
                mustPlaces = req.mustPlaces + places,
                avoidPlaces = req.avoidPlaces - places,
            )
            changes += Text(
                "加入：" + activities.joinToString("、") { kb.activity(it).name.zh },
                "Added: " + activities.joinToString(", ") { kb.activity(it).name.en },
            )
        }

        val added = kb.matchPlaces(text, req.destination) - removed - req.mustPlaces
        if (added.isNotEmpty()) {
            req = req.copy(mustPlaces = req.mustPlaces + added, avoidPlaces = req.avoidPlaces - added)
            changes += Text(
                "加入目的地：" + added.joinToString("、") { kb.place(it).name.zh },
                "Added destination: " + added.joinToString(", ") { kb.place(it).name.en },
            )
        }
        return Modification(req, changes)
    }
}
