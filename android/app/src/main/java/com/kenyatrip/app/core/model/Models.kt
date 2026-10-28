package com.kenyatrip.app.core.model

import java.time.LocalDate

enum class Lang { ZH, EN }

data class Text(val zh: String, val en: String) {
    operator fun invoke(lang: Lang): String = if (lang == Lang.ZH) zh else en

    companion object {
        fun same(value: String) = Text(value, value)
    }
}

enum class Destination { KENYA, CHINA }

enum class TravelStyle(val label: Text) {
    FAMILY(Text("亲子", "Family")),
    PHOTOGRAPHY(Text("摄影", "Photography")),
    HONEYMOON(Text("蜜月", "Honeymoon")),
    SENIOR(Text("长辈同行", "Seniors")),
    BUSINESS(Text("商务采购", "Business")),
    ADVENTURE(Text("探险", "Adventure")),
    BEACH(Text("海滩", "Beach")),
    CULTURE(Text("人文", "Culture")),
    RELAXED(Text("轻松慢游", "Relaxed")),
}

enum class Tier(val label: Text) {
    BUDGET(Text("经济", "Budget")),
    COMFORT(Text("舒适", "Comfort")),
    LUXURY(Text("豪华", "Luxury"));

    fun up(): Tier? = entries.getOrNull(ordinal + 1)
    fun down(): Tier? = entries.getOrNull(ordinal - 1)
}

data class TripRequest(
    val destination: Destination,
    val month: Int,
    val days: Int,
    val adults: Int = 2,
    val childAges: List<Int> = emptyList(),
    val budgetPerPersonUsd: Int? = null,
    val styles: Set<TravelStyle> = emptySet(),
    val mustPlaces: Set<String> = emptySet(),
    val avoidPlaces: Set<String> = emptySet(),
    val mustActivities: Set<String> = emptySet(),
    val tier: Tier? = null,
    val maxDailyDriveHours: Double? = null,
    val originCity: String? = null,
    val startDate: LocalDate? = null,
) {
    val travelers: Int get() = adults + childAges.size
    val nights: Int get() = days - 1
    val hasChildren: Boolean get() = childAges.isNotEmpty()
}

enum class ComponentType { FLIGHT, DOMESTIC_TRANSPORT, STAY, GROUND, PARK_FEES, ACTIVITY }

/**
 * A single bookable unit of an itinerary. [costUsd] is the supplier net cost,
 * [priceUsd] is what the traveler pays after platform markup.
 */
data class BookableComponent(
    val id: String,
    val type: ComponentType,
    val title: Text,
    val supplier: String,
    val refId: String,
    val placeId: String?,
    val startDate: LocalDate,
    val units: Int,
    val quantity: Int,
    val costUsd: Int,
    val priceUsd: Int,
    val refundPolicy: Text,
    val tier: Tier? = null,
)

/** FLIGHT is the international flight; domestic legs use TRANSFER (road), TRAIN or DOMESTIC_FLIGHT. */
enum class ItemType { FLIGHT, TRANSFER, TRAIN, DOMESTIC_FLIGHT, GAME_DRIVE, ACTIVITY, STAY, MEAL, FREE, BUSINESS }

data class ItineraryItem(
    val time: String,
    val type: ItemType,
    val title: Text,
    val detail: Text? = null,
    val componentId: String? = null,
)

data class ItineraryDay(
    val dayNo: Int,
    val date: LocalDate,
    val base: Text,
    val items: List<ItineraryItem>,
)

data class Stop(val placeId: String, val nights: Int, val lodgeId: String?)

enum class Severity { INFO, WARNING, BLOCKER }

data class Warning(val severity: Severity, val text: Text)

data class Itinerary(
    val id: String,
    val request: TripRequest,
    val name: Text,
    val stops: List<Stop>,
    val transfers: List<Edge>,
    val tier: Tier,
    val startDate: LocalDate,
    val days: List<ItineraryDay>,
    val components: List<BookableComponent>,
    val reasons: List<Text>,
    val warnings: List<Warning>,
    val score: Double,
) {
    val endDate: LocalDate get() = startDate.plusDays((request.days - 1).toLong())
    val totalPriceUsd: Int get() = components.sumOf { it.priceUsd }
    val totalCostUsd: Int get() = components.sumOf { it.costUsd }
    val pricePerPersonUsd: Int get() = totalPriceUsd / request.travelers.coerceAtLeast(1)
    val feasible: Boolean get() = warnings.none { it.severity == Severity.BLOCKER }
}

enum class PlaceKind { GATEWAY, PARK, LAKE, CITY, BEACH }

enum class TransportMode { DRIVE, FLIGHT, TRAIN }

data class Place(
    val id: String,
    val destination: Destination,
    val name: Text,
    val kind: PlaceKind,
    val aliases: List<String>,
    /** Experience score 0–10 for each month, index 0 = January. */
    val seasonScore: List<Int>,
    val affinity: Map<TravelStyle, Double>,
    val minNights: Int,
    val maxNights: Int,
    val highlight: Text,
    val parkFeePeakUsd: Int = 0,
    val parkFeeLowUsd: Int = 0,
    val parkPeakMonths: Set<Int> = emptySet(),
    val hotelPeakMonths: Set<Int> = emptySet(),
    val hotelPeakMultiplier: Double = 1.0,
    val internationalAirport: Boolean = false,
) {
    fun season(month: Int): Int = seasonScore[month - 1]
    fun parkFee(month: Int): Int = if (month in parkPeakMonths) parkFeePeakUsd else parkFeeLowUsd
    val chargesParkFee: Boolean get() = parkFeePeakUsd > 0
}

enum class Slot { DAWN, MORNING, AFTERNOON, EVENING, FULL_DAY }

data class Activity(
    val id: String,
    val placeId: String,
    val name: Text,
    val priceUsd: Int,
    val minAge: Int,
    val slot: Slot,
    val styles: Set<TravelStyle>,
    val aliases: List<String>,
    /** Added automatically when no style filter applies. */
    val default: Boolean = false,
    val maxDays: Int = 1,
    val months: Set<Int> = (1..12).toSet(),
)

data class Lodge(
    val id: String,
    val placeId: String,
    val name: Text,
    val tier: Tier,
    /** Per person per night, sharing a double room. */
    val pricePerNightUsd: Int,
    val chineseFriendly: Boolean = false,
)

data class Edge(
    val a: String,
    val b: String,
    val mode: TransportMode,
    val hours: Double,
    val pricePerPersonUsd: Int = 0,
) {
    fun connects(x: String, y: String) = (a == x && b == y) || (a == y && b == x)
    fun other(x: String) = if (a == x) b else a
}

data class EntryRule(val destination: Destination, val text: Text, val severity: Severity)

data class OriginCity(val id: String, val name: Text, val aliases: List<String>)
