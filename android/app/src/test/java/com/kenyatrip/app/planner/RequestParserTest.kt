package com.kenyatrip.app.planner

import com.kenyatrip.app.TestFixtures
import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.Tier
import com.kenyatrip.app.core.model.TravelStyle
import com.kenyatrip.app.core.planner.RequestParser
import com.kenyatrip.app.core.planner.Slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestParserTest {
    private val parser = RequestParser(TestFixtures.kb) { TestFixtures.today }

    @Test
    fun `chinese family migration request`() {
        val r = parser.parse("8月带孩子看动物大迁徙，8天，人均2万", Destination.KENYA).request
        assertEquals(Destination.KENYA, r.destination)
        assertEquals(8, r.month)
        assertEquals(8, r.days)
        assertEquals(2, r.adults)
        assertEquals(1, r.childAges.size)
        assertTrue(TravelStyle.FAMILY in r.styles)
        assertEquals(2778, r.budgetPerPersonUsd)
        assertTrue("masai_mara" in r.mustPlaces)
    }

    @Test
    fun `english canton fair request switches destination to china`() {
        val result = parser.parse("Canton Fair in October, 7 days, 3 traders, budget \$2500 each", Destination.KENYA)
        val r = result.request
        assertEquals(Destination.CHINA, r.destination)
        assertEquals(10, r.month)
        assertEquals(7, r.days)
        assertEquals(3, r.adults)
        assertEquals(2500, r.budgetPerPersonUsd)
        assertTrue(TravelStyle.BUSINESS in r.styles)
        assertTrue("guangzhou" in r.mustPlaces)
        assertTrue(Slot.DESTINATION in result.recognized)
    }

    @Test
    fun `honeymoon with beach and luxury`() {
        val r = parser.parse("蜜月10天，想去海边和马赛马拉，住豪华一点", Destination.KENYA).request
        assertEquals(10, r.days)
        assertEquals(2, r.adults)
        assertEquals(Tier.LUXURY, r.tier)
        assertTrue(TravelStyle.HONEYMOON in r.styles)
        assertTrue(r.mustPlaces.containsAll(setOf("diani", "masai_mara")))
    }

    @Test
    fun `family of three with child age and summer holiday`() {
        val r = parser.parse("一家三口，孩子5岁，暑假去肯尼亚", Destination.CHINA).request
        assertEquals(Destination.KENYA, r.destination)
        assertEquals(7, r.month)
        assertEquals(2, r.adults)
        assertEquals(listOf(5), r.childAges)
    }

    @Test
    fun `origin city is not treated as a destination and balloon becomes a must`() {
        val r = parser.parse("从广州出发，9月，十天，4人摄影团，要坐热气球", Destination.KENYA).request
        assertEquals("CAN", r.originCity)
        assertEquals(Destination.KENYA, r.destination)
        assertEquals(10, r.days)
        assertEquals(4, r.adults)
        assertTrue(TravelStyle.PHOTOGRAPHY in r.styles)
        assertTrue("mara_balloon" in r.mustActivities)
        assertTrue("masai_mara" in r.mustPlaces)
    }

    @Test
    fun `total budget is split across travellers`() {
        val r = parser.parse("两个人，预算10万，9天", Destination.KENYA).request
        assertEquals(2, r.adults)
        assertEquals(6944, r.budgetPerPersonUsd)
    }

    @Test
    fun `kes budget for kenyan travellers`() {
        val r = parser.parse("Beijing and Shanghai in May, 8 days, 2 adults, KSh 350k per person", Destination.CHINA).request
        assertEquals(5, r.month)
        assertEquals(8, r.days)
        assertEquals(2713, r.budgetPerPersonUsd)
        assertTrue(r.mustPlaces.containsAll(setOf("beijing", "shanghai")))
    }

    @Test
    fun `empty input falls back to defaults`() {
        val result = parser.parse("", Destination.KENYA)
        assertEquals(12, result.request.month)
        assertEquals(8, result.request.days)
        assertNull(result.request.budgetPerPersonUsd)
        assertTrue(result.recognized.isEmpty())
    }

    @Test
    fun `chinese numerals`() {
        assertEquals(10, RequestParser.cnNumber("十"))
        assertEquals(12, RequestParser.cnNumber("十二"))
        assertEquals(20, RequestParser.cnNumber("二十"))
        assertEquals(2, RequestParser.cnNumber("两"))
        assertEquals(15, RequestParser.cnNumber("十五"))
    }
}
