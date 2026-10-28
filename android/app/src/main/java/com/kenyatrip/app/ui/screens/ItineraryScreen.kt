package com.kenyatrip.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kenyatrip.app.core.knowledge.KnowledgeBase
import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.Itinerary
import com.kenyatrip.app.core.model.ItineraryDay
import com.kenyatrip.app.core.model.Severity
import com.kenyatrip.app.core.model.Text as T
import com.kenyatrip.app.core.pricing.Currency
import com.kenyatrip.app.core.pricing.Money
import com.kenyatrip.app.ui.LocalLang
import com.kenyatrip.app.ui.S
import com.kenyatrip.app.ui.components.IconBadge
import com.kenyatrip.app.ui.components.SectionCard
import com.kenyatrip.app.ui.components.componentIcon
import com.kenyatrip.app.ui.components.itemIcon
import com.kenyatrip.app.ui.formatDate
import com.kenyatrip.app.ui.formatDateRange
import com.kenyatrip.app.ui.t
import com.kenyatrip.app.ui.theme.Danger
import com.kenyatrip.app.ui.theme.Info
import com.kenyatrip.app.ui.theme.Sunset

private val kenyaEdits = listOf(
    T("轻松一点", "More relaxed"), T("加热气球", "Add balloon"), T("便宜一点", "Cheaper"), T("多一天", "One more day"),
)
private val chinaEdits = listOf(
    T("轻松一点", "More relaxed"), T("加上桂林", "Add Guilin"), T("升级酒店", "Upgrade hotels"), T("少一天", "One less day"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItineraryScreen(
    itinerary: Itinerary,
    kb: KnowledgeBase,
    modifying: Boolean,
    feedback: T?,
    onModify: (String) -> Unit,
    onBook: () -> Unit,
    padding: PaddingValues,
) {
    val lang = LocalLang.current
    val currency = Currency.forDestination(itinerary.request.destination)
    var edit by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.testTag("itinerary-list"),
        ) {
            item {
                SectionCard(container = MaterialTheme.colorScheme.primaryContainer) {
                    Text(t(itinerary.name), style = MaterialTheme.typography.titleLarge)
                    Text(
                        formatDateRange(itinerary.startDate, itinerary.endDate, lang) + " · " +
                            "${itinerary.request.days}" + t("天", " days") + " · " +
                            "${itinerary.request.travelers} " + t(S.travelersUnit) + " · " + t(itinerary.tier.label),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        itinerary.stops.map { t(kb.place(it.placeId).name) + " ${it.nights}" + t(S.night) }.joinToString("  →  "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item {
                SectionCard(title = t(S.aiAdjust)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = edit,
                            onValueChange = { edit = it },
                            placeholder = { Text(t(S.aiAdjustHint)) },
                            modifier = Modifier.weight(1f).testTag("modify-input"),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                        )
                        if (modifying) {
                            CircularProgressIndicator(Modifier.padding(start = 12.dp).size(24.dp))
                        } else {
                            IconButton(onClick = { onModify(edit); edit = "" }, modifier = Modifier.testTag("modify-send")) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "send", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (if (itinerary.request.destination == Destination.KENYA) kenyaEdits else chinaEdits).forEach { e ->
                            val label = t(e)
                            SuggestionChip(onClick = { onModify(label) }, label = { Text(label) }, enabled = !modifying)
                        }
                    }
                    feedback?.let {
                        Text(t(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item {
                SectionCard(title = t(S.whyThisRoute)) {
                    itinerary.reasons.forEach { r ->
                        Row {
                            Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(t(r), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            if (itinerary.warnings.isNotEmpty()) {
                item {
                    SectionCard(title = t(S.notices)) {
                        itinerary.warnings.forEach { w ->
                            val (icon, color) = when (w.severity) {
                                Severity.BLOCKER -> Icons.Filled.ErrorOutline to Danger
                                Severity.WARNING -> Icons.Filled.WarningAmber to Sunset
                                Severity.INFO -> Icons.Filled.Info to Info
                            }
                            Row {
                                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(t(w.text), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            item { Text(t(S.dayPlan), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp)) }
            items(itinerary.days, key = { it.dayNo }) { day -> DayCard(day) }
            item {
                SectionCard(title = t(S.priceBreakdown)) {
                    itinerary.components.forEach { c ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(componentIcon(c.type), MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text(t(c.title), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(Money.format(c.priceUsd, currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider()
                    Row {
                        Text(t(S.total), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(Money.format(itinerary.totalPriceUsd, currency), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            shadowElevation = 12.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(Money.format(itinerary.totalPriceUsd, currency), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text(
                        t(S.perPerson) + " " + Money.format(itinerary.pricePerPersonUsd, currency) +
                            if (currency != Currency.USD) " · ≈ " + Money.format(itinerary.totalPriceUsd, Currency.USD) else "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    onClick = onBook,
                    enabled = itinerary.feasible && !modifying,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(50.dp).testTag("book-button"),
                ) { Text(t(S.bookNow)) }
            }
        }
    }
}

@Composable
private fun DayCard(day: ItineraryDay) {
    val lang = LocalLang.current
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(t(S.day).format(day.dayNo), color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(10.dp))
            Text(formatDate(day.date, lang) + " · " + t(day.base), style = MaterialTheme.typography.titleMedium)
        }
        day.items.forEach { item ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    item.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(44.dp).padding(top = 8.dp),
                )
                IconBadge(itemIcon(item.type), MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).padding(top = 2.dp)) {
                    Text(t(item.title), style = MaterialTheme.typography.bodyLarge)
                    item.detail?.let {
                        val d = t(it)
                        if (d.isNotBlank()) Text(d, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
