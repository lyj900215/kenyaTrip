package com.kenyatrip.app.core.planner

import com.kenyatrip.app.core.knowledge.KnowledgeBase
import com.kenyatrip.app.core.model.Activity
import com.kenyatrip.app.core.model.BookableComponent
import com.kenyatrip.app.core.model.ComponentType
import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.Edge
import com.kenyatrip.app.core.model.ItemType
import com.kenyatrip.app.core.model.Itinerary
import com.kenyatrip.app.core.model.ItineraryDay
import com.kenyatrip.app.core.model.ItineraryItem
import com.kenyatrip.app.core.model.Lodge
import com.kenyatrip.app.core.model.Place
import com.kenyatrip.app.core.model.Slot
import com.kenyatrip.app.core.model.Stop
import com.kenyatrip.app.core.model.Text
import com.kenyatrip.app.core.model.Tier
import com.kenyatrip.app.core.model.TransportMode
import com.kenyatrip.app.core.model.TravelStyle
import com.kenyatrip.app.core.model.TripRequest
import com.kenyatrip.app.core.pricing.PricingEngine
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Ordered stays plus the transport legs that connect them.
 * [arrival] reaches the first stop from the arrival airport (null when the first stop is the airport);
 * [departure] reaches the departure airport from the last stop.
 */
data class RouteSkeleton(
    val stops: List<Pair<String, Int>>,
    val arrival: Edge?,
    val between: List<Edge>,
    val departure: Edge?,
) {
    val allEdges: List<Edge> get() = listOfNotNull(arrival) + between + listOfNotNull(departure)
    val placeIds: Set<String> get() = stops.map { it.first }.toSet()
}

