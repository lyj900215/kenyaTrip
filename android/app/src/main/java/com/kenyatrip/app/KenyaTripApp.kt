package com.kenyatrip.app

import android.app.Application
import com.kenyatrip.app.core.ai.LlmTripParser
import com.kenyatrip.app.core.knowledge.KnowledgeBase
import com.kenyatrip.app.core.planner.PlannerService
import com.kenyatrip.app.core.pricing.PricingEngine
import com.kenyatrip.app.data.SettingsStore
import com.kenyatrip.app.data.TripStore

class KenyaTripApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(app: Application) {
    val kb: KnowledgeBase = KnowledgeBase.default
    val pricing = PricingEngine()
    val settingsStore = SettingsStore(app)
    val tripStore = TripStore(app)
    val llmParser = LlmTripParser(kb, { settingsStore.load().llmConfig })
    val planner = PlannerService(kb, pricing, aiParser = llmParser)
}
