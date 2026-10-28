package com.kenyatrip.app.core.booking

import com.kenyatrip.app.core.model.BookableComponent
import com.kenyatrip.app.core.model.ComponentType
import com.kenyatrip.app.core.model.Itinerary
import com.kenyatrip.app.core.model.Text
import com.kenyatrip.app.core.supply.ConfirmResult
import com.kenyatrip.app.core.supply.HoldResult
import com.kenyatrip.app.core.supply.SupplyNetwork
import java.util.UUID
import kotlin.math.abs

enum class BookingStep { REPRICE, HOLD, PAY, CONFIRM, DONE }

enum class LegStatus { PENDING, HELD, CONFIRMED, SUBSTITUTED, FAILED, CANCELLED, RELEASED }

data class BookingLeg(
    val component: BookableComponent,
    val status: LegStatus = LegStatus.PENDING,
    val holdId: String? = null,
    val reference: String? = null,
    val replaced: BookableComponent? = null,
)

enum class BookingStatus { BOOKED, PRICE_CHANGED, FAILED_NO_CHARGE, PAYMENT_FAILED, REFUNDED }

data class BookingResult(
    val bookingId: String,
    val status: BookingStatus,
    val legs: List<BookingLeg>,
    val quotedTotalUsd: Int,
    val chargedUsd: Int,
    val refundedUsd: Int,
    val platformAbsorbedUsd: Int,
    val paymentRef: String?,
    val notes: List<Text>,
)

sealed interface BookingEvent {
    data class StepStarted(val step: BookingStep) : BookingEvent
    data class LegsUpdated(val legs: List<BookingLeg>) : BookingEvent
    data class Note(val text: Text) : BookingEvent
}

/**
 * One-click booking as a saga: reprice → hold everything → take one payment → confirm legs
 * (scarcest first) → on failure substitute an equivalent product or compensate by cancelling
 * confirmed legs in reverse order and refunding.
 */
