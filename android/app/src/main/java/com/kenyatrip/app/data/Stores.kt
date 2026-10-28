package com.kenyatrip.app.data

import android.content.Context
import com.kenyatrip.app.core.ai.LlmConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

data class AppSettings(
    val aiEnabled: Boolean = false,
    val aiBaseUrl: String = "https://dashscope.aliyuncs.com/compatible-mode/v1",
    val aiApiKey: String = "",
    val aiModel: String = "qwen-plus",
    val demoSoldOut: Boolean = false,
    val demoPaymentDecline: Boolean = false,
    val demoNoAlternatives: Boolean = false,
) {
    val llmConfig: LlmConfig?
        get() = if (aiEnabled && aiApiKey.isNotBlank() && aiBaseUrl.isNotBlank()) LlmConfig(aiBaseUrl, aiApiKey, aiModel) else null
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load() = AppSettings(
        aiEnabled = prefs.getBoolean("ai_enabled", false),
        aiBaseUrl = prefs.getString("ai_base_url", null) ?: AppSettings().aiBaseUrl,
        aiApiKey = prefs.getString("ai_api_key", "") ?: "",
        aiModel = prefs.getString("ai_model", null) ?: AppSettings().aiModel,
        demoSoldOut = prefs.getBoolean("demo_sold_out", false),
        demoPaymentDecline = prefs.getBoolean("demo_payment_decline", false),
        demoNoAlternatives = prefs.getBoolean("demo_no_alternatives", false),
    )

    fun save(s: AppSettings) {
        prefs.edit()
            .putBoolean("ai_enabled", s.aiEnabled)
            .putString("ai_base_url", s.aiBaseUrl)
            .putString("ai_api_key", s.aiApiKey)
            .putString("ai_model", s.aiModel)
            .putBoolean("demo_sold_out", s.demoSoldOut)
            .putBoolean("demo_payment_decline", s.demoPaymentDecline)
            .putBoolean("demo_no_alternatives", s.demoNoAlternatives)
            .apply()
    }
}

class TripStore(context: Context) {
    private val file = File(context.filesDir, "trips.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(TripRecord.serializer())

    suspend fun load(): List<TripRecord> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        runCatching { json.decodeFromString(serializer, file.readText()) }.getOrDefault(emptyList())
    }

    suspend fun save(trips: List<TripRecord>) = withContext(Dispatchers.IO) {
        val tmp = File(file.parentFile, "trips.json.tmp")
        tmp.writeText(json.encodeToString(serializer, trips))
        tmp.renameTo(file)
    }
}
