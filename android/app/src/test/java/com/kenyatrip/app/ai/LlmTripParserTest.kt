package com.kenyatrip.app.ai

import com.kenyatrip.app.TestFixtures
import com.kenyatrip.app.core.ai.LlmConfig
import com.kenyatrip.app.core.ai.LlmTripParser
import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.Tier
import com.kenyatrip.app.core.model.TravelStyle
import com.kenyatrip.app.core.planner.ParseSource
import com.kenyatrip.app.core.planner.PlannerService
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.add
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmTripParserTest {
    private val server = MockWebServer().apply { start() }

    @After
    fun tearDown() = server.shutdown()

    private fun completion(content: String) = buildJsonObject {
        putJsonArray("choices") {
            add(buildJsonObject { putJsonObject("message") { put("role", "assistant"); put("content", content) } })
        }
    }.toString()

    private fun service() = PlannerService(
        TestFixtures.kb, TestFixtures.pricing, { TestFixtures.today },
        LlmTripParser(TestFixtures.kb, { LlmConfig(server.url("/v1").toString(), "test-key", "qwen-plus") }, { TestFixtures.today }),
    )

    @Test
    fun `llm output is validated and merged`() = runTest {
        val content = """{"destination":"KENYA","month":9,"days":9,"adults":4,"child_ages":[],
            "budget_per_person_usd":5000,"styles":["PHOTOGRAPHY","UNKNOWN"],"must_places":["masai_mara","atlantis"],
            "must_activities":["mara_balloon"],"tier":"luxury"}"""
        server.enqueue(MockResponse().setBody(completion(content)))

        val result = service().understand("九月份四个人去拍动物，坐热气球", Destination.KENYA)
        val r = result.request
        assertEquals(ParseSource.AI, result.source)
        assertEquals(9, r.month)
        assertEquals(9, r.days)
        assertEquals(4, r.adults)
        assertEquals(5000, r.budgetPerPersonUsd)
        assertEquals(setOf(TravelStyle.PHOTOGRAPHY), r.styles)
        assertEquals(setOf("masai_mara"), r.mustPlaces)
        assertEquals(Tier.LUXURY, r.tier)

        val sent = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).toString()
        assertTrue(sent.contains("qwen-plus"))
        assertFalse(sent.contains("test-key"))
    }

    @Test
    fun `server error falls back to local parser`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = service().understand("8月带孩子看动物大迁徙，8天", Destination.KENYA)
        assertEquals(ParseSource.LOCAL, result.source)
        assertEquals(8, result.request.month)
    }
}
