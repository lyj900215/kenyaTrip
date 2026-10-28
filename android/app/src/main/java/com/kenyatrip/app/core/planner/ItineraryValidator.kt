package com.kenyatrip.app.core.planner

import com.kenyatrip.app.core.knowledge.KnowledgeBase
import com.kenyatrip.app.core.model.ComponentType
import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.Itinerary
import com.kenyatrip.app.core.model.Severity
import com.kenyatrip.app.core.model.Text
import com.kenyatrip.app.core.model.TransportMode
import com.kenyatrip.app.core.model.TravelStyle
import com.kenyatrip.app.core.model.Warning
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Deterministic checks applied to every generated itinerary. Anything the LLM or the
 * search produces must pass these before it can be booked.
 */
class ItineraryValidator(
    private val kb: KnowledgeBase,
    private val today: () -> LocalDate = { LocalDate.now() },
) {
    fun validate(it: Itinerary, maxDriveHours: Double): List<Warning> {
        val req = it.request
        val out = mutableListOf<Warning>()

        it.transfers
            .filter { e -> e.mode == TransportMode.DRIVE && e.hours > maxDriveHours }
            .forEach { e ->
                out += Warning(
                    Severity.BLOCKER,
                    Text(
                        "${kb.place(e.a).name.zh}–${kb.place(e.b).name.zh} 车程 ${e.hours} 小时，超过每日上限",
                        "Drive ${kb.place(e.a).name.en}–${kb.place(e.b).name.en} (${e.hours} h) exceeds the daily limit",
                    ),
                )
            }

        req.mustPlaces.filter { p -> it.stops.none { s -> s.placeId == p } }.forEach { p ->
            out += Warning(Severity.BLOCKER, Text("未能安排：${kb.place(p).name.zh}", "Could not include ${kb.place(p).name.en}"))
        }

        val scheduledActivities = it.components.filter { c -> c.type == ComponentType.ACTIVITY }.map { c -> c.refId }.toSet()
        req.mustActivities.forEach { id ->
            val a = kb.activity(id)
            val youngest = req.childAges.minOrNull()
            when {
                youngest != null && youngest < a.minAge -> out += Warning(
                    Severity.WARNING,
                    Text(
                        "${a.name.zh}要求 ${a.minAge} 岁以上，${youngest} 岁的孩子无法参加，已为其他成员预订",
                        "${a.name.en} requires age ${a.minAge}+; the ${youngest}-year-old cannot join, others are booked",
                    ),
                )
                req.month !in a.months -> out += Warning(
                    Severity.WARNING,
                    Text("${a.name.zh}在 ${req.month} 月不开放", "${a.name.en} is not available in ${RoutePlanner.monthName(req.month)}"),
                )
                a.priceUsd > 0 && id !in scheduledActivities -> out += Warning(
                    Severity.WARNING,
                    Text("天数不足，未能安排${a.name.zh}", "Not enough days to fit ${a.name.en}"),
                )
            }
        }

        kb.entryRules.filter { r -> r.destination == req.destination }.forEach { r -> out += Warning(r.severity, r.text) }

        if (req.destination == Destination.KENYA && req.month in 4..5) {
            out += Warning(
                Severity.INFO,
                Text("4–5 月为长雨季，部分道路泥泞，价格更低但观赏性下降", "April–May is the long rains: lower prices but muddy roads"),
            )
        }
        if (req.destination == Destination.CHINA) {
            if (TravelStyle.BUSINESS in req.styles && req.month !in RoutePlanner.CANTON_FAIR) {
                out += Warning(
                    Severity.INFO,
                    Text("广交会每年 4 月和 10 月举办，其他月份建议以市场与工厂考察为主", "The Canton Fair runs in April and October; other months focus on markets and factories"),
                )
            }
            if (req.month == 2) {
                out += Warning(
                    Severity.WARNING,
                    Text("2 月春节期间多数市场和工厂休市，交通拥挤", "Chinese New Year in February: most markets and factories close, transport is crowded"),
                )
            }
        }

        req.budgetPerPersonUsd?.let { budget ->
            if (it.pricePerPersonUsd > budget * 1.15) {
                val over = (it.pricePerPersonUsd - budget) * 100 / budget
                out += Warning(Severity.WARNING, Text("人均价格超出预算约 $over%", "About $over% over your per-person budget"))
            }
        }

        val lead = ChronoUnit.DAYS.between(today(), it.startDate)
        if (lead < 30) {
            out += Warning(
                Severity.WARNING,
                Text("距出发不足 30 天，营地与签证时间紧张", "Less than 30 days before departure: limited camp availability and visa time"),
            )
        }
        return out.sortedBy { w -> -w.severity.ordinal }
    }
}
