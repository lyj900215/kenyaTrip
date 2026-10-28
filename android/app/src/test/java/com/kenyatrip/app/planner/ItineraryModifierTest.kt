package com.kenyatrip.app.planner

import com.kenyatrip.app.TestFixtures
import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.Tier
import com.kenyatrip.app.core.model.TransportMode
import com.kenyatrip.app.core.model.TripRequest
import com.kenyatrip.app.core.planner.ModifyOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItineraryModifierTest {
    private val service = TestFixtures.planner()
    private val base = service.recommend(
        TripRequest(destination = Destination.KENYA, month = 8, days = 9, adults = 2, mustPlaces = setOf("amboseli", "masai_mara")),
    ).first()

    private fun updated(text: String) = service.modify(base, text) as ModifyOutcome.Updated

    @Test
    fun `relaxed lowers the drive limit`() {
        val out = updated("轻松一点，别太赶")
        val limit = out.itinerary.request.maxDailyDriveHours!!
        assertTrue(limit < 6.0)
        assertTrue(out.itinerary.transfers.filter { it.mode == TransportMode.DRIVE }.all { it.hours <= limit })
    }

    @Test
    fun `remove a place`() {
        val out = updated("不去安博塞利了")
        assertFalse(out.itinerary.stops.any { it.placeId == "amboseli" })
    }

    @Test
    fun `add the balloon`() {
        val out = updated("加热气球")
        assertTrue(out.itinerary.components.any { it.refId == "mara_balloon" })
    }

    @Test
    fun `cheaper and upgrade move the tier`() {
        assertEquals(Tier.BUDGET, updated("便宜一点").itinerary.tier)
        assertEquals(Tier.LUXURY, updated("upgrade the hotels").itinerary.tier)
    }

    @Test
    fun `change trip length`() {
        assertEquals(10, updated("多一天").itinerary.request.days)
        assertEquals(7, updated("改成7天").itinerary.days.size)
    }

    @Test
    fun `unknown request gives a hint`() {
        assertTrue(service.modify(base, "嗯嗯好的") is ModifyOutcome.NotUnderstood)
    }
}
