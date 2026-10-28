package com.kenyatrip.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kenyatrip.app.core.booking.PaymentMethod
import com.kenyatrip.app.core.model.Itinerary
import com.kenyatrip.app.core.pricing.Currency
import com.kenyatrip.app.core.pricing.Money
import com.kenyatrip.app.ui.S
import com.kenyatrip.app.ui.components.SectionCard
import com.kenyatrip.app.ui.t

@Composable
fun CheckoutScreen(
    itinerary: Itinerary,
    onPay: (PaymentMethod, String) -> Unit,
    padding: PaddingValues,
) {
    val currency = Currency.forDestination(itinerary.request.destination)
    val methods = PaymentMethod.forDestination(itinerary.request.destination)
    var name by rememberSaveable { mutableStateOf("") }
    var passport by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var method by remember { mutableStateOf(methods.first()) }
    var agreed by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(title = t(itinerary.name)) {
            Text(
                "${itinerary.request.travelers} " + t(S.travelersUnit) + " · " + t(itinerary.tier.label),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                name, { name = it }, label = { Text(t(S.leadTraveler)) },
                modifier = Modifier.fillMaxWidth().testTag("lead-name"), singleLine = true,
            )
            OutlinedTextField(passport, { passport = it.uppercase() }, label = { Text(t(S.passport)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(
                phone, { phone = it }, label = { Text(t(S.phone)) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
        }
        SectionCard(title = t(S.paymentMethod)) {
            methods.forEach { m ->
                Row(
                    Modifier.fillMaxWidth().clickable { method = m },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = method == m, onClick = { method = m })
                    Text(t(m.label), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        SectionCard(title = t(S.refundPolicies)) {
            itinerary.components.forEach { c ->
                Column {
                    Text(t(c.title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(t(c.refundPolicy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.clickable { agreed = !agreed }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(agreed, { agreed = it }, modifier = Modifier.testTag("agree"))
                Text(t(S.agree), style = MaterialTheme.typography.bodyMedium)
            }
        }
        SectionCard {
            Row {
                Text(t(S.total), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(Money.format(itinerary.totalPriceUsd, currency), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider()
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(t(S.oneCharge), style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(
            onClick = { onPay(method, name.trim()) },
            enabled = name.isNotBlank() && agreed,
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("pay-button"),
            shape = RoundedCornerShape(16.dp),
        ) { Text(t(S.payAndBook) + " · " + Money.format(itinerary.totalPriceUsd, currency)) }
    }
}
