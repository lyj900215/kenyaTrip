package com.kenyatrip.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kenyatrip.app.core.knowledge.KnowledgeBase
import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.Tier
import com.kenyatrip.app.core.model.TravelStyle
import com.kenyatrip.app.core.model.TripRequest
import com.kenyatrip.app.core.planner.ParseSource
import com.kenyatrip.app.core.pricing.Currency
import com.kenyatrip.app.core.pricing.Money
import com.kenyatrip.app.ui.ReviewState
import com.kenyatrip.app.ui.S
import com.kenyatrip.app.ui.components.SectionCard
import com.kenyatrip.app.ui.components.Stepper
import com.kenyatrip.app.ui.t
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    review: ReviewState,
    kb: KnowledgeBase,
    planning: Boolean,
    onEdit: ((TripRequest) -> TripRequest) -> Unit,
    onGenerate: () -> Unit,
    padding: PaddingValues,
) {
    val req = review.request
    val currency = Currency.forDestination(req.destination)
    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(
                    color = if (review.source == ParseSource.AI) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (review.source == ParseSource.AI) Icons.Filled.AutoAwesome else Icons.Filled.Memory, null)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            t(if (review.source == ParseSource.AI) S.parsedByAi else S.parsedLocal) +
                                " · " + t(S.recognized) + " ${review.recognized.size} " + t("项", "fields"),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            item {
                SectionCard(title = t(S.destination)) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        Destination.entries.forEachIndexed { i, d ->
                            SegmentedButton(
                                selected = req.destination == d,
                                onClick = {
                                    onEdit {
                                        it.copy(destination = d, mustPlaces = emptySet(), mustActivities = emptySet(), avoidPlaces = emptySet(), originCity = null)
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(i, Destination.entries.size),
                            ) { Text(t(if (d == Destination.KENYA) S.toKenya else S.toChina)) }
                        }
                    }
                }
            }
            item {
                SectionCard(title = t(S.month)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..12).forEach { m ->
                            FilterChip(
                                selected = req.month == m,
                                onClick = { onEdit { it.copy(month = m, startDate = null) } },
                                label = { Text(t("${m}月", com.kenyatrip.app.core.planner.RoutePlanner.monthName(m).take(3))) },
                            )
                        }
                    }
                }
            }
            item {
                SectionCard {
                    Stepper(t(S.days), req.days, { v -> onEdit { it.copy(days = v) } }, 3, 16)
                    Stepper(t(S.adults), req.adults, { v -> onEdit { it.copy(adults = v) } }, 1, 12)
                    Stepper(
                        t(S.children), req.childAges.size,
                        { v ->
                            onEdit {
                                val ages = if (v > it.childAges.size) it.childAges + 8 else it.childAges.dropLast(1)
                                it.copy(childAges = ages, styles = if (ages.isNotEmpty()) it.styles + TravelStyle.FAMILY else it.styles)
                            }
                        },
                        0, 6,
                    )
                    req.childAges.forEachIndexed { index, age ->
                        Stepper(
                            "  " + t("孩子", "Child") + " ${index + 1}", age,
                            { v -> onEdit { it.copy(childAges = it.childAges.toMutableList().also { l -> l[index] = v }) } },
                            0, 17, " " + t(S.childAge),
                        )
                    }
                }
            }
            item {
                SectionCard(title = t(S.budget)) {
                    val shown = req.budgetPerPersonUsd?.let { (it * currency.perUsd).roundToLong().toString() } ?: ""
                    OutlinedTextField(
                        value = shown,
                        onValueChange = { v ->
                            val amount = v.filter { it.isDigit() }.toDoubleOrNull()
                            onEdit { it.copy(budgetPerPersonUsd = amount?.let { a -> Money.toUsd(a, currency) }) }
                        },
                        prefix = { Text(currency.symbol) },
                        placeholder = { Text(t(S.budgetHint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Text(t(S.tier), style = MaterialTheme.typography.labelLarge)
                    val tiers = listOf<Tier?>(null) + Tier.entries
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        tiers.forEachIndexed { i, tier ->
                            SegmentedButton(
                                selected = req.tier == tier,
                                onClick = { onEdit { it.copy(tier = tier) } },
                                shape = SegmentedButtonDefaults.itemShape(i, tiers.size),
                            ) { Text(tier?.let { t(it.label) } ?: t(S.auto)) }
                        }
                    }
                }
            }
            item {
                SectionCard(title = t(S.styles)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TravelStyle.entries
                            .filter { req.destination == Destination.CHINA || it != TravelStyle.BUSINESS }
                            .forEach { style ->
                                FilterChip(
                                    selected = style in req.styles,
                                    onClick = { onEdit { it.copy(styles = if (style in it.styles) it.styles - style else it.styles + style) } },
                                    label = { Text(t(style.label)) },
                                )
                            }
                    }
                }
            }
            item {
                SectionCard(title = t(S.mustPlaces)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        kb.placesIn(req.destination).forEach { p ->
                            FilterChip(
                                selected = p.id in req.mustPlaces,
                                onClick = {
                                    onEdit {
                                        if (p.id in it.mustPlaces) it.copy(mustPlaces = it.mustPlaces - p.id)
                                        else it.copy(mustPlaces = it.mustPlaces + p.id, avoidPlaces = it.avoidPlaces - p.id)
                                    }
                                },
                                label = { Text(t(p.name)) },
                            )
                        }
                    }
                    Text(t(S.mustActivities), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        kb.activities
                            .filter { kb.place(it.placeId).destination == req.destination && it.priceUsd >= 40 }
                            .forEach { a ->
                                FilterChip(
                                    selected = a.id in req.mustActivities,
                                    onClick = {
                                        onEdit {
                                            if (a.id in it.mustActivities) it.copy(mustActivities = it.mustActivities - a.id)
                                            else it.copy(mustActivities = it.mustActivities + a.id, mustPlaces = it.mustPlaces + a.placeId)
                                        }
                                    },
                                    label = { Text(t(a.name)) },
                                )
                            }
                    }
                }
            }
        }
        Button(
            onClick = onGenerate,
            enabled = !planning,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .height(54.dp)
                .testTag("generate-button"),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (planning) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.size(10.dp))
                Text(t(S.planning), maxLines = 1)
            } else {
                Icon(Icons.Filled.AutoAwesome, null)
                Spacer(Modifier.size(8.dp))
                Text(t(S.generate))
            }
        }
    }
}