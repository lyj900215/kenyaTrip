package com.kenyatrip.app.core.planner

import com.kenyatrip.app.core.knowledge.KnowledgeBase
import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.Edge
import com.kenyatrip.app.core.model.Itinerary
import com.kenyatrip.app.core.model.Place
import com.kenyatrip.app.core.model.Text
import com.kenyatrip.app.core.model.Tier
import com.kenyatrip.app.core.model.TransportMode
import com.kenyatrip.app.core.model.TravelStyle
import com.kenyatrip.app.core.model.TripRequest
import com.kenyatrip.app.core.pricing.Currency
import com.kenyatrip.app.core.pricing.Money
import java.time.LocalDate

/**
 * Searches all feasible sequences of stays (a small constraint search over the destination
 * graph), scores them for season, traveler style, travel fatigue and budget fit, and returns
 * a diverse top-N as fully built itineraries.
 */
class RoutePlanner(
    private val kb: KnowledgeBase,
    private val builder: ItineraryBuilder,
    private val validator: ItineraryValidator,
    private val today: () -> LocalDate = { LocalDate.now() },
) {
    fun recommend(req: TripRequest, count: Int = 3): List<Itinerary> {
        val start = startDate(req)
        val maxDrive = maxDriveHours(req)
        val scored = enumerate(req, maxDrive)
            .sortedByDescending { score(req, it, perPersonUsd = null) }
            .take(SHORTLIST)
            .map { route ->
                val tier = chooseTier(req, route, start)
                val perPerson = builder.estimatePerPerson(req, route, tier, start)
                Triple(route, tier, score(req, route, perPerson))
            }
            .sortedByDescending { it.third }

        val picked = mutableListOf<Triple<RouteSkeleton, Tier, Double>>()
        for (candidate in scored) {
            if (picked.size == count) break
            if (picked.none { it.first.placeIds == candidate.first.placeIds }) picked += candidate
        }
        return picked.mapIndexed { index, (route, tier, score) ->
            val built = builder.build("itn-${index + 1}-${route.placeIds.sorted().joinToString("-")}", req, route, tier, start)
            val itinerary = built.copy(name = routeName(req, route), score = score, reasons = reasons(req, route, built, maxDrive))
            itinerary.copy(warnings = validator.validate(itinerary, maxDrive))
        }
    }

    fun startDate(req: TripRequest): LocalDate {
        req.startDate?.let { return it }
        val now = today()
        val day = if (req.destination == Destination.CHINA && TravelStyle.BUSINESS in req.styles && req.month in CANTON_FAIR) 15 else 10
        var candidate = LocalDate.of(now.year, req.month, day)
        if (candidate.isBefore(now.plusDays(MIN_LEAD_DAYS))) candidate = candidate.plusYears(1)
        return candidate
    }

    fun maxDriveHours(req: TripRequest): Double = req.maxDailyDriveHours ?: when {
        TravelStyle.RELAXED in req.styles -> 4.5
        TravelStyle.FAMILY in req.styles || TravelStyle.SENIOR in req.styles -> 5.0
        else -> 6.0
    }

    private fun enumerate(req: TripRequest, maxDrive: Double): List<RouteSkeleton> {
        val dest = req.destination
        val gateway = kb.gateway(dest)
        val candidates = kb.placesIn(dest).filter { it.id !in req.avoidPlaces }
        val results = mutableListOf<RouteSkeleton>()
        val maxStops = (req.nights / 2 + 1).coerceIn(1, 5)

        if (dest == Destination.KENYA) {
            val wantsGatewayNight = gateway.id in req.mustPlaces || (req.nights >= 5 && gateway.id !in req.avoidPlaces)
            val gatewayNights = if (wantsGatewayNight) 1 else 0
            val parks = candidates.filter { it.id != gateway.id }
            fun dfs(seq: List<Pair<String, Int>>, edges: List<Edge>, remaining: Int) {
                if (remaining == 0) {
                    if (seq.isEmpty()) return
                    val last = seq.last().first
                    val back = kb.bestEdge(last, gateway.id, maxDrive) ?: return
                    val firstPark = seq.first().first
                    val firstEdge = kb.bestEdge(gateway.id, firstPark, maxDrive) ?: return
                    val stops = (if (gatewayNights > 0) listOf(gateway.id to gatewayNights) else emptyList()) + seq
                    val route = RouteSkeleton(
                        stops = stops,
                        arrival = if (gatewayNights > 0) null else firstEdge,
                        between = (if (gatewayNights > 0) listOf(firstEdge) else emptyList()) + edges,
                        departure = back,
                    )
                    if (satisfiesMust(req, route)) results += route
                    return
                }
                if (seq.size >= maxStops) return
                for (p in parks) {
                    if (seq.any { it.first == p.id }) continue
                    val prev = seq.lastOrNull()
                    val edge = prev?.let { kb.bestEdge(it.first, p.id, maxDrive) }
                    if (prev != null && edge == null) continue
                    for (n in p.minNights..minOf(p.maxNights, remaining)) {
                        dfs(seq + (p.id to n), if (edge != null) edges + edge else edges, remaining - n)
                    }
                }
            }
            dfs(emptyList(), emptyList(), req.nights - gatewayNights)
        } else {
            fun dfs(seq: List<Pair<String, Int>>, edges: List<Edge>, remaining: Int) {
                if (remaining == 0) {
                    if (seq.isEmpty() || !kb.place(seq.last().first).internationalAirport) return
                    val route = RouteSkeleton(seq, arrival = null, between = edges, departure = null)
                    if (satisfiesMust(req, route)) results += route
                    return
                }
                if (seq.size >= maxStops) return
                for (p in candidates) {
                    if (seq.any { it.first == p.id }) continue
                    if (seq.isEmpty() && !p.internationalAirport) continue
                    val prev = seq.lastOrNull()
                    val edge = prev?.let { kb.bestEdge(it.first, p.id, maxDrive) }
                    if (prev != null && edge == null) continue
                    for (n in p.minNights..minOf(p.maxNights, remaining)) {
                        dfs(seq + (p.id to n), if (edge != null) edges + edge else edges, remaining - n)
                    }
                }
            }
            dfs(emptyList(), emptyList(), req.nights)
        }
        return results
    }

    private fun satisfiesMust(req: TripRequest, route: RouteSkeleton) = req.mustPlaces.all { it in route.placeIds }

    private fun chooseTier(req: TripRequest, route: RouteSkeleton, start: LocalDate): Tier {
        req.tier?.let { return it }
        val budget = req.budgetPerPersonUsd ?: return Tier.COMFORT
        return Tier.entries.reversed().firstOrNull { builder.estimatePerPerson(req, route, it, start) <= budget }
            ?: Tier.BUDGET
    }

    private fun relaxedLike(req: TripRequest) =
        req.styles.any { it == TravelStyle.RELAXED || it == TravelStyle.FAMILY || it == TravelStyle.SENIOR }

    /** Pass [perPersonUsd] = null to rank on experience and fatigue only. */
    fun score(req: TripRequest, route: RouteSkeleton, perPersonUsd: Int?): Double {
        var experience = 0.0
        route.stops.forEach { (placeId, nights) ->
            val p = kb.place(placeId)
            val affinity = 1.0 + req.styles.sumOf { p.affinity[it] ?: 0.0 } + if (placeId in req.mustPlaces) 0.5 else 0.0
            val weight = if (p.kind == com.kenyatrip.app.core.model.PlaceKind.GATEWAY) 0.5 else 1.0
            experience += p.season(req.month) * NIGHT_CURVE[nights.coerceAtMost(NIGHT_CURVE.lastIndex)] * affinity * weight
        }
        val relaxed = relaxedLike(req)
        val overland = route.allEdges.filter { it.mode != TransportMode.FLIGHT }.sumOf { it.hours }
        val flights = route.allEdges.count { it.mode == TransportMode.FLIGHT }
        val fatigue = overland * (if (relaxed) 1.2 else 0.7) + flights * 1.0 + (route.stops.size - 1) * (if (relaxed) 2.0 else 1.0)
        val budgetPenalty = if (perPersonUsd == null) 0.0 else req.budgetPerPersonUsd?.let { budget ->
            val over = perPersonUsd.toDouble() / budget - 1.0
            if (over > 0) over * 60 else 0.0
        } ?: 0.0
        return experience - fatigue - budgetPenalty
    }

    private fun routeName(req: TripRequest, route: RouteSkeleton): Text {
        val ids = route.placeIds
        val theme = when (req.destination) {
            Destination.KENYA -> when {
                "masai_mara" in ids && req.month in 7..10 -> Text("大迁徙", "Great Migration")
                "diani" in ids -> Text("Safari + 海滩", "Safari & Beach")
                else -> Text("经典 Safari", "Classic Safari")
            }
            Destination.CHINA -> when {
                TravelStyle.BUSINESS in req.styles && "guangzhou" in ids && req.month in CANTON_FAIR -> Text("广交会采购", "Canton Fair")
                TravelStyle.BUSINESS in req.styles -> Text("采购考察", "Sourcing Trip")
                else -> Text("中国精华", "China Highlights")
            }
        }
        val gateway = kb.gateway(req.destination).id
        val shown = route.stops.map { kb.place(it.first) }
            .filter { req.destination == Destination.CHINA || it.id != gateway }
        return Text(
            theme.zh + " · " + shown.joinToString(" + ") { it.name.zh },
            theme.en + " · " + shown.joinToString(" + ") { it.name.en },
        )
    }

    private fun reasons(req: TripRequest, route: RouteSkeleton, it: Itinerary, maxDrive: Double): List<Text> {
        val out = mutableListOf<Text>()
        val best: Place = route.stops.map { kb.place(it.first) }.maxBy { it.season(req.month) }
        out += Text(
            "${req.month} 月是${best.name.zh}的好季节（${best.season(req.month)}/10）：${best.highlight.zh}",
            "${monthName(req.month)} is a great time for ${best.name.en} (${best.season(req.month)}/10): ${best.highlight.en}",
        )
        val longest = route.allEdges.filter { it.mode != TransportMode.FLIGHT }.maxOfOrNull { it.hours } ?: 0.0
        val flights = route.allEdges.count { it.mode == TransportMode.FLIGHT }
        out += Text(
            "单日最长陆路 ${fmt(longest)} 小时（上限 ${fmt(maxDrive)} 小时）" + if (flights > 0) "，含 $flights 段内陆航班减少长途奔波" else "",
            "Longest overland leg ${fmt(longest)} h (limit ${fmt(maxDrive)} h)" + if (flights > 0) ", with $flights domestic flight(s) to cut travel time" else "",
        )
        val currency = Currency.forDestination(req.destination)
        val pp = Money.format(it.pricePerPersonUsd, currency)
        val budgetText = req.budgetPerPersonUsd?.let { b ->
            val diff = (it.pricePerPersonUsd - b) * 100 / b
            if (diff <= 0) Text("，在预算内", ", within budget") else Text("，超出预算约 $diff%", ", about $diff% over budget")
        } ?: Text("", "")
        out += Text(
            "人均约 $pp（${it.tier.label.zh}档住宿）" + budgetText.zh,
            "About $pp per person (${it.tier.label.en} stays)" + budgetText.en,
        )
        if (req.mustPlaces.isNotEmpty() || req.mustActivities.isNotEmpty()) {
            val names = req.mustActivities.map { kb.activity(it).name } + req.mustPlaces.map { kb.place(it).name }
            out += Text(
                "已包含你指定的：" + names.distinct().joinToString("、") { it.zh },
                "Includes what you asked for: " + names.distinct().joinToString(", ") { it.en },
            )
        }
        return out
    }

    private fun fmt(h: Double) = if (h % 1.0 == 0.0) h.toInt().toString() else "%.1f".format(h)

    companion object {
        val CANTON_FAIR = setOf(4, 10)
        const val MIN_LEAD_DAYS = 21L
        private const val SHORTLIST = 60
        private val NIGHT_CURVE = listOf(0.0, 1.0, 1.7, 2.2, 2.5, 2.7)
        private val MONTHS = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December",
        )

        fun monthName(month: Int) = MONTHS[month - 1]
    }
}
