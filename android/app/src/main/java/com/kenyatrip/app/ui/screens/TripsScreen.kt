package com.kenyatrip.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kenyatrip.app.core.booking.LegStatus
import com.kenyatrip.app.core.model.ComponentType
import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.ItemType
import com.kenyatrip.app.core.pricing.Currency
import com.kenyatrip.app.core.pricing.Money
import com.kenyatrip.app.data.TripRecord
import com.kenyatrip.app.ui.LocalLang
import com.kenyatrip.app.ui.S
import com.kenyatrip.app.ui.components.IconBadge
import com.kenyatrip.app.ui.components.Pill
import com.kenyatrip.app.ui.components.SectionCard
import com.kenyatrip.app.ui.components.componentIcon
import com.kenyatrip.app.ui.components.itemIcon
import com.kenyatrip.app.ui.formatDate
import com.kenyatrip.app.ui.formatDateRange
import com.kenyatrip.app.ui.t
import com.kenyatrip.app.ui.theme.Success
import com.kenyatrip.app.ui.theme.Sunset
import java.time.LocalDate

private fun TripRecord.currency() = Currency.forDestination(Destination.valueOf(destination))

@Composable
private fun StatusPill(status: String) {
    if (status == "BOOKED") Pill(t("已确认", "Confirmed"), Success) else Pill(t("已退款", "Refunded"), Sunset)
}

@Composable
fun TripsScreen(trips: List<TripRecord>, onOpen: (String) -> Unit, onPlan: () -> Unit, padding: PaddingValues) {
    val lang = LocalLang.current
    if (trips.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(padding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Icon(Icons.Filled.Luggage, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Text(t(S.noTrips), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onPlan) { Text(t(S.startPlanning)) }
        }
        return
    }
    LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(trips, key = { it.bookingId }) { trip ->
            SectionCard(Modifier.clickable { onOpen(trip.bookingId) }.testTag("trip-${trip.bookingId}")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(trip.bookingId, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    StatusPill(trip.status)
                }
                Text(trip.name.get(lang), style = MaterialTheme.typography.titleMedium)
                Text(
                    formatDateRange(LocalDate.parse(trip.startDate), LocalDate.parse(trip.endDate), lang) +
                        " · ${trip.travelers} " + t(S.travelersUnit) + " · " + Money.format(trip.chargedUsd - trip.refundedUsd, trip.currency()),
                    style = MaterialTheme.typography.bodyMedium,
                )
                val done = trip.checklist.count { it.done }
                Text(t(S.checklist) + " $done/${trip.checklist.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun TripDetailScreen(trip: TripRecord, onToggle: (String) -> Unit, padding: PaddingValues) {
    val lang = LocalLang.current
    LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard(container = MaterialTheme.colorScheme.primaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(trip.name.get(lang), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    StatusPill(trip.status)
                }
                Text(
                    formatDateRange(LocalDate.parse(trip.startDate), LocalDate.parse(trip.endDate), lang) +
                        " · ${trip.travelers} " + t(S.travelersUnit) + " · " + trip.leadTraveler,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    t(S.charged) + " " + Money.format(trip.chargedUsd, trip.currency()) +
                        (if (trip.refundedUsd > 0) " · " + t(S.refundedAmount) + " " + Money.format(trip.refundedUsd, trip.currency()) else "") +
                        " · " + trip.paymentRef.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (trip.notes.isNotEmpty()) {
            item {
                SectionCard(container = MaterialTheme.colorScheme.secondaryContainer) {
                    trip.notes.forEach { Text(it.get(lang), style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
        item {
            SectionCard(title = t(S.checklist)) {
                trip.checklist.forEach { c ->
                    Row(Modifier.clickable { onToggle(c.id) }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(c.done, { onToggle(c.id) })
                        Text(c.text.get(lang), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            SectionCard(title = t(S.bookings)) {
                trip.legs.forEach { leg ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(componentIcon(ComponentType.valueOf(leg.type), leg.supplier), MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(leg.title.get(lang), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                leg.supplier + (leg.reference?.let { " · " + t(S.bookingRef) + " " + it } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        LegStatusPill(LegStatus.valueOf(leg.status))
                    }
                }
            }
        }
        item { Text(t(S.dayPlan), style = MaterialTheme.typography.titleLarge) }
        items(trip.days, key = { it.dayNo }) { day ->
            SectionCard {
                Text(
                    t(S.day).format(day.dayNo) + " · " + formatDate(LocalDate.parse(day.date), lang) + " · " + day.base.get(lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                HorizontalDivider()
                day.items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.time, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(44.dp))
                        IconBadge(itemIcon(ItemType.valueOf(item.type)), MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(10.dp))
                        Text(item.title.get(lang), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