class BookingOrchestrator(
    private val supply: SupplyNetwork,
    private val payment: PaymentGateway,
    private val priceChangeThreshold: Double = 0.02,
    private val maxAbsorbPct: Double = 0.15,
    private val newId: () -> String = { "BK-" + UUID.randomUUID().toString().take(8).uppercase() },
) {
    suspend fun book(
        itinerary: Itinerary,
        method: PaymentMethod,
        acceptPriceChange: Boolean = false,
        onEvent: (BookingEvent) -> Unit = {},
    ): BookingResult {
        val bookingId = newId()
        val legs = itinerary.components.map { BookingLeg(it) }.toMutableList()
        val notes = mutableListOf<Text>()
        var customerTotal: Int
        var charged = 0
        var refunded = 0
        var absorbed = 0
        var paymentRef: String? = null

        fun key(c: BookableComponent) = "$bookingId:${c.id}"
        fun emit() = onEvent(BookingEvent.LegsUpdated(legs.toList()))
        fun note(text: Text) {
            notes += text
            onEvent(BookingEvent.Note(text))
        }
        fun result(status: BookingStatus, quoted: Int) =
            BookingResult(bookingId, status, legs.toList(), quoted, charged, refunded, absorbed, paymentRef, notes.toList())

        suspend fun releaseHolds() {
            legs.indices.filter { legs[it].status == LegStatus.HELD }.forEach { i ->
                legs[i].holdId?.let { supply.adapterFor(legs[i].component.type).release(it) }
                legs[i] = legs[i].copy(status = LegStatus.RELEASED)
            }
            emit()
        }

        /** A substitute is acceptable if it is cheaper, or pricier within the absorb limit. */
        fun acceptable(original: BookableComponent, alt: BookableComponent): Boolean =
            alt.priceUsd <= original.priceUsd * (1 + maxAbsorbPct)

        onEvent(BookingEvent.StepStarted(BookingStep.REPRICE))
        legs.indices.forEach { i ->
            val c = legs[i].component
            val q = supply.adapterFor(c.type).quote(c)
            legs[i] = legs[i].copy(component = c.copy(costUsd = q.costUsd, priceUsd = q.priceUsd))
        }
        emit()
        customerTotal = legs.sumOf { it.component.priceUsd }
        val original = itinerary.totalPriceUsd.coerceAtLeast(1)
        if (abs(customerTotal - original).toDouble() / original > priceChangeThreshold && !acceptPriceChange) {
            note(Text("价格已变动，需要确认新价格", "Price changed, please confirm the new total"))
            return result(BookingStatus.PRICE_CHANGED, customerTotal)
        }
        val quotedTotal = customerTotal

        onEvent(BookingEvent.StepStarted(BookingStep.HOLD))
        for (i in legs.indices) {
            val c = legs[i].component
            val adapter = supply.adapterFor(c.type)
            when (val h = adapter.hold(c, key(c))) {
                is HoldResult.Held -> legs[i] = legs[i].copy(status = LegStatus.HELD, holdId = h.holdId)
                is HoldResult.Unavailable -> {
                    val alt = adapter.alternatives(c).filter { acceptable(c, it) }.firstNotNullOfOrNull { a ->
                        (adapter.hold(a, key(a)) as? HoldResult.Held)?.let { a to it.holdId }
                    }
                    if (alt == null) {
                        legs[i] = legs[i].copy(status = LegStatus.FAILED)
                        note(Text("${c.title.zh} 无库存且无可替代产品，未扣款", "${c.title.en} unavailable with no substitute; nothing was charged"))
                        releaseHolds()
                        return result(BookingStatus.FAILED_NO_CHARGE, quotedTotal)
                    }
                    val (a, holdId) = alt
                    val diff = a.priceUsd - c.priceUsd
                    if (diff > 0) absorbed += diff else customerTotal += diff
                    legs[i] = BookingLeg(a, LegStatus.HELD, holdId, replaced = c)
                    note(substitutionNote(c, a, diff))
                }
            }
            emit()
        }

        onEvent(BookingEvent.StepStarted(BookingStep.PAY))
        when (val p = payment.charge(customerTotal, method, "$bookingId:payment")) {
            is PaymentResult.Declined -> {
                note(p.reason)
                releaseHolds()
                return result(BookingStatus.PAYMENT_FAILED, quotedTotal)
            }
            is PaymentResult.Paid -> {
                paymentRef = p.reference
                charged = customerTotal
            }
        }

        onEvent(BookingEvent.StepStarted(BookingStep.CONFIRM))
        val order = legs.indices.sortedBy { PRIORITY.indexOf(legs[it].component.type) }
        val confirmedOrder = mutableListOf<Int>()
        for (i in order) {
            val leg = legs[i]
            val adapter = supply.adapterFor(leg.component.type)
            val r = adapter.confirm(leg.component, leg.holdId.orEmpty(), paymentRef!!, key(leg.component))
            if (r is ConfirmResult.Confirmed) {
                legs[i] = leg.copy(status = if (leg.replaced != null) LegStatus.SUBSTITUTED else LegStatus.CONFIRMED, reference = r.reference)
                confirmedOrder += i
                emit()
                continue
            }
            val c = leg.component
            var substituted = false
            for (a in adapter.alternatives(c).filter { acceptable(c, it) }) {
                val held = adapter.hold(a, key(a)) as? HoldResult.Held ?: continue
                val ok = adapter.confirm(a, held.holdId, paymentRef, key(a)) as? ConfirmResult.Confirmed ?: continue
                val diff = a.priceUsd - c.priceUsd
                if (diff > 0) {
                    absorbed += diff
                } else if (diff < 0 && payment.refund(paymentRef, -diff)) {
                    refunded += -diff
                }
                legs[i] = BookingLeg(a, LegStatus.SUBSTITUTED, held.holdId, ok.reference, replaced = leg.replaced ?: c)
                confirmedOrder += i
                note(substitutionNote(c, a, diff))
                substituted = true
                break
            }
            if (substituted) {
                emit()
                continue
            }

            legs[i] = leg.copy(status = LegStatus.FAILED)
            note(Text("${c.title.zh} 确认失败且无可替代产品，已自动取消全部预订并退款", "${c.title.en} failed with no substitute; everything was cancelled and refunded"))
            for (j in confirmedOrder.reversed()) {
                val l = legs[j]
                l.reference?.let { supply.adapterFor(l.component.type).cancel(it) }
                legs[j] = l.copy(status = LegStatus.CANCELLED)
                emit()
            }
            releaseHolds()
            val remaining = charged - refunded
            if (remaining > 0 && payment.refund(paymentRef, remaining)) refunded += remaining
            return result(BookingStatus.REFUNDED, quotedTotal)
        }

        onEvent(BookingEvent.StepStarted(BookingStep.DONE))
        return result(BookingStatus.BOOKED, quotedTotal)
    }

    private fun substitutionNote(from: BookableComponent, to: BookableComponent, diff: Int): Text {
        val (zh, en) = when {
            diff > 0 -> "差价由平台承担" to "the platform covers the difference"
            diff < 0 -> "差价已退还" to "the difference is refunded"
            else -> "价格不变" to "same price"
        }
        return Text(
            "${from.title.zh} 已满，已自动更换为 ${to.title.zh}，$zh",
            "${from.title.en} was sold out; switched to ${to.title.en}, $en",
        )
    }

    companion object {
        /** Confirm the hardest-to-replace inventory first so late failures are cheap to unwind. */
        val PRIORITY = listOf(
            ComponentType.STAY,
            ComponentType.FLIGHT,
            ComponentType.DOMESTIC_TRANSPORT,
            ComponentType.GROUND,
            ComponentType.PARK_FEES,
            ComponentType.ACTIVITY,
        )
    }
}
