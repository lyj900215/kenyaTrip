package com.kenyatrip.app.data

import com.kenyatrip.app.core.booking.BookingResult
import com.kenyatrip.app.core.booking.LegStatus
import com.kenyatrip.app.core.knowledge.KnowledgeBase
import com.kenyatrip.app.core.model.Itinerary
import com.kenyatrip.app.core.model.Lang
import com.kenyatrip.app.core.model.Text
import kotlinx.serialization.Serializable

@Serializable
data class LocalText(val zh: String, val en: String) {
    fun get(lang: Lang) = if (lang == Lang.ZH) zh else en
}

private fun Text.local() = LocalText(zh, en)

@Serializable
data class LegRecord(
    val type: String,
    val title: LocalText,
    val supplier: String,
    val reference: String?,
    val status: String,
    val priceUsd: Int,
    val refundPolicy: LocalText,
)

@Serializable
data class DayRecord(val dayNo: Int, val date: String, val base: LocalText, val items: List<ItemRecord>)

@Serializable
data class ItemRecord(val time: String, val type: String, val title: LocalText, val detail: LocalText? = null)

@Serializable
data class ChecklistItem(val id: String, val text: LocalText, val done: Boolean = false)

@Serializable
data class TripRecord(
    val bookingId: String,
    val createdAtEpochMs: Long,
    val destination: String,
    val name: LocalText,
    val startDate: String,
    val endDate: String,
    val travelers: Int,
    val tier: String,
    val status: String,
    val chargedUsd: Int,
    val refundedUsd: Int,
    val paymentMethod: String,
    val paymentRef: String?,
    val leadTraveler: String,
    val legs: List<LegRecord>,
    val days: List<DayRecord>,
    val notes: List<LocalText>,
    val checklist: List<ChecklistItem>,
) {
    companion object {
        fun from(
            itinerary: Itinerary,
            result: BookingResult,
            paymentMethod: String,
            leadTraveler: String,
            kb: KnowledgeBase,
            now: Long,
        ): TripRecord = TripRecord(
            bookingId = result.bookingId,
            createdAtEpochMs = now,
            destination = itinerary.request.destination.name,
            name = itinerary.name.local(),
            startDate = itinerary.startDate.toString(),
            endDate = itinerary.endDate.toString(),
            travelers = itinerary.request.travelers,
            tier = itinerary.tier.name,
            status = result.status.name,
            chargedUsd = result.chargedUsd,
            refundedUsd = result.refundedUsd,
            paymentMethod = paymentMethod,
            paymentRef = result.paymentRef,
            leadTraveler = leadTraveler,
            legs = result.legs.map {
                LegRecord(
                    type = it.component.type.name,
                    title = it.component.title.local(),
                    supplier = it.component.supplier,
                    reference = it.reference.takeIf { _ -> it.status == LegStatus.CONFIRMED || it.status == LegStatus.SUBSTITUTED },
                    status = it.status.name,
                    priceUsd = it.component.priceUsd,
                    refundPolicy = it.component.refundPolicy.local(),
                )
            },
            days = itinerary.days.map { d ->
                DayRecord(
                    d.dayNo, d.date.toString(), d.base.local(),
                    d.items.map { ItemRecord(it.time, it.type.name, it.title.local(), it.detail?.local()) },
                )
            },
            notes = result.notes.map { it.local() },
            checklist = checklist(itinerary, kb),
        )

        private fun checklist(itinerary: Itinerary, kb: KnowledgeBase): List<ChecklistItem> {
            val dest = itinerary.request.destination
            val rules = kb.entryRules.filter { it.destination == dest }.mapIndexed { i, r ->
                ChecklistItem("rule-$i", r.text.local())
            }
            val common = when (dest) {
                com.kenyatrip.app.core.model.Destination.KENYA -> listOf(
                    LocalText("护照有效期 6 个月以上，至少 2 页空白页", "Passport valid 6+ months with 2 blank pages"),
                    LocalText("购买含医疗转运的境外旅行保险", "Travel insurance with medical evacuation"),
                    LocalText("准备中性色衣物、防晒、望远镜", "Neutral clothing, sun protection, binoculars"),
                    LocalText("软包行李（内陆航班限重 15kg）", "Soft bags (15 kg limit on bush flights)"),
                )
                com.kenyatrip.app.core.model.Destination.CHINA -> listOf(
                    LocalText("准备邀请函/行程单与银行流水用于签证", "Invitation letter, itinerary and bank statements for the visa"),
                    LocalText("开通国际漫游或购买中国电话卡", "International roaming or a China SIM card"),
                    LocalText("准备名片与采购清单（商务行程）", "Business cards and sourcing list (business trips)"),
                    LocalText("下载翻译、地图与打车应用", "Install translation, maps and ride-hailing apps"),
                )
            }.mapIndexed { i, t -> ChecklistItem("prep-$i", t) }
            return rules + common
        }
    }
}