class ItineraryBuilder(
    private val kb: KnowledgeBase,
    private val pricing: PricingEngine,
) {
    fun build(
        id: String,
        req: TripRequest,
        route: RouteSkeleton,
        tier: Tier,
        startDate: LocalDate,
    ): Itinerary {
        val stops = route.stops.map { (placeId, nights) -> Stop(placeId, nights, chooseLodge(placeId, tier)?.id) }
        val schedule = schedule(req, route, stops, startDate)
        val components = components(req, route, stops, tier, startDate, schedule.activityDays)
        val days = schedule.days.map { day ->
            day.copy(items = day.items.map { item -> item.copy(componentId = linkComponent(item, components)) })
        }
        return Itinerary(
            id = id,
            request = req,
            name = Text("", ""),
            stops = stops,
            transfers = route.allEdges,
            tier = tier,
            startDate = startDate,
            days = days,
            components = components,
            reasons = emptyList(),
            warnings = emptyList(),
            score = 0.0,
        )
    }

    fun chooseLodge(placeId: String, tier: Tier): Lodge? {
        val options = kb.lodgesAt(placeId, tier).ifEmpty { kb.lodgesAt(placeId) }
        return options.firstOrNull { it.chineseFriendly } ?: options.firstOrNull()
    }

    /** Quick per-person price estimate used while ranking candidate routes. */
    fun estimatePerPerson(req: TripRequest, route: RouteSkeleton, tier: Tier, startDate: LocalDate): Int {
        val stops = route.stops.map { (placeId, nights) -> Stop(placeId, nights, chooseLodge(placeId, tier)?.id) }
        val activityDays = schedule(req, route, stops, startDate).activityDays
        return components(req, route, stops, tier, startDate, activityDays).sumOf { it.priceUsd } /
            req.travelers.coerceAtLeast(1)
    }

    private data class Schedule(val days: List<ItineraryDay>, val activityDays: Map<String, Int>)

    private fun arrivalAirport(req: TripRequest, route: RouteSkeleton): Place {
        if (req.destination == Destination.KENYA) return kb.gateway(req.destination)
        val first = route.stops.first().first
        return kb.place(route.arrival?.other(first) ?: first)
    }

    private fun departureAirport(req: TripRequest, route: RouteSkeleton): Place {
        if (req.destination == Destination.KENYA) return kb.gateway(req.destination)
        val last = route.stops.last().first
        return kb.place(route.departure?.other(last) ?: last)
    }

    private fun schedule(req: TripRequest, route: RouteSkeleton, stops: List<Stop>, start: LocalDate): Schedule {
        val dest = req.destination
        val dayItems = MutableList(req.days) { mutableListOf<ItineraryItem>() }
        val dayBase = MutableList(req.days) { Text("", "") }
        val activityDays = linkedMapOf<String, Int>()
        val origin = originName(req)

        val firstPlace = kb.place(stops.first().placeId)
        val arrivalAirport = arrivalAirport(req, route)
        dayItems[0] += if (dest == Destination.KENYA) {
            ItineraryItem(
                "06:30", ItemType.FLIGHT,
                Text("抵达内罗毕（${origin.zh}出发）", "Arrive in Nairobi from ${origin.en}"),
                Text("专车接机，中文导游举牌迎接", "Met at arrivals by your guide and driver"),
            )
        } else {
            ItineraryItem(
                "14:00", ItemType.FLIGHT,
                Text("抵达${arrivalAirport.name.zh}（${origin.zh}出发）", "Arrive in ${arrivalAirport.name.en} from ${origin.en}"),
                Text("接机后入住酒店，领取当地电话卡", "Airport pickup, hotel check-in and local SIM card"),
            )
        }

        var day = 0
        stops.forEachIndexed { index, stop ->
            val place = kb.place(stop.placeId)
            val edgeIn = if (index == 0) route.arrival else route.between[index - 1]
            val lodge = stop.lodgeId?.let { kb.lodge(it) }
            val arrivalDay = day
            val earliest = when {
                arrivalDay == 0 && dest == Destination.CHINA -> 15.0
                arrivalDay == 0 -> 9.0
                else -> 8.0
            }
            var readyAt = earliest
            if (edgeIn != null) {
                val from = kb.place(edgeIn.other(place.id))
                dayItems[arrivalDay] += transferItem(edgeIn, from, place, earliest)
                readyAt = earliest + edgeIn.hours + 0.5
            } else if (arrivalDay == 0 && dest == Destination.CHINA) {
                readyAt = 16.0
            }
            val queue = activityQueue(req, place).toMutableList()
            if (isGameDrivePlace(place) && readyAt <= 15.0) {
                dayItems[arrivalDay] += ItineraryItem(
                    "15:30", ItemType.GAME_DRIVE,
                    Text("${place.name.zh}午后游猎", "Afternoon game drive in ${place.name.en}"),
                    place.highlight,
                )
            } else if (!isGameDrivePlace(place) && readyAt <= 17.0) {
                if (readyAt <= 13.0) {
                    queue.firstOrNull { it.slot == Slot.AFTERNOON || it.slot == Slot.MORNING }?.let {
                        dayItems[arrivalDay] += activityItem("14:30", it)
                        val count = activityDays.merge(it.id, 1, Int::plus) ?: 1
                        if (count >= it.maxDays) queue.remove(it)
                    }
                }
                queue.firstOrNull { it.slot == Slot.EVENING }?.let {
                    dayItems[arrivalDay] += activityItem("19:00", it)
                    activityDays.merge(it.id, 1, Int::plus)
                    queue.remove(it)
                }
            }
            if (lodge != null) {
                dayItems[arrivalDay] += ItineraryItem(
                    "20:00", ItemType.STAY,
                    Text("入住 ${lodge.name.zh} · ${stop.nights} 晚", "Check in: ${lodge.name.en} · ${stop.nights} night(s)"),
                    Text(lodge.tier.label.zh + "档" + if (lodge.chineseFriendly) " · 可提供中餐" else "", lodge.tier.label.en),
                )
            }
            for (d in arrivalDay until arrivalDay + stop.nights) dayBase[d] = place.name
            for (d in arrivalDay + 1 until arrivalDay + stop.nights) {
                dayItems[d] += fullDay(place, queue, activityDays)
            }
            day += stop.nights
        }

        val last = req.days - 1
        val lastPlace = kb.place(stops.last().placeId)
        dayBase[last] = lastPlace.name
        val departureAirport = departureAirport(req, route)
        var flightTime = if (dest == Destination.KENYA) "21:30" else "13:00"
        route.departure?.let { edge ->
            dayItems[last] += transferItem(edge, lastPlace, departureAirport, 8.0)
            if (edge.mode == TransportMode.DRIVE && edge.hours > 4) flightTime = "23:30"
            if (dest == Destination.CHINA) {
                val departHour = ceil(8.0 + edge.hours + 3.0)
                flightTime = "%02d:00".format(departHour.toInt())
            }
        }
        if (dest == Destination.KENYA && route.departure == null) {
            dayItems[last] += ItineraryItem(
                "10:00", ItemType.FREE,
                Text("自由活动 · 马赛市场购物", "Free time · Maasai Market shopping"),
            )
        }
        dayItems[last] += ItineraryItem(
            flightTime, ItemType.FLIGHT,
            Text("${departureAirport.name.zh}搭乘航班返回${origin.zh}", "Fly home from ${departureAirport.name.en} to ${origin.en}"),
            Text("提前 3 小时送机", "Airport drop-off 3 hours before departure"),
        )

        val days = dayItems.mapIndexed { i, items ->
            ItineraryDay(i + 1, start.plusDays(i.toLong()), dayBase[i], items.sortedBy { it.time })
        }
        return Schedule(days, activityDays)
    }

    private fun fullDay(place: Place, queue: MutableList<Activity>, used: MutableMap<String, Int>): List<ItineraryItem> {
        val items = mutableListOf<ItineraryItem>()
        fun take(predicate: (Activity) -> Boolean): Activity? = queue.firstOrNull(predicate)?.also { a ->
            val count = used.merge(a.id, 1, Int::plus) ?: 1
            if (count >= a.maxDays) queue.remove(a)
        }
        if (isGameDrivePlace(place)) {
            val dawn = take { it.slot == Slot.DAWN }
            items += if (dawn != null) activityItem("05:30", dawn) else ItineraryItem(
                "06:30", ItemType.GAME_DRIVE, Text("清晨游猎", "Early-morning game drive"), place.highlight,
            )
            items += ItineraryItem("12:30", ItemType.MEAL, Text("营地午餐与休息", "Lunch and siesta at camp"))
            val afternoon = take { it.slot == Slot.AFTERNOON || it.slot == Slot.MORNING }
            items += if (afternoon != null) activityItem("15:00", afternoon) else ItineraryItem(
                "15:30", ItemType.GAME_DRIVE, Text("午后游猎", "Afternoon game drive"), place.highlight,
            )
        } else {
            val fullDay = take { it.slot == Slot.FULL_DAY }
            if (fullDay != null) {
                items += activityItem("09:00", fullDay)
            } else {
                items += take { it.slot == Slot.MORNING }?.let { activityItem("09:00", it) }
                    ?: ItineraryItem("09:30", ItemType.FREE, Text("自由探索 ${place.name.zh}", "Explore ${place.name.en} at leisure"))
                items += take { it.slot == Slot.AFTERNOON }?.let { activityItem("14:30", it) }
                    ?: ItineraryItem("14:30", ItemType.FREE, Text("休闲时光", "Free afternoon"))
            }
            take { it.slot == Slot.EVENING }?.let { items += activityItem("19:00", it) }
        }
        return items
    }

    private fun isGameDrivePlace(place: Place) = place.chargesParkFee

    private fun activityQueue(req: TripRequest, place: Place): List<Activity> {
        val minChildAge = req.childAges.minOrNull()
        return kb.activitiesAt(place.id)
            .filter { req.month in it.months }
            .filter { a ->
                val must = a.id in req.mustActivities
                val styleMatch = a.styles.any { it in req.styles }
                val fitsKids = minChildAge == null || a.minAge <= minChildAge || must
                val businessOk = a.minAge < 18 || TravelStyle.BUSINESS in req.styles || must
                val defaultOk = a.default && (a.slot != Slot.FULL_DAY || req.styles.isEmpty() || place.id in req.mustPlaces)
                (must || styleMatch || defaultOk) &&
                    fitsKids && businessOk
            }
            .sortedWith(compareByDescending<Activity> { it.id in req.mustActivities }.thenBy { it.slot != Slot.FULL_DAY })
    }

    private fun activityItem(time: String, a: Activity) = ItineraryItem(
        time,
        if (a.styles.contains(TravelStyle.BUSINESS) && a.styles.size == 1) ItemType.BUSINESS else ItemType.ACTIVITY,
        a.name,
    )

    private fun transferItem(edge: Edge, from: Place, to: Place, startHour: Double): ItineraryItem {
        val hours = formatHours(edge.hours)
        val (zhMode, enMode) = when (edge.mode) {
            TransportMode.DRIVE -> "驱车" to "Drive"
            TransportMode.FLIGHT -> "内陆航班" to "Domestic flight"
            TransportMode.TRAIN -> "高铁" to "High-speed train"
        }
        return ItineraryItem(
            "%02d:%02d".format(startHour.toInt(), ((startHour % 1) * 60).roundToInt()),
            when (edge.mode) {
                TransportMode.DRIVE -> ItemType.TRANSFER
                TransportMode.FLIGHT -> ItemType.DOMESTIC_FLIGHT
                TransportMode.TRAIN -> ItemType.TRAIN
            },
            Text("$zhMode ${from.name.zh} → ${to.name.zh}", "$enMode ${from.name.en} → ${to.name.en}"),
            Text("约 $hours 小时", "About $hours h"),
        )
    }

    private fun formatHours(h: Double) = if (h % 1.0 == 0.0) h.toInt().toString() else "%.1f".format(h)

    private fun originName(req: TripRequest): Text {
        val cities = kb.originCities[req.destination].orEmpty()
        return (cities.firstOrNull { it.id == req.originCity } ?: cities.first()).name
    }

    private fun components(
        req: TripRequest,
        route: RouteSkeleton,
        stops: List<Stop>,
        tier: Tier,
        start: LocalDate,
        activityDays: Map<String, Int>,
    ): List<BookableComponent> {
        val result = mutableListOf<BookableComponent>()
        val adults = req.adults
        val kids = req.childAges.size
        val pax = req.travelers

        val flightRates = kb.flights.getValue(req.destination)
        val firstAirport = arrivalAirport(req, route)
        val lastAirport = departureAirport(req, route)
        val viaHubFactor = if (firstAirport.id != kb.gateway(req.destination).id || lastAirport.id != firstAirport.id) 1.05 else 1.0
        val seasonFactor = if (req.month in flightRates.peakMonths) flightRates.peakMultiplier else 1.0
        val perAdultFlight = flightRates.roundTripUsd * seasonFactor * viaHubFactor
        val flightCost = (perAdultFlight * (adults + kids * flightRates.childFactor)).roundToInt()
        val origin = originName(req)
        val route1 = if (firstAirport.id == lastAirport.id) "${origin.zh} ⇄ ${firstAirport.name.zh}"
        else "${origin.zh} → ${firstAirport.name.zh} / ${lastAirport.name.zh} → ${origin.zh}"
        val route1En = if (firstAirport.id == lastAirport.id) "${origin.en} ⇄ ${firstAirport.name.en}"
        else "${origin.en} → ${firstAirport.name.en} / ${lastAirport.name.en} → ${origin.en}"
        result += BookableComponent(
            id = "flight-intl", type = ComponentType.FLIGHT,
            title = Text("国际往返机票 $route1 · $pax 人", "Return flights $route1En · $pax pax"),
            supplier = flightRates.supplier, refId = "${firstAirport.id}-${lastAirport.id}", placeId = null,
            startDate = start, units = 1, quantity = pax,
            costUsd = flightCost, priceUsd = pricing.price(ComponentType.FLIGHT, flightCost, tickets = pax),
            refundPolicy = Text("按航司票规：改期收费，退票部分退款", "Airline fare rules apply: change fee, partial refund"),
        )

        val nightsBefore = stops.runningFold(0) { acc, s -> acc + s.nights }
        val lastStopId = stops.last().placeId
        val datedEdges = listOfNotNull(route.arrival?.let { Triple(it, stops.first().placeId, 0) }) +
            route.between.mapIndexed { k, e -> Triple(e, stops[k + 1].placeId, nightsBefore[k + 1]) } +
            listOfNotNull(route.departure?.let { Triple(it, it.other(lastStopId), req.days - 1) })
        datedEdges.forEachIndexed { i, (edge, toId, dayOffset) ->
            if (edge.mode == TransportMode.DRIVE) return@forEachIndexed
            val from = kb.place(edge.other(toId))
            val to = kb.place(toId)
            val cost = edge.pricePerPersonUsd * pax
            val (zh, en) = if (edge.mode == TransportMode.FLIGHT) "内陆航班" to "Domestic flight" else "高铁" to "High-speed rail"
            result += BookableComponent(
                id = "transport-$i", type = ComponentType.DOMESTIC_TRANSPORT,
                title = Text("$zh ${from.name.zh} – ${to.name.zh}", "$en ${from.name.en} – ${to.name.en}"),
                supplier = if (edge.mode == TransportMode.FLIGHT) "Domestic airline (sample)" else "China Railway via partner (sample)",
                refId = "${edge.a}-${edge.b}-${edge.mode}", placeId = null,
                startDate = start.plusDays(dayOffset.toLong()), units = 1, quantity = pax,
                costUsd = cost, priceUsd = pricing.price(ComponentType.DOMESTIC_TRANSPORT, cost),
                refundPolicy = Text("出发前 24 小时可退，收取 10% 手续费", "Refundable up to 24 h before, 10% fee"),
            )
        }

        var night = 0L
        stops.forEach { stop ->
            val place = kb.place(stop.placeId)
            val lodge = stop.lodgeId?.let { kb.lodge(it) } ?: return@forEach
            val multiplier = if (req.month in place.hotelPeakMonths) place.hotelPeakMultiplier else 1.0
            val cost = (lodge.pricePerNightUsd * multiplier * stop.nights * (adults + kids * 0.5)).roundToInt()
            result += BookableComponent(
                id = "stay-${place.id}", type = ComponentType.STAY,
                title = Text("${lodge.name.zh} · ${stop.nights} 晚", "${lodge.name.en} · ${stop.nights} night(s)"),
                supplier = lodge.name.en, refId = lodge.id, placeId = place.id,
                startDate = start.plusDays(night), units = stop.nights, quantity = pax,
                costUsd = cost, priceUsd = pricing.price(ComponentType.STAY, cost),
                refundPolicy = Text("入住前 30 天免费取消，之后收取 50%", "Free cancellation up to 30 days before, then 50%"),
                tier = lodge.tier,
            )
            night += stop.nights
        }

        val ground = kb.ground.getValue(req.destination)
        val seats = if (TravelStyle.PHOTOGRAPHY in req.styles) ground.photoSeatsPerVehicle else ground.seatsPerVehicle
        val vehicles = ceil(pax / seats.toDouble()).toInt().coerceAtLeast(1)
        val groundCost = vehicles * ground.vehiclePerDayUsd * req.days + ground.guidePerDayUsd * req.days
        result += BookableComponent(
            id = "ground", type = ComponentType.GROUND,
            title = Text(
                "${ground.vehicleLabel.zh} ×$vehicles + ${ground.guideLabel.zh} · ${req.days} 天",
                "${ground.vehicleLabel.en} ×$vehicles + ${ground.guideLabel.en} · ${req.days} days",
            ),
            supplier = ground.supplier, refId = "ground-${req.destination}", placeId = null,
            startDate = start, units = req.days, quantity = vehicles,
            costUsd = groundCost, priceUsd = pricing.price(ComponentType.GROUND, groundCost),
            refundPolicy = Text("出发前 14 天免费取消", "Free cancellation up to 14 days before"),
        )

        val parkStops = stops.filter { kb.place(it.placeId).chargesParkFee }
        if (parkStops.isNotEmpty()) {
            val cost = parkStops.sumOf { s ->
                val p = kb.place(s.placeId)
                (p.parkFee(req.month) * s.nights * (adults + kids * 0.5)).roundToInt()
            }
            result += BookableComponent(
                id = "park-fees", type = ComponentType.PARK_FEES,
                title = Text(
                    "国家公园/保护区门票：" + parkStops.joinToString("、") { kb.place(it.placeId).name.zh },
                    "Park & reserve fees: " + parkStops.joinToString(", ") { kb.place(it.placeId).name.en },
                ),
                supplier = "KWS / County reserves via operator", refId = "parks", placeId = null,
                startDate = start, units = parkStops.sumOf { it.nights }, quantity = pax,
                costUsd = cost, priceUsd = pricing.price(ComponentType.PARK_FEES, cost),
                refundPolicy = Text("门票由官方收取，出票后不可退", "Official fees, non-refundable once issued"),
            )
        }

        activityDays.forEach { (activityId, count) ->
            val a = kb.activity(activityId)
            if (a.priceUsd == 0) return@forEach
            val eligible = adults + req.childAges.count { it >= a.minAge }
            if (eligible == 0) return@forEach
            val cost = a.priceUsd * eligible * count
            result += BookableComponent(
                id = "activity-${a.id}", type = ComponentType.ACTIVITY,
                title = Text(
                    a.name.zh + (if (count > 1) " ×$count 天" else "") + " · $eligible 人",
                    a.name.en + (if (count > 1) " ×$count days" else "") + " · $eligible pax",
                ),
                supplier = "Local activity partner (sample)", refId = a.id, placeId = a.placeId,
                startDate = start, units = count, quantity = eligible,
                costUsd = cost, priceUsd = pricing.price(ComponentType.ACTIVITY, cost),
                refundPolicy = Text("出发前 7 天免费取消；天气原因取消全额退款", "Free cancellation 7 days before; full refund if weather cancels"),
            )
        }
        return result
    }

    private fun linkComponent(item: ItineraryItem, components: List<BookableComponent>): String? = when (item.type) {
        ItemType.STAY -> components.firstOrNull {
            it.type == ComponentType.STAY && item.title.en.contains(it.supplier)
        }?.id
        ItemType.ACTIVITY, ItemType.BUSINESS -> components.firstOrNull {
            it.type == ComponentType.ACTIVITY && it.title.en.startsWith(item.title.en)
        }?.id
        ItemType.FLIGHT -> "flight-intl"
        ItemType.GAME_DRIVE -> "ground"
        else -> null
    }
}
