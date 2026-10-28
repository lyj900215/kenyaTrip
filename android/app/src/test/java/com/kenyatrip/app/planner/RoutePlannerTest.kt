package com.kenyatrip.app.planner

import com.kenyatrip.app.TestFixtures
import com.kenyatrip.app.core.model.ComponentType
import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.Severity
import com.kenyatrip.app.core.model.Tier
import com.kenyatrip.app.core.model.TransportMode
import com.kenyatrip.app.core.model.TravelStyle
import com.kenyatrip.app.core.model.TripRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutePlannerTest {
    private val service = TestFixtures.planner()

    private val familyAugust = TripRequest(
        destination = Destination.KENYA, month = 8, days = 8, adults = 2, childAges = listOf(10),
        styles = setOf(TravelStyle.FAMILY),
    )

    @Test
    fun `returns three diverse feasible kenya itineraries`() {
        val results = service.recommend(familyAugust)
        assertEquals(3, results.size)
        assertEquals(3, results.map { it.stops.map { s -> s.placeId }.toSet() }.toSet().size)
        results.forEach { itn ->
            assertTrue(itn.feasible)
            assertEquals(8, itn.days.size)
            assertEquals(7, itn.stops.sumOf { it.nights })
            assertTrue(itn.days.all { it.items.isNotEmpty() })
            val types = itn.components.map { it.type }.toSet()
            assertTrue(types.containsAll(listOf(ComponentType.FLIGHT, ComponentType.STAY, ComponentType.GROUND, ComponentType.PARK_FEES)))
            assertTrue(itn.transfers.filter { it.mode == TransportMode.DRIVE }.all { it.hours <= 5.0 })
            assertTrue(itn.reasons.isNotEmpty())
        }
        assertTrue(results.first().stops.any { it.placeId == "masai_mara" })
    }

    @Test
    fun `august migration route name and peak park fee`() {
        val top = service.recommend(familyAugust.copy(mustPlaces = setOf("masai_mara"))).first()
        assertTrue(top.name.zh.startsWith("大迁徙"))
        val fees = top.components.first { it.type == ComponentType.PARK_FEES }
        assertTrue(fees.costUsd >= 200 * top.stops.first { it.placeId == "masai_mara" }.nights)
    }

    @Test
    fun `budget drives the stay tier`() {
        val tight = service.recommend(familyAugust.copy(budgetPerPersonUsd = 2200)).first()
        val generous = service.recommend(familyAugust.copy(budgetPerPersonUsd = 20000)).first()
        assertTrue(tight.tier < generous.tier)
        assertEquals(Tier.LUXURY, generous.tier)
    }

    @Test
    fun `must and avoid places are respected`() {
        val results = service.recommend(familyAugust.copy(mustPlaces = setOf("amboseli"), avoidPlaces = setOf("nakuru")))
        assertTrue(results.isNotEmpty())
        results.forEach { itn ->
            assertTrue(itn.stops.any { it.placeId == "amboseli" })
            assertFalse(itn.stops.any { it.placeId == "nakuru" })
        }
    }

    @Test
    fun `balloon with a young child books adults only and warns`() {
        val req = TripRequest(
            destination = Destination.KENYA, month = 9, days = 7, adults = 2, childAges = listOf(5),
            styles = setOf(TravelStyle.FAMILY), mustActivities = setOf("mara_balloon"), mustPlaces = setOf("masai_mara"),
        )
        val itn = service.recommend(req).first()
        val balloon = itn.components.first { it.refId == "mara_balloon" }
        assertEquals(2, balloon.quantity)
        assertTrue(itn.warnings.any { it.severity == Severity.WARNING && it.text.en.contains("age 7+") })
    }

    @Test
    fun `canton fair trip for kenyan traders`() {
        val req = TripRequest(
            destination = Destination.CHINA, month = 10, days = 7, adults = 3,
            styles = setOf(TravelStyle.BUSINESS), mustPlaces = setOf("guangzhou"), budgetPerPersonUsd = 2500,
        )
        val itn = service.recommend(req).first()
        assertEquals(15, itn.startDate.dayOfMonth)
        assertEquals(10, itn.startDate.monthValue)
        assertTrue(itn.name.en.startsWith("Canton Fair"))
        assertTrue(itn.components.any { it.refId == "canton_fair" })
        assertTrue(TestFixtures.kb.place(itn.stops.first().placeId).internationalAirport)
        assertTrue(TestFixtures.kb.place(itn.stops.last().placeId).internationalAirport)
        assertTrue(itn.warnings.any { it.text.en.contains("Chinese visa") })
    }

    @Test
    fun `long trips are planned quickly`() {
        val started = System.currentTimeMillis()
        val results = service.recommend(familyAugust.copy(days = 16, styles = emptySet()))
        val elapsed = System.currentTimeMillis() - started
        assertTrue(results.isNotEmpty())
        assertTrue("planning took $elapsed ms", elapsed < 5000)
    }

    @Test
    fun `start date is at least three weeks away`() {
        val itn = service.recommend(familyAugust.copy(month = 11)).first()
        assertEquals(2027, itn.startDate.year)
    }
}
