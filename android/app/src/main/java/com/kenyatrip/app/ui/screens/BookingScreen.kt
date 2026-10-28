package com.kenyatrip.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kenyatrip.app.core.booking.BookingStatus
import com.kenyatrip.app.core.booking.BookingStep
import com.kenyatrip.app.core.booking.LegStatus
import com.kenyatrip.app.core.model.Itinerary
import com.kenyatrip.app.core.pricing.Currency
import com.kenyatrip.app.core.pricing.Money
import com.kenyatrip.app.ui.BookingUi
import com.kenyatrip.app.ui.S
import com.kenyatrip.app.ui.components.IconBadge
import com.kenyatrip.app.ui.components.Pill
import com.kenyatrip.app.ui.components.SectionCard
import com.kenyatrip.app.ui.components.componentIcon
import com.kenyatrip.app.ui.t
import com.kenyatrip.app.ui.theme.Danger
import com.kenyatrip.app.ui.theme.Info
import com.kenyatrip.app.ui.theme.Success
import com.kenyatrip.app.ui.theme.Sunset

@Composable
fun BookingScreen(
    itinerary: Itinerary,
    booking: BookingUi,
    onAcceptPriceChange: () -> Unit,
    onBack: () -> Unit,
    onViewTrip: (String) -> Unit,
    onHome: () -> Unit,
    padding: PaddingValues,
) {
    val currency = Currency.forDestination(itinerary.request.destination)
    val result = booking.result

    if (result?.status == BookingStatus.PRICE_CHANGED) {
        AlertDialog(
            onDismissRequest = onBack,
            title = { Text(t(S.priceChanged)) },
            text = { Text(t(S.priceChangedBody).format(Money.format(result.quotedTotalUsd, currency))) },
            confirmButton = { TextButton(onClick = onAcceptPriceChange) { Text(t(S.continueBooking)) } },
            dismissButton = { TextButton(onClick = onBack) { Text(t(S.cancel)) } },
        )
    }

    Column(
        Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard {
            val steps = listOf(
                BookingStep.REPRICE to S.stepReprice,
                BookingStep.HOLD to S.stepHold,
                BookingStep.PAY to S.stepPay,
                BookingStep.CONFIRM to S.stepConfirm,
                BookingStep.DONE to S.stepDone,
            )
            val current = booking.step?.ordinal ?: -1
            val finished = result != null
            steps.forEach { (step, label) ->
                val done = step.ordinal < current || (finished && result?.status == BookingStatus.BOOKED)
                val active = step.ordinal == current && !finished
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        done -> Icon(Icons.Filled.CheckCircle, null, tint = Success)
                        active -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else -> Icon(Icons.Filled.RadioButtonUnchecked, null, tint = MaterialTheme.colorScheme.outline)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(t(label), style = MaterialTheme.typography.bodyLarge, color = if (done || active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
                }
            }
        }

        AnimatedVisibility(result != null && result.status != BookingStatus.PRICE_CHANGED) {
            result?.let { r ->
                val (title, color) = when (r.status) {
                    BookingStatus.BOOKED -> S.booked to Success
                    BookingStatus.REFUNDED -> S.refunded to Sunset
                    BookingStatus.FAILED_NO_CHARGE -> S.failedNoCharge to Danger
                    BookingStatus.PAYMENT_FAILED -> S.paymentFailed to Danger
                    BookingStatus.PRICE_CHANGED -> S.priceChanged to Info
                }
                SectionCard(container = color.copy(alpha = 0.1f), modifier = Modifier.testTag("booking-result")) {
                    Text(t(title), style = MaterialTheme.typography.headlineMedium, color = color)
                    if (r.status == BookingStatus.BOOKED) Text(t(S.bookedSub))
                    Text(
                        t(S.charged) + " " + Money.format(r.chargedUsd, currency) +
                            (if (r.refundedUsd > 0) " · " + t(S.refundedAmount) + " " + Money.format(r.refundedUsd, currency) else "") +
                            (if (r.platformAbsorbedUsd > 0) " · " + t(S.platformCovered) + " " + Money.format(r.platformAbsorbedUsd, currency) else ""),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(t(S.bookingRef) + " " + r.bookingId, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (r.status == BookingStatus.BOOKED || r.status == BookingStatus.REFUNDED) {
                            Button(onClick = { onViewTrip(r.bookingId) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.testTag("view-trip")) {
                                Text(t(S.viewTrip))
                            }
                        } else {
                            Button(onClick = onBack, shape = RoundedCornerShape(14.dp)) { Text(t(S.retry)) }
                        }
                        OutlinedButton(onClick = onHome, shape = RoundedCornerShape(14.dp)) { Text(t(S.backHome)) }
                    }
                }
            }
        }

        booking.notes.forEach { n ->
            SectionCard(container = MaterialTheme.colorScheme.secondaryContainer) {
                Row {
                    Icon(Icons.Filled.SwapHoriz, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(t(n), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        SectionCard(title = t(S.bookings)) {
            val legs = booking.legs.ifEmpty { itinerary.components.map { com.kenyatrip.app.core.booking.BookingLeg(it) } }
            legs.forEach { leg ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(componentIcon(leg.component.type), MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t(leg.component.title), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            leg.component.supplier + (leg.reference?.let { " · " + t(S.bookingRef) + " " + it } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LegStatusPill(leg.status)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun LegStatusPill(status: LegStatus) {
    val (label, color) = when (status) {
        LegStatus.PENDING -> com.kenyatrip.app.core.model.Text("待处理", "Pending") to Color(0xFF9E9384)
        LegStatus.HELD -> com.kenyatrip.app.core.model.Text("已锁定", "Held") to Info
        LegStatus.CONFIRMED -> com.kenyatrip.app.core.model.Text("已确认", "Confirmed") to Success
        LegStatus.SUBSTITUTED -> com.kenyatrip.app.core.model.Text("已替换", "Substituted") to Sunset
        LegStatus.FAILED -> com.kenyatrip.app.core.model.Text("失败", "Failed") to Danger
        LegStatus.CANCELLED -> com.kenyatrip.app.core.model.Text("已取消", "Cancelled") to Color(0xFF6D6457)
        LegStatus.RELEASED -> com.kenyatrip.app.core.model.Text("已释放", "Released") to Color(0xFF6D6457)
    }
    Pill(t(label), color)
}