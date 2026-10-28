package com.kenyatrip.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kenyatrip.app.core.knowledge.KnowledgeBase
import com.kenyatrip.app.core.model.Itinerary
import com.kenyatrip.app.core.pricing.Currency
import com.kenyatrip.app.core.pricing.Money
import com.kenyatrip.app.ui.LocalLang
import com.kenyatrip.app.ui.S
import com.kenyatrip.app.ui.components.Pill
import com.kenyatrip.app.ui.components.SectionCard
import com.kenyatrip.app.ui.formatDateRange
import com.kenyatrip.app.ui.t
import com.kenyatrip.app.ui.theme.Sunset

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecommendationsScreen(
    items: List<Itinerary>,
    kb: KnowledgeBase,
    onOpen: (Itinerary) -> Unit,
    padding: PaddingValues,
) {
    if (items.isEmpty()) {
        Column(Modifier.padding(padding).padding(24.dp)) { Text(t(S.noRoutes)) }
        return
    }
    val lang = LocalLang.current
    LazyColumn(
        Modifier.padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        itemsIndexed(items) { index, itn ->
            val currency = Currency.forDestination(itn.request.destination)
            SectionCard(Modifier.clickable { onOpen(itn) }.testTag("route-card-$index")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(
                        if (index == 0) t(S.bestMatch) else t(S.option) + " ${index + 1}",
                        if (index == 0) Sunset else MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(formatDateRange(itn.startDate, itn.endDate, lang), style = MaterialTheme.typography.bodySmall)
                }
                Text(t(itn.name), style = MaterialTheme.typography.titleLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itn.stops.forEach { s ->
                        AssistChip(onClick = { onOpen(itn) }, label = { Text(t(kb.place(s.placeId).name) + " ${s.nights}" + t(S.night)) })
                    }
                }
                itn.reasons.take(3).forEach { r ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(t(r), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            Money.format(itn.pricePerPersonUsd, currency),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            t(S.perPerson) + " · " + t(S.total) + " " + Money.format(itn.totalPriceUsd, currency) +
                                " · " + t(itn.tier.label),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(onClick = { onOpen(itn) }) { Text(t(S.viewItinerary)) }
                }
                if (itn.warnings.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("${itn.warnings.size} " + t(S.tips), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
