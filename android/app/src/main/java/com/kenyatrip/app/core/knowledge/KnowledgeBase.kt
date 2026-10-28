package com.kenyatrip.app.core.knowledge

import com.kenyatrip.app.core.model.Activity
import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.Edge
import com.kenyatrip.app.core.model.EntryRule
import com.kenyatrip.app.core.model.Lodge
import com.kenyatrip.app.core.model.OriginCity
import com.kenyatrip.app.core.model.Place
import com.kenyatrip.app.core.model.Tier
import com.kenyatrip.app.core.model.TransportMode

class KnowledgeBase(
    val places: List<Place>,
    val activities: List<Activity>,
    val lodges: List<Lodge>,
    val edges: List<Edge>,
    val entryRules: List<EntryRule>,
    val originCities: Map<Destination, List<OriginCity>>,
    private val gateways: Map<Destination, String>,
    val ground: Map<Destination, GroundRates>,
    val flights: Map<Destination, FlightRates>,
) {
    private val placeById = places.associateBy { it.id }
    private val activityById = activities.associateBy { it.id }
    private val lodgeById = lodges.associateBy { it.id }

    fun place(id: String): Place = placeById.getValue(id)
    fun activity(id: String): Activity = activityById.getValue(id)
    fun lodge(id: String): Lodge = lodgeById.getValue(id)

    fun gateway(destination: Destination): Place = place(gateways.getValue(destination))

    fun placesIn(destination: Destination) = places.filter { it.destination == destination }

    fun activitiesAt(placeId: String) = activities.filter { it.placeId == placeId }

    fun lodgesAt(placeId: String, tier: Tier? = null) =
        lodges.filter { it.placeId == placeId && (tier == null || it.tier == tier) }

    /**
     * Best way to travel between two places: the shortest overland option that fits the
     * daily limit, otherwise a flight.
     */
    fun bestEdge(from: String, to: String, maxDriveHours: Double): Edge? {
        val options = edges.filter { it.connects(from, to) }
        val overland = options
            .filter {
                (it.mode == TransportMode.DRIVE && it.hours <= maxDriveHours) ||
                    (it.mode == TransportMode.TRAIN && it.hours <= maxDriveHours + TRAIN_SLACK_HOURS)
            }
            .minByOrNull { it.hours }
        return overland ?: options.filter { it.mode == TransportMode.FLIGHT }.minByOrNull { it.hours }
    }

    fun matchPlaces(text: String, destination: Destination): Set<String> {
        val lower = text.lowercase()
        return placesIn(destination)
            .filter { p -> p.aliases.any { lower.contains(it.lowercase()) } }
            .map { it.id }
            .toSet()
    }

    fun matchActivities(text: String, destination: Destination): Set<String> {
        val lower = text.lowercase()
        val ids = placesIn(destination).map { it.id }.toSet()
        return activities
            .filter { it.placeId in ids && it.aliases.any { a -> lower.contains(a.lowercase()) } }
            .map { it.id }
            .toSet()
    }

    fun matchOrigin(text: String, destination: Destination): OriginCity? {
        val lower = text.lowercase()
        return originCities[destination].orEmpty().firstOrNull { c -> c.aliases.any { lower.contains(it.lowercase()) } }
    }

    companion object {
        /** Train time is less tiring than road time, so trains get extra allowance. */
        const val TRAIN_SLACK_HOURS = 1.5

        val default: KnowledgeBase by lazy { build() }

        fun build(): KnowledgeBase = KnowledgeBase(
            places = KenyaData.places + ChinaData.places,
            activities = KenyaData.activities + ChinaData.activities,
            lodges = KenyaData.lodges + ChinaData.lodges,
            edges = KenyaData.edges + ChinaData.edges,
            entryRules = KenyaData.entryRules + ChinaData.entryRules,
            originCities = mapOf(
                Destination.KENYA to KenyaData.originCities,
                Destination.CHINA to ChinaData.originCities,
            ),
            gateways = mapOf(Destination.KENYA to "nairobi", Destination.CHINA to "guangzhou"),
            ground = mapOf(Destination.KENYA to KenyaData.ground, Destination.CHINA to ChinaData.ground),
            flights = mapOf(Destination.KENYA to KenyaData.flights, Destination.CHINA to ChinaData.flights),
        )
    }
}

data class GroundRates(
    val supplier: String,
    val vehiclePerDayUsd: Int,
    val seatsPerVehicle: Int,
    val photoSeatsPerVehicle: Int,
    val guidePerDayUsd: Int,
    val guideLabel: com.kenyatrip.app.core.model.Text,
    val vehicleLabel: com.kenyatrip.app.core.model.Text,
)

data class FlightRates(
    val supplier: String,
    val roundTripUsd: Int,
    val peakMonths: Set<Int>,
    val peakMultiplier: Double,
    val childFactor: Double = 0.75,
)
