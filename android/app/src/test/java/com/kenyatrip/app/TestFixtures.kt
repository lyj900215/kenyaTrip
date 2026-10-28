package com.kenyatrip.app

import com.kenyatrip.app.core.knowledge.KnowledgeBase
import com.kenyatrip.app.core.planner.PlannerService
import com.kenyatrip.app.core.pricing.PricingEngine
import java.time.LocalDate

object TestFixtures {
    val today: LocalDate = LocalDate.of(2026, 10, 28)
    val kb: KnowledgeBase = KnowledgeBase.build()
    val pricing = PricingEngine()
    fun planner() = PlannerService(kb, pricing, { today })
}
